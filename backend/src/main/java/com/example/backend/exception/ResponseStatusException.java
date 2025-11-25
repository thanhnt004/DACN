package com.example.backend.exception;


public class ResponseStatusException extends RequestException {

    /**
     * Constructs a ResponseStatusException with the specified status code and message.
     *
     * @param statusCode the HTTP status code to return
     * @param message    the detail message
     */
    public ResponseStatusException(int statusCode, String message) {
        super(statusCode, message);
    }
}
