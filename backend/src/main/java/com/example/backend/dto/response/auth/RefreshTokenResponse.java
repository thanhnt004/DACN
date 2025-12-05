package com.example.backend.dto.response.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenResponse {
    private String accessToken;
    @Builder.Default
    private String tokenType = "Bearer"; // "Bearer"
    @Value("${ACCESS_TOKEN_EXPIRATION}")
    private Long expiresIn;
    @JsonProperty("isAdmin")
    private boolean isAdmin;
    @Builder.Default
    private Instant issuedAt = Instant.now();
}