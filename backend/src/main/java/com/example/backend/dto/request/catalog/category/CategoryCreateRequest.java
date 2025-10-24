package com.example.backend.dto.request.catalog.category;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryCreateRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String slug;

    private String description;

    private UUID parentId;
}
