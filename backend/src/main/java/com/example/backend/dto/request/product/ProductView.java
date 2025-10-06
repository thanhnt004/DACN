package com.example.backend.dto.request.product;

import com.example.backend.model.product.Gender;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
public class ProductView {
    private UUID id;
    private String imageUrl;
    private String name;
    private List<String> colorHex;
    private Gender gender;
    private String price;
}
