package com.example.backend.exception.auth;

import com.example.backend.exception.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when authentication token is invalid or malformed.
 */
public class TokenInvalidException extends DomainException {

    public TokenInvalidException() {
        super(HttpStatus.UNAUTHORIZED.value(), "AUTH_TOKEN_INVALID", "Token xác thực không hợp lệ");
    }

    public TokenInvalidException(String message) {
        super(HttpStatus.UNAUTHORIZED.value(), "AUTH_TOKEN_INVALID", message);
    }
}

