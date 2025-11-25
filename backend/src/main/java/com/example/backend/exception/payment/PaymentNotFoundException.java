package com.example.backend.exception.payment;

import com.example.backend.exception.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when payment record is not found.
 */
public class PaymentNotFoundException extends DomainException {

    public PaymentNotFoundException() {
        super(HttpStatus.NOT_FOUND.value(), "PAYMENT_NOT_FOUND", "Không tìm thấy thông tin thanh toán");
    }

    public PaymentNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND.value(), "PAYMENT_NOT_FOUND", message);
    }
}

