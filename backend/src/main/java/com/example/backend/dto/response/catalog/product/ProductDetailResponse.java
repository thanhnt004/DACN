package com.example.backend.dto.response.catalog.product;

import com.example.backend.controller.catalog.product.Options;
import com.example.backend.dto.response.catalog.BrandDto;
import com.example.backend.dto.response.catalog.category.CategoryResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    private Options options;

    private UUID brandId;
    private List<CategoryResponse> categories;
    private List<ProductImageResponse> images;
    private List<VariantResponse> variants;

    private Instant createdAt;
    private Instant updatedAt;
}
