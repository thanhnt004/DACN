package com.example.backend.service.order;


import com.example.backend.dto.response.checkout.CheckoutItemDetail;
import com.example.backend.dto.response.checkout.CheckoutSession;
import com.example.backend.excepton.NotFoundException;
import com.example.backend.model.User;
import com.example.backend.model.order.Order;
import com.example.backend.model.order.OrderItem;
import com.example.backend.repository.catalog.product.ProductRepository;
import com.example.backend.repository.catalog.product.ProductVariantRepository;
import com.example.backend.repository.order.OrderRepository;
import com.example.backend.repository.user.UserRepository;
import com.example.backend.util.CookieUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderService {
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;

    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;

    private final CookieUtil cookieUtil;
    public Order createOrderFromSession(
            CheckoutSession session,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        UUID guestId = null;
        log.info("Creating order from session: sessionId={}, id={}",
                session.getId(), session.getSessionToken());
        Optional<User> userOptional = userRepository.findById(session.getUserId());
        if (userOptional.isEmpty())
        {
            var guestIdCookie = cookieUtil.readCookie(httpRequest, "guest_id");
            if (guestIdCookie.isEmpty())
            {
                guestId = UUID.randomUUID();
                response.addCookie(cookieUtil.createGuestId(guestId));
            }else {
                guestId = UUID.fromString(guestIdCookie.get().getValue());
            }
        }
        // 1. Generate order number
        String orderNumber = generateOrderNumber();

        // 3. Tạo order
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .user(userOptional.orElse(null))
                .guestId(guestId)
                .status(Order.OrderStatus.PENDING)
                .subtotalAmount(session.getSubtotalAmount())
                .discountAmount(session.getDiscountAmount())
                .shippingAmount(session.getShippingAmount())
                .totalAmount(session.getTotalAmount())
                .shippingAddress(session.getShippingAddress())
                .notes(session.getNotes())
                .placedAt(Instant.now())
                .build();

        // 4. Tạo order items
        for (CheckoutItemDetail item : session.getItems()) {
            OrderItem orderItem = OrderItem.builder()
                    .product(productRepository.getReferenceById(item.getProductId()))
                    .variant(productVariantRepository.getReferenceById(item.getVariantId()))
                    .sku(item.getSku())
                    .productName(item.getProductName())
                    .variantName(item.getVariantName())
                    .imageUrl(item.getImageUrl())
                    .quantity(item.getQuantity())
                    .unitPriceAmount(item.getUnitPriceAmount())
                    .totalAmount(item.getTotalAmount())
                    .build();

            order.addItem(orderItem);
        }

        // 5. Lưu order
        order = orderRepository.save(order);

        log.info("Order created: orderId={}, orderNumber={}",
                order.getId(), order.getOrderNumber());

        return order;
    }
    public Page<Order> getOrdersByUserOrGuest(Optional<User> userOps,UUID guestId, String status, Pageable pageable) {
        Specification<Order> spec = buildOrderSpecification(userOps, status, guestId);
        return orderRepository.findAll(spec, pageable);
    }
    /**
     * Get order by ID
     */
    @Transactional(readOnly = true)
    public Order getOrderById(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy đơn hàng: " + orderId
                ));
    }
    /**
     * Get order by order number
     */
    @Transactional(readOnly = true)
    public Order getOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy đơn hàng: " + orderNumber
                ));
    }

    /**
     * Update order status
     */
    @Transactional
    public void updateStatus(UUID orderId, Order.OrderStatus status) {
        Order order = getOrderById(orderId);
        order.setStatus(status);
        orderRepository.save(order);

        log.info("Order status updated: orderId={}, status={}", orderId, status);
    }

    /**
     * Mark order as paid
     */
    @Transactional
    public void markAsPaid(UUID orderId) {
        Order order = getOrderById(orderId);
        order.setPaidAt(Instant.now());
        order.setStatus(Order.OrderStatus.CONFIRMED);
        orderRepository.save(order);

        log.info("Order marked as paid: orderId={}", orderId);
    }

    /**
     * Generate unique order number
     * Format: ORD-YYYYMMDD-XXXXXX
     */
    private String generateOrderNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        String orderNumber = "ORD-" + date + "-" + random;

        // Đảm bảo unique (retry nếu duplicate)
        while (orderRepository.existsByOrderNumber(orderNumber)) {
            random = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
            orderNumber = "ORD-" + date + "-" + random;
        }

        return orderNumber;
    }
    private Specification<Order> buildOrderSpecification(Optional<User> userOps, String status, UUID guestId) {
        return (root, query, criteriaBuilder) -> {
            var predicates = criteriaBuilder.conjunction();

            userOps.ifPresent(user ->
                predicates.getExpressions().add(
                    criteriaBuilder.equal(root.get("user"), user)
                )
            );

            if (guestId != null) {
                predicates.getExpressions().add(
                    criteriaBuilder.equal(root.get("guestId"), guestId)
                );
            }

            if (status != null && !status.isEmpty()) {
                predicates.getExpressions().add(
                    criteriaBuilder.equal(root.get("status"), Order.OrderStatus.valueOf(status))
                );
            }

            return predicates;
        };
    }

    public void mergeOrders(User user, UUID guestId) {
        List<Order> guestOrders = orderRepository.findAll((root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("guestId"), guestId)
        );

        for (Order order : guestOrders) {
            order.setUser(user);
            order.setGuestId(null);
            orderRepository.save(order);
            log.info("Merged guest order to user: orderId={}, userId={}", order.getId(), user.getId());
        }
    }
}
