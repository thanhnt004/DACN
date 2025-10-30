package com.example.backend.dto.response.catalog.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class VariantResponse {
    private UUID id;
    private UUID productId;
    private String sku;
    private String barcode;
    private UUID sizeId;
    private UUID colorId;
    private Long priceAmount;
    private Long compareAtAmount;
    private Integer weightGrams;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Integer version;

    private InventoryResponse inventory;
}