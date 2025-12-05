package com.example.backend.dto.response.shipping;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ShipmentResponse {
    private UUID id;
    private String carrier;
    private String trackingNumber;
    private String status;
    private Instant deliveredAt;
    private String warehouse;
}
