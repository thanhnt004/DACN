package com.example.backend.dto.response.product;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ProductImageResponse {
    private UUID id;
    private String imageUrl;
    private String alt;
    private Integer position;
}