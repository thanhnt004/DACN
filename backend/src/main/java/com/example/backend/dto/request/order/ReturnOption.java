package com.example.backend.dto.request.order;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReturnOption {
    private String name;
    private String description;
}
