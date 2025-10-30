package com.example.backend.service.auth;

import com.example.backend.dto.response.auth.CustomUserDetail;
import com.example.backend.dto.request.auth.LoginRequest;
import com.example.backend.dto.request.auth.RegisterRequest;
import com.example.backend.dto.response.auth.LoginResponse;
import com.example.backend.dto.response.auth.LogoutResponse;
import com.example.backend.dto.response.auth.RefreshTokenResponse;
import com.example.backend.dto.response.auth.RegisterResponse;
import com.example.backend.excepton.AuthenticationException;
import com.example.backend.excepton.NotFoundException;
import com.example.backend.mapper.UserMapper;
import com.example.backend.model.RefreshToken;
import com.example.backend.model.enumrator.Role;
import com.example.backend.model.User;
import com.example.backend.model.enumrator.UserStatus;
import com.example.backend.repository.user.UserRepository;
import com.example.backend.util.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    private final AccessTokenService accessTokenService;
    private final RefreshTokenService refreshTokenService;
    private final EmailVerificationService emailVerificationService;
    private final CookieUtil cookieUtil;
    @Value("${app.jwt.ttlSeconds.ACCESS}")
    private Long accessTokenExpiration;
    private final PasswordEncoder passwordEncoder;

    public RegisterResponse register(RegisterRequest request)
    {
        //check exist
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if(user != null)
        {
            throw new AuthenticationException(409,"Email is used by another account");
        }
        User newUser = userMapper.toUser(request,passwordEncoder);
        newUser.setRole(Role.CUSTOMER);
        newUser.setStatus(UserStatus.ACTIVE);
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
                            request.getIdentifier(),
                            request.getPassword()
                    )
            );
        }
        catch (RuntimeException e) {
            throw new AuthenticationException(401,e.getMessage());
        }
        //set context
        SecurityContextHolder.getContext().setAuthentication(authentication);
        //get userdetails
        CustomUserDetail userDetail = (CustomUserDetail) authentication.getPrincipal();

        User currentUser = userRepository.findById(userDetail.getId()).orElseThrow(()->new NotFoundException("User not found!"));
        currentUser.setLastLoginAt(LocalDateTime.now());
        userRepository.save(currentUser);
        String accessToken = accessTokenService.generateAccessToken(currentUser);
        String refreshToken = refreshTokenService.createToken(currentUser);

        response.addCookie(cookieUtil.createRefreshTokenCookie(refreshToken));
        return LoginResponse.builder()
                .requireEmailVerification(currentUser.getEmailVerifiedAt() == null)
                .accessToken(accessToken)
                .expiresIn(accessTokenExpiration)
                .isAdmin(currentUser.getRole().equals(Role.ADMIN))
                .build();
    }
    @Transactional
    public RefreshTokenResponse refreshToken(String token, HttpServletResponse response)
    {
        //check exist
        RefreshToken refreshToken = refreshTokenService.findByRawToken(token);
        //Check expiry
        refreshTokenService.verifyToken(refreshToken);
        //Check user state
        User user = refreshToken.getUser();
        if (user.isDisable()||user.isLocked())
        {
            refreshTokenService.revokeAllByUser(user,"Account is disable!");
            throw new AuthenticationException(401,"Account is "+user.getStatus().toString());
        }
        //gen AC
        String accessToken = accessTokenService.generateAccessToken(user);
        //Rotate(gen and save RT)
        String newToken = refreshTokenService.rotateToken(refreshToken);

        response.addCookie(cookieUtil.createRefreshTokenCookie(newToken));

        return RefreshTokenResponse.builder()
                .accessToken(accessToken)
                .expiresIn(accessTokenExpiration)
                .build();
    }
    @Transactional
    public LogoutResponse logOut(String token, HttpServletResponse response)
    {
        RefreshToken refreshToken = refreshTokenService.findByRawToken(token);
        if(refreshTokenService.revokeToken(refreshToken,"Logout"))
        {
           cookieUtil.clearCookie(response,CookieUtil.REFRESH_TOKEN_COOKIE);
            return LogoutResponse.builder()
                    .message("Log out successful!")
                    .build();
        }
        else return LogoutResponse.builder()
                .message("Log out fail!!")
                .build();
    }
    @Transactional
    public LogoutResponse logOutAll(String token,HttpServletResponse response)
    {
        RefreshToken refreshToken = refreshTokenService.findByRawToken(token);
        if(refreshTokenService.revokeAllByUser(refreshToken.getUser(),"Log out")>0)
        {
            cookieUtil.clearCookie(response,CookieUtil.REFRESH_TOKEN_COOKIE);
            return LogoutResponse.builder()
                    .message("Log out successful!")
                    .build();
        }
        else return LogoutResponse.builder()
                .message("Log out fail!!")
                .build();
    }

}
