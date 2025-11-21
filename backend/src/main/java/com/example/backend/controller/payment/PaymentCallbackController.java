package com.example.backend.controller.payment;

import com.example.backend.service.payment.VNPayService;
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
    @GetMapping("/status/stream/{orderId}")
    public SseEmitter streamPaymentStatus(@PathVariable String orderId) throws IOException {
        SseEmitter emitter = vnPayService.createEmitter(orderId);
        // Gửi trạng thái hiện tại ngay
        try {
            emitter.send(SseEmitter.event().name("payment-status").data("PENDING"));
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
        return emitter;
    }
    @PostMapping("/vnpay/ipn")
    public ResponseEntity<Map<String, String>> vnpayIpn(@RequestParam Map<String, String> params, HttpServletRequest request) {
        log.info("Received VNPay callback: txnRef={}", params.get("vnp_TxnRef"));
        return vnPayService.handleIpn(params,request);
    }

}
