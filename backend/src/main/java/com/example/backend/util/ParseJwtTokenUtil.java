package com.example.backend.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.experimental.UtilityClass;

import javax.crypto.SecretKey;
import java.util.function.Function;

@UtilityClass
public class ParseJwtTokenUtil {
    public final String keyId = "key-hs512";
    public   <T> T getClaims(String token, Function<Claims, T> extractor,SecretKey secretKey)
    {
        Claims c = parseClaims(token,secretKey);
        return extractor.apply(c);
    }
    public Claims parseClaims(String token,SecretKey secretKey)
    {
        try {
            return  Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }

    //gen key
    public SecretKey buildSecretKey(String raw) {
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
