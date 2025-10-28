package com.example.backend.dto.response.cart;

import com.example.backend.model.cart.Cart;
import com.example.backend.model.cart.CartItem;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class CartResponse {
    private UUID id;
    private List<CartItemResponse> items;
    private Cart.CartStatus cartStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
