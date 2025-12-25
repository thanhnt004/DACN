package com.example.backend.dto.response.catalog.product;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ProductBulkImportResponse {
    private int totalRows;
    private int successCount;
    private int errorCount;
    private List<ImportResultItem> results;
    private List<String> errors;

    @Data
    @Builder
    public static class ImportResultItem {
        private int rowNumber;
        private String status; // SUCCESS, ERROR, SKIPPED
        private String productName;
        private String sku;
        private UUID productId;
        private UUID variantId;
        private String message;
        private List<String> errors;
    }
}
