package com.example.backend.dto.response.catalog;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class BrandDto {
    private UUID id;

    @NotBlank(
            message = "Name is required"
    )
    private String name;

    @NotBlank(
            message = "Slug is required"
    )
    private String slug;

    private String description;

    private Integer productsCount;

    private Long totalSales;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
    public BrandDto(UUID id,String name,String slug, String description,Integer productsCount,LocalDateTime createdAt,LocalDateTime updatedAt)
    {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.productsCount = productsCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
