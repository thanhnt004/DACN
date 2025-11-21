package com.example.backend.repository.payment;

import com.example.backend.model.payment.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    @Modifying
    @Query("UPDATE Payment p SET p.status = 'FAILED' WHERE p.order.id = :orderId AND p.id != :paymentId")
    void setAllOtherPaymentsToFailed(@Param("orderId") UUID orderId, @Param("paymentId") UUID paymentId);
}
