package com.example.backend.service.shipping;

public class GhnApiException extends RuntimeException {
    private Exception exception;
    public GhnApiException(String message,Exception e) {
        super(message);
        this.exception = e;
    }
}
