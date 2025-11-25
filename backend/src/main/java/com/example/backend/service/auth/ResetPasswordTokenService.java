package com.example.backend.service.auth;

import com.example.backend.config.TokenType;
import com.example.backend.exception.auth.ResetTokenInvalidException;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResetPasswordTokenService {
    private final JwtService jwtService;
    public String generateResetPasswordToken(UUID userId, String email)
    {
        String jti = UUID.randomUUID().toString();
        Map<String,Object> claims = new HashMap<>();
        claims.put("jti",jti);
        claims.put("userId",userId);
        claims.put("email",email);
        return jwtService.createToken(TokenType.RESET_PASSWORD, String.valueOf(userId),claims);
    }

    public String getEmail(String token)
    {
        return getClaims(token).get("email", String.class);
    }
    public UUID getUserId(String token)
    {
        return UUID.fromString(getClaims(token).get("userId", String.class));
    }
    public boolean isExpired(String token)
    {
        return getClaims(token).getExpiration().before(new Date());
    }
    private Claims getClaims(String token)
    {
        try {
            return jwtService.parseAndValidate(token);
        }catch (Exception e)
        {
            throw new ResetTokenInvalidException("Token đặt lại mật khẩu không hợp lệ");
        }
    }
}
