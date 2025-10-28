package com.example.backend.controller.cart;

import com.example.backend.dto.request.cart.CartItemRequest;
import com.example.backend.dto.request.cart.UpdateCartItemVariantRequest;
import com.example.backend.dto.response.cart.CartResponse;
import com.example.backend.service.cart.CartService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/v1/carts")
public class CartController {
    private final CartService cartService;
    @PostMapping
    public ResponseEntity<?> createCart(HttpServletResponse response)
    {
        UUID cardId = cartService.createCart(response);
        URI location = URI.create("/api/v1/carts/%s".formatted(cardId));
        return ResponseEntity.created(location).build();
    }
    @PostMapping("/merge")
    public ResponseEntity<CartResponse> mergeCart(
            @RequestHeader(name = "X-Cart-ID", required = false) UUID guestCartId
    ) {
        CartResponse mergedCart = cartService.mergeCarts(guestCartId);
        return ResponseEntity.ok(mergedCart);
    }
    @GetMapping
    public ResponseEntity<CartResponse> getCart(@RequestHeader(name = "X-Cart-ID", required = false) UUID guestCartId)
    {
        Optional<UUID> cartIdOptional = Optional.ofNullable(guestCartId);
        CartResponse cartResponse=cartService.getCart(cartIdOptional);
        return ResponseEntity.ok(cartResponse);
    }
    @PostMapping(value = "/items")
    public ResponseEntity<CartResponse> addItemToCart(@Valid @RequestBody CartItemRequest cartItem,
                                                      @RequestHeader(name = "X-Cart-ID", required = false) UUID guestCartId)
    {
        Optional<UUID> cartIdOptional = Optional.ofNullable(guestCartId);
        return ResponseEntity.ok( cartService.addItemToCart(cartItem,cartIdOptional));
    }
    @DeleteMapping(value = "/items/{itemId}")
    public ResponseEntity<CartResponse> removeItemFromCart(@PathVariable UUID itemId,
                                                @RequestHeader(name = "X-Cart-ID", required = false) UUID guestCartId)
    {
        Optional<UUID> cartIdOptional = Optional.ofNullable(guestCartId);
        CartResponse cartResponse=cartService.removeItemFromCart(itemId,cartIdOptional);
        return ResponseEntity.ok(cartResponse);
    }
    @DeleteMapping(value = "/items")
    public ResponseEntity<CartResponse> removeAllItems(@RequestHeader(name = "X-Cart-ID", required = false) UUID guestCartId)
    {
        Optional<UUID> cartIdOptional = Optional.ofNullable(guestCartId);
        CartResponse cartResponse=cartService.removeAllItem(cartIdOptional);
        return ResponseEntity.ok(cartResponse);
    }
    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> updateItem(
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateCartItemVariantRequest dto,
            @RequestHeader(name = "X-Cart-ID", required = false) UUID guestCartId
    ) {
        Optional<UUID> cartIdOpt = Optional.ofNullable(guestCartId);

        CartResponse updatedCart = cartService.updateCartItemVariant(
                itemId,
                dto,
                cartIdOpt
        );
        return ResponseEntity.ok(updatedCart);
    }
}
