package com.example.backend.dto.response.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;

@Data
@Builder
public class LoginResponse {
    private String accessToken;
    @Builder.Default
    private String tokenType = "Bearer"; // "Bearer"
    @Value("${ACCESS_TOKEN_EXPIRATION}")
    private Long expiresIn;
    @JsonProperty("isAdmin")
    private boolean isAdmin = false;
    @Builder.Default
    private LocalDateTime loginAt = LocalDateTime.now();

    private boolean requireEmailVerification;
}