package com.example.backend.dto.request.auth;

import com.example.backend.validate.Password;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {
    private String currentPassword;
    @Password
    private String newPassword;
    @Password
    private String newPasswordConfirm;
}
