package com.example.backend.exception.payment;

import com.example.backend.exception.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when payment processing fails.
 */
public class PaymentFailedException extends DomainException {

    public PaymentFailedException() {
        super(HttpStatus.BAD_REQUEST.value(), "PAYMENT_FAILED", "Thanh toán thất bại");
    }

    public PaymentFailedException(String message) {
        super(HttpStatus.BAD_REQUEST.value(), "PAYMENT_FAILED", message);
    }
}

