package com.example.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductImageDto {
    private UUID id;
    @NotBlank
    private String imageUrl;

    private String alt;

    private int position = 0;

    private UUID colorId;

}
