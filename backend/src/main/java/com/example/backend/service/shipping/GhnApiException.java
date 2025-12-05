package com.example.backend.service.shipping;

import com.example.backend.exception.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when GHN API call fails or returns an error.
 * This exception is used for API communication errors, not for business logic errors.
 */
public class GhnApiException extends DomainException {

    public GhnApiException(String code, String message) {
        super(HttpStatus.BAD_GATEWAY.value(), code, message);
    }

    public GhnApiException(String message, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY.value(), "GHN_API_ERROR", message);
        if (cause != null) {
            initCause(cause);
        }
    }
}
