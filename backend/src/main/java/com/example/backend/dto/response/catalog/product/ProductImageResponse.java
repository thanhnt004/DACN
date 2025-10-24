package com.example.backend.dto.response.catalog.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductImageResponse {
    private UUID id;
    private String imageUrl;
    private String alt;
    private Integer position;
    private UUID colorId;
    private boolean isDefault;
}