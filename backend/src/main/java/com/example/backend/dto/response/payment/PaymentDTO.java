package com.example.backend.dto.response.payment;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class PaymentDTO {
    UUID id;
    String provider;
    String status;
    Long amount;
    String transactionId;
    Instant expireAt;
}
