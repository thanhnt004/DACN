package com.example.backend.dto.response.catalog.category;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Builder
public class CategoryResponse {
    private UUID id;
    private String name;
    private String slug;
    private String description;
    private Integer productsCount;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
    private UUID parentId;
    @Builder.Default
    private List<CategoryResponse> children = new ArrayList<>();


}
