package com.example.backend.dto.response.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductResponse {
    private UUID id;
    private String name;
    private String slug;
    private UUID brandId;
    private String description;
    private String material;
    private long priceAmount;
    private String gender;
    private String status;
    private String seoTitle;
    private String seoDescription;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Integer version;

    private List<CategoryDto> categories;
    private List<ProductImageResponse> images;
    private List<VariantSummaryResponse> variants;
}
