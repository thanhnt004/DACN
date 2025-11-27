package com.example.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "idempotency_keys")
public class IdempotencyKey {
    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false)
    private UUID id;

    @NotNull
    @Column(name = "key_value", nullable = false, length = Integer.MAX_VALUE)
    private String keyValue;

    @NotNull
    @Column(name = "status", nullable = false, length = Integer.MAX_VALUE)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "response_body")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> responseBody;

    @Column(name = "hash", length = Integer.MAX_VALUE)
    private String hash;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    public enum Status {
        PROCESSING, SUCCESS, FAILED
    }
}