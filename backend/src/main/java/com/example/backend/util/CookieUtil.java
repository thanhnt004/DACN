package com.example.backend.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class CookieUtil {

    @Value("${app.cookie.domain:localhost}")
    private String cookieDomain;

    @Value("${app.cookie.secure:false}")
    private boolean isSecure;

    @Value("${REFRESH_TOKEN_EXPIRATION}")
    private long maxAgeSeconds;
    public static final String REFRESH_TOKEN_COOKIE = "refreshToken";
    public static final String OAUTH_STATE_COOKIE = "oauth_link_state";
    //Refresh token
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
    //OAuth state
    public Cookie creatOAuthState(String state) {
        Cookie cookie = new Cookie("oauth_link_state", state);
        cookie.setHttpOnly(true);
        cookie.setSecure(isSecure);
        cookie.setPath("/");
        cookie.setDomain(cookieDomain);
        cookie.setMaxAge(300);
        // Set domain if not localhost
        if (!"localhost".equals(cookieDomain)) {
            cookie.setDomain(cookieDomain);
        }

        // SameSite=Lax allows cookie to be sent when redirecting back from OAuth provider
        // Strict would block the cookie on cross-site top-level navigation
        cookie.setAttribute("SameSite", "Lax");
        return cookie;
    }
    //Guest id
    //OAuth state
    public Cookie createGuestId(UUID guestId) {
        Cookie cookie = new Cookie("guest_id", guestId.toString());
        cookie.setHttpOnly(true);
        cookie.setSecure(isSecure);
        cookie.setPath("/");
        cookie.setDomain(cookieDomain);
        cookie.setMaxAge(2592000);
        // Set domain if not localhost
        if (!"localhost".equals(cookieDomain)) {
            cookie.setDomain(cookieDomain);
        }

        // SameSite attribute (Spring Boot 2.6+)
        cookie.setAttribute("SameSite", "Strict");
        return cookie;
    }
    //Clear cookie
    public void clearCookie(HttpServletResponse response,String cookieName)
    {
        Cookie cookie = new Cookie(cookieName, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(isSecure);
        cookie.setPath("/");
        cookie.setMaxAge(0); // Expire immediately

        if (!"localhost".equals(cookieDomain)) {
            cookie.setDomain(cookieDomain);
        }
        response.addCookie(cookie);
    }
    public Optional<Cookie> readCookie(HttpServletRequest req, String name) {
        if (req.getCookies() == null)
            return Optional.empty();
        for (Cookie c : req.getCookies()) {
            if (name.equals(c.getName()))
                return Optional.of(c);
        }
        return Optional.empty();
    }

}