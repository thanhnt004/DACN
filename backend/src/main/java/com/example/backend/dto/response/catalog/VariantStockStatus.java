package com.example.backend.dto.response.catalog;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantStockStatus {
    private Boolean inStock;

    private Integer availableQuantity;

    private String message;
}