package com.example.backend.dto.request.auth;

import com.example.backend.validate.Password;
import com.example.backend.validate.ValidBirthDate;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterRequest {
    @Email
    @Size(max = 100, message = "Email không được vượt quá 100 ký tự")
    String email;

    @NotBlank
    @Size(max = 30)
    private String fullName;

    @Password
    String password;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(\\+84|0)[1-9]\\d{8,9}$",
            message = "Số điện thoại không hợp lệ")
    String phone;

    @ValidBirthDate
    private LocalDate dateOfBirth;
}
