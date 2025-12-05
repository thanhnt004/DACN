package com.example.backend.exception.payment;

import com.example.backend.exception.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when payment method is not supported.
 */
public class UnsupportedPaymentMethodException extends DomainException {

    public UnsupportedPaymentMethodException(String paymentMethod) {
        super(HttpStatus.BAD_REQUEST.value(), "PAYMENT_METHOD_UNSUPPORTED",
              "Phương thức thanh toán không được hỗ trợ: " + paymentMethod);
    }

    public UnsupportedPaymentMethodException(String code, String message) {
        super(HttpStatus.BAD_REQUEST.value(), code, message);
    }
}

