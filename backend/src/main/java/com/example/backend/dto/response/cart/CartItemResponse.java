package com.example.backend.dto.response.cart;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CartItemResponse {
    private UUID id;
    private int quantity;
    private long unitPriceAmount;
    private String productName;
    private CartItemResponse items;
    private boolean isInStock;
    private String imageUrl;
}
