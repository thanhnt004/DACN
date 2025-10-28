package com.example.backend.repository.cart;

import com.example.backend.model.cart.Cart;
import com.example.backend.model.cart.CartItem;
import com.example.backend.model.product.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    Optional<CartItem> findByCartAndVariant(Cart userCart, ProductVariant variant);

    Optional<CartItem> findByIdAndCart(UUID cartItemId, Cart cart);
}
