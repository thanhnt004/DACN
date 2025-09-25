package com.example.backend.dto.response.product;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class VariantResponse {
    private UUID id;
    private UUID productId;
    private String sku;
    private String barcode;
    private UUID sizeId;
    private UUID colorId;
    private Long priceAmount;
    private Long compareAtAmount;
    private String currency;
    private Integer weightGrams;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
    private Integer version;

    private InventoryResponse inventory; // optional khi include=inventory
}