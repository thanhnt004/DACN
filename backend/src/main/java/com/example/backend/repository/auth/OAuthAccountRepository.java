package com.example.backend.repository.auth;

import com.example.backend.model.OAuthAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, UUID> {

    Optional<OAuthAccount> findByProviderAndProviderUserId(String provider, String providerUserId);

    Optional<OAuthAccount> findByUserIdAndProvider(UUID userId, String provider);

    List<OAuthAccount> findByUserId(UUID userId);

    boolean existsByProviderAndProviderUserId(String provider, String providerUserId);

    boolean existsByUserIdAndProvider(UUID userId, String provider);

    long countByUserId(UUID userId);
}
