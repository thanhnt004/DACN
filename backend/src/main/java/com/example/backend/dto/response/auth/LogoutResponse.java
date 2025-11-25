package com.example.backend.dto.response.auth;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class LogoutResponse {
    private String message;
    @Builder.Default
    private Instant logoutAt = Instant.now();
}
