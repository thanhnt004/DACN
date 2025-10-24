package com.example.backend.controller.catalog.product;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
public class ProductFilter {
    public static List<String> ALLOW_SORT_LIST = List.of("priceAmount","createdAt");
    private UUID categoryId;
    private UUID brandId;
    private Long minPriceAmount;
    private Long maxPriceAmount;
    private List<UUID> sizeIds;
    private List<UUID> colorIds;
    private String sortBy;
    private String direction;
}
