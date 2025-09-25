package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class SizeDto
{
    private UUID id;
    @NotBlank
    private String code;
    @NotBlank
    private String name;
}
