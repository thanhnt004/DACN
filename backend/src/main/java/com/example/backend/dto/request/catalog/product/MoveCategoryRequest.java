package com.example.backend.dto.request.catalog.product;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class MoveCategoryRequest {
    @NotNull
    private UUID newParentId;
}
