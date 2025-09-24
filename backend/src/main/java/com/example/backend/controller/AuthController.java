package com.example.backend.controller;

import com.example.backend.dto.request.LoginRequest;
import com.example.backend.dto.request.RegisterRequest;
import com.example.backend.dto.response.LoginResponse;
import com.example.backend.dto.response.LogoutResponse;
import com.example.backend.dto.response.RefreshTokenResponse;
import com.example.backend.dto.response.RegisterResponse;
import com.example.backend.service.auth.AuthService;
import com.example.backend.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping(value = "/register")
    public ResponseEntity<RegisterResponse> register(@Validated @RequestBody RegisterRequest registerRequest)
    {
        RegisterResponse registerResponse = authService.register(registerRequest);
        return ResponseEntity.status(HttpStatusCode.valueOf(201)).body(registerResponse);
    }
    @PostMapping(value = "/login")
    public ResponseEntity<LoginResponse> login(@Validated @RequestBody LoginRequest loginRequest
            , HttpServletRequest request, HttpServletResponse response)
    {
        LoginResponse loginResponse = authService.login(loginRequest,response);
        return ResponseEntity.status(HttpStatusCode.valueOf(200)).body(loginResponse);
    }
    @PostMapping(value = "/refresh")
    public ResponseEntity<RefreshTokenResponse> refresh(HttpServletRequest request, HttpServletResponse response)
    {
        String refreshToken = CookieUtil.getRefreshTokenFromRequest(request);
        RefreshTokenResponse refreshTokenResponse = authService.refreshToken(refreshToken,response);
        return ResponseEntity.status(HttpStatusCode.valueOf(201)).body(refreshTokenResponse);
    }
    @PostMapping(value = "/logout")
    public ResponseEntity<LogoutResponse> logOut(HttpServletRequest request, HttpServletResponse response)
    {
        String refreshToken = CookieUtil.getRefreshTokenFromRequest(request);
        LogoutResponse logoutResponse = authService.logOut(refreshToken,response);
        return ResponseEntity.status(HttpStatusCode.valueOf(201)).body(logoutResponse);
    }
    @PostMapping(value = "/logout-all")
    public ResponseEntity<LogoutResponse> logOutAll(HttpServletRequest request, HttpServletResponse response)
    {
        String refreshToken = CookieUtil.getRefreshTokenFromRequest(request);
        LogoutResponse logoutResponse = authService.logOutAll(refreshToken,response);
        return ResponseEntity.status(HttpStatusCode.valueOf(201)).body(logoutResponse);
    }

}
