package com.example.backend.controller.order;

import com.example.backend.dto.request.order.DirectCheckoutRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/v1")
public class OrderController {
    @PostMapping(value = "/checkout/direct")
    public void checkoutDirectly(DirectCheckoutRequest request) {
        // Implementation for direct checkout
    }
}
