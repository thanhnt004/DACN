package com.example.backend.dto.request.cart;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CartItemRequest {
    @NotNull
    private UUID variantId;
    @Positive
    private int quantity;
}
