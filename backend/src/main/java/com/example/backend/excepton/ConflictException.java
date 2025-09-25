package com.example.backend.excepton;

public class ConflictException extends RequestException{

    public ConflictException( String message) {
        super(409, message);
    }
}
