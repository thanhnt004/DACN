package com.example.backend.dto.request.product;

import com.example.backend.dto.ProductImageDto;
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

    @Positive
    @NotNull
    private long priceAmount;

    private String description;

    private String material;

    @Pattern(regexp = "men|women|unisex")
    private String gender;

    private String seoTitle;

    private String seoDescription;

    @Pattern(regexp = "ACTIVE|DRAFT|ARCHIVED")
    private String status = "DRAFT";

    private List<ProductImageDto> images;
}