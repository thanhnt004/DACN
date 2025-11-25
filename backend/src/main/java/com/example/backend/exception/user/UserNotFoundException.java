package com.example.backend.exception.user;

import com.example.backend.exception.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when user is not found.
 */
public class UserNotFoundException extends DomainException {

    public UserNotFoundException() {
        super(HttpStatus.NOT_FOUND.value(), "USER_NOT_FOUND", "Không tìm thấy người dùng");
    }

    public UserNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND.value(), "USER_NOT_FOUND", message);
    }
}

