package com.example.backend.dto.response.checkout;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class OrderChangeRequestResponse {
    private UUID id;
    private String type;
    private String status;
    private String reason;
    private String adminNote;
    private Map<String,String> metadata;
    private List<String> images;
}
