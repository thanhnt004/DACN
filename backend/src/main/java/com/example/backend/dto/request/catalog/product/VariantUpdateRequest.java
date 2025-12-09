package com.example.backend.dto.request.catalog.product;

import com.example.backend.validate.ValidCompareAtPrice;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
@ValidCompareAtPrice
public class VariantUpdateRequest {
    private String barcode;
    private Long priceAmount;
    private String sku;
    private java.util.UUID sizeId;
    private java.util.UUID colorId;
    private Long compareAtAmount;
    @PositiveOrZero(message = "Giá gốc phải >= 0")
    private Long historyCost;
    @Pattern(regexp = "ACTIVE|DISCONTINUED")
    private String status;
    private Integer weightGrams;
    @NotNull
    private Integer version;

    private InventoryRequest inventory;

    private ProductImageRequest image;
}