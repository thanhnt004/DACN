package com.example.backend.exception.order;

import com.example.backend.exception.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when order validation fails.
 */
public class OrderValidationException extends DomainException {

    public OrderValidationException(String message) {
        super(HttpStatus.BAD_REQUEST.value(), "ORDER_VALIDATION_FAILED", message);
    }
}

