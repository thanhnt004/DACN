package com.example.backend.dto.request.product;

import com.example.backend.dto.ProductImageDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
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
    private Integer priceAmount;
    private String gender;
    private String seoTitle;
    private String seoDescription;
    private String status;
    private List<ProductImageDto> images;
    @NotNull
    private Integer version;
}
