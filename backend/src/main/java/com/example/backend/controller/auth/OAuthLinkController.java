package com.example.backend.controller.auth;

import com.example.backend.dto.response.auth.CustomUserDetail;
import com.example.backend.service.auth.oautth.OAuthAccountService;
import com.example.backend.service.auth.oautth.StateTokenService;
import com.example.backend.util.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.Cookie;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/auth/oauth2")
@RequiredArgsConstructor
public class OAuthLinkController {
    private final OAuthAccountService oauthService;
    private final StateTokenService tokenService;
    private final CookieUtil cookieUtil;
    @GetMapping("/link/{provider}")
    public void linkStart(@AuthenticationPrincipal CustomUserDetail userDetail,
                          @PathVariable String provider,
                          @RequestParam(name = "redirect", required = false) String redirect,
                          HttpServletResponse response) {
        if (!"google".equals(provider) && !"facebook".equals(provider)) {
            throw new IllegalArgumentException("Provider không hợp lệ.");
        }
        String state = tokenService.createStateToken(userDetail.getId(), provider,redirect);
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
