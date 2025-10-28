package com.example.backend.service.order;

import com.example.backend.dto.request.order.DirectCheckoutRequest;
import com.example.backend.dto.request.order.PlaceOrderRequest;
import com.example.backend.dto.response.order.OrderConfirmationDto;
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
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
    @Transactional
    public UUID prepareDirectCheckout(DirectCheckoutRequest dto, User user) {

        // 1. Kiểm tra tồn kho (lần 1)
        Inventory inventory = inventoryRepository.findById(dto.getVariantId())
                .orElseThrow(() -> new RuntimeException("Inventory not found"));

        if (dto.getQuantity() > inventory.getAvailableStock()) {
            throw new ConflictException("Không đủ hàng");
        }

        // 2. Lấy thông tin variant
        ProductVariant variant = variantRepository.findById(dto.getVariantId())
                .orElseThrow(() -> new RuntimeException("Variant not found"));

        // 3. Tạo giỏ hàng tạm thời
        Cart tempCart = new Cart();
        tempCart.setUser(user); // Sẽ là null nếu user là guest
        tempCart.setStatus(Cart.CartStatus.CHECKOUT_SESSION); // Status đặc biệt
        Cart savedCart = cartRepository.save(tempCart);

        // 4. Tạo cart item
        CartItem item = new CartItem();
        item.setCart(savedCart);
        item.setVariant(variant);
        item.setQuantity(dto.getQuantity());
        item.setUnitPriceAmount(variant.getPriceAmount()); // Lấy giá từ variant

        // Thêm item vào list và lưu lại
        savedCart.getItems().add(item);
        cartRepository.save(savedCart);

        return savedCart.getId();
    }
    @Transactional(isolation = Isolation.SERIALIZABLE) // Mức độ cô lập cao nhất
    public OrderConfirmationDto placeOrder(PlaceOrderRequest dto, User user) {

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
        List<Inventory> inventories = inventoryRepository.findAllById(variantIds, LockModeType.PESSIMISTIC_WRITE);

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

        order.setShippingAddress(dto.getShippingAddress); // (Chuyển đổi sang JSON string nếu cần)

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
}
