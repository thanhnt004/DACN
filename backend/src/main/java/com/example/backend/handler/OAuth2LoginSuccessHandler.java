package com.example.backend.handler;

import com.example.backend.excepton.AuthenticationException;
import com.example.backend.model.OAuthAccount;
import com.example.backend.model.User;
import com.example.backend.model.enumrator.Role;
import com.example.backend.repository.OAuthAccountRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.auth.RefreshTokenService;
import com.example.backend.service.auth.oautth.OAuthAccountService;
import com.example.backend.service.auth.oautth.StateTokenService;
import com.example.backend.util.CookieUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final UserRepository users;
    private final RefreshTokenService refreshTokenService;
    private final OAuthAccountRepository oauthAccounts;
    private final OAuthAccountService oauthService;
    private final StateTokenService stateTokens;
    @Value("${app.oauth2.provider}")
    private final Set<String> providers;
    private final CookieUtil cookieUtil;
    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        OAuth2User oUser = (OAuth2User) authentication.getPrincipal();
        String provider = inferProvider(request);

        // Attributes
        String providerUserId = extractProviderUserId(oUser, provider);
        String email = extractEmail(oUser, provider);
        String name = extractName(oUser, provider);
        String avatar = extractAvatar(oUser, provider);

        String redirectOverride = null;
        // neu de link tai khoan
        Optional<Cookie> linkStateCookie = readCookie(request, "oauth_link_state");
        // Process link state safely
        if (linkStateCookie.isPresent()) {
            var claims = stateTokens.parse(linkStateCookie.get().getValue());
            String mode = (String) claims.get("mode");
            String uid = (String) claims.get("uid");
            String prov = (String) claims.get("provider");
            redirectOverride = Optional.ofNullable((String) claims.get("redirect")).filter(s -> !s.isBlank()).orElse(null);
            if ("link".equals(mode) && provider.equals(prov)) {
                UUID linkUserId = UUID.fromString(uid);
                User targetUser = users.findById(linkUserId).orElseThrow(
                        ()->new AuthenticationException(401,"Not found user!")
                );
                // Thực hiện liên kết/idempotent
                oauthService.link(targetUser.getId(), provider, providerUserId, email, name);
                issueCookieAndRedirect(response, provider, targetUser, redirectOverride);
                cookieUtil.clearCookie(response, CookieUtil.OAUTH_STATE_COOKIE);
                return;
            }
        }
        // Đã liên kết
        Optional<OAuthAccount> byLink = oauthAccounts.findByProviderAndProviderUserId(provider, providerUserId);
        User user;
        if (byLink.isPresent()) {
            user = users.findById(byLink.get().getUserId()).orElseThrow(
                    ()->new AuthenticationException(401,"User not found")
            );
        } else {
            //cập nhật user theo email hoặc tạo mới
            if (email != null && !email.isBlank()) {
                user = users.findByEmailIgnoreCase(email).orElseGet(() -> User.builder()
                        .email(email)
                        .emailVerifiedAt(Instant.now())
                        .fullName(name)
                        .role(Role.CUSTOMER)
                        .avatarUrl(avatar)
                        .build());
            } else {
                user = User.builder()
                        .email(email)
                        .emailVerifiedAt(Instant.now())
                        .fullName(name)
                        .role(Role.CUSTOMER)
                        .avatarUrl(avatar)
                        .build();
            }
            users.save(user);
            // Ghi liên kết mới (idempotent)
            oauthService.link(user.getId(), provider, providerUserId, email, name);
        }
        // Issue refresh token cookie & redirect
        issueCookieAndRedirect(response, provider, user, redirectOverride);
    }

    private void issueCookieAndRedirect(HttpServletResponse response, String provider, User user, String redirectOverride) {
        String rawRefresh = refreshTokenService.createToken(user);
        Cookie cookie = cookieUtil.createRefreshTokenCookie(rawRefresh);
        response.addCookie(cookie);

        String url = redirectOverride
                + "?provider=" + URLEncoder.encode(provider, StandardCharsets.UTF_8);
        try {
            if(redirectOverride != null && !redirectOverride.isBlank())
                response.sendRedirect(url);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String inferProvider(HttpServletRequest request) {
        String registrationId = (String) request.getAttribute("org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter.CLIENT_REGISTRATION_ID");
        if (registrationId != null) return registrationId;
        String uri = request.getRequestURI();
        for(String provider:providers)
        {
            if (uri.contains(provider)) return provider;
        }
        return "oauth2";
    }

    private Optional<Cookie> readCookie(HttpServletRequest req, String name) {
        if (req.getCookies() == null) return Optional.empty();
        for (Cookie c : req.getCookies()) {
            if (name.equals(c.getName())) return Optional.of(c);
        }
        return Optional.empty();
    }

    private String extractProviderUserId(OAuth2User user, String provider) {
        Object id = user.getName();
        if (id != null) return id.toString();
        throw new IllegalStateException("Cannot get user id from provider! " + provider);
    }

    private String extractEmail(OAuth2User user, String provider) {
        Object email = user.getAttributes().get("email");
        return email != null ? email.toString() : null;
    }

    private String extractName(OAuth2User user, String provider) {
        Object name = user.getAttributes().get("name");
        if (name != null) return name.toString();
        //fallback for google
        if ("google".equals(provider)) {
            Object given = user.getAttributes().get("given_name");
            Object family = user.getAttributes().get("family_name");
            String n = ((given != null ? given : "") + " " + (family != null ? family : "")).trim();
            return n.isEmpty() ? null : n;
        }
        return null;
    }

    private String extractAvatar(OAuth2User user, String provider) {
        if ("google".equals(provider)) {
            Object pic = user.getAttributes().get("picture");
            return pic != null ? pic.toString() : null;
        }
        if ("github".equals(provider)) {
            Object pictureObj = user.getAttributes().get("picture");
            if (pictureObj instanceof Map<?, ?> pictureMap) {
                Object dataObj = pictureMap.get("data");
                if (dataObj instanceof Map<?, ?> dataMap) {
                    Object urlObj = dataMap.get("url");
                    if (urlObj != null) {
                        return urlObj.toString();
                    }
                }
            }
        }
        return null;
    }
}
