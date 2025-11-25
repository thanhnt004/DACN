package com.example.backend.exception;

/**
 * Exception thrown when a requested resource is not found (HTTP 404).
 * Use this for general not found scenarios that don't fit specific domain exceptions.
 */
public class NotFoundException extends RequestException {

    /**
     * Constructs a NotFoundException with the specified message.
     *
     * @param message the detail message explaining what resource was not found
     */
    public NotFoundException(String message) {
        super(404, message);
    }
}
