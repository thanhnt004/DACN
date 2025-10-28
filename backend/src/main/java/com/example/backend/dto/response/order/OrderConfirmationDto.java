package com.example.backend.dto.response.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.UUID;

@Data
@AllArgsConstructor
public class OrderConfirmationDto {
    private UUID orderId;
    private String orderNumber;
    private String orderStatus;
    private String paymentStatus;
    private String paymentUrl; // (Nếu cần chuyển hướng, ví dụ: VNPAY)
}