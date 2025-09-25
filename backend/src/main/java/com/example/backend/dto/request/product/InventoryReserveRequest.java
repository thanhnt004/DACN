package com.example.backend.dto.request.product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class InventoryReserveRequest {
    @NotNull
    private UUID orderId;

    @NotNull
    @Min(1)
    private Integer quantity;

    private Instant holdUntil; // optional TTL
}
