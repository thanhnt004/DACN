package com.example.backend.config;

import com.example.backend.filter.JwtAuthenticationFilter;
import com.example.backend.handler.OAuth2LoginSuccessHandler;
import com.example.backend.service.auth.UserDetailService;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final UserDetailService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2LoginSuccessHandler successHandler;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(c -> c.configurationSource(corsConfigurationSource()))
                .anonymous(Customizer.withDefaults()) // Enable anonymous authentication
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"" + authException.getMessage() + "\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"" + accessDeniedException.getMessage() + "\"}");
                        }))
                .authorizeHttpRequests(
                        auth -> auth
                                .requestMatchers(
                                        // Orders (public for now)
                                        "/api/v1/orders/**",
                                        // Auth endpoints
                                        "/api/v1/auth/login",
                                        "/api/v1/auth/register",
                                        "/api/v1/auth/refresh-token",
                                        "/api/v1/auth/refresh",
                                        "/api/v1/auth/logout",
                                        "/api/v1/auth/logout-all",
                                        "/api/v1/auth/verify-email",
                                        "/api/v1/auth/resend-verification",
                                        "/api/v1/auth/forgot-password",
                                        "/api/v1/auth/reset-password",
                                        "/api/v1/auth/oauth2/link/**",
                                        // OAuth2 login & callback (Spring Security generated pages)
                                        "/login/oauth2/**",
                                        "/oauth2/**",
                                        // Public product/category endpoints
                                        "/api/v1/products",
                                        "/api/v1/products/**",
                                        "/api/v1/categories",
                                        "/api/v1/categories/**",
                                        "/api/v1/brands",
                                        "/api/v1/brands/**",
                                        "/api/v1/sizes",
                                        "/api/v1/sizes/**",
                                        "/api/v1/colors",
                                        "/api/v1/colors/**",
                                        "/api/v1/banners/public",
                                        // Public shipping endpoints
                                        "/api/v1/shipping/provinces",
                                        "/api/v1/shipping/provinces/**",
                                        "/api/v1/shipping/districts/**",
                                        "/api/v1/shipping/wards/**",
                                        // Checkout (supports guest users)
                                        "/api/v1/checkout/**",
                                        // Cart (supports guest users)
                                        "/api/v1/carts",
                                        "/api/v1/carts/**",
                                        // TEMP: Admin orders for testing
                                        "/api/v1/admin/orders/**",
                                        // TEMP: Reports for testing
                                        "/api/v1/reports/**",
                                        // Discounts (public read)
                                        "/api/v1/discounts",
                                        "/api/v1/discounts/*",
                                        // Chatbot (public access)
                                        "/api/v1/chatbot/**",
                                        // Payment callbacks & IPN
                                        "/api/v1/payment/vnpay/callback",
                                        "/api/v1/payment/*/callback",
                                        "/api/v1/payments/vnpay/ipn",
                                        "/api/v1/payments/status/stream/**",
                                        // Webhooks (external services)
                                        "/api/v1/webhooks/**",
                                        // Health & docs
                                        "/actuator/health",
                                        "/actuator/info",
                                        "/v3/api-docs/**",
                                        "/v3/api-docs",
                                        "/api-docs/**",
                                        "/swagger-ui/**",
                                        "/swagger-ui.html",
                                        "/swagger-ui/index.html",
                                        "/swagger-resources/**",
                                        "/swagger-resources",
                                        "/swagger-config/**",
                                        "/webjars/**",
                                        "/configuration/ui",
                                        "/configuration/security",
                                        // Static resources
                                        "/static/**",
                                        "/public/**",
                                        "/resources/**",
                                        "/favicon.ico",
                                        "/error")
                                .permitAll()
                                .anyRequest().authenticated())

                .oauth2Login(o -> o.successHandler(successHandler));

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        // Allow your frontend during development; adjust in prod
        cfg.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000", "${FRONTEND_URL}"));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With","X-Cart-ID","X-Session-Token","Idempotency-Key"));
        cfg.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
