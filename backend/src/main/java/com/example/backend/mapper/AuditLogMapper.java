package com.example.backend.mapper;

import com.example.backend.dto.response.audit.AuditLogResponse;
import com.example.backend.model.AuditLog;
import com.example.backend.model.enumrator.AuditActionType;
import com.example.backend.model.enumrator.AuditEntityType;
import org.springframework.stereotype.Component;

@Component
public class AuditLogMapper {

    public AuditLogResponse toResponse(AuditLog auditLog) {
        if (auditLog == null) {
            return null;
        }

        AuditLogResponse.ActorInfo actorInfo = null;
        if (auditLog.getActor() != null) {
            actorInfo = AuditLogResponse.ActorInfo.builder()
                    .id(auditLog.getActor().getId())
                    .email(auditLog.getActor().getEmail())
                    .fullName(auditLog.getActor().getFullName())
                    .role(auditLog.getActor().getRole().name())
                    .build();
        }

        // Get descriptions from enums
        String actionDescription = null;
        try {
            actionDescription = AuditActionType.valueOf(auditLog.getAction()).getDescription();
        } catch (IllegalArgumentException e) {
            actionDescription = auditLog.getAction();
        }

        String entityTypeDescription = null;
        try {
            entityTypeDescription = AuditEntityType.valueOf(auditLog.getEntityType()).getDescription();
        } catch (IllegalArgumentException e) {
            entityTypeDescription = auditLog.getEntityType();
        }

        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .actor(actorInfo)
                .action(auditLog.getAction())
                .actionDescription(actionDescription)
                .entityType(auditLog.getEntityType())
                .entityTypeDescription(entityTypeDescription)
                .entityId(auditLog.getEntityId())
                .metadata(auditLog.getMetadata())
                .traceId(auditLog.getTraceId())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }
}

