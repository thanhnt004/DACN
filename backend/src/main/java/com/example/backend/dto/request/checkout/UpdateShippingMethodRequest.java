package com.example.backend.dto.request.checkout;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateShippingMethodRequest {

    /**
     * ID của phương thức vận chuyển
     * VD: "standard", "express", "same-day"
     *
     * Phải nằm trong danh sách `availableShippingMethods` của session
     */
    @NotBlank(message = "Shipping method ID không được để trống")
    private String shippingMethodId;
}