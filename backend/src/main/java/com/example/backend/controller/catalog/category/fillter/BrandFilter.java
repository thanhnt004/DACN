package com.example.backend.controller.catalog.category.fillter;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class BrandFilter {
    public static final List<String> ALLOWED_SORT_FIELDS = List.of("name", "createdAt", "updatedAt", "productsCount");
    private String name;
    private Integer minProduct;
    private Integer maxProduct;
    private String sortBy;
    private String direction;
}
