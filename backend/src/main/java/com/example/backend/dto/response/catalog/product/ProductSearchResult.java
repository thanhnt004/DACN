package com.example.backend.dto.response.catalog.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for product search results with similarity score
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSearchResult {
    private UUID productId;
    private UUID embeddingId;
    private String content;
    private Double similarity;
    private ProductSummaryResponse product;
}

