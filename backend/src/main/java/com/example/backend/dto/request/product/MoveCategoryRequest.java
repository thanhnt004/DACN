package com.example.backend.dto.request.product;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class MoveCategoryRequest {
    @NotNull
    private UUID newParentId;
}
