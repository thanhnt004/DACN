package com.example.backend.exception.auth;

import com.example.backend.exception.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when authentication token has expired.
 */
public class TokenExpiredException extends DomainException {

    public TokenExpiredException() {
        super(HttpStatus.UNAUTHORIZED.value(), "AUTH_TOKEN_EXPIRED", "Token xác thực đã hết hạn");
    }

    public TokenExpiredException(String message) {
        super(HttpStatus.UNAUTHORIZED.value(), "AUTH_TOKEN_EXPIRED", message);
    }
}

