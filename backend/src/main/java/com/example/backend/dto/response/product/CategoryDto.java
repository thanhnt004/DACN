package com.example.backend.dto.response.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Builder
public class CategoryDto {
    private UUID id;
    private String name;
    private String slug;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    private UUID parentId;

    private List<CategoryDto> children = new ArrayList<>();

    // optional: metadata
    private int productCount;          // số lượng sản phẩm (nếu muốn)
}
