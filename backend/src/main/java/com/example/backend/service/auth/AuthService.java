package com.example.backend.service.auth;

import com.example.backend.dto.CustomUserDetail;
import com.example.backend.dto.request.LoginRequest;
import com.example.backend.dto.request.RegisterRequest;
import com.example.backend.dto.response.LoginResponse;
import com.example.backend.dto.response.LogoutResponse;
import com.example.backend.dto.response.RefreshTokenResponse;
import com.example.backend.dto.response.RegisterResponse;
import com.example.backend.excepton.AuthenticationException;
import com.example.backend.mapper.UserMapper;
import com.example.backend.model.RefreshToken;
import com.example.backend.model.Role;
import com.example.backend.model.User;
import com.example.backend.model.enumrator.UserStatus;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.JwtService;
import com.example.backend.service.RefreshTokenService;
import com.example.backend.util.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
public class AuthService {
    private final AuthenticationManager authenticationManager;
    UserRepository userRepository;
    UserMapper userMapper;
    JwtService jwtService;
    RefreshTokenService refreshTokenService;
    EmailVerificationService emailVerificationService;
    public RegisterResponse register(RegisterRequest request)
    {
        //check exist
        userRepository.findByEmail(request.getEmail()).orElseThrow(
                ()-> new AuthenticationException(409,"Email is used by another account")
        );
        User newUser = userMapper.toUser(request);
        newUser.setRole(Role.CUSTOMER);
        newUser.setStatus(UserStatus.DISABLED);
        newUser = userRepository.save(newUser);
        //send email
        emailVerificationService.sendVerificationEmailAsync(newUser.getId(), newUser.getEmail());
        return RegisterResponse.builder()
                .userId(newUser.getId().toString())
                .message("Register successful!")
                .emailVerificationRequired(true)
                .email(newUser.getEmail())
                .build();
    }
    public LoginResponse login(LoginRequest request, HttpServletResponse response)
    {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (RuntimeException e) {
            throw new AuthenticationException(401,"Wrong email or password!");
        }
        //set context
        SecurityContextHolder.getContext().setAuthentication(authentication);
        //get userdetails
        CustomUserDetail userDetail = (CustomUserDetail) authentication.getPrincipal();
        User user  = userDetail.getUser();
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.createToken(user);

        response.addCookie(CookieUtil.createRefreshTokenCookie(refreshToken));

        return LoginResponse.builder()
                .accessToken(accessToken)
                .build();
    }
    @Transactional
    public RefreshTokenResponse refreshToken(String token, HttpServletResponse response)
    {
        //check exist
        RefreshToken refreshToken = refreshTokenService.findByRawToken(token);
        //Check expiry
        refreshTokenService.verifyToken(refreshToken);
        //gen AC
        String accessToken = jwtService.generateAccessToken(refreshToken.getUser());
        //Rotate(gen and save RT)
        String newToken = refreshTokenService.rotateToken(refreshToken);

        response.addCookie(CookieUtil.createRefreshTokenCookie(newToken));

        return RefreshTokenResponse.builder()
                .accessToken(accessToken)
                .build();
    }
    public LogoutResponse logOut(String token, HttpServletResponse response)
    {
        RefreshToken refreshToken = refreshTokenService.findByRawToken(token);
        if(refreshTokenService.revokeToken(refreshToken,"Logout"))
        {
            response.addCookie(CookieUtil.createExpiredRefreshTokenCookie());
            return LogoutResponse.builder()
                    .message("Log out successful!")
                    .build();
        }
        else return LogoutResponse.builder()
                .message("Log out fail!!")
                .build();
    }
    public LogoutResponse logOutAll(String token,HttpServletResponse response)
    {
        RefreshToken refreshToken = refreshTokenService.findByRawToken(token);
        if(refreshTokenService.deleteByUserId(refreshToken.getUser().getId())>0)
        {
            response.addCookie(CookieUtil.createExpiredRefreshTokenCookie());
            return LogoutResponse.builder()
                    .message("Log out successful!")
                    .build();
        }
        else return LogoutResponse.builder()
                .message("Log out fail!!")
                .build();
    }
}
