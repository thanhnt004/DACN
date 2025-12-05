package com.example.backend.exception.product;

import com.example.backend.exception.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when product image is not found or cannot be resolved.
 */
public class ProductImageNotFoundException extends DomainException {

    public ProductImageNotFoundException() {
        super(HttpStatus.NOT_FOUND.value(), "PRODUCT_IMAGE_NOT_FOUND", "Ảnh sản phẩm không tìm thấy");
    }

    public ProductImageNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND.value(), "PRODUCT_IMAGE_NOT_FOUND", message);
    }
}

