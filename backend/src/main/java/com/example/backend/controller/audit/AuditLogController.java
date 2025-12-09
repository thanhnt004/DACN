package com.example.backend.controller.audit;

import com.example.backend.dto.response.audit.AuditLogResponse;
import com.example.backend.dto.response.wraper.PageResponse;
import com.example.backend.mapper.AuditLogMapper;
import com.example.backend.model.AuditLog;
import com.example.backend.service.audit.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Audit Log", description = "API quản lý audit logs - chỉ dành cho Admin")
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final AuditLogMapper auditLogMapper;

    @GetMapping
    @Operation(summary = "Lấy danh sách audit logs với filter",
               description = "Hỗ trợ filter theo entityType, action, entityId, actorId, và khoảng thời gian")
    public ResponseEntity<PageResponse<AuditLogResponse>> getAuditLogs(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        log.info("Getting audit logs with filters: entityType={}, action={}, entityId={}, actorId={}, startDate={}, endDate={}",
                entityType, action, entityId, actorId, startDate, endDate);

        Page<AuditLog> logs = auditLogService.getLogsByFilters(
                entityType, action, entityId, actorId, startDate, endDate, pageable
        );

        Page<AuditLogResponse> responsePage = logs.map(auditLogMapper::toResponse);

        return ResponseEntity.ok(new PageResponse<>(responsePage));
    }

    @GetMapping("/entity/{entityId}")
    @Operation(summary = "Lấy lịch sử thay đổi của một entity",
               description = "Trả về 10 thay đổi gần nhất của entity")
    public ResponseEntity<List<AuditLogResponse>> getEntityHistory(
            @PathVariable UUID entityId
    ) {
        log.info("Getting audit history for entity: {}", entityId);

        List<AuditLog> logs = auditLogService.getRecentLogsByEntityId(entityId);
        List<AuditLogResponse> responses = logs.stream()
                .map(auditLogMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/entity/{entityId}/paginated")
    @Operation(summary = "Lấy lịch sử thay đổi của một entity (có phân trang)",
               description = "Trả về lịch sử thay đổi của entity với phân trang")
    public ResponseEntity<PageResponse<AuditLogResponse>> getEntityHistoryPaginated(
            @PathVariable UUID entityId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        log.info("Getting paginated audit history for entity: {}", entityId);

        Page<AuditLog> logs = auditLogService.getLogsByEntityId(entityId, pageable);
        Page<AuditLogResponse> responsePage = logs.map(auditLogMapper::toResponse);

        return ResponseEntity.ok(new PageResponse<>(responsePage));
    }
}

