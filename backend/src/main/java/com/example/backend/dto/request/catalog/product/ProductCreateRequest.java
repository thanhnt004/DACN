package com.example.backend.dto.request.catalog.product;

import com.example.backend.model.product.ProductStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ProductCreateRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String slug;

    private UUID brandId;

    private List<UUID> categoryId;

    private Long priceAmount; // Optional, can be set later with variants

    private String description;

    private String material;

    @Pattern(regexp = "men|women|unisex")
    private String gender;

    private ProductStatus status = ProductStatus.DRAFT; // Default to DRAFT when creating

    private String seoTitle;

    private String seoDescription;

    private List<ProductImageRequest> images;
}