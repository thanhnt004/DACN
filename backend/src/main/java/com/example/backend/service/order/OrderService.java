package com.example.backend.service.order;

import com.example.backend.dto.request.order.DirectCheckoutRequest;
import com.example.backend.dto.request.order.PlaceOrderRequest;
import com.example.backend.dto.response.order.CheckoutResponse;
import com.example.backend.dto.response.order.OrderConfirmationDto;
import com.example.backend.dto.response.user.UserAddress;
import com.example.backend.excepton.ConflictException;
import com.example.backend.model.DiscountRedemption;
import com.example.backend.model.User;
import com.example.backend.model.cart.Cart;
import com.example.backend.model.cart.CartItem;
import com.example.backend.model.discount.Discount;
import com.example.backend.model.order.Order;
import com.example.backend.model.order.OrderItem;
import com.example.backend.model.payment.Payment;
import com.example.backend.model.product.Inventory;
import com.example.backend.model.product.ProductVariant;
import com.example.backend.repository.cart.CartRepository;
import com.example.backend.repository.catalog.product.InventoryRepository;
import com.example.backend.repository.catalog.product.ProductVariantRepository;
import com.example.backend.repository.discount.DiscountRedemptionRepository;
import com.example.backend.repository.order.OrderItemRepository;
import com.example.backend.repository.order.OrderRepository;
import com.example.backend.repository.payment.PaymentRepository;
import com.example.backend.util.AuthenUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {
    private final CartRepository cartRepository;
    private final ProductVariantRepository variantRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final DiscountRedemptionRepository discountRedemptionRepository;

    private final AuthenUtil authenUtil;
    @Transactional
    public CheckoutResponse directCheckOut(DirectCheckoutRequest dto) {
        Optional<User> user =authenUtil.getAuthenUser();
        ProductVariant variant = variantRepository.findById(dto.getVariantId())
                .orElseThrow(() -> new RuntimeException("Product variant not found"));
        Inventory inventory = variant.getInventory();
        if (inventory.getAvailableStock() < dto.getQuantity()) {
            throw new ConflictException("Insufficient stock for variant: " + variant.getSku());
        }
        //Tính giảm giá
        long discountAmount = 0L;
//        if (dto.getDiscountCode() != null && !dto.getDiscountCode().isEmpty()) {
//            Discount discount = validateAndGetDiscount(dto.getDiscountCode(), variant.getPriceAmount() * dto.getQuantity());
//            if (discount != null) {
//                discountAmount = discount.getValue();
//            }
//        }
        long shippingAmount = 0L; // Giả sử miễn phí vận chuyển
        // Tạo đơn hàng trực tiếp (bỏ qua giỏ hàng)
            Order order = Order.builder()
                    .orderNumber(generateOrderNumber())
                    .user(user.orElse(null))
                    .status(Order.OrderStatus.PENDING)
                    .subtotalAmount(variant.getPriceAmount() * dto.getQuantity())
                    .shippingAmount(shippingAmount)
                    .discountAmount(discountAmount)
                    .totalAmount(variant.getPriceAmount() * dto.getQuantity() + shippingAmount - discountAmount)
                    .build();
            Order savedOrder = orderRepository.save(order);
            // Tạo mục đơn hàng
            OrderItem orderItem = OrderItem.builder()
                    .order(savedOrder)
                    .variant(variant)
                    .quantity(dto.getQuantity())
                    .unitPriceAmount(variant.getPriceAmount())
                    .totalAmount(variant.getPriceAmount() * dto.getQuantity())
                    .build();
            orderItemRepository.save(orderItem);
            // Cập nhật tồn kho
            inventoryRepository.reserveStock(variant.getId(), dto.getQuantity());
            // Tạo thanh toán
            Payment payment = Payment.builder()
                    .order(savedOrder)
                    .provider(dto.getPaymentMethod())
                    .amount(savedOrder.getTotalAmount())
                    .status(Payment.PaymentStatus.PENDING)
                    .build();
            paymentRepository.save(payment);
            return  CheckoutResponse.builder()
                .status("SUCCESS")
                .message("Đặt hàng thành công! Chúng tôi sẽ sớm liên hệ với bạn.")
                .orderId(savedOrder.getId())
                .orderNumber(savedOrder.getOrderNumber())
                .totalAmount(savedOrder.getTotalAmount())
                .paymentMethod("COD")
                .paymentStatus("PENDING")
                .orderStatus("PROCESSING")
                .nextAction("REDIRECT_TO_SUCCESS")
                // Không cần set paymentUrl, nó sẽ mặc định là null
                .build();
    }

    @Transactional(isolation = Isolation.SERIALIZABLE) // Mức độ cô lập cao nhất
    public OrderConfirmationDto placeOrder(PlaceOrderRequest dto, User user) throws JsonProcessingException {

        // 1. Lấy giỏ hàng
        Cart cart = cartRepository.findById(dto.getCartId())
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        // (Thêm logic: Xác thực cart này thuộc về user hoặc là cart guest)

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Giỏ hàng trống");
        }

        // 2. Lấy và KHÓA CÁC DÒNG TỒN KHO (Rất quan trọng)
        List<UUID> variantIds = cart.getItems().stream()
                .map(item -> item.getVariant().getId())
                .toList();

        // Sử dụng PESSIMISTIC_WRITE để thực thi "SELECT ... FOR UPDATE"
        List<Inventory> inventories = inventoryRepository.findAllByIdInForUpdate(variantIds);

        // 3. Kiểm tra tồn kho (Lần 2 - Lần cuối cùng)
        for (CartItem item : cart.getItems()) {
            Inventory inv = inventories.stream()
                    .filter(i -> i.getId().equals(item.getVariant().getId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Inventory data missing"));

            if (item.getQuantity() > inv.getAvailableStock()) {
                throw new ConflictException("Hết hàng cho: " + item.getVariant().getSku());
                // Giao dịch sẽ tự động ROLLBACK
            }
        }

        // 4. Tính toán tổng tiền (Lần cuối - Server-side)
        // (Đây là các hàm helper bạn tự viết)
        long subtotal = calculateSubtotal(cart);
        long shipping = getShippingFee(dto.getShippingAddress());
        Discount validatedDiscount = validateAndGetDiscount(dto.getDiscountCode(), subtotal);
        long discountAmount = (validatedDiscount != null) ? validatedDiscount.getValue() : 0;
        long total = subtotal + shipping - discountAmount;

        // 5. Tạo `orders`
        Order order = new Order();
        order.setOrderNumber(generateOrderNumber()); // (Hàm helper)
        order.setUser(user);
        order.setStatus(Order.OrderStatus.PENDING);
        ObjectMapper mapper = new ObjectMapper();
        order.setShippingAddress(mapper.writeValueAsString(dto.getShippingAddress())); // (Chuyển đổi sang JSON string nếu cần)

        order.setSubtotalAmount(subtotal);
        order.setShippingAmount(shipping);
        order.setDiscountAmount(discountAmount);
        order.setTotalAmount(total);

        Order savedOrder = orderRepository.save(order);

        // 6. Chuyển `cart_items` -> `order_items`
        for (CartItem item : cart.getItems()) {
            OrderItem orderItem = new OrderItem();
            // ... (Copy dữ liệu từ CartItem và Variant sang OrderItem)
            orderItem.setOrder(savedOrder);
            orderItem.setVariant(item.getVariant());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setUnitPriceAmount(item.getUnitPriceAmount());
            orderItem.setTotalAmount(item.getQuantity() * item.getUnitPriceAmount());
            orderItemRepository.save(orderItem);
        }

        // 7. Cập nhật `inventory` (Tạo 'reserved' stock)
        for (CartItem item : cart.getItems()) {
            inventoryRepository.reserveStock(item.getVariant().getId(), item.getQuantity());
            // (Bạn cũng có thể tạo 'inventory_reservations' ở đây)
        }

        // 8. Ghi nhận `discount_redemptions`
        if (validatedDiscount != null) {
            DiscountRedemption redemption = new DiscountRedemption();
            redemption.setDiscount(validatedDiscount);
            redemption.setUser(user);
            redemption.setOrder(savedOrder);
            discountRedemptionRepository.save(redemption);
        }

        // 9. Cập nhật `carts` -> COMPLETED
        cart.setStatus(Cart.CartStatus.CONVERTED);
        cartRepository.save(cart);

        // 10. Tạo `payments`
        Payment payment = new Payment();
        payment.setOrder(savedOrder);
        payment.setProvider(dto.getPaymentMethod());
        payment.setAmount(total);
        payment.setStatus(Payment.PaymentStatus.PENDING);
        paymentRepository.save(payment);

        // 11. Giao dịch COMMIT tại đây

        // 12. Trả về
        String paymentUrl = null;
        if (dto.getPaymentMethod().equals("VNPAY")) {
            // paymentUrl = vnpayService.createPaymentUrl(savedOrder.getId(), total);
        }

        return new OrderConfirmationDto(
                savedOrder.getId(),
                savedOrder.getOrderNumber(),
                savedOrder.getStatus().name(),
                payment.getStatus().name(),
                paymentUrl
        );
    }

    private String generateOrderNumber() {
        String orderNumber = "";
        do {
            String randomPart = RandomStringUtils.randomAlphanumeric(8).toUpperCase();
            orderNumber = "ORD-" + randomPart;
        }while (orderRepository.existsByOrderNumber(orderNumber));

        return orderNumber;
    }

    private Discount validateAndGetDiscount(String discountCode, long subtotal) {
        return null;
    }

    private long getShippingFee(@NotNull @Valid UserAddress shippingAddress) {
        return 0;
    }

    private long calculateSubtotal(Cart cart) {
        return 0;
    }
}
