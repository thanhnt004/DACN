package com.example.backend.service.facade;

import com.example.backend.dto.response.checkout.*;
import com.example.backend.dto.response.discount.DiscountRedemptionResponse;
import com.example.backend.dto.response.payment.PaymentDTO;
import com.example.backend.dto.response.wraper.PageResponse;
import com.example.backend.excepton.AuthenticationException;
import com.example.backend.excepton.BadRequestException;
import com.example.backend.excepton.ConflictException;
import com.example.backend.model.order.Order;
import com.example.backend.model.order.OrderItem;
import com.example.backend.model.payment.Payment;
import com.example.backend.service.MessageService;
import com.example.backend.service.cart.CartService;
import com.example.backend.service.order.OrderService;
import com.example.backend.service.payment.PaymentService;
import com.example.backend.service.product.InventoryService;
import com.example.backend.service.product.ProductVariantService;
import com.example.backend.service.redis.RedisCheckoutStoreService;
import com.example.backend.util.AuthenUtil;
import com.example.backend.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderFacadeService {
    private final InventoryService inventoryService;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final CartService cartService;
    private final MessageService emailService;
    private final RedisCheckoutStoreService redisCheckoutStoreService;
    private final ProductVariantService variantService;

    private final AuthenUtil authenUtil;
    private final CookieUtil cookieUtil;

    public OrderCreatedResponse confirmCheckoutSession(UUID sessionId, HttpServletRequest httpRequest,
            HttpServletResponse response) {
        try {
            CheckoutSession checkoutSession = redisCheckoutStoreService.findById(sessionId).orElseThrow(
                    () -> new IllegalArgumentException("Checkout session not found"));
            if (!checkoutSession.getCanConfirm())
                throw new BadRequestException("cannot confirm this checkout session");
            validateSessionItemsBeforeOrder(checkoutSession);
            // 3. Tạo Order
            Order order = orderService.createOrderFromSession(
                    checkoutSession, httpRequest, response);

            log.info("Order created: orderId={}, orderNumber={}",
                    order.getId(), order.getOrderNumber());

            // 4. Đặt giữ hàng tồn kho
            inventoryService.reserveStockForOrder(order);
            log.debug("Inventory reserved for order: {}", order.getId());

            // 5. Tạo Payment record
            Payment payment = paymentService.createPayment(
                    order,
                    checkoutSession.getSelectedPaymentMethod().getId());

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
        List<DiscountRedemptionResponse> discountResponses = List.of();
        return OrderResponse.builder()
                .id(order.getId())
                .notes(order.getNotes())
                .orderNumber(order.getOrderNumber())
                .status(String.valueOf(order.getStatus()))
                .totalAmount(order.getTotalAmount())
                .placedAt(order.getPlacedAt())
                .userId(order.getId())
                .discountAmount(order.getDiscountAmount())
                .paidAt(order.getPaidAt())
                .shippingAddress(order.getShippingAddress())
                .shippingAmount(order.getShippingAmount())
                .subtotalAmount(order.getSubtotalAmount())
                .items(itemResponses)
                .payments(paymentResponses)
                .placedAt(order.getPlacedAt())
                .version(order.getVersion())
                .updatedAt(order.getUpdatedAt())
                .discountRedemptions(discountResponses)
                .build();
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

    public OrderResponse getOrderDetail(String orderId, HttpServletRequest request, HttpServletResponse response) {
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

    public void cancelOrder(String orderId, HttpServletRequest request, HttpServletResponse response) {
        var userOps = authenUtil.getAuthenUser();
        var guestIdCookie = cookieUtil.readCookie(request, "guest_id");
        if (userOps.isEmpty() && guestIdCookie.isEmpty()) {
            throw new AuthenticationException(401, "User not authenticated");
        }
        if (guestIdCookie.isPresent()) {
            UUID guestId = UUID.fromString(guestIdCookie.get().getValue());
            // reset cookie to extend expiration
            response.addCookie(cookieUtil.createGuestId(guestId));
            orderService.cancelOrderByUserOrGuest(userOps, guestId, orderId);
        } else {
            orderService.cancelOrderByUserOrGuest(userOps, null, orderId);
        }
    }

    public PageResponse<OrderResponse> getOrderListByAdmin(String status, String paymentType, Pageable pageable,
            HttpServletRequest request, HttpServletResponse response) {
        return new PageResponse<>(orderService.getPageOrder(status, paymentType, pageable)
                .map(this::buildOrderResponse));
    }

    public OrderResponse updateOrderStatusByAdmin(UUID orderId, Order.OrderStatus status, HttpServletRequest request,
            HttpServletResponse response) {
        Order order = orderService.updateStatus(orderId, status);
        return buildOrderResponse(order);
    }
}
