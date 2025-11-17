package com.example.backend.dto.response.checkout;

import com.example.backend.dto.response.catalog.VariantStockStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutItemDetail {
    //Optional
    private UUID cartItemId;


    private UUID variantId;
    private UUID productId;
    private String productName;
    private String variantName; // VD: "Size 42 - Màu Đen"
    private String sku;
    private Long unitPriceAmount;
    private Long compareAtAmount;
    private int quantity;
    private Long totalAmount;
    private Integer weight;

    private String imageUrl;
    private VariantStockStatus stockStatus;

}