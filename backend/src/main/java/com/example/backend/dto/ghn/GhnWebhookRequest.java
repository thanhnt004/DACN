package com.example.backend.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GhnWebhookRequest(
        @JsonProperty("OrderCode") String orderCode,     // Mã vận đơn (tracking_number)
        @JsonProperty("Status") String status,// Trạng thái GHN (ready_to_pick, picking, stored, delivering, delivered...)
        @JsonProperty("Type") String type,               // Loại webhook (switch_status)
        @JsonProperty("Time") Long time,                 // Unix timestamp
        @JsonProperty("CODAmount") Integer codAmount,
        @JsonProperty("Fee") Integer fee,
        @JsonProperty("Warehouse") String warehouse
) {}