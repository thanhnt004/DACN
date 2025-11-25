package com.example.backend.exception.auth;

import com.example.backend.exception.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when email/password reset token is invalid or malformed.
 */
public class ResetTokenInvalidException extends DomainException {

    public ResetTokenInvalidException() {
        super(HttpStatus.CONFLICT.value(), "AUTH_RESET_TOKEN_INVALID", "Token đặt lại mật khẩu không hợp lệ");
    }

    public ResetTokenInvalidException(String message) {
        super(HttpStatus.CONFLICT.value(), "AUTH_RESET_TOKEN_INVALID", message);
    }
}

