package com.example.backend.dto.response.checkout;

import com.example.backend.dto.response.catalog.VariantStockStatus;
import com.example.backend.dto.response.discount.DiscountResult;
import com.example.backend.dto.response.shipping.ShippingOption;
import com.example.backend.dto.response.user.UserAddress;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("checkout_sessions")
public class CheckoutSession implements Serializable {
    private UUID id;
    /**
     * Danh sách sản phẩm trong session
     */
    private UUID cartId;
    private List<CheckoutItemDetail> items;

    // ============== PRICING ==============
    /**
     * Tổng tiền sản phẩm (chưa giảm giá)
     */
    private Long subtotalAmount;

    /**
     * Số tiền được giảm giá
     */
    private Long discountAmount;

    /**
     * Phí vận chuyển
     */
    private Long shippingAmount;

    /**
     * Tổng tiền phải thanh toán
     */
    private Long totalAmount;

    // ============== DISCOUNT ==============

    private DiscountResult discountInfo;

    // ============== SHIPPING ==============

    /**
     * Địa chỉ giao hàng đã chọn (nếu có)
     */
    private UserAddress shippingAddress;

    /**
     * Phương thức vận chuyển đã chọn (nếu có)
     */
    private ShippingOption selectedShippingMethod;

    /**
     * Danh sách các phương thức vận chuyển khả dụng
     */
    private List<ShippingOption> availableShippingMethods;
    // ============== PAYMENT ==============

    /**
     * Phương thức thanh toán đã chọn (nếu có)
     */
    private PaymentMethodResponse selectedPaymentMethod;

    /**
     * Danh sách phương thức thanh toán khả dụng
     */
    private List<PaymentMethodResponse> availablePaymentMethods;
    // ============== VALIDATION ==============


    /**
     * Session có thể confirm không
     * Chỉ true khi:
     * - Có địa chỉ giao hàng
     * - Có phương thức vận chuyển
     * - Có phương thức thanh toán
     * - Tất cả sản phẩm còn hàng
     */
    private Boolean canConfirm;

    /**
     * Các lỗi ngăn không cho confirm (nếu có)
     */
    private List<String> validationErrors;

    /**
     * Cảnh báo (không block confirm)
     */
    private List<String> warnings;

    // ============== METADATA ==============

    /**
     * Thời gian tạo session
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant createdAt;

    /**
     * Thời gian cập nhật cuối
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant updatedAt;

    /**
     * Thời điểm hết hạn session (thường 30 phút)
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant expiresAt;

    private String sessionToken;

    private UUID userId;
}
