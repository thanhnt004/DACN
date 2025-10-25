package com.example.backend.dto.response.cart;

import com.example.backend.model.cart.Cart;
import com.example.backend.model.cart.CartItem;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class CartResponse {
    private UUID id;
    private UUID cartToken;
    private Cart.CartStatus status;
    private CartItemResponse cartItem;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
