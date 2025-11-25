package com.example.backend.exception.product;

import com.example.backend.exception.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when product variant is not found.
 */
public class VariantNotFoundException extends DomainException {

    public VariantNotFoundException() {
        super(HttpStatus.NOT_FOUND.value(), "PRODUCT_VARIANT_NOT_FOUND", "Không tìm thấy phiên bản sản phẩm");
    }

    public VariantNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND.value(), "PRODUCT_VARIANT_NOT_FOUND", message);
    }
}

