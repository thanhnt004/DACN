package com.example.backend.service.auth;

import com.example.backend.config.TokenType;
import com.example.backend.exception.AuthenticationException;
import com.example.backend.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AccessTokenService{
    private final JwtService jwtService;
    public String generateAccessToken(User user)
    {
        String jti = UUID.randomUUID().toString();
        String role =  "ROLE_"+user.getRole().toString();
        Map<String,Object> claims = new HashMap<>();
        claims.put("jti", jti);
        claims.put("email", user.getEmail());
        claims.put("role", role);
        // lưu jti vào store để check sau
        //
         //
        return jwtService.createToken(TokenType.ACCESS, String.valueOf(user.getId()), claims);
    }
    public boolean validateAccessToken(String token) {
       try {
           var claims = jwtService.parseAndValidate(token);
       } catch (Exception e) {
           throw new AuthenticationException(401,"Invalid access token");
       }
       return true;
    }

    public String extractEmail(String token) {
        var claims = jwtService.parseAndValidate(token);
        return claims.get("email",String.class);
    }
    public UUID extractUserId(String token)
    {
        var claims = jwtService.parseAndValidate(token);
        return UUID.fromString(claims.getSubject());
    }
    public List<SimpleGrantedAuthority> extractRole(String token)
    {
        var claims = jwtService.parseAndValidate(token);
        Object role = claims.get("role");
        if (role == null) return List.of();
        return Arrays.stream(role.toString().split(",")).map(SimpleGrantedAuthority::new).toList();
    }
}
