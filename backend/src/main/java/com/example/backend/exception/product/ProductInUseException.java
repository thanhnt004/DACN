package com.example.backend.exception.product;

import com.example.backend.exception.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when attempting to delete product/variant/attribute that is in use.
 */
public class ProductInUseException extends DomainException {

    public ProductInUseException(String message) {
        super(HttpStatus.BAD_REQUEST.value(), "PRODUCT_IN_USE", message);
    }

    public ProductInUseException() {
        super(HttpStatus.BAD_REQUEST.value(), "PRODUCT_IN_USE",
              "Không thể xóa vì đang được sử dụng");
    }
}

