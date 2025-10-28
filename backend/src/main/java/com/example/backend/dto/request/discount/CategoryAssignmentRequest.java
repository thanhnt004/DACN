package com.example.backend.dto.request.discount;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CategoryAssignmentRequest {
    @NotEmpty
    private List<UUID> categoryIds;
}
