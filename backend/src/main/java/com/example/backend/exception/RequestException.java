package com.example.backend.exception;

import lombok.Getter;

/**
 * Base exception for all application-level exceptions with HTTP status code.
 * All custom exceptions should extend this class or its subclasses.
 */
@Getter
public abstract class RequestException extends RuntimeException {
    private final int statusCode;

    /**
     * Constructs a new RequestException with the specified status code and message.
     *
     * @param statusCode the HTTP status code to return
     * @param message    the detail message
     */
    public RequestException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }
}
