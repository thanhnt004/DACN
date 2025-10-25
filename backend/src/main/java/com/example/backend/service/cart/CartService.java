package com.example.backend.service.cart;

import com.example.backend.model.cart.Cart;
import com.example.backend.repository.cart.CartRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {
    private final CartRepository cartRepository;
    public void createGuessCart(HttpServletResponse response) {
        Cart cart = Cart.builder().build();
        cart = cartRepository.save(cart);
        response.addHeader("Card-Id", cart.getId().toString());
    }
}
