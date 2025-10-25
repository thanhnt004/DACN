package com.example.backend.dto.request.catalog.product;

import com.example.backend.validate.ValidCompareAtPrice;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@ValidCompareAtPrice
public class VariantUpdateRequest {
    private String barcode;
    private Long priceAmount;
    private String sku;
    private Long compareAtAmount;
    @Pattern(regexp = "ACTIVE|DISCONTINUED")
    private String status;
    private Integer weightGrams;
    @NotNull
    private Integer version;

    private InventoryRequest inventory;
}