package com.example.backend.dto.request.discount;

import com.example.backend.model.discount.Discount;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class DiscountUpdateRequest {
    @NotBlank
    @Size(max = 100)
    String code;
    @NotBlank
    @Size(max = 255)
    String name;
    String description;
    @NotNull
    Discount.DiscountType type; // "PERCENTAGE", "FIXED_AMOUNT"
    @NotNull
    @Min(1)
    Integer value;
    Instant startsAt;
    Instant endsAt;
    @Min(1)
    Integer maxRedemptions;
    @Min(1)
    Integer perUserLimit;
    @Min(0)
    Long minOrderAmount;
    @NotNull
    @Builder.Default
    Boolean active = false;
}
