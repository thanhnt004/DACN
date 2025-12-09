package com.example.backend.dto.response.catalog.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@AllArgsConstructor
@NoArgsConstructor
public class VariantResponse {
    private UUID id;
    private UUID productId;
    private String sku;
    private String barcode;
    private UUID sizeId;
    private UUID colorId;
    private SizeDto size; // Size details: id, name, code
    private ColorDto color; // Color details: id, name, hexCode
    private Long priceAmount;
    private Long historyCost;
    private Long compareAtAmount;
    private Integer weightGrams;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
    private Integer version;

    private InventoryResponse inventory;
    private ProductImageResponse image;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SizeDto {
        private UUID id;
        private String name;
        private String code;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColorDto {
        private UUID id;
        private String name;
        private String hexCode;
    }
}