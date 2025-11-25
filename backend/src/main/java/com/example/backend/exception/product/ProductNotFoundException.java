package com.example.backend.exception.product;

import com.example.backend.exception.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when product is not found.
 */
public class ProductNotFoundException extends DomainException {

    public ProductNotFoundException() {
        super(HttpStatus.NOT_FOUND.value(), "PRODUCT_NOT_FOUND", "Không tìm thấy sản phẩm");
    }

    public ProductNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND.value(), "PRODUCT_NOT_FOUND", message);
    }
}

