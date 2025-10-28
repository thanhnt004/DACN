package com.example.backend.dto.request.order;

import com.example.backend.dto.response.user.UserAddress;
import lombok.Data;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Data
public class PlaceOrderRequest {
    @NotNull
    private UUID cartId;

    @NotNull
    @Valid
    private UserAddress shippingAddress;

    @NotEmpty
    private String paymentMethod; // (ví dụ: "COD", "VNPAY")

    private String discountCode; // (Optional)

    private String notes;
}