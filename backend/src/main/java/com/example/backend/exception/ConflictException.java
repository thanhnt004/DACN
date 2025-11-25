package com.example.backend.exception;

/**
 * Exception thrown when there is a conflict with the current state of the resource (HTTP 409).
 * Common scenarios: duplicate resources, concurrent modification conflicts, business rule violations.
 */
public class ConflictException extends RequestException {

    /**
     * Constructs a ConflictException with the specified message.
     *
     * @param message the detail message explaining the conflict
     */
    public ConflictException(String message) {
        super(409, message);
    }
}
