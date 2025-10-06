package com.example.backend.dto.response.product;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class InventoryResponse {
    private UUID variantId;
    private Integer quantityOnHand;
    private Integer quantityReserved;
    private Integer available;
    private Integer reorderLevel;
    private LocalDateTime updatedAt;
}
