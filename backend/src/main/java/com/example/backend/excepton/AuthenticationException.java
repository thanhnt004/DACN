package com.example.backend.excepton;

public class AuthenticationException extends RequestException {

    public AuthenticationException(int statusCode, String message) {
        super(statusCode, message);
    }
}
