package com.example.backend.controller.order;

import com.example.backend.dto.request.order.DirectCheckoutRequest;
import com.example.backend.dto.response.order.CheckoutResponse;
import com.example.backend.service.order.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/v1")
public class OrderController {
    private final OrderService orderService;

    @PostMapping(value = "/checkout/direct")
    public ResponseEntity<CheckoutResponse> checkoutDirectly(DirectCheckoutRequest request) {
        return ResponseEntity.ok(orderService.directCheckOut(request));
    }
//    @GetMapping(value = "/checkout/summary")
//    public ResponseEntity<CheckoutResponse> summary( r) {
//        return ResponseEntity.ok(orderService.directCheckOut(request));
//    }

}
