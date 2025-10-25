package com.example.backend.dto.request.catalog.product;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class ProductImageRequest {
    private UUID id;
    @NotBlank
    private String imageUrl;

    private String alt;

    private int position = 0;

    private String publicId;

    private boolean isDefault;

    private UUID colorId;

}
