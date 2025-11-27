package com.example.backend.exception.shipping;

import com.example.backend.exception.DomainException;
import org.springframework.http.HttpStatus;

public class ShippingNotFoundException extends DomainException {
    public ShippingNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND.value(), "SHIPPING_INFO_NOTFOUND", message);
    }

    public ShippingNotFoundException(String code, String message) {
        super(HttpStatus.NOT_FOUND.value(), code, message);
    }
}
