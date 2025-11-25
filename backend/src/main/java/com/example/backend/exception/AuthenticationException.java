package com.example.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when authentication fails (HTTP 401).
 * Use this for general authentication failures that don't fit specific domain exceptions.
 * Consider using domain-specific exceptions like InvalidCredentialsException for better error handling.
 */
public class AuthenticationException extends RequestException {

    /**
     * Constructs an AuthenticationException with the specified status code and message.
     *
     * @param statusCode the HTTP status code to return
     * @param message    the detail message explaining the authentication failure
     */
    public AuthenticationException(int statusCode, String message) {
        super(statusCode, message);
    }

    /**
     * Constructs an AuthenticationException with HTTP 401 status and the specified message.
     *
     * @param message the detail message explaining the authentication failure
     */
    public AuthenticationException(String message) {
        super(HttpStatus.UNAUTHORIZED.value(), message);
    }
}
