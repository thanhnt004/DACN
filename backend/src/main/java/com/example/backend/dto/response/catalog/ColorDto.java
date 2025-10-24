package com.example.backend.dto.response.catalog;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ColorDto
{
    private UUID id;
    @NotBlank
    private String name;

    @NotBlank
    private String hexCode;
}
