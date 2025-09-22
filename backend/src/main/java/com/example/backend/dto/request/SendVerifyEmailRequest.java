package com.example.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendVerifyEmailRequest {
    @Email
    @NotBlank
    private String email;
}
