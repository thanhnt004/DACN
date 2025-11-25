package com.example.backend.exception.auth;

import com.example.backend.exception.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when attempting to register with an already registered email.
 */
public class UserAlreadyExistsException extends DomainException {

    public UserAlreadyExistsException() {
        super(HttpStatus.CONFLICT.value(), "AUTH_USER_ALREADY_EXISTS", "Email đã được đăng ký");
    }

    public UserAlreadyExistsException(String message) {
        super(HttpStatus.CONFLICT.value(), "AUTH_USER_ALREADY_EXISTS", message);
    }
}

