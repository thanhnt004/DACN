package com.example.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ErrorResponse {
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    private Integer status;
    private String message;
}