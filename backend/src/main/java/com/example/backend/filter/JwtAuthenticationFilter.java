package com.example.backend.filter;

import com.example.backend.config.SecurityProperties;
import com.example.backend.dto.response.auth.CustomUserDetail;
import com.example.backend.service.auth.AccessTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;


import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AccessTokenService accessTokenService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final SecurityProperties securityProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws IOException, ServletException {
        String path = request.getRequestURI();
        log.trace("JwtFilter start for path: {}", path);

        try {
            String token = extractJwtFromRequest(request);

            if (!StringUtils.hasText(token)) {
                log.trace("No JWT token found in request for path: {}", path);
                filterChain.doFilter(request, response);
                return;
            }

            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                log.trace("SecurityContext already contains authentication: {}, skip jwt parsing",
                        SecurityContextHolder.getContext().getAuthentication().getName());
                filterChain.doFilter(request, response);
                return;
            }
            // Validate token
            if (accessTokenService.validateAccessToken(token)) {
                String email = accessTokenService.extractEmail(token);
                UUID id = accessTokenService.extractUserId(token);
                var roles = accessTokenService.extractRole(token);
                UserDetails userDetails = new CustomUserDetail(id,email,roles,null,false,true);
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.info("JWT authentication successful for {} with authorities={}", email, userDetails.getAuthorities());
            } else {
                log.trace("JWT token invalid or expired for path: {}", path);
            }

        } catch (Exception e) {
            log.warn("JWT authentication failed: {}", e.getMessage(), e);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        log.info("request path: {} method: {} should not filter check", request.getRequestURI(), request.getMethod());
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        // Allow GET requests for catalog endpoints without authentication
        // This allows guests to browse products, categories, brands, etc.
        if ("GET".equalsIgnoreCase(method)) {
            if (path.startsWith("/api/v1/categories") || 
                path.startsWith("/api/v1/brands") || 
                path.startsWith("/api/v1/sizes") || 
                path.startsWith("/api/v1/colors") ||
                path.startsWith("/api/v1/discounts")) {
                log.trace("Allowing GET request for catalog endpoint: {}", path);
                return true;
            }
        }
        
        // Allow GET requests for products (public read)
        if ("GET".equalsIgnoreCase(method) && path.startsWith("/api/v1/products")) {
            log.trace("Allowing GET request for products endpoint: {}", path);
            return true;
        }
        
        // Check against configured public endpoints
        return isPublicEndpoint(path);
    }

    /**
     * Check if the path matches any public endpoint pattern
     * Uses AntPathMatcher for pattern matching (supports wildcards like ** and *)
     */
    private boolean isPublicEndpoint(String path) {
        if (securityProperties.getPublicEndpoints() == null || securityProperties.getPublicEndpoints().isEmpty()) {
            log.warn("Public endpoints list is empty! All endpoints will require authentication.");
            return false;
        }

        for (String pattern : securityProperties.getPublicEndpoints()) {
            if (pathMatcher.match(pattern, path)) {
                log.trace("Path {} matches public pattern: {}", path, pattern);
                return true;
            }
        }

        log.trace("Path {} does not match any public endpoint pattern", path);
        return false;
    }
}