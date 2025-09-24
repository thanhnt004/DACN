package com.example.backend.service.auth.oautth;

import com.example.backend.config.TokenType;
import com.example.backend.excepton.AuthenticationException;
import com.example.backend.service.auth.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StateTokenService {
    private final JwtService jwtService;
    public String createStateToken(UUID userId, String provider, String redirect)
    {
        Map<String,Object> claims = Map.of(
                "mode", "link",
                "uid", userId.toString(),
                "provider", provider,
                "redirect", redirect != null ? redirect : ""
        );
        return jwtService.createToken(TokenType.OAUTH_STATE,userId.toString(),claims);
    }
    public Claims parse(String token)
    {
        try {
            return jwtService.parseAndValidate(token);
        }catch(Exception e)
        {
            throw new AuthenticationException(401,"Invalid token");
        }
    }
}
