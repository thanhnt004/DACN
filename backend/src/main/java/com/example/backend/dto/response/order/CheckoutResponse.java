package com.example.backend.dto.response.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Lớp này đại diện cho cấu trúc JSON trả về cho frontend
 * sau khi người dùng yêu cầu đặt hàng (checkout).
 * * Nó có thể xử lý cho cả 2 trường hợp:
 * 1. COD (paymentUrl sẽ là null và bị ẩn).
 * 2. Cổng thanh toán như VNPay (paymentUrl sẽ chứa link redirect).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CheckoutResponse {

    private String status;

    private String message;

    private UUID orderId;

    private String orderNumber;

    private Long totalAmount;
    /**
     * Phương thức thanh toán.
     * Ví dụ: "COD", "VNPAY"
     */
    private String paymentMethod;

    /**
     * Trạng thái của bản ghi 'payments' liên quan.
     * Ví dụ: "PENDING"
     */
    private String paymentStatus;

    /**
     * Trạng thái của bản ghi 'orders'.
     * Ví dụ: "PROCESSING" (cho COD), "PENDING" (cho VNPay)
     */
    private String orderStatus;

    /**
     * Gợi ý hành động tiếp theo cho frontend.
     * Ví dụ: "REDIRECT_TO_SUCCESS", "REDIRECT_TO_PAYMENT_GATEWAY"
     */
    private String nextAction;

    /**
     * URL của cổng thanh toán.
     * Trường này SẼ LÀ NULL trong trường hợp thanh toán COD.
     * Trường này SẼ CÓ GIÁ TRỊ trong trường hợp thanh toán VNPay.
     */
    private String paymentUrl;
}