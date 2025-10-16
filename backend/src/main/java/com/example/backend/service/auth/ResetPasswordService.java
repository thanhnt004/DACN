package com.example.backend.service.auth;

import com.example.backend.dto.CustomUserDetail;
import com.example.backend.dto.request.ChangePasswordRequest;
import com.example.backend.excepton.*;
import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.EmailService;
import com.example.backend.util.CryptoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ResetPasswordService {
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final ResetPasswordTokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    @Value("${app.frontend.base-url}")
    private String frontendUrl;
    @Async
    public void sendPasswordResetEmail(String email)
    {
        User user = userRepository.findByEmail(email).orElseThrow(()->new NotFoundException("Email not found!"));
        String token = tokenService.generateResetPasswordToken(user.getId(),email);
        String verificationUrl = frontendUrl + "api/v1/auth/verify-email?token=" + token;
        String subject = "Verify your email address";
        String content = "<h3>Chào bạn!</h3>"
                + "<p>Nhấn vào link để xác minh:</p>"
                + "<a> " + verificationUrl + "</a>";
        emailService.sendMessage(user.getEmail(),content,subject);
    }
    public void validateToken(String token)
    {
        //used dung redis
        //expiry
        if (tokenService.isExpired(token))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN.value(), "Token expired");

    }
    @Transactional
    public void confirmReset(String token, String newPassword)
    {
        var userId = tokenService.getUserId(token);
        var userEmailFromToken = tokenService.getEmail(token);

        //change password
        User user  = userRepository.findById(userId).orElseThrow(()->new NotFoundException("user not found!"));
        if (!Objects.equals(user.getEmail(), userEmailFromToken))
            throw new ConflictException("User email is not email from token!");
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        //disable token dung redis

    }
    @Transactional
    public void changePassword(CustomUserDetail userDetail, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getNewPasswordConfirm()))
            throw new ConflictException("Password confirm invalid");
        User currentUser = userRepository.findById(userDetail.getId()).orElseThrow(()->new NotFoundException("User not found!"));
        String passwordHash = CryptoUtils.hash(request.getCurrentPassword());
        if (!currentUser.getPasswordHash().equals(passwordHash))
            throw new ConflictException("Incorrect password");
        String newPasswordHash = CryptoUtils.hash(request.getNewPassword());
        currentUser.setPasswordHash(newPasswordHash);
        userRepository.save(currentUser);
    }
}
