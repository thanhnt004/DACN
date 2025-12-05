package com.example.backend.dto.request.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelOrderRequest {
    @Schema(description = "Lý do hủy đơn hàng", example = "Tôi đổi ý không muốn mua nữa", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Lý do hủy đơn không được để trống")
    @Size(min = 5, max = 500, message = "Lý do phải từ 5 đến 500 ký tự")
    private String reason;
    private PaymentRefundOption paymentRefundOption;
}
