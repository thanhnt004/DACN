package com.example.backend.controller.payment;

import com.example.backend.dto.response.checkout.PaymentMethodResponse;
import com.example.backend.service.payment.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
                                                             @RequestParam String paymentMethodId) {
        return ResponseEntity.ok(Map.of("url", paymentService.getPaymentUrl(orderId,paymentMethodId )));
    }

    @GetMapping("/payment-method")
    public ResponseEntity<List<PaymentMethodResponse>> getPaymentMethod(@RequestParam(value = "orderId") UUID orderId) {
        return ResponseEntity.ok(paymentService.getAvailablePaymentMethodsForOrder(orderId));
    }

    @PostMapping("/repay")
    @Operation(summary = "Get payment URL for repaying an order")
    public ResponseEntity<Map<String, String>> repayOrder(@RequestParam(value = "orderId") UUID orderId,@RequestParam String paymentMethodId) {
        return ResponseEntity.ok(Map.of("url", paymentService.getPaymentUrl(orderId, paymentMethodId)));
    }

}
