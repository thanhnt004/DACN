package com.example.backend.dto.response.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {
    private String message;
    @Builder.Default
    private Boolean emailVerificationRequired = true;
    private String email;
    @Builder.Default
    private Instant createdAt = Instant.now();
    private String userId;
}