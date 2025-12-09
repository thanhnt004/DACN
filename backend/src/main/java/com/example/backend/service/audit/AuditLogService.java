package com.example.backend.service.audit;

import com.example.backend.model.AuditLog;
import com.example.backend.model.User;
import com.example.backend.model.enumrator.AuditActionType;
import com.example.backend.model.enumrator.AuditEntityType;
import com.example.backend.repository.AuditLogRepository;
import com.example.backend.util.AuthenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Service để ghi lại audit log cho các hoạt động quan trọng trong hệ thống
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AuthenUtil authenUtil;

    /**
     * Ghi log một cách bất đồng bộ
     * Sử dụng @Async để không ảnh hưởng đến performance của business logic
     * Sử dụng REQUIRES_NEW để đảm bảo log được lưu ngay cả khi transaction chính rollback
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(
            AuditActionType action,
            AuditEntityType entityType,
            UUID entityId,
            Map<String, Object> metadata
    ) {
        try {
            User actor = authenUtil.getAuthenUser().orElse(null);

            AuditLog auditLog = AuditLog.builder()
                    .actor(actor)
                    .action(action.name())
                    .entityType(entityType.name())
                    .entityId(entityId)
                    .metadata(metadata)
                    .build();

            auditLogRepository.save(auditLog);

            log.info("Audit log created: action={}, entityType={}, entityId={}, actor={}",
                    action, entityType, entityId, actor != null ? actor.getEmail() : "SYSTEM");
        } catch (Exception e) {
            // Không throw exception để tránh ảnh hưởng đến business logic
            log.error("Failed to create audit log: action={}, entityType={}, entityId={}",
                    action, entityType, entityId, e);
        }
    }

    /**
     * Overload method với actor được chỉ định rõ (dùng cho system actions)
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(
            User actor,
            AuditActionType action,
            AuditEntityType entityType,
            UUID entityId,
            Map<String, Object> metadata
    ) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .actor(actor)
                    .action(action.name())
                    .entityType(entityType.name())
                    .entityId(entityId)
                    .metadata(metadata)
                    .build();

            auditLogRepository.save(auditLog);

            log.info("Audit log created: action={}, entityType={}, entityId={}, actor={}",
                    action, entityType, entityId, actor != null ? actor.getEmail() : "SYSTEM");
        } catch (Exception e) {
            log.error("Failed to create audit log: action={}, entityType={}, entityId={}",
                    action, entityType, entityId, e);
        }
    }

    /**
     * Lấy audit logs theo entity ID
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getLogsByEntityId(UUID entityId, Pageable pageable) {
        return auditLogRepository.findByEntityId(entityId, pageable);
    }

    /**
     * Lấy audit logs với nhiều filter
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getLogsByFilters(
            String entityType,
            String action,
            UUID entityId,
            UUID actorId,
            Instant startDate,
            Instant endDate,
            Pageable pageable
    ) {
        return auditLogRepository.findByFilters(
                entityType, action, entityId, actorId, startDate, endDate, pageable
        );
    }

    /**
     * Lấy lịch sử thay đổi gần nhất của một entity
     */
    @Transactional(readOnly = true)
    public java.util.List<AuditLog> getRecentLogsByEntityId(UUID entityId) {
        return auditLogRepository.findTop10ByEntityIdOrderByCreatedAtDesc(entityId);
    }
}

