package com.example.backend.util;

import lombok.experimental.UtilityClass;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@UtilityClass
public class CryptoUtils {

    private final SecureRandom secureRandom = new SecureRandom();
    private static final String ALGORITHM = "SHA-256";

    public static String hash(String raw) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Hash token lỗi", e);
        }
    }
    //raw: base64url 256 bit
    public String generateSecureTokenRaw()
    {
        byte[] buf = new byte[32];
        secureRandom.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }
}