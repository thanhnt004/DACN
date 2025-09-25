package com.example.backend.dto.response.product;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ProductResponse {
    private UUID id;
    private String name;
    private String slug;
    private UUID brandId;
    private String description;
    private String material;
    private String gender;
    private String status;
    private String seoTitle;
    private String seoDescription;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
    private Integer version;

    private List<ProductImageResponse> images;
    private List<VariantSummaryResponse> variants;
}
