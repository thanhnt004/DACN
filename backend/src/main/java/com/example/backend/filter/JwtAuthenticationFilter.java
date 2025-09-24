package com.example.backend.filter;

import com.example.backend.service.auth.AccessTokenService;
import com.example.backend.service.auth.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AccessTokenService accessTokenService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String authHeader = request.getHeader("Authorization");
            String token = extractJwtFromRequest(request);
            if (!StringUtils.hasText(token) && SecurityContextHolder.getContext().getAuthentication()!=null)
                filterChain.doFilter(request,response);
            if(accessTokenService.validateAccessToken(token))
            {
                String email = accessTokenService.extractEmail(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception e) {
            log.warn("JWT authentication failed: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }
        filterChain.doFilter(request, response);
    }
    private String extractJwtFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7); // Remove "Bearer " prefix
        }
        return null;
    }
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        return isPublicEndpoint(path);
    }
    private boolean isPublicEndpoint(String path) {
        // Public auth endpoints
        if (path.startsWith("/api/v1/auth/")) {
            return true;
        }

        // OAuth2 endpoints
        if (path.startsWith("/api/v1/oauth2/") || path.startsWith("/oauth2/")) {
            return true;
        }

        // Actuator health endpoints
        if (path.equals("/actuator/health") || path.equals("/actuator/info")) {
            return true;
        }

        // OpenAPI/Swagger documentation
        if (path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui")) {
            return true;
        }

        // Public product browsing (GET only được check ở SecurityConfig)
        if (path.startsWith("/api/v1/products/") ||
                path.startsWith("/api/v1/categories/") ||
                path.startsWith("/api/v1/brands/")) {
            return false; // Vẫn cần check method ở SecurityConfig
        }
        return false;
    }
}
