package com.example.backend.controller.order;

import com.example.backend.aop.Idempotent;
import com.example.backend.dto.request.order.ReviewRequestDTO;
import com.example.backend.dto.request.refund.RefundConfirmRequest;
import com.example.backend.dto.response.batch.BatchResult;
import com.example.backend.dto.response.checkout.OrderResponse;
import com.example.backend.dto.response.wraper.PageResponse;
import com.example.backend.service.facade.OrderFacadeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequestMapping(value = "/api/v1/admin/orders")
@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminOrderController {
    private final OrderFacadeService orderFacadeService;

    @GetMapping("/get-order-list")
    public ResponseEntity<PageResponse<OrderResponse>> getOrderList(
            @RequestParam(value = "status") String status,
            @RequestParam(value = "paymentType", required = false) String paymentType,
            @RequestParam(required = false) String orderNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @PageableDefault(page = 0, size = 20) Pageable pageable,
            HttpServletResponse response,
            HttpServletRequest request) {
        return ResponseEntity
                .ok(orderFacadeService.getOrderListByAdmin(status, paymentType, orderNumber, startDate, endDate, pageable));
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
    @Idempotent(expire = 300, scope = "requestId")
    @PostMapping("/requests/{requestId}/review")
    @PreAuthorize("hasRole('ADMIN')") // Hoặc 'STAFF'
    public ResponseEntity<Void> reviewRequest(
            @PathVariable UUID requestId,
            @RequestBody ReviewRequestDTO dto // status: APPROVED/REJECTED, adminNote
    ) {
        orderFacadeService.reviewChangeRequest(requestId, dto);
        return ResponseEntity.ok().build();
    }
    @PutMapping("{orderId}/cancel")
    public ResponseEntity<?> cancelOrderByAdmin(@PathVariable("orderId") UUID orderId,@RequestBody (required = false) String adminNote) {
        orderFacadeService.cancelOrderByAdmin(orderId,adminNote);
        return ResponseEntity.noContent().build();
    }
    @PutMapping("{orderId}/return")
    public ResponseEntity<?> returnOrderByAdmin(@PathVariable("orderId") UUID orderId,@RequestBody (required = false) String adminNote) {
        orderFacadeService.returnOrderByAdmin(orderId,adminNote);
        return ResponseEntity.noContent().build();
    }
    @Idempotent(expire = 300, scope = "requestId")
    @PostMapping("/admin/confirm-refund/{requestId}")
    public ResponseEntity<?> confirmRefund(@PathVariable UUID requestId, @RequestBody RefundConfirmRequest dto) {
        orderFacadeService.confirmManualRefund(requestId, dto.getImageProof(), dto.getNote());
        return ResponseEntity.ok("Đã xác nhận hoàn tiền");
    }
}
