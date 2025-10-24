package com.example.backend.dto.request.catalog.product;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ProductUpdateRequest {
    @NotNull
    private UUID id;
    private String name;
    private String slug;
    private UUID brandId;
    private String description;
    private String material;
    @Positive
    private Integer priceAmount;
    private String gender;
    private String seoTitle;
    private String seoDescription;
    private List<ProductImageRequest> images;
    @NotNull
    private Integer version;
}
