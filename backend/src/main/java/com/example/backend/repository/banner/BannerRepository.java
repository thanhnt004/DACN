package com.example.backend.repository.banner;

import com.example.backend.model.banner.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface BannerRepository extends JpaRepository<Banner, UUID> {

    /**
     * Find all active banners ordered by display_order
     */
    List<Banner> findByIsActiveTrueOrderByDisplayOrderAsc();

    /**
     * Find all banners ordered by display_order
     */
    List<Banner> findAllByOrderByDisplayOrderAsc();

    /**
     * Find currently valid banners (active + within date range)
     */
    @Query("""
        SELECT b FROM Banner b 
        WHERE b.isActive = true 
        AND (b.startDate IS NULL OR b.startDate <= :now) 
        AND (b.endDate IS NULL OR b.endDate >= :now)
        ORDER BY b.displayOrder ASC
        """)
    List<Banner> findCurrentlyValidBanners(Instant now);

    /**
     * Check if display order exists
     */
    boolean existsByDisplayOrder(Integer displayOrder);

    /**
     * Find by display order
     */
    List<Banner> findByDisplayOrder(Integer displayOrder);
}

