package com.example.backend.service.payment;

import com.example.backend.dto.response.checkout.PaymentMethodResponse;
import com.example.backend.excepton.NotFoundException;
import com.example.backend.model.order.Order;
import com.example.backend.model.payment.Payment;
import com.example.backend.repository.order.OrderRepository;
import com.example.backend.repository.payment.PaymentRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PaymentService {
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final VNPayService vnPayService;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;
    public List<PaymentMethodResponse> getAvailablePaymentMethodsForOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
        return getAvailablePaymentMethods(order.getTotalAmount());
    }
    public List<PaymentMethodResponse> getAvailablePaymentMethods(long totalAmount) {
        List<PaymentMethodResponse> methods = new ArrayList<>();

        // VNPay - Always available
        methods.add(PaymentMethodResponse.builder()
                .id("VNPAY")
                .name("VNPay - Cổng thanh toán điện tử")
                .type("EWALLET")
                .description("Thanh toán qua VNPay (ATM, VISA, MasterCard, JCB, QR Code)")
                .iconUrl("/images/payment/vnpay.png")
                .isAvailable(true)
                .feeAmount(0L)
                .isRecommended(true)
                .build());

        // COD - Chỉ cho đơn hàng < 5 triệu
        boolean codAvailable = totalAmount < 5_000_000L;
        methods.add(PaymentMethodResponse.builder()
                .id("COD")
                .name("Thanh toán khi nhận hàng (COD)")
                .type("COD")
                .description("Trả tiền mặt khi nhận hàng")
                .iconUrl("/images/payment/cod.png")
                .isAvailable(codAvailable)
                .unavailableReason(codAvailable ? null : "Đơn hàng quá lớn cho COD (> 5 triệu)")
                .feeAmount(0L)
                .isRecommended(false)
                .build());

        return methods;
    }

    public Payment createPayment(Order order, String paymentMethodId) {
        PaymentMethodResponse paymentMethod = getAvailablePaymentMethods(order.getTotalAmount()).stream()
                .filter(m -> m.getId().equals(paymentMethodId) && m.getIsAvailable())
                .toList()
                .getFirst();
        Payment payment = Payment.builder()
                .order(order)
                .provider(paymentMethod.getName())
                .status(Payment.PaymentStatus.PENDING)
                .amount(order.getTotalAmount())
                .expireAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                .build();

        payment = paymentRepository.save(payment);
        order.addPayment(payment);

        log.info("Payment created: paymentId={}, orderId={}, provider={}",
                payment.getId(), order.getId(), paymentMethod);

        return payment;
    }

    public String generatePaymentUrl(Order order, Payment payment, String id) {
        if ("VNPAY".equals(id)) {
            return vnPayService.createPaymentUrl(order, payment);
        } else if ("COD".equals(id)) {
            // COD không cần payment URL, redirect về trang order detail
            return frontendBaseUrl + "/orders/" + order.getId();
        }

        throw new IllegalArgumentException("Unsupported payment method: " + id);
    }

    public void markPaymentCaptured(UUID uuid) {
        Payment payment = paymentRepository.findById(uuid)
                .orElseThrow(() -> new NotFoundException("Payment not found: " + uuid));
        payment.setStatus(Payment.PaymentStatus.CAPTURED);
        paymentRepository.save(payment);

        log.info("Payment marked as captured: paymentId={}", uuid);
    }

    public void markPaymentFailed(UUID uuid) {
        Payment payment = paymentRepository.findById(uuid)
                .orElseThrow(() -> new NotFoundException("Payment not found: " + uuid));
        payment.setStatus(Payment.PaymentStatus.FAILED);
        paymentRepository.save(payment);

        log.info("Payment marked as failed: paymentId={}", uuid);
    }

    public String getPaymentUrl(UUID orderId, String paymentMethodId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
        Payment payment = createPayment(order, paymentMethodId);
        paymentRepository.setAllOtherPaymentsToFailed(order.getId(), payment.getId());
        if ("VNPAY".equals(paymentMethodId)) {
            return vnPayService.createPaymentUrl(order, payment);
        } else if ("COD".equals(paymentMethodId)) {
            // COD không cần payment URL, redirect về trang order detail
            return frontendBaseUrl + "/orders/" + order.getId();
        }

        throw new IllegalArgumentException("Unsupported payment method: " + paymentMethodId);
    }
}
