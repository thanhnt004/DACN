package com.example.backend.exception;

import com.example.backend.dto.response.common.ProblemDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final URI DEFAULT_TYPE = URI.create("about:blank");

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
     * Handle AuthenticationException separately for clarity.
     * Note: This is redundant since AuthenticationException extends RequestException.
     * Consider removing this handler or migrating AuthenticationException to extend DomainException.
     */
    @ExceptionHandler(value = AuthenticationException.class)
    public ResponseEntity<ProblemDetails> authenticationException(AuthenticationException e) {
        HttpStatus status = HttpStatus.resolve(e.getStatusCode());
        if (status == null) {
            status = HttpStatus.UNAUTHORIZED;
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

    @ExceptionHandler(value = BadCredentialsException.class)
    public ResponseEntity<ProblemDetails> wrongEmailOrPassword(BadCredentialsException ex) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;

        ProblemDetails body = ProblemDetails.builder()
                .type(DEFAULT_TYPE)
                .title("Bad credentials")
                .status(status.value())
                .detail("Email or password is incorrect")
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(status).body(body);
    }

}
