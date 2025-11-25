package com.example.backend.exception.cart;

import com.example.backend.exception.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when cart item is not found.
 */
public class CartItemNotFoundException extends DomainException {

    public CartItemNotFoundException() {
        super(HttpStatus.NOT_FOUND.value(), "CART_ITEM_NOT_FOUND", "Không tìm thấy sản phẩm trong giỏ hàng");
    }

    public CartItemNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND.value(), "CART_ITEM_NOT_FOUND", message);
    }
}

