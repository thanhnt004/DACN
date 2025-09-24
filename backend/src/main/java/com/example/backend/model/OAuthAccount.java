package com.example.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "oauth_accounts",
        uniqueConstraints = {
                @UniqueConstraint(name = "ux_oauth_provider_uid", columnNames = {"provider", "provider_user_id"}),
                @UniqueConstraint(name = "ux_oauth_user_provider", columnNames = {"user_id", "provider"})
        },
        indexes = {
                @Index(name = "ix_oauth_accounts_user", columnList = "user_id"),
                @Index(name = "ix_oauth_accounts_provider", columnList = "provider, provider_user_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthAccount {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column(nullable = false, length = 32)
    private String provider; // google | github

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @Column(length = 255)
    private String email;

    @Column(name = "display_name")
    private String displayName;

    @Builder.Default
    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt = Instant.now();

    @Builder.Default
    @Column(name = "last_login_at")
    private Instant lastLoginAt = Instant.now();
}