package com.example.backend.dto.ghn;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
@Builder
public class GHNItem {
    String name;
    int quantity;
    int price;
    int weight;

    public GHNItem(String productName, String sku, Integer quantity, int i, int i1) {
        this.name = productName + (sku != null ? " - " + sku : "");
        this.quantity = quantity;
        this.price = i;
        this.weight = i1;
    }
}
