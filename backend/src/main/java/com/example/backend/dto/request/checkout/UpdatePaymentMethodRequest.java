package com.example.backend.dto.request.checkout;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request để chọn phương thức thanh toán
 *
 * Endpoint: PUT /api/v1/checkout/sessions/{sessionId}/payment-method
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePaymentMethodRequest {

    /**
     * ID của phương thức thanh toán
     * VD: "vnpay", "momo", "cod", "bank-transfer"
     *
     * Phải nằm trong danh sách `availablePaymentMethods` của session
     */
    @NotBlank(message = "Payment method ID không được để trống")
    private String paymentMethodId;
}