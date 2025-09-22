package com.example.backend.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;
import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;

@UtilityClass
public class CookieUtil {

    @Value("${app.cookie.domain:localhost}")
    private String cookieDomain;

    @Value("${app.cookie.secure:false}")
    private boolean isSecure;

    @Value("${REFRESH_TOKEN_EXPIRATION}")
    private long maxAgeSeconds;
    public static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    public Cookie createRefreshTokenCookie(String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, refreshToken);
        cookie.setHttpOnly(true);  // Prevent XSS
        cookie.setSecure(isSecure); // HTTPS only in production
        cookie.setPath("/");
        cookie.setMaxAge((int) maxAgeSeconds);

        // Set domain if not localhost
        if (!"localhost".equals(cookieDomain)) {
            cookie.setDomain(cookieDomain);
        }

        // SameSite attribute (Spring Boot 2.6+)
        cookie.setAttribute("SameSite", "Strict");

        return cookie;
    }

    public Cookie createExpiredRefreshTokenCookie() {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(isSecure);
        cookie.setPath("/");
        cookie.setMaxAge(0); // Expire immediately

        if (!"localhost".equals(cookieDomain)) {
            cookie.setDomain(cookieDomain);
        }
        return cookie;
    }

    public String getRefreshTokenFromRequest(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> REFRESH_TOKEN_COOKIE.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}