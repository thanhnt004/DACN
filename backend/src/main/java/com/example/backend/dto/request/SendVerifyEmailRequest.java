package com.example.backend.dto.request;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class SendVerifyEmailRequest {
    @Email
    private String email;
}
