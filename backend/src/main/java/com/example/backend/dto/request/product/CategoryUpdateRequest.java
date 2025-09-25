package com.example.backend.dto.request.product;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CategoryUpdateRequest {
    @NotNull
    private UUID id;

    private String name;
    private String slug;
    private String description;
    private UUID parentId;
}
