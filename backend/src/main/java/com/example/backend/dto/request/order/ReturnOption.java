package com.example.backend.dto.request.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnOption {
    private Type type;
    private String description;
    public enum Type {
        PICKUP,
        DROP_OFF
    }
}
