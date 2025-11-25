package com.example.backend.exception.cart;

import com.example.backend.exception.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when there is insufficient stock for cart item.
 */
public class InsufficientStockException extends DomainException {

    public InsufficientStockException(String message) {
        super(HttpStatus.CONFLICT.value(), "CART_INSUFFICIENT_STOCK", message);
    }

    public InsufficientStockException(int available) {
        super(HttpStatus.CONFLICT.value(), "CART_INSUFFICIENT_STOCK",
              "Không đủ hàng trong kho. Chỉ còn " + available + " sản phẩm");
    }
}

