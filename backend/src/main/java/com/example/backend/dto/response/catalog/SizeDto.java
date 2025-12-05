package com.example.backend.dto.response.catalog;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SizeDto
{
    private UUID id;
    @NotBlank
    private String code;
    @NotBlank
    private String name;
}
