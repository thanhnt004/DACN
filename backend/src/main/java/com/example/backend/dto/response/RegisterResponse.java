package com.example.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RegisterResponse {
    private String message;
    @Builder.Default
    private Boolean emailVerificationRequired = true;
    private String email;
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    private String userId;
}