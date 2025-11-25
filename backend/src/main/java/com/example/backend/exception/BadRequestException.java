package com.example.backend.exception;

/**
 * Exception thrown when client sends an invalid request (HTTP 400).
 * Use this for general bad request scenarios that don't fit specific domain exceptions.
 */
public class BadRequestException extends RequestException {

    /**
     * Constructs a BadRequestException with the specified message.
     *
     * @param message the detail message explaining why the request is bad
     */
    public BadRequestException(String message) {
        super(400, message);
    }
}
