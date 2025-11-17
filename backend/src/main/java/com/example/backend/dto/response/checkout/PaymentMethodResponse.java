package com.example.backend.dto.response.checkout;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Phương thức thanh toán khả dụng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodResponse {

    /**
     * ID của phương thức thanh toán
     * VD: "cod", "vnpay", "momo", "credit-card"
     */
    private String id;

    /**
     * Tên hiển thị
     * VD: "Thanh toán khi nhận hàng", "VNPay", "MoMo"
     */
    private String name;

    /**
     * Loại
     * - COD: Cash on delivery
     * - EWALLET: Ví điện tử
     * - BANK_TRANSFER: Chuyển khoản
     * - CREDIT_CARD: Thẻ tín dụng
     */
    private String type;

    /**
     * Mô tả
     */
    private String description;

    /**
     * Icon/Logo URL
     */
    private String iconUrl;

    /**
     * Có khả dụng không
     */
    private Boolean isAvailable;

    /**
     * Lý do không khả dụng (nếu có)
     * VD: "Đơn hàng quá lớn cho COD"
     */
    private String unavailableReason;

    /**
     * Phí xử lý (nếu có)
     * VD: VNPay tính phí 1%
     */
    private Long feeAmount;

    /**
     * Có phải phương thức được khuyến nghị không
     */
    private Boolean isRecommended;
}