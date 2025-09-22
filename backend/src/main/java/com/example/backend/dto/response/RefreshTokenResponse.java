package com.example.backend.dto.response;

import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;

@Data
@Builder
public class RefreshTokenResponse {
    private String accessToken;
    @Builder.Default
    private String tokenType = "Bearer"; // "Bearer"
    @Value("${ACCESS_TOKEN_EXPIRATION}")
    private Long expiresIn;
    @Builder.Default
    private LocalDateTime issuedAt = LocalDateTime.now();
}