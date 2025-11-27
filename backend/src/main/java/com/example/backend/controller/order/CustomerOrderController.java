package com.example.backend.controller.order;

import com.example.backend.dto.request.order.ReturnOrderRequest;
import com.example.backend.dto.response.checkout.OrderResponse;
import com.example.backend.dto.response.wraper.PageResponse;
import com.example.backend.service.facade.OrderFacadeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/v1/orders")
public class CustomerOrderController {
    private final OrderFacadeService orderFacadeService;
    @GetMapping("/get-order-list")
    public ResponseEntity<PageResponse<OrderResponse>> getOrderList(@RequestParam(value = "status") String status,
                                                                    @RequestParam(value = "paymentType", required = false) String paymentType,
                                                                    @PageableDefault(page = 0, size = 20) Pageable pageable,
                                                                    HttpServletResponse response,
                                                                    HttpServletRequest request) {
        return ResponseEntity.ok(orderFacadeService.getOrderList(status, paymentType, pageable, request, response));
    }
    @PostMapping("{orderId}/cancel")
    public ResponseEntity<?> cancelOrderByCus(@PathVariable("orderId") UUID orderId,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        String message = orderFacadeService.cancelOrder(orderId, request, response);
        return ResponseEntity.ok(message);
    }
    @PostMapping("{orderId}/return")
    public ResponseEntity<?> returnOrderByCus(@PathVariable("orderId") UUID orderId,
                                              @RequestBody ReturnOrderRequest returnOrderRequest,
                                              HttpServletRequest request,
                                              HttpServletResponse response) {
        String message = orderFacadeService.returnOrder(orderId, request, response,returnOrderRequest);
        return ResponseEntity.ok(message);
    }
    @PostMapping("/merge-orders")
    public ResponseEntity<?> mergeOrders(
            HttpServletRequest request,
            HttpServletResponse response) {
        orderFacadeService.mergeOrders(request, response);
        return ResponseEntity.ok().build();
    }
}
