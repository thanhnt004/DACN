package com.example.backend.controller.order;

import com.example.backend.dto.response.checkout.OrderResponse;
import com.example.backend.dto.response.wraper.PageResponse;
import com.example.backend.service.facade.OrderFacadeService;
import com.example.backend.service.order.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/v1/orders")
public class OrderController {
    private final OrderFacadeService orderFacadeService;

    @GetMapping("{orderId}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<OrderResponse> getOrderDetail(@PathVariable("orderId") UUID orderId,
            HttpServletRequest request,
            HttpServletResponse response) {
        return ResponseEntity.ok(orderFacadeService.getOrderDetail(orderId, request, response));
    }

}
