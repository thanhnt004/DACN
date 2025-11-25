package com.example.backend.exception.shipping;

import com.example.backend.exception.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when shipping service (GHN) API call fails.
 */
public class ShippingServiceException extends DomainException {

    public ShippingServiceException(String message) {
        super(HttpStatus.BAD_REQUEST.value(), "SHIPPING_SERVICE_ERROR", message);
    }

    public ShippingServiceException(String code, String message) {
        super(HttpStatus.BAD_REQUEST.value(), code, message);
    }
}

