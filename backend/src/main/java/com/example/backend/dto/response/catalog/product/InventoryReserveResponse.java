package com.example.backend.dto.response.catalog.product;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class InventoryReserveResponse {
    private UUID reservationId;
    private UUID variantId;
    private UUID orderId;
    private Integer quantity;
    private Instant reservedAt;
    private Instant holdUntil;
}
