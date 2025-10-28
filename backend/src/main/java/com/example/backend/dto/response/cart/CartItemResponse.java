package com.example.backend.dto.response.cart;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CartItemResponse {
    private UUID id;
    private UUID productId;
    private String productName;
    private UUID variantId;
    private String variantName;
    private String imageUrl;

    private int quantity;
    private int stockQuantity;

    private long unitPriceAmount;
    private boolean isInStock;

}
