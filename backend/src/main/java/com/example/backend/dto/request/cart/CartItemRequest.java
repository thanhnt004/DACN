package com.example.backend.dto.request.cart;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CartItemRequest {
    private UUID variantId;
    private int quantity;
}
