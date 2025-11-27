package com.example.backend.service.facade;

import com.example.backend.dto.request.checkout.*;
import com.example.backend.dto.response.catalog.VariantStockStatus;
import com.example.backend.dto.response.checkout.CheckoutItemDetail;
import com.example.backend.dto.response.checkout.CheckoutSession;
import com.example.backend.dto.response.checkout.PaymentMethodResponse;
import com.example.backend.dto.response.discount.DiscountResult;
import com.example.backend.dto.response.shipping.ShippingOption;
import com.example.backend.dto.response.user.UserAddress;
import com.example.backend.exception.ConflictException;
import com.example.backend.mapper.AddressMapper;
import com.example.backend.model.User;
import com.example.backend.service.auth.AccessTokenService;
import com.example.backend.service.redis.RedisCheckoutStoreService;
import com.example.backend.service.shipping.ShippingService;
import com.example.backend.service.payment.PaymentService;
import com.example.backend.service.cart.CartService;
import com.example.backend.service.discount.DiscountService;
import com.example.backend.service.product.InventoryService;
import com.example.backend.service.product.ProductVariantService;
import com.example.backend.service.user.UserManagerService;
import com.example.backend.util.AuthenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CheckoutFacadeService {
    private final AuthenUtil authenUtil;
    private final DiscountService discountService;
    private final ShippingService shippingService;
    private final PaymentService paymentService;
    private final InventoryService inventoryService;
    private final ProductVariantService variantService;
    private final UserManagerService userManagerService;

    private final RedisCheckoutStoreService sessionStore;

    private final AddressMapper addressMapper;
    public CheckoutSession createCheckoutSession(CheckoutSessionCreateRequest request) {
        log.info("Initializing checkout session! ");
        //Nếu đã đăng nhập
        Optional<User> userOps = authenUtil.getAuthenUser();
        Optional<UserAddress> userAddress = Optional.empty();
        String sessionToken = UUID.randomUUID().toString();
        if (userOps.isPresent()) {
            userAddress = userManagerService.getDefaultAddress(userOps.get()).map(addressMapper::toDto);
        }
        UUID userId = userOps.map(User::getId).orElse(null);
        //Lấy thông tin items
        //Lấy thông tin variant
        List<CheckOutItem> checkOutItems = request.getItems();
        List<CheckoutItemDetail> items = getItems(checkOutItems);
        if (items.isEmpty()) {
            throw new ConflictException("Không có sản phẩm để checkout");
        }
        //Lấy cartItemId nếu check out qua cart
        if (request.getCartId()!=null)
        {
            Map<UUID,UUID> cartItemIdMap = checkOutItems.stream().collect(Collectors.toMap(
                    CheckOutItem::getVariantId,
                    CheckOutItem::getCartItemId
            ));
            items.stream().peek(item-> item.setCartItemId(cartItemIdMap.get(item.getVariantId()))).toList();
        }
        //Lấy thông tin tồn kho
        Map<UUID, VariantStockStatus> stockStatusMap = inventoryService.getStockStatusForVariants(
                items.stream()
                        .map(CheckoutItemDetail::getVariantId)
                        .toList()
        );
        items.stream().peek(item-> item.setStockStatus(stockStatusMap.get(item.getVariantId()))).toList();
        //lấy số lượng
        Map<UUID, Integer> quantityMap = checkOutItems.stream().collect(Collectors.toMap(
                CheckOutItem::getVariantId,
                CheckOutItem::getQuantity
        ));
        items.stream().peek(item-> item.setQuantity(quantityMap.get(item.getVariantId()))).toList();
        //tính tiền chưa giảm giá
        Long subtotalAmount = items.stream()
                .mapToLong(CheckoutItemDetail::getTotalAmount)
                .sum();
        // Áp dụng mã giảm giá nếu có
        Long discountAmount = 0L;
        DiscountResult discountResult = null;
        if (request.getDiscountCode() != null) {
             discountResult = discountService.validateAndCalculate(
                    request.getDiscountCode(),
                    subtotalAmount,
                    items,
                    userId
            );

            if (discountResult.getIsValid()) {
                discountAmount = discountResult.getDiscountAmount();
            }
        }
        //Lấy thông tin shipping options
        List<ShippingOption> availableShippingMethods = shippingService.getAvailableShippingOptions(
                userAddress,
                items
        );
        ShippingOption selectedShippingMethod = null;
        if (availableShippingMethods.getFirst().getIsAvailable())
        {
            selectedShippingMethod = availableShippingMethods.getFirst();
        }
        Long shippingAmount = selectedShippingMethod != null ? selectedShippingMethod.getAmount().longValue() : 0L;

        //Tính tổng tiền
        Long totalAmount = subtotalAmount - discountAmount + shippingAmount;
        //Lấy thông tin phương thức thanh toán
        List<PaymentMethodResponse> paymentMethodResponses = paymentService.getAvailablePaymentMethods(totalAmount);
        PaymentMethodResponse paymentMethodResponse = paymentMethodResponses.get(0);
        //Tạo phiên thanh toán
        CheckoutSession checkoutSession = CheckoutSession.builder()
                .id(UUID.randomUUID())
                .items(items)
                .subtotalAmount(subtotalAmount)
                .shippingAmount(shippingAmount)
                .discountAmount(discountAmount)
                .discountInfo(discountResult)
                .totalAmount(totalAmount)
                .shippingAddress(userAddress.orElse(null))
                .selectedShippingMethod(selectedShippingMethod)
                .availableShippingMethods(availableShippingMethods)
                .availablePaymentMethods(paymentMethodResponses)
                .selectedPaymentMethod(paymentMethodResponse)
                .createdAt(Instant.now())
                .userId(userId)
                .sessionToken(sessionToken)
                .build();
        //Luuw phiên thanh toán vào redis
        checkoutSession.setCanConfirm(canConfirmSession(checkoutSession));
        sessionStore.save(checkoutSession);
        return checkoutSession;
    }
    @Transactional
    public CheckoutSession updateAddress(
            UUID sessionId,
            UpdateAddressRequest newAddress
    ) {
        var session = sessionStore.findById(sessionId);
        if (session.isEmpty()) {
            throw new ConflictException("Phiên thanh toán không tồn tại hoặc đã hết hạn");
        }
        var checkoutSession = session.get();
        //Cập nhật địa chỉ
        checkoutSession.setShippingAddress(newAddress.getUserAddress());
        //Cập nhật phương thức vận chuyển
        List<ShippingOption> availableShippingMethods = shippingService.getAvailableShippingOptions(
                Optional.ofNullable(newAddress.getUserAddress()),
                checkoutSession.getItems()
        );
        checkoutSession.setAvailableShippingMethods(availableShippingMethods);
        ShippingOption selectedShippingMethod = null;
        if (availableShippingMethods.getFirst().getIsAvailable())
        {
            selectedShippingMethod = availableShippingMethods.getFirst();
        }
        checkoutSession.setSelectedShippingMethod(selectedShippingMethod);
        //Cập nhật phí vận chuyển
        Long shippingAmount = selectedShippingMethod != null ? selectedShippingMethod.getAmount().longValue() : 0L;
        checkoutSession.setShippingAmount(shippingAmount);
        //Cập nhật tổng tiền
        Long totalAmount = checkoutSession.getSubtotalAmount()
                - checkoutSession.getDiscountAmount()
                + shippingAmount;
        checkoutSession.setTotalAmount(totalAmount);
        //Lưu lại phiên thanh toán
        checkoutSession.setCanConfirm(canConfirmSession(checkoutSession));
        sessionStore.save(checkoutSession);
        return checkoutSession;
    }
    public CheckoutSession updateDiscount(
            String sessionId,
            UpdateDiscountRequest request
    ) {
        //Lấy phiên thanh toán
        CheckoutSession checkoutSession = sessionStore.findById(UUID.fromString(sessionId))
                .orElseThrow(() -> new ConflictException("Phiên thanh toán không tồn tại hoặc đã hết hạn"));
        //Áp dụng mã giảm giá
        DiscountResult discountResult = discountService.validateAndCalculate(request.getCode(),
                checkoutSession.getSubtotalAmount(),
                checkoutSession.getItems(),
                checkoutSession.getUserId()
        );
        Long discountAmount = 0L;
        if (discountResult.getIsValid()) {
            discountAmount = discountResult.getDiscountAmount();}
        checkoutSession.setDiscountAmount(discountAmount);
        checkoutSession.setDiscountInfo(discountResult);
        //Cập nhật tổng tiền
        Long totalAmount = checkoutSession.getSubtotalAmount()
                - discountAmount
                + checkoutSession.getShippingAmount();
        checkoutSession.setTotalAmount(totalAmount);
        checkoutSession.setUpdatedAt(Instant.now());
        //Lưu lại phiên thanh toán
        checkoutSession.setCanConfirm(canConfirmSession(checkoutSession));
        sessionStore.save(checkoutSession);
        return checkoutSession;

    }
    public CheckoutSession updatePaymentMethod(
            String sessionId,
            UpdatePaymentMethodRequest request
    ) {
        CheckoutSession checkoutSession = sessionStore.findById(UUID.fromString(sessionId))
                .orElseThrow(() -> new ConflictException("Phiên thanh toán không tồn tại hoặc đã hết hạn"));
        //Cập nhật phương thức thanh toán
        List<PaymentMethodResponse> paymentMethods = checkoutSession.getAvailablePaymentMethods();
        Optional<PaymentMethodResponse> selectedMethodOpt = paymentMethods.stream()
                .filter(method -> method.getId().equals(request.getPaymentMethodId()))
                .findFirst();
        if (selectedMethodOpt.isEmpty()) {
            throw new ConflictException("Phương thức thanh toán không hợp lệ");
        }
        checkoutSession.setSelectedPaymentMethod(selectedMethodOpt.get());
        checkoutSession.setUpdatedAt(Instant.now());
        //Lưu lại phiên thanh toán
        checkoutSession.setCanConfirm(canConfirmSession(checkoutSession));
        sessionStore.save(checkoutSession);

        return checkoutSession;
    }
    public boolean canConfirmSession(CheckoutSession session) {
        // Kiểm tra các điều kiện cần thiết
        if (session.getShippingAddress() == null) {
            return false;
        }

        if (session.getSelectedPaymentMethod() == null) {
            return false;
        }

        // Kiểm tra stock cho tất cả items
        for (CheckoutItemDetail item : session.getItems()) {
            int availableStock = inventoryService.getAvailableStock(item.getVariantId());
            if (availableStock < item.getQuantity()) {
                return false;
            }
        }

        return true;
    }

    public void deleteSession(UUID sessionId) {
        sessionStore.delete(sessionId);
        log.debug("Deleted session: {}", sessionId);
    }
    private List<CheckoutItemDetail> getItems(
            List<CheckOutItem> checkOutItems
    ) {
        return variantService.getItemsForCheckout(
                checkOutItems
        );
    }

    public CheckoutSession getSession(UUID sessionId) {
        return sessionStore.findById(sessionId)
                .orElseThrow(() -> new ConflictException("Phiên thanh toán không tồn tại hoặc đã hết hạn"));
    }


}
