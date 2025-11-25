package com.example.backend.exception.auth;

import com.example.backend.exception.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when user provides invalid email/password credentials.
 */
public class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED.value(), "AUTH_INVALID_CREDENTIALS", "Email hoặc mật khẩu không đúng");
    }

    public InvalidCredentialsException(String message) {
        super(HttpStatus.UNAUTHORIZED.value(), "AUTH_INVALID_CREDENTIALS", message);
    }
}

