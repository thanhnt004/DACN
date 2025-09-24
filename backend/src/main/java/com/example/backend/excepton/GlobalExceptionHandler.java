package com.example.backend.excepton;


import com.example.backend.dto.response.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(value = AuthenticationException.class)
    public ResponseEntity<ErrorResponse> authenticationException(AuthenticationException e)
    {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .message(e.getMessage())
                .status(e.getStatusCode())
                .build();
        return ResponseEntity.status(e.getStatusCode()).body(errorResponse);
    }
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<?> invalidInputException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        if(message == null) message = "Invalid input: " + ex.getBindingResult().getAllErrors().get(0).getObjectName();
        return ResponseEntity.badRequest().body(Map.of(
                "message", message
        ));
    }
    @ExceptionHandler(value = BadCredentialsException.class)
    public ResponseEntity<?> wrongEmailOrPassword(BadCredentialsException ex) {
        String message = ex.getMessage();
        if(message == null) message = "Invalid input: " + ex.getMessage();
        return ResponseEntity.badRequest().body(Map.of(
                "message", message
        ));
    }
}
