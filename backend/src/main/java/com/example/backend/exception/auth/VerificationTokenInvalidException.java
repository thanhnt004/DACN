package com.example.backend.exception.auth;

import com.example.backend.exception.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when verification token is invalid or malformed.
 */
public class VerificationTokenInvalidException extends DomainException {

    public VerificationTokenInvalidException() {
        super(HttpStatus.CONFLICT.value(), "AUTH_VERIFICATION_TOKEN_INVALID", "Token xác thực email không hợp lệ");
    }

    public VerificationTokenInvalidException(String message) {
        super(HttpStatus.CONFLICT.value(), "AUTH_VERIFICATION_TOKEN_INVALID", message);
    }
}

