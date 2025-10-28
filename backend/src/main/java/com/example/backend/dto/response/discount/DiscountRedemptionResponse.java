package com.example.backend.dto.response.discount;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class DiscountRedemptionResponse {
        private UUID id;
        private UUID discountId;
        private UUID userId;
        private UUID orderId;
        private LocalDateTime redeemedAt;
}
