package com.example.backend.dto.request.discount;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductAssignmentRequest {
    @NotEmpty
    private List<UUID> productIds;
}
