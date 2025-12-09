package com.example.backend.dto.request.catalog.product;

import com.example.backend.validate.ValidCompareAtPrice;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.UUID;

@Data
@ValidCompareAtPrice
public class VariantCreateRequest {
    // productId is provided via path parameter, not required in request body
    private UUID productId;

    @NotBlank
    private String sku;

    private String barcode;

    private UUID sizeId;
    private UUID colorId;

    @NotNull(message = "Giá bán không được null")
    @Positive(message = "Giá bán phải lớn hơn 0")
    private Long priceAmount;

    @PositiveOrZero(message = "Giá so sánh phải >= 0")
    private Long compareAtAmount;

    @PositiveOrZero(message = "Giá gốc phải >= 0")
    private Long historyCost;

    @Positive(message = "Cân nặng (grams) phải > 0")
    private Integer weightGrams = 200; // Default weight 200g

    @Pattern(regexp = "ACTIVE|DISCONTINUED")
    private String status = "ACTIVE";

    @Valid
    private InventoryRequest inventory;

    @Valid
    private ProductImageRequest image;
}
