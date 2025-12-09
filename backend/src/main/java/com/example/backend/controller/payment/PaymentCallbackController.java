package com.example.backend.controller.payment;

import com.example.backend.service.payment.VNPayService;
import com.example.backend.repository.payment.PaymentRepository;
import com.example.backend.dto.response.payment.PaymentStatusEvent;
import java.time.Instant;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentCallbackController {
    private final VNPayService vnPayService;
    private final PaymentRepository paymentRepository;

    @GetMapping("/status/stream/{orderId}")
    public SseEmitter streamPaymentStatus(@PathVariable String orderId) throws IOException {
        SseEmitter emitter = vnPayService.createEmitter(orderId);

        String status = "PENDING";
        String message = "Đang chờ kết quả thanh toán...";

        try {
            var paymentOpt = paymentRepository.findFirstByOrder_IdOrderByCreatedAtDesc(UUID.fromString(orderId));
            if (paymentOpt.isPresent()) {
                var payment = paymentOpt.get();
                switch (payment.getStatus()) {
                    case CAPTURED:
                        status = "PAID";
                        message = "Thanh toán thành công";
                        break;
                    case FAILED:
                        status = "FAILED";
                        message = payment.getErrorMessage() != null ? payment.getErrorMessage() : "Thanh toán thất bại";
                        break;
                    case REFUNDED:
                        status = "CANCELLED";
                        message = "Đã hoàn tiền";
                        break;
                    default:
                        status = "PENDING";
                }
            }
        } catch (Exception e) {
            log.error("Error fetching payment status for order {}", orderId, e);
        }

        // Gửi trạng thái hiện tại ngay
        try {
            PaymentStatusEvent pendingEvent = PaymentStatusEvent.builder()
                    .orderId(orderId)
                    .status(status)
                    .message(message)
                    .timestamp(Instant.now())
                    .build();
            emitter.send(SseEmitter.event().name("payment-status").data(pendingEvent));

            if ("PAID".equals(status) || "FAILED".equals(status) || "CANCELLED".equals(status)) {
                emitter.complete();
            }
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
        return emitter;
    }
    @RequestMapping(
            value = "/vnpay/ipn",
            method = {RequestMethod.GET, RequestMethod.POST}
    )
    public ResponseEntity<Map<String, String>> vnpayIpn(
            @RequestParam Map<String, String> params,
            HttpServletRequest request) {

        log.info("Received VNPay IPN: txnRef={}", params.get("vnp_TxnRef"));
        return vnPayService.handleIpn(params, request);
    }

}
