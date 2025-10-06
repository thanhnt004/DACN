package com.example.backend.controller.auth;

import com.example.backend.dto.request.SendVerifyEmailRequest;
import com.example.backend.dto.request.VerifyEmailRequest;
import com.example.backend.service.auth.EmailVerificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    public EmailVerificationController(EmailVerificationService emailVerificationService) {
        this.emailVerificationService = emailVerificationService;
    }

    // POST /api/v1/auth/verify-email
    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@Valid @RequestBody VerifyEmailRequest req) {
        var result = emailVerificationService.verifyEmail(req.getToken());
        return ResponseEntity.ok(result);
    }

    // POST /api/v1/auth/verify-email/resend
    @PostMapping("/verify-email/resend")
    public ResponseEntity<?> resend(@Valid @RequestBody SendVerifyEmailRequest req) {
        emailVerificationService.resendVerification(req.getEmail());
        return ResponseEntity.accepted().build();
    }

}