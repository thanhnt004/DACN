package com.example.backend.dto.response.cart;

import com.example.backend.dto.response.catalog.VariantStockStatus;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CartItemResponse {
    private UUID id;
    private UUID variantId;
    private UUID productId;

    private String productName;
    private String variantName;
    private String sku;
    private String imageUrl;

    private long unitPriceAmount;
    private long compareAtAmount;
    private long totalAmount;

    private int quantity;
    //    private int stockQuantity;
    private VariantStockStatus stockStatus;
}
