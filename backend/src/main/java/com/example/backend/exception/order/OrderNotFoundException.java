package com.example.backend.exception.order;

import com.example.backend.exception.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when order is not found.
 */
public class OrderNotFoundException extends DomainException {

    public OrderNotFoundException() {
        super(HttpStatus.NOT_FOUND.value(), "ORDER_NOT_FOUND", "Không tìm thấy đơn hàng");
    }

    public OrderNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND.value(), "ORDER_NOT_FOUND", message);
    }
}

