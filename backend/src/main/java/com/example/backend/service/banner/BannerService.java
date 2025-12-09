package com.example.backend.service.banner;

import com.example.backend.config.CacheConfig;
import com.example.backend.dto.request.banner.BannerCreateRequest;
import com.example.backend.dto.request.banner.BannerUpdateRequest;
import com.example.backend.dto.response.banner.BannerResponse;
import com.example.backend.exception.banner.BannerNotFoundException;
import com.example.backend.mapper.BannerMapper;
import com.example.backend.model.banner.Banner;
import com.example.backend.repository.banner.BannerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BannerService {

    private final BannerRepository bannerRepository;
    private final BannerMapper bannerMapper;
    private final CacheConfig cacheConfig;

    /**
     * Get all banners (for admin)
     */
    @Cacheable(
        value = "#{@cacheConfig.bannerCache}",
        key = "'all'",
        unless = "#result == null || #result.isEmpty()"
    )
    @Transactional(readOnly = true)
    public List<BannerResponse> getAllBanners() {
        log.info("Fetching all banners");
        List<Banner> banners = bannerRepository.findAllByOrderByDisplayOrderAsc();
        return bannerMapper.toResponseList(banners);
    }

    /**
     * Get active banners (for public display)
     */
    @Cacheable(
        value = "#{@cacheConfig.bannerCache}",
        key = "'active'",
        unless = "#result == null || #result.isEmpty()"
    )
    @Transactional(readOnly = true)
    public List<BannerResponse> getActiveBanners() {
        log.info("Fetching active banners");
        List<Banner> banners = bannerRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        return bannerMapper.toResponseList(banners);
    }

    /**
     * Get currently valid banners (active + within date range)
     */
    @Cacheable(
        value = "#{@cacheConfig.bannerCache}",
        key = "'valid:' + T(java.time.Instant).now().toString().substring(0, 16)",
        unless = "#result == null || #result.isEmpty()"
    )
    @Transactional(readOnly = true)
    public List<BannerResponse> getCurrentlyValidBanners() {
        log.info("Fetching currently valid banners");
        List<Banner> banners = bannerRepository.findCurrentlyValidBanners(Instant.now());
        return bannerMapper.toResponseList(banners);
    }

    /**
     * Get banner by ID
     */
    @Cacheable(
        value = "#{@cacheConfig.bannerCache}",
        key = "#id",
        unless = "#result == null"
    )
    @Transactional(readOnly = true)
    public BannerResponse getBannerById(UUID id) {
        log.info("Fetching banner with id: {}", id);
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new BannerNotFoundException("Banner không tồn tại với ID: " + id));
        return bannerMapper.toResponse(banner);
    }

    /**
     * Create new banner
     */
    @Caching(evict = {
        @CacheEvict(value = "#{@cacheConfig.bannerCache}", allEntries = true)
    })
    public BannerResponse createBanner(BannerCreateRequest request) {
        log.info("Creating new banner with title: {}", request.getTitle());

        // Validate dates
        if (request.getStartDate() != null && request.getEndDate() != null) {
            if (request.getEndDate().isBefore(request.getStartDate())) {
                throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu");
            }
        }

        Banner banner = bannerMapper.toEntity(request);
        banner = bannerRepository.save(banner);

        log.info("Successfully created banner with id: {}", banner.getId());
        return bannerMapper.toResponse(banner);
    }

    /**
     * Update banner
     */
    @Caching(evict = {
        @CacheEvict(value = "#{@cacheConfig.bannerCache}", key = "#id"),
        @CacheEvict(value = "#{@cacheConfig.bannerCache}", allEntries = true)
    })
    public BannerResponse updateBanner(UUID id, BannerUpdateRequest request) {
        log.info("Updating banner with id: {}", id);

        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new BannerNotFoundException("Banner không tồn tại với ID: " + id));

        // Validate dates if both are provided in the request
        if (request.getStartDate() != null && request.getEndDate() != null) {
            if (request.getEndDate().isBefore(request.getStartDate())) {
                throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu");
            }
        }

        bannerMapper.updateFromDto(request, banner);
        banner = bannerRepository.save(banner);

        log.info("Successfully updated banner with id: {}", id);
        return bannerMapper.toResponse(banner);
    }

    /**
     * Delete banner (soft delete)
     */
    @Caching(evict = {
        @CacheEvict(value = "#{@cacheConfig.bannerCache}", key = "#id"),
        @CacheEvict(value = "#{@cacheConfig.bannerCache}", allEntries = true)
    })
    public void deleteBanner(UUID id) {
        log.info("Deleting banner with id: {}", id);

        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new BannerNotFoundException("Banner không tồn tại với ID: " + id));

        bannerRepository.delete(banner);
        log.info("Successfully deleted banner with id: {}", id);
    }

    /**
     * Toggle banner active status
     */
    @Caching(evict = {
        @CacheEvict(value = "#{@cacheConfig.bannerCache}", key = "#id"),
        @CacheEvict(value = "#{@cacheConfig.bannerCache}", allEntries = true)
    })
    public BannerResponse toggleBannerStatus(UUID id) {
        log.info("Toggling banner status for id: {}", id);

        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new BannerNotFoundException("Banner không tồn tại với ID: " + id));

        banner.setIsActive(!banner.getIsActive());
        banner = bannerRepository.save(banner);

        log.info("Successfully toggled banner status for id: {}. New status: {}", id, banner.getIsActive());
        return bannerMapper.toResponse(banner);
    }

    /**
     * Reorder banners
     */
    @Caching(evict = {
        @CacheEvict(value = "#{@cacheConfig.bannerCache}", allEntries = true)
    })
    public void reorderBanners(List<UUID> bannerIds) {
        log.info("Reordering {} banners", bannerIds.size());

        for (int i = 0; i < bannerIds.size(); i++) {
            UUID bannerId = bannerIds.get(i);
            Banner banner = bannerRepository.findById(bannerId)
                    .orElseThrow(() -> new BannerNotFoundException("Banner không tồn tại với ID: " + bannerId));
            banner.setDisplayOrder(i);
            bannerRepository.save(banner);
        }

        log.info("Successfully reordered banners");
    }
}

