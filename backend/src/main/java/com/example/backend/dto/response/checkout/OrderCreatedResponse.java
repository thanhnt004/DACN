package com.example.backend.dto.response.checkout;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response sau khi tạo order thành công
 *
 * Frontend sẽ dùng paymentUrl để redirect user đến payment gateway
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedResponse {

    /**
     * ID của đơn hàng vừa tạo
     */
    @JsonProperty("id")
    private UUID orderId;

    /**
     * Mã đơn hàng (dạng human-readable)
     * VD: "ORD-20250104-ABC123"
     */
    private String orderNumber;

    /**
     * Trạng thái đơn hàng
     * Thường là "PENDING" hoặc "AWAITING_PAYMENT"
     */
    private String status;

    /**
     * Tổng tiền phải thanh toán
     */
    private Long totalAmount;

    /**
     * ID của payment record
     */
    private UUID paymentId;

    /**
     * Phương thức thanh toán đã chọn
     */
    private String paymentMethod;

    /**
     * URL để redirect user đến trang thanh toán
     *
     * - VNPay/MoMo/ZaloPay: URL của payment gateway
     * - COD: URL trang xác nhận đơn hàng
     * - Bank Transfer: URL trang hướng dẫn chuyển khoản
     */
    private String paymentUrl;

    /**
     * URL để redirect sau khi thanh toán thành công
     * Backend sẽ xử lý callback từ payment gateway
     */
    private String returnUrl;

    /**
     * Thời gian hết hạn thanh toán (nếu có)
     * VD: VNPay cho 15 phút để thanh toán
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant paymentExpiresAt;

    /**
     * Thời gian tạo đơn hàng
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant createdAt;

    /**
     * Thông tin bổ sung (nếu cần)
     * VD: QR code cho VNPay, số tài khoản cho Bank Transfer
     */
    private PaymentInstructions instructions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentInstructions {
        /**
         * Loại hướng dẫn
         * - QR_CODE: Hiển thị QR code
         * - BANK_INFO: Hiển thị thông tin tài khoản
         * - REDIRECT: Redirect ngay
         */
        private String type;

        /**
         * QR code data (nếu type = QR_CODE)
         */
        private String qrCodeData;

        /**
         * Thông tin tài khoản (nếu type = BANK_INFO)
         */
        private BankInfo bankInfo;

        /**
         * Nội dung hướng dẫn
         */
        private String instructions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BankInfo {
        private String bankName;
        private String accountNumber;
        private String accountName;
        private String transferContent; // Nội dung chuyển khoản
    }
}