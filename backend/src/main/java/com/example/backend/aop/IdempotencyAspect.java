package com.example.backend.aop;

import com.example.backend.exception.BadRequestException;
import com.example.backend.exception.ConflictException;
import com.example.backend.model.IdempotencyKey;
import com.example.backend.service.IdempotencyService;
import com.example.backend.util.CryptoUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.HandlerMapping;

import java.lang.reflect.Method;
import java.util.Map;

@Aspect
@Component
@Slf4j
public class IdempotencyAspect {

    @Autowired
    private IdempotencyService idempotencyStore; // Service thao tác với bảng idempotency_keys

    @Around("@annotation(com.example.backend.aop.Idempotent)")
    public Object handleIdempotency(ProceedingJoinPoint joinPoint) throws Throwable {
        //get key from header
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String key = request.getHeader("Idempotency-Key");
        log.info("Request Url: {}, Idempotency-Key: {}", request.getRequestURI(), key);
        // Lấy đối tượng Annotation từ method
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Idempotent idempotentAnnotation = method.getAnnotation(Idempotent.class);
        //get sessionId from path if exists
        String scopeParamName = idempotentAnnotation.scope(); // Lấy tên biến, VD: "orderId"
        
        // Try to get from method arguments first
        String scopeValue = null;
        Object[] args = joinPoint.getArgs();
        java.lang.annotation.Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        String[] parameterNames = signature.getParameterNames();
        
        for (int i = 0; i < parameterAnnotations.length; i++) {
            for (java.lang.annotation.Annotation annotation : parameterAnnotations[i]) {
                if (annotation instanceof org.springframework.web.bind.annotation.PathVariable) {
                    org.springframework.web.bind.annotation.PathVariable pathVar = (org.springframework.web.bind.annotation.PathVariable) annotation;
                    String pathVarName = pathVar.value().isEmpty() ? parameterNames[i] : pathVar.value();
                    if (pathVarName.equals(scopeParamName) && args[i] != null) {
                        scopeValue = args[i].toString();
                        break;
                    }
                }
            }
            if (scopeValue != null) break;
        }
        
        // Fallback to request attribute if not found in method args
        if (scopeValue == null) {
            @SuppressWarnings("unchecked")
            Map<String, String> pathVariables = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            scopeValue = (pathVariables != null) ? pathVariables.get(scopeParamName) : null;
        }

        // Nếu endpoint này cần scope mà không tìm thấy -> Lỗi
        if (scopeValue == null) {
            throw new BadRequestException("Missing required path variable: " + scopeParamName);
        }
        // Hash giá trị scope này (VD: hash của orderId hoặc paymentId)
        String scopeHash = CryptoUtils.hash(scopeValue);
        log.info("Processing Idempotency for Scope: {} = {}", scopeParamName, scopeValue);
        if (StringUtils.isEmpty(key)) {
            throw new BadRequestException("Idempotency-Key header is missing");
        }
        //check key in db
        IdempotencyKey record = idempotencyStore.getRecord(key);
        if (record != null&&!record.getHash().equals(scopeHash))
        {
            throw new BadRequestException("Idempotency-Key is invalid for this session");
        }
        // Case A: Đã xử lý xong -> Trả lại kết quả cũ
        if (record != null && record.getStatus() == IdempotencyKey.Status.SUCCESS) {
            return ResponseEntity.ok(record.getResponseBody());
        }

        // Case B: Đang xử lý -> Chặn lại
        if (record != null && record.getStatus() == IdempotencyKey.Status.PROCESSING) {
            long timeDiff = System.currentTimeMillis() - record.getCreatedAt().getNano();
            // Giả sử timeout là 5 giây, nếu quá 5s mà vẫn Processing nghĩa là server cũ đã chết
            if (timeDiff < 5000) {
                throw new ConflictException("Request is already being processed");
            }
        }
        long expireTime = idempotentAnnotation.expire();
        // 3. Tạo record mới trạng thái PROCESSING
        idempotencyStore.saveProcessing(key, scopeHash, expireTime);

        Object result;
        try {
            // 4. Chạy logic tạo Order thực sự (createOrder)
            result = joinPoint.proceed();

            // 5. Thành công -> Update trạng thái SUCCESS và lưu response
            idempotencyStore.saveSuccess(key, result);
        } catch (Exception e) {
            // 6. Lỗi -> Xóa key hoặc update FAILED để client retry được
            idempotencyStore.delete(key);
            throw e;
        }
        return result;
    }
}