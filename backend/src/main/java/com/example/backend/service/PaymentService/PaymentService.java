package com.example.backend.service.PaymentService;

import com.example.backend.excepton.NotFoundException;
import com.example.backend.excepton.RequestException;
import com.example.backend.model.order.Order;
import com.example.backend.model.payment.Payment;
import com.example.backend.repository.order.OrderRepository;
import com.example.backend.service.order.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {
    private final OrderRepository orderRepository;
    private final VNPayService vnPayService;
    public String getPaymentUrl(UUID orderId, HttpServletRequest request){
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new NotFoundException("Order not found"));
        return vnPayService.createPaymentUrl(order, request);
    }

    public boolean checkPayment(String gateway, Map<String, String> params){
        PaymentGateway paymentGateway = PaymentGateway.fromString(gateway);
        if (paymentGateway == PaymentGateway.VNPAY) {
            String txnRef = params.get("vnp_TxnRef");
            String[] p = txnRef.split("-");
            Order order =orderRepository.findById(UUID.fromString(p[0])).orElseThrow(() -> new NotFoundException("Order not found"));
            boolean ok = vnPayService.checkPayment(order, params);
            if(ok){
                Payment payment = order.getActivePayment();
                payment.setStatus(Payment.PaymentStatus.CAPTURED);
                orderRepository.save(order);
            }
            else return false;
        }
        return true;
    }
}
