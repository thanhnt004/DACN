package com.example.backend.dto.response.catalog.product;

import com.example.backend.model.product.Gender;
import com.example.backend.model.product.ProductStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

//To create product card in product listing pages
@Data
@NoArgsConstructor
public class ProductSummaryResponse {
    private UUID id;
    private String slug;
    private List<String> colors;
    private double ratingAvg;
    private String imageUrl;
    private String name;
    private ProductStatus status;
    private String gender;
    private long priceAmount;
    public ProductSummaryResponse(UUID id, String imageUrl, String name, Gender gender, Long priceAmount) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.name = name;
        this.gender = gender != null ? gender.toString() : null;
        this.priceAmount = priceAmount;
        this.colors = null;
    }

    public ProductSummaryResponse(UUID id, String imageUrl, String name, List<String> colors, String gender, Long priceAmount) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.name = name;
        this.colors = colors;
        this.gender = gender;
        this.priceAmount = priceAmount;
    }
}
