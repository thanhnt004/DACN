package com.example.backend.exception.auth;

import com.example.backend.exception.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when email does not match the expected email.
 */
public class EmailMismatchException extends DomainException {

    public EmailMismatchException() {
        super(HttpStatus.CONFLICT.value(), "AUTH_EMAIL_MISMATCH", "Email không khớp");
    }

    public EmailMismatchException(String message) {
        super(HttpStatus.CONFLICT.value(), "AUTH_EMAIL_MISMATCH", message);
    }
}

