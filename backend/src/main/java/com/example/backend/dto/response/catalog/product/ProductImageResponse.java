package com.example.backend.dto.response.catalog.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
public class ProductImageResponse {
    private UUID id;
    private String imageUrl;
    private String alt;
    private Integer position;
    
    @JsonProperty("isDefault")
    private boolean isDefault;
    
    private UUID colorId;

    private UUID variantId;
}