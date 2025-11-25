package com.example.backend.exception;

import lombok.Getter;

/**
 * Base domain exception with HTTP status and error code for ProblemDetails.
 * This exception includes an application-specific error code for programmatic error handling.
 * All domain-specific exceptions should extend this class.
 *
 * <p>Error codes follow the format: DOMAIN_ERROR_TYPE (e.g., AUTH_INVALID_CREDENTIALS, PRODUCT_NOT_FOUND)</p>
 */
@Getter
public abstract class DomainException extends RequestException {

    private final String code;

    /**
     * Constructs a new DomainException with the specified status code, error code, and message.
     *
     * @param statusCode the HTTP status code to return
     * @param code       the application-specific error code
     * @param message    the detail message
     */
    protected DomainException(int statusCode, String code, String message) {
        super(statusCode, message);
        this.code = code;
    }
}
