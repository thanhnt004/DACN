package com.example.backend.excepton;

import com.example.backend.dto.response.common.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Handle custom RequestException and its subclasses (BadRequestException,
    // NotFoundException, ConflictException)
    @ExceptionHandler(value = RequestException.class)
    public ResponseEntity<ErrorResponse> handleRequestException(RequestException e) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .message(e.getMessage())
                .status(e.getStatusCode())
                .build();
        return ResponseEntity.status(e.getStatusCode()).body(errorResponse);
    }

    @ExceptionHandler(value = AuthenticationException.class)
    public ResponseEntity<ErrorResponse> authenticationException(AuthenticationException e) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .message(e.getMessage())
                .status(e.getStatusCode())
                .build();
        return ResponseEntity.status(e.getStatusCode()).body(errorResponse);
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> invalidInputException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField(); // tên trường
            String errorMessage = error.getDefaultMessage(); // thông báo lỗi
            errors.put(fieldName, errorMessage);
        });

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errors);
    }

    @ExceptionHandler(value = IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> illegalArgument(IllegalArgumentException ex) {
        String message = ex.getMessage();
        return ResponseEntity.badRequest().body(Map.of(
                "message", message));
    }

    @ExceptionHandler(value = BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> wrongEmailOrPassword(BadCredentialsException ex) {
        String message = ex.getMessage();
        return ResponseEntity.badRequest().body(Map.of(
                "message", message));
    }

}
