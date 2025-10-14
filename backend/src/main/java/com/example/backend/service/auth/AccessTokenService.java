package com.example.backend.service.auth;

import com.example.backend.config.TokenType;
import com.example.backend.excepton.AuthenticationException;
import com.example.backend.model.User;
import com.example.backend.model.enumrator.Role;
import com.example.backend.util.ParseJwtTokenUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccessTokenService{
    private final JwtService jwtService;
    public String generateAccessToken(User user)
    {
        String jti = UUID.randomUUID().toString();
        Role role = user.getRole() == null ? Role.CUSTOMER : user.getRole();
        Map<String,Object> claims = new HashMap<>();
        claims.put("jti", jti);
        claims.put("email", user.getEmail());
        claims.put("role", role.name());
        // lưu jti vào store để check sau
        //
         //
        return jwtService.createToken(TokenType.ACCESS, String.valueOf(user.getId()), claims);
    }
    public boolean validateAccessToken(String token) {
       try {
           var claims = jwtService.parseAndValidate(token);
           if (claims.getExpiration().before(new Date()))
               throw new ExpiredJwtException(null,claims,"Access token is expired");
       } catch (Exception e) {
           throw new AuthenticationException(401,"Invalid access token");
       }
       return true;
    }

    public String extractEmail(String token) {
        var claims = jwtService.parseAndValidate(token);
        return claims.get("email",String.class);
    }

}
