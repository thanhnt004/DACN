package com.example.backend.dto.request.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.UUID;

@Data
public class VariantCreateRequest {
    @NotBlank
    private String sku;

    private String barcode;

    private UUID sizeId;
    private UUID colorId;

    @NotNull
    private Long priceAmount;

    private Long compareAtAmount;
    private String currency = "VND";

    private Integer weightGrams;

    @Pattern(regexp = "ACTIVE|DISCONTINUED")
    private String status = "ACTIVE";
}
