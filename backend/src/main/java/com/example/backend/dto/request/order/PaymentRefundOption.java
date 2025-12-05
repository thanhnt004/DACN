package com.example.backend.dto.request.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRefundOption {
    private Method method;
    private Map<String, Object> data;
    public enum Method {
        BANK_TRANSFER,
        OTHER
    }
}
