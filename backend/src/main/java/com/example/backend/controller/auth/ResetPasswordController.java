package com.example.backend.controller.auth;

import com.example.backend.dto.request.ResetPasswordRequest;
import com.example.backend.dto.request.SendVerifyEmailRequest;
import com.example.backend.service.auth.ResetPasswordService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ResetPasswordController {
    ResetPasswordService resetPasswordService;
    @PostMapping(value = "/forgot-password")
    public ResponseEntity<String> sendForgotPasswordEmail(@Valid @RequestBody SendVerifyEmailRequest request, HttpServletRequest httpServletRequest){
        resetPasswordService.sendPasswordResetEmail(request.getEmail());
        return ResponseEntity.status(200).body("Send email successful!");
    }
    @PostMapping(value = "/reset-password/validate-token")
    public ResponseEntity<String> validateToken(@RequestParam String token){
        resetPasswordService.validateToken(token);
        return ResponseEntity.status(200).body("Successful!");
    }
    @PostMapping(value = "/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request)
    {
        resetPasswordService.confirmReset(request.getToken(),request.getPassword());
        return ResponseEntity.status(200).body("Successful!");
    }
}
