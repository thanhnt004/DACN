package com.example.backend.service.facade;

import com.example.backend.dto.request.order.CancelOrderRequest;
import com.example.backend.dto.request.order.PaymentRefundOption;
import com.example.backend.dto.request.order.ReturnOrderRequest;
import com.example.backend.dto.request.order.ReviewRequestDTO;
import com.example.backend.dto.response.batch.BatchResult;
import com.example.backend.dto.response.checkout.*;
import com.example.backend.dto.response.discount.DiscountRedemptionResponse;
import com.example.backend.dto.response.payment.PaymentDTO;
import com.example.backend.dto.response.user.UserAddress;
import com.example.backend.dto.response.wraper.PageResponse;
import com.example.backend.exception.AuthenticationException;
import com.example.backend.exception.BadRequestException;
import com.example.backend.exception.ConflictException;
import com.example.backend.exception.NotFoundException;
import com.example.backend.exception.cart.CheckoutSessionNotFoundException;
import com.example.backend.exception.shipping.ShippingNotFoundException;
import com.example.backend.model.User;
import com.example.backend.model.enumrator.AuditActionType;
import com.example.backend.model.enumrator.AuditEntityType;
import com.example.backend.model.order.Order;
import com.example.backend.model.order.OrderChangeRequest;
import com.example.backend.model.order.OrderItem;
import com.example.backend.model.order.Shipment;
import com.example.backend.model.payment.Payment;
import com.example.backend.repository.order.OrderChangeRequestRepository;
import com.example.backend.service.MessageService;
import com.example.backend.service.audit.AuditLogService;
import com.example.backend.service.cart.CartService;
import com.example.backend.service.order.OrderService;
import com.example.backend.service.payment.PaymentService;
import com.example.backend.service.product.InventoryService;
import com.example.backend.service.product.ProductVariantService;
import com.example.backend.service.redis.RedisCheckoutStoreService;
import com.example.backend.service.shipping.ShippingService;
import com.example.backend.util.AuthenUtil;
import com.example.backend.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class OrderFacadeService {
    private final InventoryService inventoryService;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final CartService cartService;
    private final MessageService emailService;
    private final RedisCheckoutStoreService redisCheckoutStoreService;
    private final ProductVariantService variantService;
    private final ShippingService shipmentService;
    private final AuthenUtil authenUtil;
    private final CookieUtil cookieUtil;
    private final AuditLogService auditLogService;

    @Autowired
    @Lazy
    private OrderFacadeService self;
    private final OrderChangeRequestRepository requestRepository;
    private final List<Order.OrderStatus> CANCELABLE_STATUSES = List.of(
            Order.OrderStatus.PENDING,
            Order.OrderStatus.CONFIRMED);
    private final OrderChangeRequestRepository orderChangeRequestRepository;

    public OrderCreatedResponse confirmCheckoutSession(UUID sessionId, HttpServletRequest httpRequest,
            HttpServletResponse response, String notes) {
        try {
            CheckoutSession checkoutSession = redisCheckoutStoreService.findById(sessionId).orElseThrow(
                    () -> new CheckoutSessionNotFoundException());

            // Check if order already exists for this session
            if (checkoutSession.getOrderId() != null) {
                Order existingOrder = orderService.getOrderById(checkoutSession.getOrderId());
                Payment latestPayment = null;
                if (existingOrder.getPayments() != null && !existingOrder.getPayments().isEmpty()) {
                    latestPayment = existingOrder.getPayments().get(existingOrder.getPayments().size() - 1);
                }
                
                String existingPaymentUrl = null;
                if (latestPayment != null && latestPayment.getRawResponse() != null) {
                    existingPaymentUrl = (String) latestPayment.getRawResponse().get("paymentUrl");
                }

                return OrderCreatedResponse.builder()
                        .orderId(existingOrder.getId())
                        .orderNumber(existingOrder.getOrderNumber())
                        .status(existingOrder.getStatus().name())
                        .totalAmount(existingOrder.getTotalAmount())
                        .paymentId(latestPayment != null ? latestPayment.getId() : null)
                        .paymentMethod(latestPayment != null ? latestPayment.getProvider() : null)
                        .paymentUrl(existingPaymentUrl)
                        .build();
            }

            if (!checkoutSession.getCanConfirm())
                throw new BadRequestException("cannot confirm this checkout session");
            validateSessionItemsBeforeOrder(checkoutSession);
            // 3. Tạo Order
            Order order = orderService.createOrderFromSession(
                    checkoutSession, httpRequest, response, notes);

            log.info("Order created: orderId={}, orderNumber={}",
                    order.getId(), order.getOrderNumber());

            // 4. Đặt giữ hàng tồn kho
            inventoryService.reserveStockForOrder(order);
            log.debug("Inventory reserved for order: {}", order.getId());

            // 5. Validate payment method
            if (checkoutSession.getSelectedPaymentMethod() == null) {
                throw new BadRequestException("Payment method is required");
            }
            String paymentMethodId = checkoutSession.getSelectedPaymentMethod().getId();

            // 6. Tạo Payment record
            Payment payment = paymentService.createPayment(order, paymentMethodId);
            //tao shipping here if needed
            Shipment shipment = shipmentService.createShipmentForOrder(order,checkoutSession);
            // 7. Generate payment URL
            String paymentUrl = paymentService.generatePaymentUrl(order, payment, paymentMethodId);

            // Save paymentUrl to payment rawResponse for idempotency
            if (paymentUrl != null) {
                Map<String, Object> rawResponse = payment.getRawResponse();
                if (rawResponse == null) {
                    rawResponse = new HashMap<>();
                }
                rawResponse.put("paymentUrl", paymentUrl);
                payment.setRawResponse(rawResponse);
            }

            log.debug("Payment URL generated: orderId={}", order.getId());

            // 7. Xóa cart nếu source = CART
            if (checkoutSession.getCartId() != null) {
                cartService.clearCart(checkoutSession);
                log.debug("Cart cleared: cartId={}", checkoutSession.getCartId());
            }

            // 8. Update session with orderId instead of deleting
            checkoutSession.setOrderId(order.getId());
            redisCheckoutStoreService.save(checkoutSession);
            log.debug("Session updated with orderId: sessionId={}, orderId={}", sessionId, order.getId());

            // 9. Gửi email confirmation (async)
            if (order.getUser() != null)
                emailService.sendOrderConfirmationAsync(order);

            // // 10. Ghi log audit
            // auditService.logOrderCreation(userId, order.getId(), order.getTotalAmount());

            log.info("Order confirmation completed: orderId={}, orderNumber={}",
                    order.getId(), order.getOrderNumber());

            // Build response
            return buildOrderCreatedResponse(order, payment, paymentUrl);
        } catch (Exception e) {
            throw e;
        }
    }

    private void validateSessionItemsBeforeOrder(CheckoutSession session) {
        for (CheckoutItemDetail item : session.getItems()) {
            // Kiểm tra stock
            int availableStock = inventoryService.getAvailableStock(item.getVariantId());
            if (availableStock < item.getQuantity()) {
                throw new BadRequestException(
                        String.format(
                                "Sản phẩm %s không đủ hàng. Còn %d sản phẩm.",
                                item.getProductName(),
                                availableStock));
            }

            // Kiểm tra giá có thay đổi không
            var variant = variantService.getById(item.getProductId(), item.getVariantId());
            if (!variant.getPriceAmount().equals(item.getUnitPriceAmount())) {
                throw new ConflictException(
                        String.format(
                                "Giá sản phẩm %s đã thay đổi từ %d thành %d.",
                                item.getProductName(),
                                item.getUnitPriceAmount(),
                                variant.getPriceAmount()));
            }
        }
    }

    private OrderCreatedResponse buildOrderCreatedResponse(
            Order order,
            Payment payment,
            String paymentUrl) {
        Instant paymentExpiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);

        return OrderCreatedResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(String.valueOf(order.getStatus()))
                .totalAmount(order.getTotalAmount())
                .paymentId(payment.getId())
                .paymentMethod(payment.getProvider())
                .paymentUrl(paymentUrl)
                .returnUrl(buildReturnUrl(order.getId()))
                .paymentExpiresAt(paymentExpiresAt)
                .instructions(buildPaymentInstructions(payment.getProvider()))
                .createdAt(order.getCreatedAt())
                .build();
    }

    /**
     * Build return URL sau khi thanh toán
     */
    private String buildReturnUrl(UUID orderId) {
        String baseUrl = System.getenv().getOrDefault(
                "FRONTEND_BASE_URL",
                "http://localhost:3000");
        return String.format("%s/orders/%s/result", baseUrl, orderId);
    }

    /**
     * Build payment instructions
     */
    private OrderCreatedResponse.PaymentInstructions buildPaymentInstructions(
            String paymentMethod) {
        if ("VNPAY".equals(paymentMethod)) {
            return OrderCreatedResponse.PaymentInstructions.builder()
                    .type("REDIRECT")
                    .instructions("Bạn sẽ được chuyển đến trang thanh toán VNPay")
                    .build();
        } else if ("COD".equals(paymentMethod)) {
            return OrderCreatedResponse.PaymentInstructions.builder()
                    .type("BANK_INFO")
                    .instructions("Thanh toán khi nhận hàng. " +
                            "Vui lòng chuẩn bị tiền mặt khi shipper giao hàng.")
                    .build();
        }
        return null;
    }

    public PageResponse<OrderResponse> getOrderList(String status, String paymentType, Pageable pageable,
            HttpServletRequest request, HttpServletResponse response) {
        var userOps = authenUtil.getAuthenUser();
        var guestIdCookie = cookieUtil.readCookie(request, "guest_id");
        if (userOps.isEmpty() && guestIdCookie.isEmpty()) {
            return new PageResponse<>(Page.empty());
        }
        if (guestIdCookie.isPresent()) {
            UUID guestId = UUID.fromString(guestIdCookie.get().getValue());
            // reset cookie to extend expiration
            response.addCookie(cookieUtil.createGuestId(guestId));
            return new PageResponse<>(
                    orderService.getOrdersByUserOrGuest(userOps, guestId, status, paymentType, pageable)
                            .map(this::buildOrderResponse));
        }
        return new PageResponse<>(orderService.getOrdersByUserOrGuest(userOps, null, status, paymentType, pageable)
                .map(this::buildOrderResponse));
    }

    private OrderResponse buildOrderResponse(Order order) {

        List<OrderItemDTO> itemResponses = order.getItems().stream()
                .map(this::buildOrderItemResponse)
                .toList();
        List<PaymentDTO> paymentResponses = order.getPayments().stream()
                .map(payment -> PaymentDTO.builder()
                        .id(payment.getId())
                        .provider(payment.getProvider())
                        .status(String.valueOf(payment.getStatus()))
                        .amount(payment.getAmount())
                        .expireAt(payment.getExpireAt())
                        .build())
                .toList();
        List<DiscountRedemptionResponse> discountResponses = List.of(); // Placeholder

        // Lấy tất cả shipment của order
        var allShipments = shipmentService.getAllShipmentsByOrder(order.getId());

        // Tìm shipment trả hàng active
        var returnShipment = allShipments.stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsActive()) && Boolean.TRUE.equals(s.getIsReturnShipment()))
                .findFirst();

        // Tìm shipment giao hàng active
        var forwardShipment = allShipments.stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsActive()) && !Boolean.TRUE.equals(s.getIsReturnShipment()))
                .findFirst();

        // Ưu tiên hiển thị return shipment nếu có, ngược lại hiển thị forward shipment
        var shipmentOpt = returnShipment.isPresent() ? returnShipment : forwardShipment;
        
        var shipmentResponse = shipmentOpt.map(s -> com.example.backend.dto.response.shipping.ShipmentResponse.builder()
                .id(s.getId())
                .carrier(s.getCarrier())
                .trackingNumber(s.getTrackingNumber())
                .status(s.getStatus())
                .deliveredAt(s.getDeliveredAt())
                .warehouse(s.getWarehouse())
                .isReturn(Boolean.TRUE.equals(s.getIsReturnShipment()))
                .build()).orElse(null);

        OrderResponse.OrderResponseBuilder responseBuilder = OrderResponse.builder()
                .id(order.getId())
                .notes(order.getNotes())
                .orderNumber(order.getOrderNumber())
                .status(String.valueOf(order.getStatus()))
                .totalAmount(order.getTotalAmount())
                .placedAt(order.getPlacedAt())
                .userId(order.getUser() != null ? order.getUser().getId() : null)
                .discountAmount(order.getDiscountAmount())
                .paidAt(order.getPaidAt())
                .shippingAddress(order.getShippingAddress())
                .shippingAmount(order.getShippingAmount())
                .subtotalAmount(order.getSubtotalAmount())
                .items(itemResponses)
                .payments(paymentResponses)
                .version(order.getVersion())
                .updatedAt(order.getUpdatedAt())
                .discountRedemptions(discountResponses)
                .shipment(shipmentResponse);
        
        // Thêm thông tin change request nếu có
        OrderChangeRequestResponse changeRequest = getChangeRequestForOrder(order.getId());
        if (changeRequest != null) {
            responseBuilder.changeRequest(changeRequest);
        }

        return responseBuilder.build();
    }

    private OrderItemDTO buildOrderItemResponse(OrderItem orderItem) {
        return OrderItemDTO.builder()
                .id(orderItem.getId())
                .productId(orderItem.getProduct()==null?null:orderItem.getProduct().getId())
                .variantId(orderItem.getVariant()==null?null:orderItem.getVariant().getId())
                .sku(orderItem.getSku())
                .productName(orderItem.getProductName())
                .variantName(orderItem.getVariantName())
                .imageUrl(orderItem.getImageUrl())
                .quantity(orderItem.getQuantity())
                .unitPriceAmount(orderItem.getUnitPriceAmount())
                .totalAmount(orderItem.getTotalAmount())
                .build();
    }

    public void mergeOrders(HttpServletRequest request, HttpServletResponse response) {
        var userOps = authenUtil.getAuthenUser();
        var guestIdCookie = cookieUtil.readCookie(request, "guest_id");
        if (userOps.isEmpty())
            throw new AuthenticationException(401, "User not authenticated");
        if (guestIdCookie.isPresent()) {
            UUID guestId = UUID.fromString(guestIdCookie.get().getValue());
            // clear
            cookieUtil.clearCookie(response, "guest_id");
            orderService.mergeOrders(userOps.get(), guestId);
        }
    }

    public OrderResponse getOrderDetail(UUID orderId, HttpServletRequest request, HttpServletResponse response) {
        var guestIdCookie = cookieUtil.readCookie(request, "guest_id");
        Order order;
        if (guestIdCookie.isPresent()) {
            UUID guestId = UUID.fromString(guestIdCookie.get().getValue());
            // reset cookie to extend expiration
            response.addCookie(cookieUtil.createGuestId(guestId));
            order = orderService.getOrderById(orderId);
        } else {
            order = orderService.getOrderById(orderId);
        }
        return buildOrderResponse(order);
    }


    /**
     * Lấy danh sách đơn hàng theo tab filter cho admin
     * @param tab Tab filter (ALL, UNPAID, TO_CONFIRM, PROCESSING, SHIPPING, COMPLETED, CANCEL_REQ, CANCELLED, RETURN_REQ, REFUNDED)
     * @param orderNumber Số đơn hàng (optional)
     * @param startDate Ngày bắt đầu (optional)
     * @param endDate Ngày kết thúc (optional)
     * @param pageable Phân trang
     * @return PageResponse of OrderResponse
     */
    public PageResponse<OrderResponse> getOrderListByAdmin(String tab, String orderNumber,
                                                           LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return new PageResponse<>(orderService.getPageOrder(tab, orderNumber, startDate, endDate, pageable)
                .map(this::buildOrderResponse));
    }

    public BatchResult<UUID> confirmOrders(List<UUID> orderIds) {
        BatchResult<UUID> result = new BatchResult<UUID>();
        orderIds.forEach(orderId->{
            try {
                self.confirmSingleOrder(orderId);
                result.addSuccess(orderId);
            } catch (Exception e) {
                log.error("Failed to confirm order: {}", orderId, e);
                result.addFailure(orderId, e.getMessage());
            }
        });
        return result;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public BatchResult<UUID> shipOrders(List<UUID> orderIds) {
        BatchResult<UUID> result = new BatchResult<UUID>();
        orderIds.forEach(orderId->{
            String errorMsg = self.shipSingleOrderWithNewTransaction(orderId);
            if (errorMsg == null) {
                result.addSuccess(orderId);
            } else {
                result.addFailure(orderId, errorMsg);
            }
        });
        return result;
    }
    
    /**
     * Returns null if success, error message if failed
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String shipSingleOrderWithNewTransaction(UUID orderId) {
        try {
            shipSingleOrder(orderId);
            return null; // Success
        } catch (Exception e) {
            log.error("Failed to ship order: {}", orderId, e);
            return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName() + ": " + e.toString();
        }
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void confirmSingleOrder(UUID orderId) {
        Order order = orderService.getOrderById(orderId);
        Hibernate.initialize(order.getItems());
        if (!order.getStatus().equals(Order.OrderStatus.PENDING) &&
                !order.getStatus().equals(Order.OrderStatus.CONFIRMED)) {
            throw new BadRequestException("Order cannot be confirmed: " + order.getOrderNumber());
        }

        orderService.updateStatus(order, Order.OrderStatus.PROCESSING);
    }

    protected void shipSingleOrder(UUID orderId) {
        Order order = orderService.getOrderById(orderId);

        if (!order.getStatus().equals(Order.OrderStatus.PROCESSING)) {
            if (!order.getStatus().equals(Order.OrderStatus.RETURNING))
                throw new BadRequestException("Không thể tạo đơn giao hàng cho đơn hàng: " + order.getOrderNumber() + ". Trạng thái hiện tại: " + order.getStatus());
        }

        if (order.getShippingAddress() == null) {
            throw new BadRequestException("Đơn hàng " + order.getOrderNumber() + " không có địa chỉ giao hàng");
        }
        
        // Validate địa chỉ có đầy đủ thông tin province/district/ward
        UserAddress addr = order.getShippingAddress();
        if (addr.getProvince() == null || addr.getProvince().isBlank() ||
            addr.getDistrict() == null || addr.getDistrict().isBlank() ||
            addr.getWard() == null || addr.getWard().isBlank()) {
            throw new BadRequestException(
                String.format("Đơn hàng %s thiếu thông tin địa chỉ giao hàng (Tỉnh/Thành phố, Quận/Huyện, Phường/Xã). " +
                    "Vui lòng cập nhật địa chỉ đầy đủ trước khi tạo đơn vận chuyển. " +
                    "Địa chỉ hiện tại: Province=%s, District=%s, Ward=%s",
                    order.getOrderNumber(),
                    addr.getProvince(),
                    addr.getDistrict(),
                    addr.getWard()
                )
            );
        }

        // Lấy hoặc tạo shipment
        Shipment shipment;
        try {
            shipment = shipmentService.getCurrentShipmentByOrder(orderId);
            log.info("Tìm thấy shipment hiện tại cho order: {}", order.getOrderNumber());
        } catch (ShippingNotFoundException e) {
            log.info("Chưa có shipment cho order: {}, tạo mới", order.getOrderNumber());
            // Tạo shipment mới nếu chưa có
            shipment = shipmentService.createShipmentForOrderManual(order);
        }

        // Validate before creating GHN order
        if (shipment.getTrackingNumber() != null && !shipment.getTrackingNumber().isEmpty()) {
            throw new BadRequestException("Đơn hàng " + order.getOrderNumber() + " đã có mã vận đơn: " + shipment.getTrackingNumber());
        }

        try {
            shipmentService.createShippingOrderInGHN(shipment, order.getShippingAddress());
            orderService.updateStatus(order, Order.OrderStatus.SHIPPED);
            log.info("Đã tạo đơn giao hàng GHN thành công cho order: {}", order.getOrderNumber());
        } catch (Exception e) {
            log.error("Lỗi khi tạo đơn giao hàng GHN cho order: {}", order.getOrderNumber(), e);
            throw new BadRequestException("Không thể tạo đơn giao hàng: " + e.getMessage());
        }
    }
    public String getPrintUrlForOrders(List<UUID> orderIds) {
        log.info("[GET_PRINT_URL] Bắt đầu xử lý {} đơn hàng", orderIds.size());
        if (orderIds == null || orderIds.isEmpty()) {
            throw new BadRequestException("Danh sách đơn hàng trống");
        }
        
        if (orderIds.size() > 100) {
            throw new BadRequestException("Không thể in quá 100 đơn hàng cùng lúc");
        }
        
        List<Order> orders = orderIds.stream()
                .map(orderService::getOrderById)
                .toList();
        log.info("[GET_PRINT_URL] Đã tải thông tin {} đơn hàng", orders.size());
        
        // Kiểm tra các đơn hàng có shipment không
        for (Order order : orders) {
            log.debug("[GET_PRINT_URL] Kiểm tra đơn {}: status={}", order.getOrderNumber(), order.getStatus());
            if (order.getStatus() == Order.OrderStatus.PENDING || 
                order.getStatus() == Order.OrderStatus.CONFIRMED) {
                throw new BadRequestException("Đơn hàng " + order.getOrderNumber() + " chưa được giao hàng");
            }
        }
        
        try {
            log.info("[GET_PRINT_URL] Gọi shipmentService.getPrintUrl");
            String printUrl = shipmentService.getPrintUrl(orders);
            log.info("[GET_PRINT_URL] Tạo URL in đơn thành công: {}", printUrl);
            return printUrl;
        } catch (Exception e) {
            log.error("[GET_PRINT_URL] Lỗi khi tạo URL in đơn", e);
            throw new BadRequestException("Không thể tạo URL in đơn: " + e.getMessage());
        }
    }
    public void cancelOrderByAdmin(UUID orderId,String adminReason) {
        Order order = orderService.getOrderById(orderId);
        
        log.info("[CANCEL_ORDER] Admin hủy đơn hàng: {} ({}), trạng thái hiện tại: {}", 
            order.getOrderNumber(), orderId, order.getStatus());

        // Audit log: Admin hủy đơn hàng
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("order_number", order.getOrderNumber());
        metadata.put("old_status", order.getStatus().name());
        metadata.put("reason", adminReason);
        metadata.put("total_amount", order.getTotalAmount());
        metadata.put("is_paid", order.getPaidAt() != null);

        auditLogService.logAction(
            AuditActionType.CANCEL_ORDER,
            AuditEntityType.ORDER,
            order.getId(),
            metadata
        );

        // Tạo request để lưu lịch sử (đánh dấu là đã duyệt xong phần hủy)
        OrderChangeRequest request = createChangeRequestEntity(order, "CANCEL", adminReason, null, null, true);
        
        try {
            // Thực hiện hủy
            doCancelOrderLogic(order, request);
            log.info("[CANCEL_ORDER] Hủy đơn hàng thành công: {}", order.getOrderNumber());
        } catch (Exception e) {
            log.error("[CANCEL_ORDER] Lỗi khi hủy đơn hàng: {}", order.getOrderNumber(), e);
            throw new BadRequestException("Không thể hủy đơn hàng: " + e.getMessage());
        }
    }

    public void returnOrderByAdmin(UUID orderId,String adminReason) {
        Order order = orderService.getOrderById(orderId);
        if (order.getStatus()!= Order.OrderStatus.DELIVERED) {
            throw new BadRequestException("Chỉ có thể trả hàng cho đơn hàng đã giao");
        }

        // Audit log: Admin xử lý trả hàng
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("order_number", order.getOrderNumber());
        metadata.put("old_status", order.getStatus().name());
        metadata.put("reason", adminReason);
        metadata.put("total_amount", order.getTotalAmount());

        auditLogService.logAction(
            AuditActionType.RETURN_ORDER,
            AuditEntityType.ORDER,
            order.getId(),
            metadata
        );

        OrderChangeRequest request = createChangeRequestEntity(order, "CANCEL", adminReason, null, null, true);
        doReturnOrderLogic(order, request);
    }

    public void reviewChangeRequest(UUID requestId, ReviewRequestDTO dto) {
        OrderChangeRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new BadRequestException("Change request not found"));
        if (!request.getStatus().equals("PENDING")) {
            throw new BadRequestException("Yêu cầu không ở trạng thái chờ duyệt");
        }

        Order order = request.getOrder();

        // Audit log: Duyệt hoặc từ chối yêu cầu thay đổi
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("request_id", requestId.toString());
        metadata.put("request_type", request.getType());
        metadata.put("order_number", order.getOrderNumber());
        metadata.put("review_status", dto.getStatus());
        metadata.put("admin_note", dto.getAdminNote());
        metadata.put("customer_reason", request.getReason());

        if (dto.getStatus().equals("APPROVED")) {
            request.setStatus("APPROVED");
            auditLogService.logAction(
                AuditActionType.APPROVE_CHANGE_REQUEST,
                AuditEntityType.ORDER_CHANGE_REQUEST,
                requestId,
                metadata
            );

            if ("CANCEL".equals(request.getType())) {
                doCancelOrderLogic(order, request);
            }
            else if ("RETURN".equals(request.getType())) {
                doReturnOrderLogic(order, request);
            }
        } else if (dto.getStatus().equals("REJECTED")) {
            auditLogService.logAction(
                AuditActionType.REJECT_CHANGE_REQUEST,
                AuditEntityType.ORDER_CHANGE_REQUEST,
                requestId,
                metadata
            );

            orderService.updateStatus(request.getOrder(), request.getOrder().getPreviousStatus());
            request.setStatus("REJECTED");
        } else {
            throw new BadRequestException("Invalid status");
        }
        request.setAdminNote(dto.getAdminNote());
        requestRepository.save(request);
    }
    @Transactional
    public void confirmManualRefund(UUID requestId, String transactionImage, String transactionNote) {
        OrderChangeRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new BadRequestException("Request not found"));

        // Chỉ xác nhận cho các yêu cầu đang chờ hoàn tiền
        if (!"WAITING_REFUND".equals(request.getStatus())) {
            throw new BadRequestException("Yêu cầu không nằm trong danh sách chờ hoàn tiền");
        }

        request.setStatus("COMPLETED"); // Hoàn tất toàn bộ quy trình
        request.setAdminNote(request.getAdminNote() + "\n[Refund Log]: " + transactionNote);

        // Thêm ảnh bằng chứng chuyển khoản
        Map<String,Object> metadata = request.getMetadata();
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put("refundTransactionImage", transactionImage);
        request.setMetadata(metadata);

        requestRepository.save(request);
    }

    public void returnOrderByCus(UUID orderId, HttpServletRequest request, HttpServletResponse response, ReturnOrderRequest returnOrderRequest) {
        var userOps = authenUtil.getAuthenUser();
        var guestIdCookie = cookieUtil.readCookie(request, "guest_id");
        if (userOps.isEmpty() && guestIdCookie.isEmpty()) {
            throw new AuthenticationException(401, "User not authenticated");
        }
        UUID guestId = null;
        String reason = returnOrderRequest.getReason();
        List<String> images = returnOrderRequest.getImages();
        PaymentRefundOption paymentRefundOption = returnOrderRequest.getPaymentRefundOption();
        UserAddress returnAddress = returnOrderRequest.getReturnAddress();
        if (guestIdCookie.isPresent())
            {
                guestId = UUID.fromString(guestIdCookie.get().getValue());
                // reset cookie to extend expiration
                response.addCookie(cookieUtil.createGuestId(guestId));
            }
        Order order = orderService.getOrderDetailByUserOrGuest(userOps, guestId, orderId);
        if (order.getStatus()!= Order.OrderStatus.DELIVERED) {
            throw new BadRequestException("Chỉ có thể yêu cầu trả hàng cho đơn hàng đã giao");}
        Shipment shipment = shipmentService.getCurrentShipmentByOrder(orderId);
            // 2. Tạo yêu cầu duyệt
        if (shipment.getDeliveredAt().plus(7,ChronoUnit.DAYS).isBefore(Instant.now())) {
                throw new BadRequestException("Đơn hàng đã quá hạn trả hàng (7 ngày kể từ ngày giao)");
        }
        List<String> activeStatuses = List.of("PENDING", "WAITING_REFUND", "APPROVED");
        if (requestRepository.existsByOrderIdAndStatusIn(orderId, activeStatuses)) {
            throw new BadRequestException("Đơn hàng đã có yêu cầu trả/hủy đang xử lý");
        }
        orderService.updateStatus(order, Order.OrderStatus.RETURNING);
        Map<String, Object> metadata = Map.of(
                "refundMethod", paymentRefundOption.getMethod(),
                "refundData", paymentRefundOption.getData(),
                "returnAddress", returnAddress
        );
        createChangeRequestEntity(order, "RETURN", reason, images, metadata, false);
    }
    public void cancelOrderByCus(UUID orderId, HttpServletRequest request, HttpServletResponse response, @Valid CancelOrderRequest cancelOrderRequest) {
        var userOps = authenUtil.getAuthenUser();
        var guestIdCookie = cookieUtil.readCookie(request, "guest_id");
        if (userOps.isEmpty() && guestIdCookie.isEmpty()) {
            throw new AuthenticationException(401, "User not authenticated");
        }
        UUID guestId = null;
        String reason = cancelOrderRequest.getReason();
        PaymentRefundOption paymentRefundOption = cancelOrderRequest.getPaymentRefundOption();
        if (guestIdCookie.isPresent())
        {
            guestId = UUID.fromString(guestIdCookie.get().getValue());
            // reset cookie to extend expiration
            response.addCookie(cookieUtil.createGuestId(guestId));
        }
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("returnAddress", null); // HashMap cho phép value null

        if (paymentRefundOption != null&& orderService.getOrderById(orderId).getPaidAt()!=null)
         {
        metadata.put("refundMethod", paymentRefundOption.getMethod());
        metadata.put("refundData", paymentRefundOption.getData());
        }
        Order order = orderService.getOrderDetailByUserOrGuest(userOps, guestId, orderId);
        if (order.getStatus().ordinal() > Order.OrderStatus.SHIPPED.ordinal())
        {
            throw new BadRequestException("Chỉ có thể yêu cầu hủy đơn hàng chưa được giao");
        }
        if (order.getStatus().ordinal() < Order.OrderStatus.SHIPPED.ordinal())
        {
            //cancel immediately
            OrderChangeRequest orderChangeRequest = createChangeRequestEntity(order,"CANCEL", reason,null,metadata, false);
            doCancelOrderLogic(order, orderChangeRequest);
            return;
        }

        createChangeRequestEntity(order,"CANCEL", reason,null,metadata, false);
        orderService.updateStatus(order, Order.OrderStatus.CANCELING);

    }
    public OrderChangeRequestResponse getChangeRequestForOrder(UUID orderId) {
        OrderChangeRequest orderChangeRequest = orderChangeRequestRepository.findByOrderId(orderId).orElse(null);
        if (orderChangeRequest == null) {
            return null;
        }
        return OrderChangeRequestResponse.builder()
                .id(orderChangeRequest.getId())
                .type(orderChangeRequest.getType())
                .images(orderChangeRequest.getImages())
                .reason(orderChangeRequest.getReason())
                .status(orderChangeRequest.getStatus())
                .adminNote(orderChangeRequest.getAdminNote())
                .metadata(orderChangeRequest.getMetadata())
                .build();
    }
    public Object rePay(UUID orderId) {
        log.info("Re-pay request for orderId: {}", orderId);
        Order order = orderService.getOrderById(orderId);

        log.info("Order status: {}, isPaid: {}, createdAt: {}", order.getStatus(), order.isPaid(), order.getCreatedAt());

        if (order.getStatus() != Order.OrderStatus.PENDING) {
            log.warn("Cannot re-pay: order status is {}", order.getStatus());
            throw new BadRequestException("Chỉ có thể thanh toán lại cho đơn hàng đang chờ xử lý.");
        }
        if (order.isPaid()) {
            log.warn("Cannot re-pay: order already paid");
            throw new BadRequestException("Đơn hàng đã được thanh toán.");
        }
        
        Instant expirationTime = order.getCreatedAt().plus(30, ChronoUnit.MINUTES);
        log.info("Expiration time: {}, current time: {}, expired: {}", expirationTime, Instant.now(), expirationTime.isBefore(Instant.now()));
        
        if (expirationTime.isBefore(Instant.now())) {
            log.warn("Cannot re-pay: payment window expired");
            throw new BadRequestException("Đã hết thời gian thanh toán cho đơn hàng này.");
        }

        Payment activePayment = order.getActivePayment();
        log.info("Active payment: {}", activePayment != null ? activePayment.getId() : "null");

        if (activePayment == null) {
            log.error("No active payment found for order: {}", orderId);
            throw new NotFoundException("Không tìm thấy thông tin thanh toán hợp lệ cho đơn hàng.");
        }
        
        log.info("Payment status: {}, provider: {}", activePayment.getStatus(), activePayment.getProvider());
        
        if (activePayment.getStatus() == Payment.PaymentStatus.CAPTURED) {
            log.warn("Cannot re-pay: payment already captured");
            throw new BadRequestException("Đơn hàng đã được thanh toán");
        }
        if ("COD".equalsIgnoreCase(activePayment.getProvider())) {
            log.warn("Cannot re-pay: payment method is COD");
            throw new BadRequestException("Không thể thanh toán lại cho đơn hàng COD.");
        }

        // Tạo payment URL mới (sẽ tạo Payment record mới và set payment cũ thành FAILED)
        log.info("Creating new payment URL for repay with provider: {}", activePayment.getProvider());
        String paymentUrl = paymentService.getPaymentUrl(orderId, activePayment.getProvider());
        log.info("Payment URL created successfully: {}", paymentUrl);

        return Map.of("paymentUrl", paymentUrl);
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void retryCreateReturnShipment(UUID orderId) {
        Order order = orderService.getOrderById(orderId);

        if (order.getStatus() != Order.OrderStatus.RETURNING) {
            throw new BadRequestException("Đơn hàng không ở trạng thái đang trả hàng");
        }

        // Kiểm tra xem đã có return shipment chưa
        List<Shipment> shipments = shipmentService.getAllShipmentsByOrder(orderId);
        boolean hasReturnShipment = shipments.stream()
                .anyMatch(s -> Boolean.TRUE.equals(s.getIsReturnShipment()) && s.getTrackingNumber() != null);

        if (hasReturnShipment) {
            throw new BadRequestException("Đơn hàng đã có đơn trả hàng");
        }

        // Tạo lại return shipment
        shipmentService.returnOrderInGHN(order);
    }
    @Transactional
    public void updateRefundPaymentInfo( UUID requestId, PaymentRefundOption newOption,HttpServletRequest request,HttpServletResponse response) {
        var userOps = authenUtil.getAuthenUser();
        var guestIdCookie = cookieUtil.readCookie(request, "guest_id");
        if (userOps.isEmpty() && guestIdCookie.isEmpty()) {
            throw new AuthenticationException(401, "User not authenticated");
        }
        UUID guestId = null;
        if (guestIdCookie.isPresent())
        {
            guestId = UUID.fromString(guestIdCookie.get().getValue());
            // reset cookie to extend expiration
            response.addCookie(cookieUtil.createGuestId(guestId));
        }
        // Tìm yêu cầu
        OrderChangeRequest changeRequest = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy yêu cầu"));
        var userId = userOps.isPresent()?userOps.get().getId():guestId;
        // Kiểm tra quyền sở hữu (User có phải chủ request không?)
        if (!changeRequest.getUserId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền chỉnh sửa yêu cầu này");
        }

        List<String> allowedStatuses = List.of("PENDING", "WAITING_REFUND");
        if (!allowedStatuses.contains(changeRequest.getStatus())) {
            throw new BadRequestException("Không thể cập nhật thông tin khi yêu cầu đã hoàn tất hoặc bị từ chối");
        }

        // Cập nhật Metadata
        // Lấy metadata hiện tại (khởi tạo mới nếu null để tránh NullPointer)
        Map<String, Object> currentMeta = changeRequest.getMetadata();
        if (currentMeta == null) {
            currentMeta = new HashMap<>();
        }
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("refundMethod", newOption.getMethod());
        metadata.put("refundData", newOption.getData());
        currentMeta.putAll(metadata);
        //Hibernate đôi khi không detect được thay đổi trong Map, nên set lại
        changeRequest.setMetadata(currentMeta);
        requestRepository.save(changeRequest);
    }
    private void doCancelOrderLogic(Order order, OrderChangeRequest request) {
        log.info("[DO_CANCEL] Bắt đầu hủy đơn hàng: {}, status: {}", order.getOrderNumber(), order.getStatus());
        
        if (order.getStatus() == Order.OrderStatus.SHIPPED) {
            try {
                log.info("[DO_CANCEL] Đơn đang ship, hủy vận đơn GHN...");
                shipmentService.cancelShippingOrderInGHN(shipmentService.getCurrentShipmentByOrder(order.getId()));
            } catch (Exception e) {
                log.error("[DO_CANCEL] Lỗi khi hủy vận đơn GHN", e);
                throw new BadRequestException("Không thể hủy đơn hàng đang được vận chuyển: " + e.getMessage());
            }
        }
        
        try {
            log.info("[DO_CANCEL] Giải phóng inventory...");
            inventoryService.releaseReservation(order);
        } catch (Exception e) {
            log.error("[DO_CANCEL] Lỗi khi giải phóng inventory", e);
            throw new BadRequestException("Không thể giải phóng hàng tồn kho: " + e.getMessage());
        }

        log.info("[DO_CANCEL] Cập nhật trạng thái đơn hàng thành CANCELLED");
        orderService.updateStatus(order, Order.OrderStatus.CANCELLED);

        // Nếu đã thanh toán (PaidAt != null) -> Chuyển request sang trạng thái chờ hoàn tiền
        if (order.getPaidAt() != null) {
            log.info("[DO_CANCEL] Đơn đã thanh toán, chuyển sang WAITING_REFUND");
            request.setStatus("WAITING_REFUND");
            request.setRequestedAmount(order.getTotalAmount()); // Mặc định hoàn full nếu hủy
        } else {
            log.info("[DO_CANCEL] Đơn chưa thanh toán, đánh dấu COMPLETED");
            request.setStatus("COMPLETED");
        }
        requestRepository.save(request);
        log.info("[DO_CANCEL] Hoàn tất hủy đơn hàng: {}", order.getOrderNumber());
    }

    private void doReturnOrderLogic(Order order, OrderChangeRequest request) {
        // 1. Cập nhật trạng thái Order
        orderService.updateStatus(order, Order.OrderStatus.RETURNING);
        // Tạo đơn ship hoàn
        shipmentService.returnOrderInGHN(order);
        // 3. Xử lý hoàn tiền
        request.setStatus("WAITING_REFUND");

        if (request.getRequestedAmount() == null) {
            request.setRequestedAmount(order.getTotalAmount());
        }
        requestRepository.save(request);
    }

    private OrderChangeRequest createChangeRequestEntity(Order order, String type, String reason, List<String> images, Map<String, Object> metadata, boolean isAdmin) {
        OrderChangeRequest request = new OrderChangeRequest();
        request.setOrder(order);
        request.setUserId(order.getUser() != null ? order.getUser().getId() : order.getGuestId());
        request.setType(type);
        request.setReason(reason);
        request.setImages(images);
        request.setMetadata(metadata);
        request.setCreatedAt(Instant.now());
        request.setUpdatedAt(Instant.now());

        request.setStatus(isAdmin ? "APPROVED" : "PENDING");

        return requestRepository.save(request);
    }

}