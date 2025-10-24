package com.example.backend.dto.request.catalog.product;

import com.example.backend.validate.ValidCompareAtPrice;
import com.example.backend.validate.ValidateInventory;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.util.UUID;

@Data
@ValidateInventory
public class InventoryRequest {
    private UUID variantId;
    @PositiveOrZero
    private Integer quantityOnHand;
    @PositiveOrZero
    private Integer quantityReserved;
    @PositiveOrZero
    private Integer reorderLevel;
}
