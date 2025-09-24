package com.example.backend.service.auth.oautth;

import com.example.backend.dto.CustomUserDetail;
import com.example.backend.model.OAuthAccount;
import com.example.backend.repository.OAuthAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OAuthAccountService {
    private final OAuthAccountRepository repo;
    @Transactional
    public void link(UUID userId, String provider, String providerUserId,
                     String email, String displayName) {
        repo.findByProviderAndProviderUserId(provider, providerUserId).map(existing -> {
            if (!existing.getUserId().equals(userId)) {
                throw new IllegalStateException("Tài khoản " + provider + " này đã liên kết với người dùng khác.");
            }
            return null;
        }).orElseGet(() -> {
            // Ensure user hasn't linked this provider already
            if (repo.existsByUserIdAndProvider(userId, provider)) {
                throw new IllegalStateException("Người dùng đã liên kết " + provider + " trước đó.");
            }
            OAuthAccount e = OAuthAccount.builder()
                    .userId(userId)
                    .provider(provider)
                    .email(email)
                    .providerUserId(providerUserId)
                    .displayName(displayName)
                    .build();
            return repo.save(e);
        });
    }

    @Transactional
    public void unlink(CustomUserDetail userDetail, String provider) {
        var userId = userDetail.getUser().getId();
        OAuthAccount acc = repo.findByUserIdAndProvider(userId, provider)
                .orElseThrow(() -> new IllegalStateException("Chưa liên kết " + provider + " để hủy."));
        long linkedCount = repo.countByUserId(userId);
        boolean hasPassword = userDetail.getUser().getPasswordHash() != null && !userDetail.getUser().getPasswordHash().isBlank();
        if (!hasPassword && linkedCount <= 1) {
            throw new IllegalStateException("Không thể hủy liên kết vì đây là phương thức đăng nhập duy nhất.");
        }
        repo.delete(acc);
    }
}
