package com.example.backend.service.payment;

import com.example.backend.config.VNPayConfig;
import com.example.backend.dto.response.payment.PaymentStatusEvent;
import com.example.backend.exception.order.OrderNotFoundException;
import com.example.backend.exception.payment.PaymentFailedException;
import com.example.backend.exception.payment.PaymentNotFoundException;
import com.example.backend.model.order.Order;
import com.example.backend.model.payment.Payment;
import com.example.backend.repository.payment.PaymentRepository;
import com.example.backend.service.order.OrderService;
import com.example.backend.util.HeaderUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.example.backend.util.VNPayUtil.hmacSHA512;

@Service
@RequiredArgsConstructor
@Slf4j
public class VNPayService {

    private final VNPayConfig vnPayConfig;
    private final OrderService orderService;
    private final PaymentRepository paymentRepository;
    @Value("${payment.vnPay.max_time}")
    private int maxPaymentTime;

    public String createPaymentUrl(Order order, Payment payment) {
        try {
            Map<String, String> vnpParams = vnPayConfig.getConfig();
            //
            vnpParams.put("vnp_Amount", String.valueOf(order.getTotalAmount() * 100)); // VNPay yêu cầu nhân 100
            vnpParams.put("vnp_TxnRef", payment.getId().toString());
            vnpParams.put("vnp_OrderInfo", "Thanh toan don hang " + order.getOrderNumber());
            //place holder
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
            throw new PaymentFailedException("Tạo URL thanh toán VNPay thất bại");
        }
    }

    @Transactional
    public ResponseEntity<Map<String, String>> handleIpn(Map<String, String> params, HttpServletRequest request) {
        String vnpSecureHash = params.remove("vnp_SecureHash");
        String clientIp = HeaderUtil.getClientIp(request);
        log.info("VNPay IPN from IP={}, txnRef={}",
                clientIp, params.get("vnp_TxnRef"));

        // 2. (Optional) Whitelist VNPay IPs
        if (!isVNPayIP(clientIp)) {
            log.warn("Received IPN from unknown IP: {}", clientIp);
            // Có thể block hoặc chỉ log warning
        }
        // Sort params theo đúng chuẩn VNPay
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        for (String fieldName : fieldNames) {
            if (hashData.length() > 0)
                hashData.append("&");
            hashData.append(fieldName).append("=").append(params.get(fieldName));
        }

        // Tạo hash để so sánh
        String signed = hmacSHA512(vnPayConfig.getSecretKey(), hashData.toString());
        if (!signed.equals(vnpSecureHash)) {
            return rsp("97", "Invalid Signature"); // Sai chữ ký
        }

        // Lấy dữ liệu cần thiết
        String orderNumber = params.get("vnp_OrderInfo");
        String paymentId = params.get("vnp_TxnRef");
        String transactionStatus = params.get("vnp_TransactionStatus");
        String responseCode = params.get("vnp_ResponseCode");
        Order order;
        // Tìm order trong DB
        try {
            order = orderService.getOrderByNumber(orderNumber);

        } catch (OrderNotFoundException e) {
            return rsp("01", "Order not found");
        }

        // Check idempotent
        if (order.isPaid())
            return rsp("02", "Already confirmed");

        // Xử lý theo status VNPay
        if ("00".equals(transactionStatus)) {
            // Thành công
            notifyPaymentStatus(String.valueOf(order.getId()), PaymentStatusEvent.builder()
                    .orderId(String.valueOf(order.getId()))
                    .status("PAID")
                    .message("Thanh toán thành công")
                    .timestamp(Instant.now())
                    .build());
            orderService.markAsPaid(order.getId());
            Payment payment = paymentRepository.findById(UUID.fromString(paymentId))
                    .orElseThrow(() -> new PaymentNotFoundException("Không tìm thấy thông tin thanh toán: " + paymentId));
            payment.setStatus(Payment.PaymentStatus.CAPTURED);
            paymentRepository.save(payment);
        } else {
            notifyPaymentStatus(String.valueOf(order.getId()), PaymentStatusEvent.builder()
                    .orderId(String.valueOf(order.getId()))
                    .status("FAILED")
                    .errorCode(responseCode)
                    .message(("Loi thanh toán, mã lỗi: " + responseCode))
                    .timestamp(Instant.now())
                    .build());
            Payment payment = paymentRepository.findById(UUID.fromString(paymentId))
                    .orElseThrow(() -> new PaymentNotFoundException("Không tìm thấy thông tin thanh toán: " + paymentId));
            payment.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepository.save(payment);
        }

        return rsp("00", "Confirm Success");
    }

    private ResponseEntity<Map<String, String>> rsp(String code, String message) {
        Map<String, String> body = new HashMap<>();
        body.put("RspCode", code);
        body.put("Message", message);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    // Lưu emitter theo orderId
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(String orderId) {
        // Timeout 5 phút = 300,000ms
        SseEmitter emitter = new SseEmitter(300_000L);

        // Lưu emitter vào map
        emitters.put(orderId, emitter);

        // Cleanup khi complete/timeout/error
        emitter.onCompletion(() -> {
            log.info("SSE completed for order {}", orderId);
            emitters.remove(orderId);
        });

        emitter.onTimeout(() -> {
            log.warn("SSE timeout for order {}", orderId);
            emitters.remove(orderId);
            emitter.complete();
        });

        emitter.onError(e -> {
            log.error("SSE error for order {}", orderId, e);
            emitters.remove(orderId);
        });

        return emitter;
    }

    /**
     * Gửi cập nhật trạng thái payment cho tất cả clients đang lắng nghe orderId
     */
    public void notifyPaymentStatus(String orderId, PaymentStatusEvent event) {
        SseEmitter emitter = emitters.get(orderId);
        if (emitter == null) {
            log.debug("No active SSE connection for order {}", orderId);
            return;
        }

        synchronized (emitter) {
            try {
                emitter.send(SseEmitter.event()
                        .name("payment-status")
                        .id(UUID.randomUUID().toString()) // Event ID cho client tracking
                        .data(event));

                log.info("Sent SSE event to order {}: {}", orderId, event.getStatus());

                // Nếu là trạng thái cuối cùng (PAID/FAILED/CANCELLED), đóng connection
                if (event.isFinal()) {
                    emitter.complete();
                    emitters.remove(orderId);
                }

            } catch (IOException e) {
                log.error("Failed to send SSE event for order {}", orderId, e);
                emitter.completeWithError(e);
                emitters.remove(orderId);
            }
        }
    }

    private boolean isVNPayIP(String ip) {
        // Danh sách IP của VNPay (lấy từ tài liệu)
        Set<String> vnpayIPs = Set.of(
                "113.160.92.202",
                "113.52.45.78",
                "203.171.19.146"
        // ... thêm IPs khác
        );
        return vnpayIPs.contains(ip);
    }
}
