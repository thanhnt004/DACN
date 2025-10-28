package com.example.backend.repository.cart;

import com.example.backend.model.cart.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {

    List<Cart> findByUserId(UUID id);

    Optional<Cart> findByUserIdAndStatus(UUID id, Cart.CartStatus cartStatus);

    Optional<Cart> findByIdAndUserIdIsNullAndStatus(UUID uuid, Cart.CartStatus cartStatus);
}
