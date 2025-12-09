package com.example.backend.controller.catalog.product;

import com.example.backend.model.product.Gender;
import com.example.backend.model.product.ProductStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
public class ProductFilter {
    public static List<String> ALLOW_SORT_LIST = List.of("priceAmount", "createdAt", "similarity");
    private UUID categoryId;
    private UUID brandId;
    private Gender gender;
    private Long minPriceAmount;
    private Long maxPriceAmount;
    private List<UUID> sizeIds;
    private List<UUID> colorIds;
    private ProductStatus status;
    private String sortBy;
    private String direction;
    private String search; // Text search query for vector search
}