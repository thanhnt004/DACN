package com.example.backend.dto.response.checkout;

import com.example.backend.dto.response.catalog.VariantStockStatus;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class OrderItemDTO {
    //Optional
    private UUID id;

    private UUID variantId;
    private UUID productId;
    private String productName;
    private String variantName;
    private String sku;
    private Long unitPriceAmount;
    private int quantity;
    private Long totalAmount;

    private String imageUrl;
}
