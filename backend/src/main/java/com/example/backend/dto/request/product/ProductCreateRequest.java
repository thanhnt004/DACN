package com.example.backend.dto.request.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.UUID;

@Data
public class ProductCreateRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String slug;

    private UUID brandId;

    private String description;
    private String material;

    @Pattern(regexp = "men|women|unisex")
    private String gender;

    private String seoTitle;
    private String seoDescription;

    @Pattern(regexp = "ACTIVE|DRAFT|ARCHIVED")
    private String status = "DRAFT";
}