package com.example.backend.controller.payment;

import com.example.backend.service.PaymentService.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payment")
public class PaymentController {
    private final PaymentService paymentService;
    @GetMapping("/payment_url")
    @Operation(summary = "Get payment URL")
    public ResponseEntity<Map<String, String>> getPaymentUrl(@RequestParam(value = "orderId") UUID orderId,
                                                             HttpServletRequest request) {
        return ResponseEntity.ok(Map.of("url", paymentService.getPaymentUrl(orderId, request)));
    }

    @PostMapping("/check")
    @Operation(summary = "Check payment status")
    public ResponseEntity<Map<String, String>> checkPayment(@RequestParam(value = "gateway") String gateway,
                                                            @RequestParam Map<String, String> params) {
        boolean ok = paymentService.checkPayment(gateway, params);
        return ResponseEntity.ok(Map.of("status", ok ? "success" : "failed"));
    }
}
