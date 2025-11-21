package com.example.backend.controller.order;

import com.example.backend.dto.response.checkout.OrderResponse;
import com.example.backend.dto.response.wraper.PageResponse;
import com.example.backend.model.order.Order;
import com.example.backend.service.facade.OrderFacadeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequestMapping(value = "/api/v1/admin/orders")
@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminOrderController {
    private final OrderFacadeService orderFacadeService;

    @GetMapping("/get-order-list")
    public ResponseEntity<PageResponse<OrderResponse>> getOrderList(@RequestParam(value = "status") String status,
            @RequestParam(value = "paymentType", required = false) String paymentType,
            @PageableDefault(page = 0, size = 20) Pageable pageable,
            HttpServletResponse response,
            HttpServletRequest request) {
        return ResponseEntity
                .ok(orderFacadeService.getOrderListByAdmin(status, paymentType, pageable, request, response));
    }

    @PutMapping("{orderId}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(@PathVariable("orderId") UUID orderId,
            @RequestParam("status") Order.OrderStatus status,
            HttpServletRequest request,
            HttpServletResponse response) {
        return ResponseEntity.ok(orderFacadeService.updateOrderStatusByAdmin(orderId, status, request, response));
    }
}
