package com.example.backend.aop;

import com.example.backend.service.facade.CheckoutFacadeService;

import com.example.backend.validate.RequireSessionOwnership;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class SessionOwnershipAspect {

    private final CheckoutFacadeService checkoutFacadeService;

    @Before("@annotation(requireSessionOwnership)")
    public void validateSessionOwnership(JoinPoint joinPoint, RequireSessionOwnership requireSessionOwnership) {
        try {
            HttpServletRequest request = getCurrentRequest();
            if (request == null) {
                throw new SecurityException("No request context found");
            }

            String sessionToken = request.getHeader("X-Session-Token");
            UUID sessionId = extractSessionIdFromArgs(joinPoint.getArgs());

            if (sessionToken == null) {
                throw new SecurityException("X-Session-Token header is required");
            }

            if (sessionId == null) {
                throw new SecurityException("Session ID not found in request parameters");
            }

            boolean isValid = checkoutFacadeService.validateSessionToken(sessionId, sessionToken);
            if (!isValid) {
                throw new SecurityException("Invalid session token or you don't have permission to access this session");
            }

            log.debug("Session ownership validated successfully for sessionId: {}", sessionId);

        } catch (SecurityException e) {
            log.warn("Session ownership validation failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error during session ownership validation", e);
            throw new SecurityException("Session validation error");
        }
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    private UUID extractSessionIdFromArgs(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof UUID) {
                return (UUID) arg;
            }
            if (arg instanceof String) {
                try {
                    return UUID.fromString((String) arg);
                } catch (IllegalArgumentException ignored) {
                    // Not a UUID string, continue
                }
            }
        }
        return null;
    }
}