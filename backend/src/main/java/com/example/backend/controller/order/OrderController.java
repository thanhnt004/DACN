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
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/v1/orders")
public class OrderController {
    private final OrderFacadeService orderFacadeService;

    // @GetMapping(value = "/checkout/summary")
    // public ResponseEntity<CheckoutResponse> summary( r) {
    // return ResponseEntity.ok(orderService.directCheckOut(request));
    // }
    @GetMapping("/get-order-list")
    public ResponseEntity<PageResponse<OrderResponse>> getOrderList(@RequestParam(value = "status") String status,
            @RequestParam(value = "paymentType", required = false) String paymentType,
            @PageableDefault(page = 0, size = 20) Pageable pageable,
            HttpServletResponse response,
            HttpServletRequest request) {
        return ResponseEntity.ok(orderFacadeService.getOrderList(status, paymentType, pageable, request, response));
    }

    @GetMapping("{orderId}")
    public ResponseEntity<OrderResponse> getOrderDetail(@PathVariable("orderId") String orderId,
            HttpServletRequest request,
            HttpServletResponse response) {
        return ResponseEntity.ok(orderFacadeService.getOrderDetail(orderId, request, response));
    }

    @PostMapping("{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable("orderId") String orderId,
            HttpServletRequest request,
            HttpServletResponse response) {
        orderFacadeService.cancelOrder(orderId, request, response);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/merge-orders")
    public ResponseEntity<?> mergeOrders(
            HttpServletRequest request,
            HttpServletResponse response) {
        orderFacadeService.mergeOrders(request, response);
        return ResponseEntity.ok().build();
    }

}
