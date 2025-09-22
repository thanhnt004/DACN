package com.example.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LogoutResponse {
    private String message;
    @Builder.Default
    private LocalDateTime logoutAt = LocalDateTime.now();
}
