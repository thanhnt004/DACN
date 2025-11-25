package com.example.backend.exception.cart;

import com.example.backend.exception.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when cart is not found.
 */
public class CartNotFoundException extends DomainException {

    public CartNotFoundException() {
        super(HttpStatus.NOT_FOUND.value(), "CART_NOT_FOUND", "Không tìm thấy giỏ hàng");
    }

    public CartNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND.value(), "CART_NOT_FOUND", message);
    }
}

