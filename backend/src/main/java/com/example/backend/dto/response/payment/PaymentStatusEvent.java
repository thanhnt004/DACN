package com.example.backend.dto.response.payment;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Set;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentStatusEvent {
    private String orderId;
    private String status; // PENDING, PAID, FAILED, CANCELLED
    private String message;
    private String errorCode;
    private Instant timestamp;

    /**
     * Kiểm tra xem đây có phải trạng thái cuối cùng không
     * Để đóng SSE connection
     */
    public boolean isFinal() {
        return Set.of("PAID", "FAILED", "CANCELLED", "EXPIRED").contains(status);
    }
}
