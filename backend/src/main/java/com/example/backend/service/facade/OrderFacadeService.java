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
import com.example.backend.model.order.Order;
import com.example.backend.model.order.OrderChangeRequest;
import com.example.backend.model.order.OrderItem;
import com.example.backend.model.order.Shipment;
import com.example.backend.model.payment.Payment;
import com.example.backend.repository.order.OrderChangeRequestRepository;
import com.example.backend.service.MessageService;
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

            // 5. Tạo Payment record
            Payment payment = paymentService.createPayment(
                    order,
                    checkoutSession.getSelectedPaymentMethod().getId());
            //tao shipping here if needed
            Shipment shipment = shipmentService.createShipmentForOrder(order,checkoutSession);
            // 6. Generate payment URL
            String paymentUrl = paymentService.generatePaymentUrl(
                    order,
                    payment,
                    checkoutSession.getSelectedPaymentMethod().getId());

            log.debug("Payment URL generated: orderId={}", order.getId());

            // 7. Xóa cart nếu source = CART
            if (checkoutSession.getCartId() != null) {
                cartService.clearCart(checkoutSession);
                log.debug("Cart cleared: cartId={}", checkoutSession.getCartId());
            }

            // 8. Xóa session khỏi Redis
            redisCheckoutStoreService.delete(sessionId);
            log.debug("Session deleted: sessionId={}", sessionId);

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
                .discountRedemptions(discountResponses);
        
