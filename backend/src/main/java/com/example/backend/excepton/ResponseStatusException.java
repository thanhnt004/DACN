package com.example.backend.excepton;

public class ResponseStatusException extends RequestException {

    public ResponseStatusException(int statusCode, String message) {
        super(statusCode, message);
    }
}
