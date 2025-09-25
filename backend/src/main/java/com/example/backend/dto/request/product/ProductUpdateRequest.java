package com.example.backend.dto.request.product;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ProductUpdateRequest {
    private String name;
    private String slug;
    private UUID brandId;
    private String description;
    private String material;
    private String gender;
    private String seoTitle;
    private String seoDescription;
    private String status;

    @NotNull
    private Integer version; // để kiểm tra optimistic locking
}
