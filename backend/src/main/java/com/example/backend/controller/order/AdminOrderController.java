package com.example.backend.controller.order;

import com.example.backend.dto.response.batch.BatchResult;
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

import java.util.List;
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

    @PutMapping("/orders/confirm")
    public ResponseEntity<?> confirmOrders(@RequestBody List<UUID> orderIds) {
        BatchResult<UUID> result = orderFacadeService.confirmOrders(orderIds);
        if (result.hasFailures()) {
            return ResponseEntity.status(207).body(result); // 207 nếu có lỗi một phần
        }
        return ResponseEntity.noContent().build(); // 204 nếu tất cả OK
    }
    @PutMapping("/orders/ship")
    public ResponseEntity<?> shipOrders(@RequestBody List<UUID> orderIds) {
        BatchResult<UUID> result = orderFacadeService.shipOrders(orderIds);
        if (result.hasFailures()) {
            return ResponseEntity.status(207).body(result); // 207 nếu có lỗi một phần
        }
        return ResponseEntity.noContent().build(); // 204 nếu tất cả OK
    }
    @GetMapping("/orders/shipment-print-url")
    public ResponseEntity<String> getPrintUrl(@RequestParam List<UUID> orderIds) {
        String printUrl = orderFacadeService.getPrintUrlForOrders(orderIds);
        return ResponseEntity.ok(printUrl);
    }
    @PutMapping("{orderId}/cancel")
    public ResponseEntity<?> cancelOrderByAdmin(@PathVariable("orderId") UUID orderId) {
        String message = orderFacadeService.cancelOrderByAdmin(orderId);
        return ResponseEntity.ok(message);
    }
}
