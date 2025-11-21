package com.example.backend.dto.request.catalog.product;

import com.example.backend.validate.ValidCompareAtPrice;
import com.example.backend.validate.ValidateInventory;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.util.UUID;

@Data
@ValidateInventory
public class InventoryRequest {
    private UUID variantId;
    @NotNull(message = "Quantity on hand is required")
    @PositiveOrZero
    private Integer quantityOnHand;
    @PositiveOrZero
    private Integer quantityReserved;
    @NotNull(message = "Reorder level is required")
    @PositiveOrZero
    private Integer reorderLevel;
}
