package com.example.backend.excepton;

public class BadRequestException extends RequestException{
    public BadRequestException(String message) {
        super(400,message);
    }
}
