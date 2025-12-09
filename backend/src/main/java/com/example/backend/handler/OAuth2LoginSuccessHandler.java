package com.example.backend.handler;

import com.example.backend.exception.AuthenticationException;
import com.example.backend.model.OAuthAccount;
import com.example.backend.model.User;
import com.example.backend.model.enumrator.Role;
import com.example.backend.repository.auth.OAuthAccountRepository;
import com.example.backend.repository.user.UserRepository;
import com.example.backend.service.auth.RefreshTokenService;
import com.example.backend.service.auth.oautth.OAuthAccountService;
import com.example.backend.service.auth.oautth.StateTokenService;
import com.example.backend.util.CookieUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
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
    @Value("${app.frontend.base-url}")
    private String frontendUrl;
    private final CookieUtil cookieUtil;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) {
        OAuth2User oUser = (OAuth2User) authentication.getPrincipal();
        String provider = inferProvider(request);

        // Attributes
        String providerUserId = extractProviderUserId(oUser, provider);
        String email = extractEmail(oUser, provider);
        String name = extractName(oUser, provider);
        String avatar = extractAvatar(oUser, provider);

        String redirectOverride = null;
        boolean isLinkMode = false;
        UUID linkUserId = null;
        
        // neu de link tai khoan
        Optional<Cookie> linkStateCookie = cookieUtil.readCookie(request, "oauth_link_state");
        // Process link state safely
        if (linkStateCookie.isPresent()) {
            var claims = stateTokens.parse(linkStateCookie.get().getValue());
            String mode = (String) claims.get("mode");
            String uid = (String) claims.get("uid");
            String prov = (String) claims.get("provider");
            redirectOverride = Optional.ofNullable((String) claims.get("redirect")).filter(s -> !s.isBlank())
                    .orElse(null);
            if ("link".equals(mode) && provider.equals(prov)) {
                isLinkMode = true;
                linkUserId = UUID.fromString(uid);
                
                User targetUser = users.findById(linkUserId).orElseThrow(
                        () -> new AuthenticationException(401, "Not found user!"));
                
                // Log trước khi link
                log.info("BEFORE LINK - User: {}, Role: {}, Phone: {}, HasPassword: {}", 
                    targetUser.getEmail(), targetUser.getRole(), targetUser.getPhone(), 
                    targetUser.getPasswordHash() != null);
                
                try {
                    // Thực hiện liên kết/idempotent
                    oauthService.link(targetUser.getId(), provider, providerUserId, email, name);
                    
                    // Reload user from DB để đảm bảo dữ liệu mới nhất
                    targetUser = users.findById(linkUserId).orElseThrow(
                            () -> new AuthenticationException(401, "Not found user!"));
                    
                    // Log sau khi link
                    log.info("AFTER LINK - User: {}, Role: {}, Phone: {}, HasPassword: {}", 
                        targetUser.getEmail(), targetUser.getRole(), targetUser.getPhone(), 
                        targetUser.getPasswordHash() != null);
                    
                    issueCookieAndRedirect(response, provider, targetUser, redirectOverride);
                } catch (IllegalStateException e) {
                    // OAuth account already linked to different user
                    log.error("Link failed: {}", e.getMessage());
                    String errorUrl = (redirectOverride != null && !redirectOverride.isBlank())
                            ? redirectOverride
                            : frontendUrl + "/member/profile";
                    String errorMessage = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
                    String url = errorUrl + (errorUrl.contains("?") ? "&" : "?") + "error=" + errorMessage;
                    try {
                        cookieUtil.clearCookie(response, CookieUtil.OAUTH_STATE_COOKIE);
                        response.sendRedirect(url);
                    } catch (Exception ex) {
                        log.error("Failed to redirect after link error", ex);
                    }
                }
                cookieUtil.clearCookie(response, CookieUtil.OAUTH_STATE_COOKIE);
                return;
            }
        }
        // Đã liên kết
        Optional<OAuthAccount> byLink = oauthAccounts.findByProviderAndProviderUserId(provider, providerUserId);
        User user;
        if (byLink.isPresent()) {
            UUID existingUserId = byLink.get().getUserId();
            
            log.info("OAuth account found - Provider: {}, ExistingUserId: {}, isLinkMode: {}, linkUserId: {}", 
                provider, existingUserId, isLinkMode, linkUserId);
            
            // Nếu đang ở LINK mode mà OAuth account đã tồn tại
            if (isLinkMode) {
                // Nếu OAuth account thuộc về user khác (không phải user đang link)
                if (!existingUserId.equals(linkUserId)) {
                    log.error("SECURITY VIOLATION: User {} tried to link OAuth account that belongs to user {}", 
                        linkUserId, existingUserId);
                    throw new IllegalStateException("Tài khoản " + provider + " này đã được liên kết với tài khoản khác. Vui lòng sử dụng tài khoản " + provider + " khác.");
                }
                // Nếu OAuth account đã thuộc về chính user này → Idempotent, cho phép
                log.info("OAuth account already linked to same user - allowing idempotent operation");
            }
            
            user = users.findById(existingUserId).orElseThrow(
                    () -> new AuthenticationException(401, "User not found"));
        } else {
            // cập nhật user theo email hoặc tạo mới
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

    private void issueCookieAndRedirect(HttpServletResponse response, String provider, User user,
            String redirectOverride) {
        // Clear OAuth state cookie to prevent interference with future requests
        cookieUtil.clearCookie(response, CookieUtil.OAUTH_STATE_COOKIE);

        String rawRefresh = refreshTokenService.createToken(user);
        Cookie cookie = cookieUtil.createRefreshTokenCookie(rawRefresh);
        response.addCookie(cookie);

        // Use redirectOverride if provided, otherwise default to frontend login page
        String baseUrl = (redirectOverride != null && !redirectOverride.isBlank())
                ? redirectOverride
                : frontendUrl + "/login";
        
        String url;
        if (baseUrl.contains("?")) {
            url = baseUrl + "&provider=" + URLEncoder.encode(provider, StandardCharsets.UTF_8);
        } else {
            url = baseUrl + "?provider=" + URLEncoder.encode(provider, StandardCharsets.UTF_8);
        }


        try {
            response.sendRedirect(url);
        } catch (Exception e) {
            log.error("Failed to redirect after OAuth2 login", e);
            throw new AuthenticationException("Không thể chuyển hướng sau khi đăng nhập");
        }
    }

    private String inferProvider(HttpServletRequest request) {
        String registrationId = (String) request.getAttribute(
                "org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter.CLIENT_REGISTRATION_ID");
        if (registrationId != null)
            return registrationId;
        String uri = request.getRequestURI();
        for (String provider : providers) {
            if (uri.contains(provider))
                return provider;
        }
        return "oauth2";
    }

    private String extractProviderUserId(OAuth2User user, String provider) {
        Object id = user.getName();
        if (id != null)
            return id.toString();
        throw new IllegalStateException("Cannot get user id from provider! " + provider);
    }

    private String extractEmail(OAuth2User user, String provider) {
        Object email = user.getAttributes().get("email");
        return email != null ? email.toString() : null;
    }

    private String extractName(OAuth2User user, String provider) {
        Object name = user.getAttributes().get("name");
        if (name != null)
            return name.toString();
        // fallback for google
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
