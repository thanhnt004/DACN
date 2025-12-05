package com.example.backend.dto.response.catalog.product;

import com.example.backend.model.product.Gender;
import com.example.backend.model.product.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

//To create product card in product listing pages
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSummaryResponse {
    private UUID id;
    private String slug;
    private List<String> colors;
    private List<String> sizes;
    private double ratingAvg;
    private String imageUrl;
    private String name;
    private ProductStatus status;
    private String gender;
    private long priceAmount;
}
