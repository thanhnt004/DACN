package com.example.backend.controller.banner;

import com.example.backend.dto.response.banner.BannerResponse;
import com.example.backend.service.banner.BannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/banners")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Banners", description = "Banner management APIs")
public class BannerController {

    private final BannerService bannerService;

    /**
     * PUBLIC: Get currently valid banners (for homepage display)
     */
    @GetMapping("/public")
    @Operation(summary = "Get currently valid banners for public display")
    public ResponseEntity<List<BannerResponse>> getPublicBanners() {
        log.info("Fetching public banners");
        List<BannerResponse> banners = bannerService.getCurrentlyValidBanners();
        return ResponseEntity.ok(banners);
    }
}

