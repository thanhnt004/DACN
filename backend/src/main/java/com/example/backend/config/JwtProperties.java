package com.example.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "app.jwt")
@Data
public class JwtProperties {
    private String issuer;
    private String alg; // "HS256" or "RS256"
    private String secret; // cho HS256 (hoặc keystore path cho RS256)
    private Map<TokenType, Long> ttlSeconds = new HashMap<>(); // ttl per type
}
