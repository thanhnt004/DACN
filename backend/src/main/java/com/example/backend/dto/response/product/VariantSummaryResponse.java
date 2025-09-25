package com.example.backend.dto.response.product;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class VariantSummaryResponse {
    private UUID id;
    private String sku;
    private Long priceAmount;
    private String status;
}