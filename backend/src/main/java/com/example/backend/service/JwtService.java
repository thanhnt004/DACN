package com.example.backend.service;

import com.example.backend.model.Role;
import com.example.backend.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService
{
    @Value("${ACCESS_TOKEN_EXPIRATION}")
    private int expiration;
    @Value("${JWT_SECRET_KEY}")
    private String jwtSecret;

    private final String keyId = "key-hs512";

    public String generateAccessToken(User user)
    {
        Instant now = Instant.now();
        Instant expiryTime = now.plusSeconds(expiration);
        Role role = user.getRole() == null ? Role.CUSTOMER : user.getRole();
        SecretKey jwtSecretKey =  buildSecretKey();
        return Jwts.builder()
                .header()
                .type("JWT")
                .add("kid",keyId)
                .and()
                .subject(user.getId().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiryTime))
                .id(UUID.randomUUID().toString())
                .claim("email", user.getEmail())
                .claim("role", role.name())
                .claim("typ", "access")
                .signWith(jwtSecretKey,Jwts.SIG.HS512)
                .compact();
    }
    public boolean validateAccessToken(String token)
    {
        Claims claims = parseClaims(token);
        if (claims.getExpiration().before(new Date()))
        {
            throw new ExpiredJwtException(null,claims,"Access token is expired");
        }
        return true;
    }

    public boolean isExpired(String token)
    {
        return getClaims(token,Claims::getExpiration).before(new Date());
    }
    public UUID extractUserId(String token)
    {
        return UUID.fromString(getClaims(token,Claims::getSubject));
    }
    public String extractEmail(String token)
    {
        return getClaims(token,claims -> claims.get("email",String.class));
    }
    public Role extractRole(String token)
    {
        return getClaims(token,claims ->claims.get("role",Role.class));
    }
    public String extractJti(String token)
    {
        return getClaims(token, Claims::getId);
    }
    //helper function
    private <T> T getClaims(String token, Function<Claims,T> extractor)
    {
        Claims c = parseClaims(token);
        return extractor.apply(c);
    }
    private Claims parseClaims(String token)
    {
        try {
            return  Jwts.parser()
                    .verifyWith(buildSecretKey())
                    .build()
                    .parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }

    //gen key
    private SecretKey buildSecretKey() {
        String raw = this.jwtSecret;
        byte[] keyBytes;
        if (raw.matches("^[0-9a-fA-F]+$") && raw.length() >= 64 && raw.length() % 2 == 0) {
            keyBytes = hexToBytes(raw);
        } else {
            // Thử base64, fallback raw bytes
            try {
                keyBytes = Decoders.BASE64.decode(raw);
            } catch (IllegalArgumentException ex) {
                keyBytes = raw.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            }
        }
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("jwt.secret quá ngắn (<32 bytes)");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            out[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return out;
    }
}
