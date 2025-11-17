package com.example.backend.dto.response.discount;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
@Builder
public class DiscountResponse {
    UUID id;
    String code;
    String name;
    String description;
    String type;
    Integer value;
    LocalDateTime startsAt;
    LocalDateTime endsAt;
    Integer maxRedemptions;
    Integer perUserLimit;
    Long minOrderAmount;
    boolean active;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    List<UUID> productIds;
    List<UUID> categoryIds;
}
