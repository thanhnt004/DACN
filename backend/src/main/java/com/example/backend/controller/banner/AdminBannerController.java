package com.example.backend.controller.banner;

import com.example.backend.dto.request.banner.BannerCreateRequest;
import com.example.backend.dto.request.banner.BannerUpdateRequest;
import com.example.backend.dto.response.banner.BannerResponse;
import com.example.backend.service.banner.BannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/banners")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Banners", description = "Banner management APIs")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBannerController {

    private final BannerService bannerService;

    @GetMapping
    @Operation(summary = "Get all banners (Admin only)")
    public ResponseEntity<List<BannerResponse>> getAllBanners() {
        log.info("Admin fetching all banners");
        List<BannerResponse> banners = bannerService.getAllBanners();
        return ResponseEntity.ok(banners);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get banner by ID")
    public ResponseEntity<BannerResponse> getBannerById(@PathVariable UUID id) {
        log.info("Fetching banner with id: {}", id);
        BannerResponse banner = bannerService.getBannerById(id);
        return ResponseEntity.ok(banner);
    }

    @PostMapping
    @Operation(summary = "Create new banner (Admin only)")
    public ResponseEntity<BannerResponse> createBanner(
            @Valid @RequestBody BannerCreateRequest request) {
        log.info("Creating new banner: {}", request.getTitle());
        BannerResponse banner = bannerService.createBanner(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(banner);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update banner (Admin only)")
    public ResponseEntity<BannerResponse> updateBanner(
            @PathVariable UUID id,
            @Valid @RequestBody BannerUpdateRequest request) {
        log.info("Updating banner with id: {}", id);
        BannerResponse banner = bannerService.updateBanner(id, request);
        return ResponseEntity.ok(banner);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete banner (Admin only)")
    public ResponseEntity<Void> deleteBanner(@PathVariable UUID id) {
        log.info("Deleting banner with id: {}", id);
        bannerService.deleteBanner(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle-status")
    @Operation(summary = "Toggle banner active status (Admin only)")
    public ResponseEntity<BannerResponse> toggleBannerStatus(@PathVariable UUID id) {
        log.info("Toggling status for banner with id: {}", id);
        BannerResponse banner = bannerService.toggleBannerStatus(id);
        return ResponseEntity.ok(banner);
    }

    @PutMapping("/reorder")
    @Operation(summary = "Reorder banners by display order (Admin only)")
    public ResponseEntity<Void> reorderBanners(@RequestBody List<UUID> bannerIds) {
        log.info("Reordering {} banners", bannerIds.size());
        bannerService.reorderBanners(bannerIds);
        return ResponseEntity.ok().build();
    }
}
