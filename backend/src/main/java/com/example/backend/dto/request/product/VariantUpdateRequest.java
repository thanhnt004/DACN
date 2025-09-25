package com.example.backend.dto.request.product;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VariantUpdateRequest {
    private String barcode;
    private Long priceAmount;
    private Long compareAtAmount;
    private String status;

    @NotNull
    private Integer version;
}