package com.example.backend.service.auth;

import com.example.backend.excepton.ResponseStatusException;
import com.example.backend.model.EmailLog;
import com.example.backend.model.enumrator.UserStatus;
import com.example.backend.repository.EmailLogRepository;
import com.example.backend.repository.user.UserRepository;
import com.example.backend.service.EmailService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailVerificationService {
    private final UserRepository userRepository;
    private final EmailLogRepository emailLogRepository;
    private final EmailService emailService;
    private final EmailVerifyTokenService tokenService;

    @Value("${app.frontend.base-url}")
    private String defaultFrontendBaseUrl;

    @Transactional
    public Map<String, Object> verifyEmail(String token) {
        var userId = tokenService.getUserId(token);
        var emailFromToken = tokenService.getEmail(token);

        if (tokenService.isExpired(token))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN.value(), "Token hết hạn");

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED.value(), "Tài khoản không tồn tại"));

        if (!user.getEmail().equalsIgnoreCase(emailFromToken)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT.value(), "Email không khớp. Vui lòng yêu cầu gửi lại liên kết xác minh.");
        }

        // Optional: check token version if you encode it
        // if (claims.version() != user.getTokenVersion()) { ... }

        if (user.getEmailVerifiedAt() != null) {
            return Map.of(
                    "status", "already_verified",
                    "verifiedAt", user.getEmailVerifiedAt()
            );
        }
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerifiedAt(Instant.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return Map.of(
                "status", "verified",
                "userId", user.getId(),
                "email", user.getEmail()
        );
    }

    @Transactional
    public void resendVerification(String email) {
        if (!StringUtils.hasText(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST.value(), "Email là bắt buộc");
        }

        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND.value(), "Không tìm thấy người dùng"));

        if (user.getEmailVerifiedAt() != null) {
            // idempotent, no-op
            return;
        }

        sendVerificationEmailAsync(user.getId(),email);
    }

    public String generateVerificationLink(String userId, String email) {
        var id = UUID.fromString(userId);
        var user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND.value(), "Không tìm thấy người dùng"));

        if (!user.getEmail().equalsIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT.value(), "Email không khớp với người dùng");
        }

        var token = tokenService.generateEmailVerifyToken(user.getId(), user.getEmail());
        var base = defaultFrontendBaseUrl;
        var url = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;

        // FE route expects /verify-email?token=...
        var encoded = URLEncoder.encode(token, StandardCharsets.UTF_8);
        return url + "/verify-email?token=" + encoded;
    }
    @Async
    public void sendVerificationEmailAsync(UUID userId,String email) {
        try {
            String verificationUrl = generateVerificationLink(userId.toString(),email);
            String subject = "Verify your email address";
            String content = "<h3>Chào bạn!</h3>"
                    + "<p>Nhấn vào link để xác minh:</p>"
                    + "<a> " + verificationUrl + "</a>";
            emailService.sendMessage(email,content,subject);

            var log = new EmailLog();
            log.setToEmail(email);
            log.setSubject("Xác minh địa chỉ email của bạn");
            log.setTemplateCode("VERIFY_EMAIL");
            log.setPayload(Map.of(
                    "verifyUrl", verificationUrl
            ));
            emailLogRepository.save(log);
        } catch (Exception e) {
            throw new RuntimeException("Email sending failed", e);
        }
    }

}
