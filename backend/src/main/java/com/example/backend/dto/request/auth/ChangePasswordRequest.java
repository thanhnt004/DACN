package com.example.backend.dto.request.auth;

import com.example.backend.validate.Password;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    private String currentPassword;
    @Password
    private String newPassword;
    @Password
    private String newPasswordConfirm;
}
