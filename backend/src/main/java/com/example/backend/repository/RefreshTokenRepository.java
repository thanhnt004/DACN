package com.example.backend.repository;

import com.example.backend.model.RefreshToken;
import com.example.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.user = :user")
    int deleteByUser(@Param("user") User user);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.revoked = true, t.revokedReason = :reason WHERE t.tokenHash = :token")
    int revokeByToken(@Param("token") String token, @Param("reason") String reason);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt <= :now")
    void deleteAllExpiredTokens(@Param("now") Instant now);
}
