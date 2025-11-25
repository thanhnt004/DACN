package com.example.backend.exception.user;

import com.example.backend.exception.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when user identifier (email/phone) is invalid.
 */
public class InvalidIdentifierException extends DomainException {

    public InvalidIdentifierException() {
        super(HttpStatus.BAD_REQUEST.value(), "USER_INVALID_IDENTIFIER", "Định danh không hợp lệ");
    }

    public InvalidIdentifierException(String message) {
        super(HttpStatus.BAD_REQUEST.value(), "USER_INVALID_IDENTIFIER", message);
    }
}

