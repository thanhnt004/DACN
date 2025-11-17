package com.example.backend.dto.response.checkout;

import com.example.backend.dto.response.discount.DiscountRedemptionResponse;
import com.example.backend.dto.response.payment.PaymentDTO;
import com.example.backend.dto.response.user.UserAddress;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class OrderResponse {
    private UUID id;
    private String orderNumber;

    private UUID userId;

    private String status;

    private Long subtotalAmount;
    private Long discountAmount;
    private Long shippingAmount;
    private Long totalAmount;

    private UserAddress shippingAddress;

    private String notes;

    private Instant placedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant paidAt;

    private Integer version;

    private List<OrderItemDTO> items;
    private List<PaymentDTO> payments;
    private List<DiscountRedemptionResponse> discountRedemptions;
}
