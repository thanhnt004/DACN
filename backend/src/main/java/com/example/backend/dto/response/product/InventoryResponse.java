package com.example.backend.dto.response.product;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class InventoryResponse {
    private UUID variantId;
    private Integer quantityOnHand;
    private Integer quantityReserved;
    private Integer available; // derived field
    private Integer reorderLevel;
    private Instant updatedAt;
}
