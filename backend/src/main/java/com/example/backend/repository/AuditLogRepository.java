package com.example.backend.repository;

import com.example.backend.model.AuditLog;
import com.example.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * Tìm audit logs theo entity ID
     */
    Page<AuditLog> findByEntityId(UUID entityId, Pageable pageable);

    /**
     * Tìm audit logs theo actor
     */
    Page<AuditLog> findByActor(User actor, Pageable pageable);

    /**
     * Tìm audit logs theo action
     */
    Page<AuditLog> findByAction(String action, Pageable pageable);

    /**
     * Tìm audit logs theo entity type
     */
    Page<AuditLog> findByEntityType(String entityType, Pageable pageable);

    /**
     * Tìm audit logs trong khoảng thời gian
     */
    @Query("SELECT a FROM AuditLog a WHERE a.createdAt BETWEEN :startDate AND :endDate ORDER BY a.createdAt DESC")
    Page<AuditLog> findByCreatedAtBetween(
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate,
        Pageable pageable
    );

    /**
     * Query phức tạp với nhiều filter
     */
    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:entityType IS NULL OR a.entityType = :entityType) AND " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:entityId IS NULL OR a.entityId = :entityId) AND " +
           "(:actorId IS NULL OR a.actor.id = :actorId) AND " +
           "(:startDate IS NULL OR a.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR a.createdAt <= :endDate) " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLog> findByFilters(
        @Param("entityType") String entityType,
        @Param("action") String action,
        @Param("entityId") UUID entityId,
        @Param("actorId") UUID actorId,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate,
        Pageable pageable
    );

    /**
     * Đếm số lượng audit logs của một user trong khoảng thời gian
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.actor.id = :actorId AND a.createdAt BETWEEN :startDate AND :endDate")
    long countByActorAndDateRange(
        @Param("actorId") UUID actorId,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
    );

    /**
     * Lấy các action gần nhất của một entity
     */
    List<AuditLog> findTop10ByEntityIdOrderByCreatedAtDesc(UUID entityId);
}

