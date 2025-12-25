package com.example.backend.exception;

import com.example.backend.dto.response.common.ProblemDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final URI DEFAULT_TYPE = URI.create("about:blank");
    @ExceptionHandler(SecurityException.class)


    public ResponseEntity<Map<String, Object>> handleSecurityException(SecurityException ex) {
        log.warn("Security exception: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.FORBIDDEN.value());
        response.put("error", "Forbidden");
        response.put("message", ex.getMessage());
        response.put("path", getCurrentPath());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }
    @ExceptionHandler(value = IOException.class)
    public ResponseEntity<ProblemDetails> handleIOException(IOException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        ProblemDetails body = ProblemDetails.builder()
                .type(DEFAULT_TYPE)
                .title("File Processing Error")
                .status(status.value())
                .detail("Lỗi xử lý file: " + ex.getMessage())
                .code("FILE_PROCESSING_ERROR")
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(value = UnsupportedOperationException.class)
    public ResponseEntity<ProblemDetails> handleUnsupportedOperation(UnsupportedOperationException ex) {
        HttpStatus status = HttpStatus.NOT_IMPLEMENTED;

        ProblemDetails body = ProblemDetails.builder()
                .type(DEFAULT_TYPE)
                .title("Feature Not Implemented")
                .status(status.value())
                .detail(ex.getMessage())
                .code("NOT_IMPLEMENTED")
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(status).body(body);
    }

    private String getCurrentPath() {
        // Implementation to get current request path
        return "/api/v1/checkout/sessions";
    }

    /**
     * Handle DomainException with error code.
     * This handler is more specific and will be matched before RequestException handler.
     */
    @ExceptionHandler(value = DomainException.class)
    public ResponseEntity<ProblemDetails> handleDomainException(DomainException e) {
        HttpStatus status = HttpStatus.resolve(e.getStatusCode());
        if (status == null) {
            status = HttpStatus.BAD_REQUEST;
        }

        ProblemDetails body = ProblemDetails.builder()
                .type(DEFAULT_TYPE)
                .title(status.getReasonPhrase())
                .status(status.value())
                .detail(e.getMessage())
                .code(e.getCode())
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(status).body(body);
    }

    /**
     * Handle generic RequestException and its subclasses that are not DomainException
     * (BadRequestException, NotFoundException, ConflictException, AuthenticationException).
     */
    @ExceptionHandler(value = RequestException.class)
    public ResponseEntity<ProblemDetails> handleRequestException(RequestException e) {
        HttpStatus status = HttpStatus.resolve(e.getStatusCode());
        if (status == null) {
            status = HttpStatus.BAD_REQUEST;
        }

        ProblemDetails body = ProblemDetails.builder()
                .type(DEFAULT_TYPE)
                .title(status.getReasonPhrase())
                .status(status.value())
                .detail(e.getMessage())
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(status).body(body);
    }

    /**
     * Handle validation errors from @Valid annotation.
     */

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetails> invalidInputException(MethodArgumentNotValidException ex) {
        Map<String, Object> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        HttpStatus status = HttpStatus.BAD_REQUEST;

        ProblemDetails body = ProblemDetails.builder()
                .type(DEFAULT_TYPE)
                .title("Validation failed")
                .status(status.value())
                .detail("One or more fields have invalid values")
                .timestamp(Instant.now())
                .errors(errors)
                .build();

        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(value = IllegalArgumentException.class)
    public ResponseEntity<ProblemDetails> illegalArgument(IllegalArgumentException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        ProblemDetails body = ProblemDetails.builder()
                .type(DEFAULT_TYPE)
                .title(status.getReasonPhrase())
                .status(status.value())
                .detail(ex.getMessage())
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(status).body(body);
    }

    /**
     * Handle Spring Security BadCredentialsException.
     */
    @ExceptionHandler(value = BadCredentialsException.class)
    public ResponseEntity<ProblemDetails> wrongEmailOrPassword(BadCredentialsException ex) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;

        ProblemDetails body = ProblemDetails.builder()
                .type(DEFAULT_TYPE)
                .title("Bad credentials")
                .status(status.value())
                .detail("Email hoặc mật khẩu không đúng")
                .code("AUTH_BAD_CREDENTIALS")
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(status).body(body);
    }

    /**
     * Fallback handler for any unhandled exceptions.
     * This ensures all exceptions return a consistent error response format.
     */
    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ProblemDetails> handleGenericException(Exception ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        ProblemDetails body = ProblemDetails.builder()
                .type(DEFAULT_TYPE)
                .title("Internal Server Error")
                .status(status.value())
                .detail("Đã xảy ra lỗi không mong muốn. Vui lòng thử lại sau")
                .code("INTERNAL_SERVER_ERROR")
                .timestamp(Instant.now())
                .build();

        // Log the full exception for debugging
        log.error("Unhandled exception occurred", ex);

        return ResponseEntity.status(status).body(body);
    }

}
