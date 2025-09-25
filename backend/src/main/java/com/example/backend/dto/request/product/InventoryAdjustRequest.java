package com.example.backend.dto.request.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InventoryAdjustRequest {
    @NotNull
    private Integer adjustment; // + tăng, - giảm

    @NotBlank
    private String reason;

    private String reference;
}
