package com.example.backend.validation;

import com.example.backend.service.facade.CheckoutFacadeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import lombok.RequiredArgsConstructor;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.annotation.*;
import java.util.UUID;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidSessionOwnership.SessionOwnershipValidator.class)
@Documented
public @interface ValidSessionOwnership {

    String message() default "Invalid session token or you don't have permission to access this session";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
    @RequiredArgsConstructor
    class SessionOwnershipValidator implements ConstraintValidator<ValidSessionOwnership, Object> {

        private final CheckoutFacadeService checkoutFacadeService;

        @Override
        public void initialize(ValidSessionOwnership constraintAnnotation) {
            // Initialization if needed
        }

        @Override
        public boolean isValid(Object value, ConstraintValidatorContext context) {
            try {
                HttpServletRequest request = getCurrentRequest();
                if (request == null) {
                    return false;
                }

                String sessionToken = request.getHeader("X-Session-Token");
                String sessionIdStr = extractSessionIdFromPath(request.getRequestURI());

                if (sessionToken == null || sessionIdStr == null) {
                    addConstraintViolation(context, "Session token and session ID are required");
                    return false;
                }

                UUID sessionId = UUID.fromString(sessionIdStr);

                // Validate session ownership through facade service
                boolean isValid = checkoutFacadeService.validateSessionToken(sessionId, sessionToken);

                if (!isValid) {
                    addConstraintViolation(context, "Invalid session token or session not found");
                    return false;
                }

                return true;

            } catch (Exception e) {
                addConstraintViolation(context, "Session validation error: " + e.getMessage());
                return false;
            }
        }

        private HttpServletRequest getCurrentRequest() {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        }

        private String extractSessionIdFromPath(String uri) {
            // Extract sessionId from path like "/api/v1/checkout/sessions/{sessionId}/..."
            String[] parts = uri.split("/");
            for (int i = 0; i < parts.length - 1; i++) {
                if ("sessions".equals(parts[i]) && i + 1 < parts.length) {
                    return parts[i + 1];
                }
            }
            return null;
        }

        private void addConstraintViolation(ConstraintValidatorContext context, String message) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(message)
                    .addConstraintViolation();
        }
    }
}