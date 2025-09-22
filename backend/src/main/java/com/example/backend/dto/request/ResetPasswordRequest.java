package com.example.backend.dto.request;


import com.example.backend.validate.Password;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank
    private String token;
    @NotBlank
    @Password
    private String password;
}
