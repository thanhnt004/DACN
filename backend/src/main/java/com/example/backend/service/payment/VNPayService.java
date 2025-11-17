package com.example.backend.service.payment;

import com.example.backend.config.VNPayConfig;
import com.example.backend.excepton.NotFoundException;
import com.example.backend.model.order.Order;
import com.example.backend.model.payment.Payment;
import com.example.backend.service.order.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.example.backend.util.VNPayUtil.hmacSHA512;

@Service
@RequiredArgsConstructor@Slf4j
public class VNPayService {

    private final VNPayConfig vnPayConfig;
    private final OrderService orderService;
    private final PaymentService paymentService;
    @Value("${payment.vnPay.max_time}")
    private int maxPaymentTime;

    public String createPaymentUrl(Order order, Payment payment) {
        try {
            Map<String, String> vnpParams = vnPayConfig.getConfig();
            //
            vnpParams.put("vnp_Amount", String.valueOf(order.getTotalAmount() * 100)); // VNPay yêu cầu nhân 100
            vnpParams.put("vnp_TxnRef", payment.getId().toString());
            vnpParams.put("vnp_OrderInfo", "Thanh toan don hang " + order.getOrderNumber());
            vnpParams.put("vnp_IpAddr", "127.0.0.1");

            Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            String vnpCreateDate = formatter.format(cld.getTime());
            vnpParams.put("vnp_CreateDate", vnpCreateDate);

            cld.add(Calendar.MINUTE, maxPaymentTime);
            String vnpExpireDate = formatter.format(cld.getTime());
            vnpParams.put("vnp_ExpireDate", vnpExpireDate);

            // Build query string
            List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
            Collections.sort(fieldNames);
            StringBuilder query = new StringBuilder();
            for (String field : fieldNames) {
                if (query.length() > 0) {
                    query.append('&');
                }
                query.append(URLEncoder.encode(field, StandardCharsets.US_ASCII));
                query.append('=');
                query.append(URLEncoder.encode(vnpParams.get(field), StandardCharsets.US_ASCII));
            }

            String queryString = query.toString();
            String vnpSecureHash = hmacSHA512(vnPayConfig.getSecretKey(), queryString);
            queryString += "&vnp_SecureHash=" + vnpSecureHash;

            String paymentUrl = vnPayConfig.getVnp_PayUrl() + "?" + queryString;

            log.info("VNPay payment URL created for order: {}", order.getOrderNumber());

            return paymentUrl;

        } catch (Exception e) {
            log.error("Failed to create VNPay payment URL", e);
            throw new RuntimeException("Failed to create VNPay payment URL", e);
        }
    }

    public boolean verifyCallback(Map<String, String> params) {
        String vnpSecureHash = params.get("vnp_SecureHash");
        params.remove("vnp_SecureHash");
        params.remove("vnp_SecureHashType");

        // Build query string
        StringBuilder query = new StringBuilder();
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        for (String fieldName : fieldNames) {
            String fieldValue = params.get(fieldName);
            if (fieldValue != null && fieldValue.length() > 0) {
                if (query.length() > 0) {
                    query.append('&');
                }
                query.append(fieldName).append('=').append(fieldValue);
            }
        }

        String queryString = query.toString();
        String computedHash = hmacSHA512(vnPayConfig.getSecretKey(), queryString);

        return computedHash.equals(vnpSecureHash);
    }

    public ResponseEntity<String> handleIpn(Map<String, String> params) {
        String vnpSecureHash = params.remove("vnp_SecureHash");

        // Sort params theo đúng chuẩn VNPay
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        for (String fieldName : fieldNames) {
            if (hashData.length() > 0) hashData.append("&");
            hashData.append(fieldName).append("=").append(params.get(fieldName));
        }

        // Tạo hash để so sánh
        String signed = hmacSHA512(vnPayConfig.getSecretKey(), hashData.toString());
        if (!signed.equals(vnpSecureHash)) {
            return ResponseEntity.ok("97"); // Sai chữ ký
        }

        // Lấy dữ liệu cần thiết
        String orderNumber = params.get("vnp_OrderInfo");
        String paymentId = params.get("vnp_TxnRef");
        String transactionStatus = params.get("vnp_TransactionStatus");
        String amount = params.get("vnp_Amount");

        Order order;
        //Tìm order trong DB
        try {
            order = orderService.getOrderByNumber(orderNumber);

        }catch (NotFoundException e)
        {
            return ResponseEntity.ok("01");
        }

        //Check idempotent
        if (order.isPaid()) return ResponseEntity.ok("02");

        // Xử lý theo status VNPay
        if ("00".equals(transactionStatus)) {
            // Thành công
            pushStatus(order.getId().toString(), "SUCCESS");
            orderService.markAsPaid(order.getId());
            paymentService.markPaymentCaptured(UUID.fromString(paymentId));
        } else {
            pushStatus(order.getId().toString(), "FAILED");
            paymentService.markPaymentFailed(UUID.fromString(paymentId));
            return ResponseEntity.ok("03");
        }

        return ResponseEntity.ok("00");
    }
    // Lưu emitter theo orderId
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    // Thêm emitter mới
    public SseEmitter createEmitter(String orderId) {
        SseEmitter emitter = new SseEmitter(0L); // 0 = không timeout
        emitters.put(orderId, emitter);

        emitter.onCompletion(() -> emitters.remove(orderId));
        emitter.onTimeout(() -> emitters.remove(orderId));
        emitter.onError((e) -> emitters.remove(orderId));

        return emitter;
    }

    // Push trạng thái mới
    public void pushStatus(String orderId, String status) {
        SseEmitter emitter = emitters.get(orderId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("payment-status")
                        .data(status));
                // Nếu đã có kết quả cuối cùng, có thể close emitter
                if ("SUCCESS".equals(status) || "FAILED".equals(status)) {
                    emitter.complete();
                    emitters.remove(orderId);
                }
            } catch (Exception e) {
                emitter.completeWithError(e);
                emitters.remove(orderId);
            }
        }
    }
}

