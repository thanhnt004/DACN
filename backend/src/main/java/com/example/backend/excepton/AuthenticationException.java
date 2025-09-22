package com.example.backend.excepton;

public class AuthenticationException extends RequestException {

    public AuthenticationException(int statusCode, String errorCode, String message) {
        super(statusCode, errorCode, message);
    }
}
