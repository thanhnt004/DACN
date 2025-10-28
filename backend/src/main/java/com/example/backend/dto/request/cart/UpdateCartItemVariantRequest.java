package com.example.backend.dto.request.cart;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateCartItemVariantRequest {
    @NotNull
    private UUID newVariantId;
    private int newQuantity;
}
