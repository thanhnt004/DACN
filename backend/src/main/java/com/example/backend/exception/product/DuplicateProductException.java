package com.example.backend.exception.product;

import com.example.backend.exception.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when product slug/SKU already exists.
 */
public class DuplicateProductException extends DomainException {

    public DuplicateProductException(String field) {
        super(HttpStatus.CONFLICT.value(), "PRODUCT_DUPLICATE_" + field.toUpperCase(),
              field + " đã tồn tại");
    }

    public DuplicateProductException(String field, String message) {
        super(HttpStatus.CONFLICT.value(), "PRODUCT_DUPLICATE_" + field.toUpperCase(), message);
    }
}

