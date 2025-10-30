package com.example.backend.dto.request.order;

import com.example.backend.dto.response.user.UserAddress;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class DirectCheckoutRequest {
    @NotNull
    private UUID variantId;

    @Min(1)
    private int quantity;

    @NotNull
    @Valid
    UserAddress address;

    List<String> discountCode;

    String paymentMethod;
}
