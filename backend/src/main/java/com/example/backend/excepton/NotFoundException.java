package com.example.backend.excepton;

public class NotFoundException extends RequestException {
    public NotFoundException(String message) {
        super(404,message);
    }
}
