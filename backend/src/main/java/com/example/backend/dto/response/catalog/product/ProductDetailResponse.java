package com.example.backend.dto.response.catalog.product;

import com.example.backend.dto.response.catalog.BrandDto;
import com.example.backend.dto.response.catalog.category.CategoryResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductDetailResponse {
    private UUID id;
    private String name;
    private String slug;
    private String description;
    private String material;
    private long priceAmount;
    private String gender;
    private String status;
    private String seoTitle;
    private String seoDescription;
    private Integer version;
    private BrandDto brand;

    private List<CategoryResponse> categories;
    private List<ProductImageResponse> images;
    private List<VariantResponse> variants;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
