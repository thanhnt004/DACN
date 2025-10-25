package com.example.backend.controller.cart;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping(value = "/api/v1/carts")
@RequiredArgsConstructor
public class GuessCartController {
    @PostMapping
    public ResponseEntity<?> createGuessCart(HttpServletResponse response)
    {
        createGuessCart(response);
        URI location = URI.create("/api/v1/carts/"+response.getHeader("Card-Id"));
        return ResponseEntity.created(location).build();
    }

}
