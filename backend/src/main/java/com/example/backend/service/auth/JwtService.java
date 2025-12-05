package com.example.backend.service.auth;

import com.example.backend.config.JwtProperties;
import com.example.backend.config.TokenType;
import com.example.backend.exception.auth.InvalidConfigurationException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.Keys;

import io.jsonwebtoken.security.SecureDigestAlgorithm;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

@Service
public class JwtService
{
    private final JwtProperties props;
    private final SecretKey signingKey; // build from props
    private final JwtParser parser;

    public JwtService(JwtProperties props) {
        this.props = props;
        this.signingKey = buildSecretKey(props);
        this.parser = Jwts.parser()
                .verifyWith(signingKey)
                .build();
    }

    public String createToken(TokenType type, String subject, Map<String, Object> extraClaims) {
        long ttl = props.getTtlSeconds().getOrDefault(type, 3600L);
        Instant now = Instant.now();
        JwtBuilder b = Jwts.builder()
                .issuer(props.getIssuer())
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttl)))
                .claim("typ", type.name()); // mark token type
        if (extraClaims != null) b.claims().add(extraClaims);

        return b.signWith(signingKey, Jwts.SIG.HS512).compact();
    }

    public Claims parseAndValidate(String token) throws JwtException {
        // sẽ ném exception nếu invalid/expired
        return parser.parseSignedClaims(token).getPayload();
    }
    public Optional<Claims> parseIfValid(String token) {
        try {
            return Optional.of(parseAndValidate(token));
        } catch (JwtException ex) {
            return Optional.empty();
        }
    }
    //gen key
    private SecretKey buildSecretKey(JwtProperties p) {
        String raw = props.getSecret();
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
            throw new InvalidConfigurationException("jwt.secret quá ngắn (<32 bytes)");
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
