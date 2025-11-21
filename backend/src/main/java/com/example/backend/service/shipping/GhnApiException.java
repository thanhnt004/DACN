package com.example.backend.service.shipping;

public class GhnApiException extends RuntimeException {
    private final Exception exception;

    public GhnApiException(String message) {
        this(message, null);
    }

    public GhnApiException(String message, Exception e) {
        super(message, e);
        this.exception = e;
    }

    public Exception getException() {
        return exception;
    }
}
