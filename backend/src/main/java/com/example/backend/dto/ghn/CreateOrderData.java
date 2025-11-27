package com.example.backend.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CreateOrderData {
    @JsonProperty("order_code") String orderCode;
    @JsonProperty("total_fee") int totalFee;
    @JsonProperty("expected_delivery_time") String expectedDeliveryTime;
}
