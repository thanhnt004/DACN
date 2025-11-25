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
        //get sessionId from path if exists
        Map<String, String> pathVariables = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        String sessionId = (pathVariables != null) ? pathVariables.get("sessionId") : null;
        if (sessionId == null) {
            throw new BadRequestException("Session ID is missing in the path");
        }
        String hashId = CryptoUtils.hash(sessionId);
        if (StringUtils.isEmpty(key)) {
            throw new BadRequestException("Idempotency-Key header is missing");
        }


        //check key in db
        IdempotencyKey record = idempotencyStore.getRecord(key);
        if (record != null&&!record.getHash().equals(hashId))
        {
            throw new BadRequestException("Idempotency-Key is invalid for this session");
        }
        // Case A: Đã xử lý xong -> Trả lại kết quả cũ
        if (record != null && record.getStatus() == IdempotencyKey.Status.SUCCESS) {
            return record.getResponseBody();
        }

        // Case B: Đang xử lý -> Chặn lại
        if (record != null && record.getStatus() == IdempotencyKey.Status.PROCESSING) {
            throw new ConflictException("Request is already being processed");
        }
        // 1. Lấy Method Signature để truy cập vào Annotation
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 2. Lấy đối tượng Annotation gắn trên method đó
        Idempotent idempotentAnnotation = method.getAnnotation(Idempotent.class);

        // 3. Lấy giá trị expire() (Sẽ là 3600 hoặc số user truyền vào)
        long expireTime = idempotentAnnotation.expire();
        // 3. Tạo record mới trạng thái PROCESSING
        idempotencyStore.saveProcessing(key, hashId, expireTime);

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