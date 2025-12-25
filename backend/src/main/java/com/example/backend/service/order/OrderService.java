package com.example.backend.service.order;

import com.example.backend.dto.response.checkout.CheckoutItemDetail;
import com.example.backend.dto.response.checkout.CheckoutSession;
import com.example.backend.exception.BadRequestException;
import com.example.backend.exception.NotFoundException;
import com.example.backend.model.User;
import com.example.backend.model.enumrator.AuditActionType;
import com.example.backend.model.enumrator.AuditEntityType;
import com.example.backend.model.order.Order;
import com.example.backend.model.order.OrderChangeRequest;
import com.example.backend.model.order.OrderItem;
import com.example.backend.model.payment.Payment;
import com.example.backend.repository.catalog.product.ProductRepository;
import com.example.backend.repository.catalog.product.ProductVariantRepository;
import com.example.backend.repository.order.OrderRepository;
import com.example.backend.repository.user.UserRepository;
import com.example.backend.service.audit.AuditLogService;
import com.example.backend.util.CookieUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderService {
    private final OrderRepository orderRepository;
    @SuppressWarnings("unused")
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;

    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;

    private final CookieUtil cookieUtil;
    private final AuditLogService auditLogService;
    private final List<Order.OrderStatus> CANCELABLE_STATUSES = List.of(
            Order.OrderStatus.PENDING,
            Order.OrderStatus.CONFIRMED);
    
    @Transactional
    public void cancelExpiredPendingOrders() {
        Instant thirtyMinutesAgo = Instant.now().minus(30, ChronoUnit.MINUTES);
        List<Order> expiredOrders = orderRepository.findExpiredPendingOrders(thirtyMinutesAgo);

        if (!expiredOrders.isEmpty()) {
            log.info("Found {} expired pending orders to cancel.", expiredOrders.size());
            for (Order order : expiredOrders) {
                order.setStatus(Order.OrderStatus.CANCELLED);
                orderRepository.save(order);
                log.info("Cancelled expired order {}", order.getOrderNumber());
                // Optionally, notify the user
            }
        }
    }

    public Order createOrderFromSession(
            CheckoutSession session,
            HttpServletRequest httpRequest,
            HttpServletResponse response, String notes) {
        UUID guestId = null;
        log.info("Creating order from session: sessionId={}, id={}",
                session.getId(), session.getSessionToken());
        
        // Check if user is authenticated or guest
        Optional<User> userOptional = Optional.empty();
        if (session.getUserId() != null) {
            userOptional = userRepository.findById(session.getUserId());
        }
        
        if (userOptional.isEmpty()) {
            var guestIdCookie = cookieUtil.readCookie(httpRequest, "guest_id");
            if (guestIdCookie.isEmpty()) {
                guestId = UUID.randomUUID();
                response.addCookie(cookieUtil.createGuestId(guestId));
            } else {
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
                .notes(notes)
                .placedAt(Instant.now())
                .build();

        // 4. Tạo order items
        for (CheckoutItemDetail item : session.getItems()) {
            var variant = productVariantRepository.findById(item.getVariantId())
                    .orElseThrow(() -> new NotFoundException("Product variant not found"));
            OrderItem orderItem = OrderItem.builder()
                    .product(productRepository.getReferenceById(item.getProductId()))
                    .variant(variant)
                    .sku(item.getSku())
                    .productName(item.getProductName())
                    .variantName(item.getVariantName())
                    .imageUrl(item.getImageUrl())
                    .quantity(item.getQuantity())
                    .unitPriceAmount(item.getUnitPriceAmount())
                    .historyCost(variant.getHistoryCost())
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

    public Page<Order> getOrdersByUserOrGuest(Optional<User> userOps, UUID guestId, String status, String paymentType,
            Pageable pageable) {
        Specification<Order> spec = buildOrderSpecification(userOps, status, guestId, paymentType);
        return orderRepository.findAll(spec, pageable);
    }

    /**
     * Get order by ID
     */
    @Transactional(readOnly = true)
    public Order getOrderById(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy đơn hàng: " + orderId));
    }

    /**
     * Get order by order number
     */
    @Transactional(readOnly = true)
    public Order getOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy đơn hàng: " + orderNumber));
    }

    /**
     * Update order status
     *
     * @return
     */
    @Transactional
    public Order updateStatus(Order order, Order.OrderStatus status) {
        Order.OrderStatus oldStatus = order.getStatus();
        order.setPreviousStatus(oldStatus);
        order.setStatus(status);
        orderRepository.save(order);

        log.info("Order status updated: orderNum={}, status={}", order.getOrderNumber(), status);

        // Audit log: Ghi lại thay đổi trạng thái đơn hàng
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("order_number", order.getOrderNumber());
        metadata.put("old_status", oldStatus != null ? oldStatus.name() : null);
        metadata.put("new_status", status.name());
        metadata.put("total_amount", order.getTotalAmount());

        auditLogService.logAction(
            AuditActionType.UPDATE_ORDER_STATUS,
            AuditEntityType.ORDER,
            order.getId(),
            metadata
        );

        return order;
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

    private Specification<Order> buildOrderSpecification(Optional<User> userOps, String status, UUID guestId,
            String paymentType) {
        return (root, query, criteriaBuilder) -> {
            if (query != null) {
                query.distinct(true);
            }

            List<Predicate> predicates = new ArrayList<>();

            // Build user/guest filter: user_id = ? OR guest_id = ?
            if (userOps.isPresent() || guestId != null) {
                List<Predicate> userOrGuestPredicates = new ArrayList<>();
                userOps.ifPresent(user -> userOrGuestPredicates.add(criteriaBuilder.equal(root.get("user"), user)));
                if (guestId != null) {
                    userOrGuestPredicates.add(criteriaBuilder.equal(root.get("guestId"), guestId));
                }
                if (!userOrGuestPredicates.isEmpty()) {
                    predicates.add(criteriaBuilder.or(userOrGuestPredicates.toArray(new Predicate[0])));
                }
            }

            if (status != null && !status.isEmpty()) {
                String normalized = status.trim();
                if (!normalized.equalsIgnoreCase("ALL")) {
                    List<Order.OrderStatus> statuses = Arrays.stream(normalized.split(","))
                            .map(String::trim)
                            .filter(token -> !token.isEmpty())
                            .map(token -> {
                                try {
                                    return Order.OrderStatus.valueOf(token.toUpperCase(Locale.ROOT));
                                } catch (IllegalArgumentException ex) {
                                    throw new BadRequestException("Trạng thái đơn hàng không hợp lệ: " + token);
                                }
                            })
                            .collect(Collectors.toList());

                    if (!statuses.isEmpty()) {
                        predicates.add(root.get("status").in(statuses));
                    }
                }
            }

            if (paymentType != null && !paymentType.isEmpty()) {
                String normalized = paymentType.trim();
                if (!normalized.equalsIgnoreCase("ALL")) {
                    Join<Order, Payment> paymentJoin = root.join("payments", JoinType.LEFT);
                    List<Predicate> paymentPredicates = Arrays.stream(normalized.split(","))
                            .map(String::trim)
                            .filter(token -> !token.isEmpty())
                            .map(token -> token.toUpperCase(Locale.ROOT))
                            .map(token -> {
                                String likePattern = "%" + token + "%";
                                if ("COD".equals(token)) {
                                    return criteriaBuilder.like(criteriaBuilder.upper(paymentJoin.get("provider")),
                                            "%COD%");
                                }
                                if ("NON_COD".equals(token) || "ONLINE".equals(token)) {
                                    return criteriaBuilder.notLike(criteriaBuilder.upper(paymentJoin.get("provider")),
                                            "%COD%");
                                }
                                return criteriaBuilder.like(criteriaBuilder.upper(paymentJoin.get("provider")),
                                        likePattern);
                            })
                            .toList();

                    if (!paymentPredicates.isEmpty()) {
                        predicates.add(criteriaBuilder.or(paymentPredicates.toArray(new Predicate[0])));
                    }
                }
            }

            return predicates.isEmpty()
                    ? criteriaBuilder.conjunction()
                    : criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public void mergeOrders(User user, UUID guestId) {
        List<Order> guestOrders = orderRepository
                .findAll((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("guestId"), guestId));

        for (Order order : guestOrders) {
            order.setUser(user);
            order.setGuestId(null);
            orderRepository.save(order);
            log.info("Merged guest order to user: orderId={}, userId={}", order.getId(), user.getId());
        }
    }

    public Order getOrderDetailByUserOrGuest(Optional<User> userOps, UUID guestId, UUID orderId) {
        Specification<Order> spec = buildOrderSpecification(userOps, null, guestId, null);
        spec = spec
                .and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("id"), orderId));

        return orderRepository.findOne(spec)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy đơn hàng: " + orderId));
    }


    /**
     * Lấy danh sách đơn hàng theo tab filter
     * @param tab Tab filter enum (ALL, UNPAID, TO_CONFIRM, PROCESSING, SHIPPING, COMPLETED, CANCEL_REQ, CANCELLED, RETURN_REQ, REFUNDED)
     * @param orderNumber Số đơn hàng (optional)
     * @param startDate Ngày bắt đầu (optional)
     * @param endDate Ngày kết thúc (optional)
     * @param pageable Phân trang
     * @return Page of Orders
     */
    public Page<Order> getPageOrder(String tab, String orderNumber, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        if (!pageable.getSort().isSorted()) {
            pageable = org.springframework.data.domain.PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")
            );
        }
        Specification<Order> spec = buildOrderSpecificationByTab(tab, orderNumber, startDate, endDate);
        return orderRepository.findAll(spec, pageable);
    }

    private Specification<Order> buildOrderSpecificationByTab(String tabStr, String orderNumber,
            LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, criteriaBuilder) -> {
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();

            // Filter by order number
            if (orderNumber != null && !orderNumber.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(root.get("orderNumber"), "%" + orderNumber.trim() + "%"));
            }

            // Filter by date range
            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("placedAt"), 
                    startDate.atZone(ZoneId.systemDefault()).toInstant()));
            }
            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("placedAt"), 
                    endDate.atZone(ZoneId.systemDefault()).toInstant()));
            }

            // Get tab filter enum
            com.example.backend.model.order.OrderTabFilter tab = 
                com.example.backend.model.order.OrderTabFilter.fromString(tabStr);

            // Apply tab-specific filtering logic
            switch (tab) {
                case ALL:
                    // No additional filter - show all orders
                    break;

                case UNPAID:
                    // Status == PENDING AND PaymentMethod != COD AND (Payment Status != SUCCESS OR Payment Status IS NULL)
                    // Đơn online chưa thanh toán (VNPAY, MOMO chưa thanh toán)
                    Join<Order, Payment> unpaidPaymentJoin = root.join("payments", JoinType.INNER);
                    predicates.add(criteriaBuilder.and(
                        criteriaBuilder.equal(root.get("status"), Order.OrderStatus.PENDING),
                        criteriaBuilder.notLike(criteriaBuilder.upper(unpaidPaymentJoin.get("provider")), "%COD%"),
                        criteriaBuilder.or(
                            criteriaBuilder.notEqual(unpaidPaymentJoin.get("status"), "SUCCESS"),
                            criteriaBuilder.isNull(unpaidPaymentJoin.get("status"))
                        )
                    ));
                    break;

                case TO_CONFIRM:
                    // (Status == CONFIRMED) OR (Status == PENDING AND PaymentMethod == COD)
                    // Bao gồm: Đơn đã thanh toán online (CONFIRMED) HOẶC đơn COD mới (PENDING + COD)
                    Join<Order, Payment> confirmPaymentJoin = root.join("payments", JoinType.INNER);
                    
                    Predicate confirmedOrders = criteriaBuilder.equal(root.get("status"), Order.OrderStatus.CONFIRMED);
                    Predicate pendingCodOrders = criteriaBuilder.and(
                        criteriaBuilder.equal(root.get("status"), Order.OrderStatus.PENDING),
                        criteriaBuilder.like(criteriaBuilder.upper(confirmPaymentJoin.get("provider")), "%COD%")
                    );
                    predicates.add(criteriaBuilder.or(confirmedOrders, pendingCodOrders));
                    break;

                case PROCESSING:
                    // Status == PROCESSING
                    predicates.add(criteriaBuilder.equal(root.get("status"), Order.OrderStatus.PROCESSING));
                    break;

                case SHIPPING:
                    // Status == SHIPPED
                    predicates.add(criteriaBuilder.equal(root.get("status"), Order.OrderStatus.SHIPPED));
                    break;

                case COMPLETED:
                    // Status == DELIVERED
                    predicates.add(criteriaBuilder.equal(root.get("status"), Order.OrderStatus.DELIVERED));
                    break;

                case CANCEL_REQ:
                    // Status == CANCELING
                    predicates.add(criteriaBuilder.equal(root.get("status"), Order.OrderStatus.CANCELING));
                    break;

                case CANCELLED:
                    // Status == CANCELLED
                    predicates.add(criteriaBuilder.equal(root.get("status"), Order.OrderStatus.CANCELLED));
                    break;

                case RETURN_REQ:
                    // Status == RETURNING
                    predicates.add(criteriaBuilder.equal(root.get("status"), Order.OrderStatus.RETURNING));
                    break;

                case REFUNDED:
                    // Status == REFUNDED OR Status == RETURNED
                    predicates.add(root.get("status").in(
                        Order.OrderStatus.REFUNDED, 
                        Order.OrderStatus.RETURNED
                    ));
                    break;

                case WAITING_REFUND:
                    // ChangeRequest.Status == WAITING_REFUND
                    Join<Order, OrderChangeRequest> requestJoin = root.join("changeRequest", JoinType.INNER);
                    predicates.add(criteriaBuilder.equal(requestJoin.get("status"), "WAITING_REFUND"));
                    break;

                default:
                    // Fallback to ALL (no filter)
                    break;
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}