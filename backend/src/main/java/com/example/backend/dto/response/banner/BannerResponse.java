package com.example.backend.dto.response.banner;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BannerResponse {

    private UUID id;

    private String title;

    private String imageUrl;

    private String linkUrl;

    private String description;

    private Integer displayOrder;

    private Boolean isActive;

    private Instant startDate;

    private Instant endDate;

    private Instant createdAt;

    private Instant updatedAt;

    /**
     * Indicates if the banner is currently valid (active and within date range)
     */
    private Boolean isCurrentlyValid;
}

