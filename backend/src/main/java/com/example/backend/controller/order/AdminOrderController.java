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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequestMapping(value = "/api/v1/admin/orders")
@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminOrderController {
    private final OrderFacadeService orderFacadeService;

    /**
     * Lấy danh sách đơn hàng theo tab filter
     * @param tab Tab filter: ALL, UNPAID, TO_CONFIRM, PROCESSING, SHIPPING, COMPLETED, CANCEL_REQ, CANCELLED, RETURN_REQ, REFUNDED
     * @param orderNumber Số đơn hàng (optional)
     * @param startDate Ngày bắt đầu (optional)
     * @param endDate Ngày kết thúc (optional)
     * @param pageable Phân trang
     * @return PageResponse of OrderResponse
     */
    @GetMapping("/get-order-list")
    public ResponseEntity<PageResponse<OrderResponse>> getOrderList(
            @RequestParam(value = "tab", defaultValue = "ALL") String tab,
            @RequestParam(required = false) String orderNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @PageableDefault(page = 0, size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable,
            HttpServletResponse response,
            HttpServletRequest request) {
        return ResponseEntity
                .ok(orderFacadeService.getOrderListByAdmin(tab, orderNumber, startDate, endDate, pageable));
    }

    @PutMapping("/orders/confirm")
    public ResponseEntity<?> confirmOrders(@RequestBody List<UUID> orderIds) {
        BatchResult<UUID> result = orderFacadeService.confirmOrders(orderIds);
        if (result.hasFailures()) {
            return ResponseEntity.status(207).body(result); // 207 nếu có lỗi một phần
        }
        return ResponseEntity.ok(result); // 200 OK trả về result
    }
    @PutMapping("/orders/ship")
    public ResponseEntity<?> shipOrders(@RequestBody List<UUID> orderIds) {
        log.info("[SHIP_ORDERS] Nhận yêu cầu giao {} đơn hàng: {}", orderIds.size(), orderIds);
        BatchResult<UUID> result = orderFacadeService.shipOrders(orderIds);
        log.info("[SHIP_ORDERS] Kết quả: Thành công={}, Thất bại={}", 
            result.getSuccessItems().size(), result.getFailedItems().size());
        if (result.hasFailures()) {
            log.warn("[SHIP_ORDERS] Các đơn thất bại: {}", result.getFailedItems());
            return ResponseEntity.status(207).body(result); // 207 nếu có lỗi một phần
        }
        return ResponseEntity.ok(result); // 200 OK trả về result
    }
    @GetMapping("/orders/shipment-print-url")
    public ResponseEntity<String> getPrintUrl(@RequestParam List<UUID> orderIds) {
        log.info("[PRINT_URL] Nhận yêu cầu lấy link in cho {} đơn hàng: {}", orderIds.size(), orderIds);
        try {
            String printUrl = orderFacadeService.getPrintUrlForOrders(orderIds);
            log.info("[PRINT_URL] Tạo link in thành công: {}", printUrl);
            return ResponseEntity.ok(printUrl);
        } catch (Exception e) {
            log.error("[PRINT_URL] Lỗi khi tạo link in cho orders: {}", orderIds, e);
            throw e;
        }
    }
    @Idempotent(expire = 300, scope = "requestId")
    @PostMapping("/requests/{requestId}/review")
    @PreAuthorize("hasRole('ADMIN')") // Hoặc 'STAFF'
    public ResponseEntity<Void> reviewRequest(
            @PathVariable("requestId") UUID requestId,
            @RequestBody ReviewRequestDTO dto // status: APPROVED/REJECTED, adminNote
    ) {
        orderFacadeService.reviewChangeRequest(requestId, dto);
        return ResponseEntity.ok().build();
    }
    @PutMapping("{orderId}/cancel")
    public ResponseEntity<?> cancelOrderByAdmin(
            @PathVariable("orderId") UUID orderId,
            @RequestParam(required = false) String adminNote) {
        orderFacadeService.cancelOrderByAdmin(orderId, adminNote);
        return ResponseEntity.noContent().build();
    }
    @PutMapping("{orderId}/return")
    public ResponseEntity<?> returnOrderByAdmin(
            @PathVariable("orderId") UUID orderId,
            @RequestParam(required = false) String adminNote) {
        orderFacadeService.returnOrderByAdmin(orderId, adminNote);
        return ResponseEntity.noContent().build();
    }
    @Idempotent(expire = 300, scope = "requestId")
    @PostMapping("/admin/confirm-refund/{requestId}")
    public ResponseEntity<?> confirmRefund(@PathVariable("requestId") UUID requestId, @RequestBody RefundConfirmRequest dto) {
        orderFacadeService.confirmManualRefund(requestId, dto.getImageProof(), dto.getNote());
        return ResponseEntity.ok(java.util.Map.of("message", "Đã xác nhận hoàn tiền"));
    }
}
