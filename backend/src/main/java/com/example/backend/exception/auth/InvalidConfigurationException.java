package com.example.backend.exception.auth;

import com.example.backend.exception.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when system configuration is invalid.
 */
public class InvalidConfigurationException extends DomainException {

    public InvalidConfigurationException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR.value(), "INVALID_CONFIGURATION", message);
    }

    public InvalidConfigurationException(String code, String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR.value(), code, message);
    }
}

