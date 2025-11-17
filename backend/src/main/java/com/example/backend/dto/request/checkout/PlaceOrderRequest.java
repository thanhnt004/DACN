package com.example.backend.dto.request.checkout;

import com.example.backend.dto.response.user.UserAddress;
import lombok.AllArgsConstructor;
import lombok.Data;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
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