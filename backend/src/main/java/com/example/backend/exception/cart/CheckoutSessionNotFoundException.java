package com.example.backend.exception.cart;

import com.example.backend.exception.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when checkout session is not found.
 */
public class CheckoutSessionNotFoundException extends DomainException {

    public CheckoutSessionNotFoundException() {
        super(HttpStatus.NOT_FOUND.value(), "CHECKOUT_SESSION_NOT_FOUND", "Phiên thanh toán không tìm thấy");
    }

    public CheckoutSessionNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND.value(), "CHECKOUT_SESSION_NOT_FOUND", message);
    }
}

