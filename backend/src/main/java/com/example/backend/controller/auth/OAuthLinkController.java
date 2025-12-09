package com.example.backend.controller.auth;

import com.example.backend.dto.response.auth.CustomUserDetail;
import com.example.backend.exception.BadRequestException;
import com.example.backend.model.RefreshToken;
import com.example.backend.model.User;
import com.example.backend.repository.user.UserRepository;
import com.example.backend.service.auth.RefreshTokenService;
import com.example.backend.service.auth.oautth.OAuthAccountService;
import com.example.backend.service.auth.oautth.StateTokenService;
import com.example.backend.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.Cookie;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth/oauth2")
@RequiredArgsConstructor
public class OAuthLinkController {
    private final OAuthAccountService oauthService;
    private final StateTokenService tokenService;
    private final CookieUtil cookieUtil;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    
    @GetMapping("/link/{provider}")
    public void linkStart(@AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetail userDetail,
                          @PathVariable String provider,
                          @RequestParam(name = "redirect", required = false) String redirect,
                          HttpServletRequest request,
                          HttpServletResponse response) {
        if (!"google".equals(provider) && !"facebook".equals(provider)) {
            throw new BadRequestException("Provider không hợp lệ.");
        }
        
        UUID userId = null;
        
        // Try to get user from JWT first
        if (userDetail != null) {
            userId = userDetail.getId();
        } else {
            // If no JWT, try to get from refresh token cookie
            var refreshTokenCookie = cookieUtil.readCookie(request, CookieUtil.REFRESH_TOKEN_COOKIE);
            if (refreshTokenCookie.isPresent()) {
                try {
                    RefreshToken refreshToken = refreshTokenService.findByRawToken(refreshTokenCookie.get().getValue());
                    userId = refreshToken.getUser().getId();
                } catch (Exception e) {
                    throw new BadRequestException("Vui lòng đăng nhập để liên kết tài khoản.");
                }
            } else {
                throw new BadRequestException("Vui lòng đăng nhập để liên kết tài khoản.");
            }
        }
        
        String state = tokenService.createStateToken(userId, provider, redirect);
        Cookie cookie = cookieUtil.creatOAuthState(state);
        response.addCookie(cookie);
        // Redirect tới flow oauth2 mặc định của Spring
        String location = "/oauth2/authorization/" + URLEncoder.encode(provider, StandardCharsets.UTF_8);
        response.setStatus(302);
        response.setHeader("Location", location);
    }
    @DeleteMapping("/link/{provider}")
    public ResponseEntity<?> unlink(@AuthenticationPrincipal CustomUserDetail userDetail,
                       @PathVariable String provider) {
        oauthService.unlink(userDetail, provider);
        return ResponseEntity.accepted().build();
    }
}
