package com.example.backend.dto.request.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class DirectCheckoutRequest {
    @NotNull
    private UUID variantId;

    @Min(1)
    private int quantity;
}
