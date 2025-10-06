package com.example.backend.dto.response.product;

import com.example.backend.model.product.Gender;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
public class ProductView {
    private UUID id;
    private String imageUrl;
    private String name;
    private List<String> colors;
    private String gender;
    private String priceAmount;
    public ProductView(UUID id, String imageUrl, String name, Gender gender, Long priceAmount) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.name = name;
        this.gender = gender != null ? gender.toString() : null;
        this.priceAmount = priceAmount != null ? priceAmount.toString() : null;
        this.colors = null;
    }

    public ProductView(UUID id, String imageUrl, String name, List<String> colors, String gender, String priceAmount) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.name = name;
        this.colors = colors;
        this.gender = gender;
        this.priceAmount = priceAmount;
    }
}
