package com.example.backend.dto.response.cart;

import com.example.backend.model.cart.Cart;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class CartResponse {
    private UUID id;
    @Builder.Default
    private List<CartItemResponse> items = new java.util.ArrayList<>();
    private Cart.CartStatus cartStatus;
    private Instant createdAt;
    private Instant updatedAt;
    @Builder.Default
    private List<String> warnings = new java.util.ArrayList<>();
}
