package com.example.backend.dto.request.cart;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateCartItemVariantRequest {
    @NotNull
    private UUID newVariantId;
    @Positive
    private int newQuantity;
}
