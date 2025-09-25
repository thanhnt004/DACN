package com.example.backend.dto.request.product;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class CategoryCreateRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String slug;

    private String description;

    // nếu muốn gán parent khi tạo
    private UUID parentId;
}
