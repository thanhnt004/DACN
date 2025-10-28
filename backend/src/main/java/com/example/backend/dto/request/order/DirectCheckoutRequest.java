package com.example.backend.dto.request.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class DirectCheckoutRequest {
    @NotNull
    private UUID variantId;

    @Min(1)
    private int quantity;
}
