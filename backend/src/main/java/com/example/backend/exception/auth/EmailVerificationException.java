package com.example.backend.exception.auth;

import com.example.backend.exception.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when email verification fails.
 */
public class EmailVerificationException extends DomainException {

    public EmailVerificationException() {
        super(HttpStatus.BAD_REQUEST.value(), "AUTH_EMAIL_VERIFICATION_FAILED", "Xác thực email thất bại");
    }

    public EmailVerificationException(String message) {
        super(HttpStatus.BAD_REQUEST.value(), "AUTH_EMAIL_VERIFICATION_FAILED", message);
    }
}

