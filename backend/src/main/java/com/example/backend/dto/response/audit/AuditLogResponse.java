package com.example.backend.dto.response.audit;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditLogResponse {
    private UUID id;
    private ActorInfo actor;
    private String action;
    private String actionDescription;
    private String entityType;
    private String entityTypeDescription;
    private UUID entityId;
    private Map<String, Object> metadata;
    private String traceId;
    private Instant createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActorInfo {
        private UUID id;
        private String email;
        private String fullName;
        private String role;
    }
}