//        // If the order is in a state that implies a pending request, fetch and attach the request details.
//        if (order.getStatus() == Order.OrderStatus.CANCELING || order.getStatus() == Order.OrderStatus.RETURNING) {
//            requestRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc(order.getId(), "PENDING")
//                    .ifPresent(changeRequest -> {
//                        OrderResponse.OrderChangeRequestResponse changeRequestResponse = OrderResponse.OrderChangeRequestResponse.builder()
//                                .id(changeRequest.getId())
//                                .type(changeRequest.getType())
//                                .status(changeRequest.getStatus())
//                                .reason(changeRequest.getReason())
//                                .images(changeRequest.getImages())
//                                .build();
//                        responseBuilder.changeRequest(changeRequestResponse);
//                    });
//        }

        return responseBuilder.build();
    }

    private OrderItemDTO buildOrderItemResponse(OrderItem orderItem) {
        return OrderItemDTO.builder()
                .id(orderItem.getId())
                .productId(orderItem.getProduct().getId())
                .variantId(orderItem.getVariant().getId())
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
        var userOps = authenUtil.getAuthenUser();
        var guestIdCookie = cookieUtil.readCookie(request, "guest_id");
        if (userOps.isEmpty() && guestIdCookie.isEmpty()) {
            throw new AuthenticationException(401, "User not authenticated");
        }
        Order order;
        if (guestIdCookie.isPresent()) {
            UUID guestId = UUID.fromString(guestIdCookie.get().getValue());
            // reset cookie to extend expiration
            response.addCookie(cookieUtil.createGuestId(guestId));
            order = orderService.getOrderDetailByUserOrGuest(userOps, guestId, orderId);
        } else {
            order = orderService.getOrderDetailByUserOrGuest(userOps, null, orderId);
        }
        return buildOrderResponse(order);
    }


    public PageResponse<OrderResponse> getOrderListByAdmin(String status, String paymentType, String orderNumber,
                                                           LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return new PageResponse<>(orderService.getPageOrder(status, paymentType, orderNumber, startDate, endDate, pageable)
                .map(this::buildOrderResponse));
    }

    public BatchResult<UUID> confirmOrders(List<UUID> orderIds) {
        BatchResult<UUID> result = new BatchResult<UUID>();
        orderIds.forEach(orderId->{
            try {
                confirmSingleOrder(orderId);
                result.addSuccess(orderId);
            } catch (Exception e) {
                log.error("Failed to confirm order: {}", orderId, e);
                result.addFailure(orderId, e.getMessage());
            }
        });
        return result;
    }

    public BatchResult<UUID> shipOrders(List<UUID> orderIds) {
        BatchResult<UUID> result = new BatchResult<UUID>();
        orderIds.forEach(orderId->{
            try {
                shipSingleOrder(orderId);
                result.addSuccess(orderId);
            }catch (Exception e)
            {
                log.error("Failed to ship order: {}", orderId, e);
                result.addFailure(orderId, e.getMessage());
            }
        });
        return result;
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void confirmSingleOrder(UUID orderId) {
        Order order = orderService.getOrderById(orderId);

        if (!order.getStatus().equals(Order.OrderStatus.PENDING) &&
                !order.getStatus().equals(Order.OrderStatus.CONFIRMED)) {
            throw new BadRequestException("Order cannot be confirmed: " + order.getOrderNumber());
        }

        orderService.updateStatus(order, Order.OrderStatus.PROCESSING);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void shipSingleOrder(UUID orderId) {
        Order order = orderService.getOrderById(orderId);

        if (!order.getStatus().equals(Order.OrderStatus.PROCESSING)) {
            if (!order.getStatus().equals(Order.OrderStatus.RETURNING))
                throw new BadRequestException("Không thể tạo đơn giao hàng cho đơn hàng: " + order.getOrderNumber());
        }

        Shipment shipment = shipmentService.getCurrentShipmentByOrder(orderId);

        // Validate before creating GHN order
        if (shipment.getTrackingNumber() != null) {
            throw new BadRequestException("Đơn hàng đã có tracking number");
        }

        shipmentService.createShippingOrderInGHN(shipment, order.getShippingAddress());
        orderService.updateStatus(order, Order.OrderStatus.SHIPPED);
    }
    public String getPrintUrlForOrders(List<UUID> orderIds) {
        List<Order> orders = orderIds.stream()
                .map(orderService::getOrderById)
                .toList();
        if (orders.size()>100)
            throw new BadRequestException("Cannot print more than 100 orders at once");
        return shipmentService.getPrintUrl(orders);
    }
    public void cancelOrderByAdmin(UUID orderId,String adminReason) {
        Order order = orderService.getOrderById(orderId);
        // Tạo request để lưu lịch sử (đánh dấu là đã duyệt xong phần hủy)
        OrderChangeRequest request = createChangeRequestEntity(order, "CANCEL", adminReason, null, null, true);
        // Thực hiện hủy
        doCancelOrderLogic(order, request);
    }

    public void returnOrderByAdmin(UUID orderId,String adminReason) {
        Order order = orderService.getOrderById(orderId);
        if (order.getStatus()!= Order.OrderStatus.DELIVERED) {
            throw new BadRequestException("Chỉ có thể trả hàng cho đơn hàng đã giao");
        }
        OrderChangeRequest request = createChangeRequestEntity(order, "CANCEL", adminReason, null, null, true);
        doReturnOrderLogic(order, request);
    }

    public void reviewChangeRequest(UUID requestId, ReviewRequestDTO dto) {
        OrderChangeRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new BadRequestException("Change request not found"));
        if (!request.getStatus().equals("PENDING")) {
            throw new BadRequestException("Yêu cầu không ở trạng thái chờ duyệt");
        }
        if (dto.getStatus().equals("APPROVED")) {
            Order order = request.getOrder();

            if ("CANCEL".equals(request.getType())) {
                doCancelOrderLogic(order, request);
            }
            else if ("RETURN".equals(request.getType())) {
                doReturnOrderLogic(order, request);
            }
        } else if (dto.getStatus().equals("REJECTED")) {
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
        Map<String, Object> metadata = Map.of(
                "refundMethod", paymentRefundOption.getMethod(),
                "refundData", paymentRefundOption.getData(),
                "returnAddress", null
        );
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
        OrderChangeRequest orderChangeRequest = orderChangeRequestRepository.findByOrderId(orderId).orElseThrow(
                ()->new NotFoundException("Change request not found")
        );
        return OrderChangeRequestResponse.builder()
                .id(orderChangeRequest.getId())
                .type(orderChangeRequest.getType())
                .images(orderChangeRequest.getImages())
                .reason(orderChangeRequest.getReason())
                .status(orderChangeRequest.getStatus())
                .adminNote(orderChangeRequest.getAdminNote())
                .build();
    }
    public Object rePay(UUID orderId) {
        Order order = orderService.getOrderById(orderId);

        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new BadRequestException("Chỉ có thể thanh toán lại cho đơn hàng đang chờ xử lý.");
        }
        if (order.isPaid()) {
            throw new BadRequestException("Đơn hàng đã được thanh toán.");
        }
        if (order.getCreatedAt().plus(30, ChronoUnit.MINUTES).isBefore(Instant.now())) {
            throw new BadRequestException("Đã hết thời gian thanh toán cho đơn hàng này.");
        }

        Payment activePayment = order.getActivePayment();

        if (activePayment == null) {
            throw new NotFoundException("Không tìm thấy thông tin thanh toán hợp lệ cho đơn hàng.");
        }
        if (activePayment.getStatus() == Payment.PaymentStatus.CAPTURED) {
            throw new BadRequestException("Đơn hàng đã được thanh toán");
        }
        if ("COD".equalsIgnoreCase(activePayment.getProvider())) {
            throw new BadRequestException("Không thể thanh toán lại cho đơn hàng COD.");
        }

        String paymentUrl = paymentService.generatePaymentUrl(order, activePayment, activePayment.getProvider());

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
                .anyMatch(s -> s.isReturnShipment() && s.getTrackingNumber() != null);

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
        Map<String, Object> metadata = Map.of(
                "refundMethod", newOption.getMethod(),
                "refundData", newOption.getData(),
                "returnAddress", null
        );
        currentMeta.putAll(metadata);
        //Hibernate đôi khi không detect được thay đổi trong Map, nên set lại
        changeRequest.setMetadata(currentMeta);
        requestRepository.save(changeRequest);
    }
    private void doCancelOrderLogic(Order order, OrderChangeRequest request) {
        if (order.getStatus() == Order.OrderStatus.SHIPPED) {
            try {
                shipmentService.cancelShippingOrderInGHN(shipmentService.getCurrentShipmentByOrder(order.getId()));
            } catch (Exception e) {
                throw new BadRequestException("Không thể hủy đơn hàng đang được vận chuyển");
            }
        }
        inventoryService.releaseReservation(order);

        orderService.updateStatus(order, Order.OrderStatus.CANCELLED);

        // Nếu đã thanh toán (PaidAt != null) -> Chuyển request sang trạng thái chờ hoàn tiền
        if (order.getPaidAt() != null) {
            request.setStatus("WAITING_REFUND");
            request.setRequestedAmount(order.getTotalAmount()); // Mặc định hoàn full nếu hủy
        } else {
            request.setStatus("COMPLETED");
        }
        requestRepository.save(request);
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

        request.setStatus(isAdmin ? "APPROVED" : "PENDING");

        return requestRepository.save(request);
    }

}
