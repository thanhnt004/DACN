package com.example.backend.controller.admin;

import com.example.backend.crons.OrderCleanupCron;
import com.example.backend.crons.ProductEmbeddingCron;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/cron-jobs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin - Cron Jobs", description = "APIs để quản lý và kích hoạt thủ công các cron jobs")
@PreAuthorize("hasRole('ADMIN')")
public class CronJobController {

    private final ProductEmbeddingCron productEmbeddingCron;
    private final OrderCleanupCron orderCleanupCron;

    @PostMapping("/product-embedding/generate-missing")
    @Operation(summary = "Kích hoạt thủ công job tạo embedding cho sản phẩm chưa có")
    public ResponseEntity<Map<String, Object>> triggerGenerateMissingEmbeddings() {
        log.info("Manually triggering generate missing embeddings job");
        
        try {
            long startTime = System.currentTimeMillis();
            productEmbeddingCron.generateMissingEmbeddings();
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Job đã được kích hoạt thành công");
            response.put("jobName", "generateMissingEmbeddings");
            response.put("triggeredAt", LocalDateTime.now());
            response.put("duration", duration + "ms");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error triggering generate missing embeddings job", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi kích hoạt job: " + e.getMessage());
            response.put("jobName", "generateMissingEmbeddings");
            response.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/product-embedding/regenerate-outdated")
    @Operation(summary = "Kích hoạt thủ công job tạo lại embedding cho sản phẩm đã cập nhật")
    public ResponseEntity<Map<String, Object>> triggerRegenerateOutdatedEmbeddings() {
        log.info("Manually triggering regenerate outdated embeddings job");
        
        try {
            long startTime = System.currentTimeMillis();
            productEmbeddingCron.regenerateOutdatedEmbeddings();
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Job đã được kích hoạt thành công");
            response.put("jobName", "regenerateOutdatedEmbeddings");
            response.put("triggeredAt", LocalDateTime.now());
            response.put("duration", duration + "ms");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error triggering regenerate outdated embeddings job", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi kích hoạt job: " + e.getMessage());
            response.put("jobName", "regenerateOutdatedEmbeddings");
            response.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/order-cleanup/cancel-expired")
    @Operation(summary = "Kích hoạt thủ công job hủy đơn hàng hết hạn")
    public ResponseEntity<Map<String, Object>> triggerCancelExpiredOrders() {
        log.info("Manually triggering cancel expired orders job");
        
        try {
            long startTime = System.currentTimeMillis();
            orderCleanupCron.cancelExpiredOrders();
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Job đã được kích hoạt thành công");
            response.put("jobName", "cancelExpiredOrders");
            response.put("triggeredAt", LocalDateTime.now());
            response.put("duration", duration + "ms");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error triggering cancel expired orders job", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi kích hoạt job: " + e.getMessage());
            response.put("jobName", "cancelExpiredOrders");
            response.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/status")
    @Operation(summary = "Lấy danh sách tất cả các cron jobs và trạng thái")
    public ResponseEntity<Map<String, Object>> getCronJobsStatus() {
        Map<String, Object> response = new HashMap<>();
        
        Map<String, Object> jobs = new HashMap<>();
        
        // Product Embedding Jobs
        Map<String, Object> generateMissing = new HashMap<>();
        generateMissing.put("name", "Tạo embedding cho sản phẩm chưa có");
        generateMissing.put("schedule", "Mỗi giờ (0 0 * * * *)");
        generateMissing.put("endpoint", "/api/v1/admin/cron-jobs/product-embedding/generate-missing");
        generateMissing.put("method", "POST");
        jobs.put("generateMissingEmbeddings", generateMissing);
        
        Map<String, Object> regenerateOutdated = new HashMap<>();
        regenerateOutdated.put("name", "Tạo lại embedding cho sản phẩm đã cập nhật");
        regenerateOutdated.put("schedule", "Mỗi ngày lúc 2:00 AM (0 0 2 * * *)");
        regenerateOutdated.put("endpoint", "/api/v1/admin/cron-jobs/product-embedding/regenerate-outdated");
        regenerateOutdated.put("method", "POST");
        jobs.put("regenerateOutdatedEmbeddings", regenerateOutdated);
        
        // Order Cleanup Job
        Map<String, Object> cancelExpired = new HashMap<>();
        cancelExpired.put("name", "Hủy đơn hàng hết hạn");
        cancelExpired.put("schedule", "Mỗi phút (0 * * * * *)");
        cancelExpired.put("endpoint", "/api/v1/admin/cron-jobs/order-cleanup/cancel-expired");
        cancelExpired.put("method", "POST");
        jobs.put("cancelExpiredOrders", cancelExpired);
        
        response.put("jobs", jobs);
        response.put("totalJobs", jobs.size());
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
}
