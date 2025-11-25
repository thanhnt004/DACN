package com.example.backend.exception.email;

import com.example.backend.exception.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when email sending fails.
 */
public class EmailSendException extends DomainException {

    public EmailSendException() {
        super(HttpStatus.BAD_REQUEST.value(), "EMAIL_SEND_FAILED", "Gửi email thất bại");
    }

    public EmailSendException(String message) {
        super(HttpStatus.BAD_REQUEST.value(), "EMAIL_SEND_FAILED", message);
    }
}

