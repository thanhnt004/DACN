package com.example.backend.dto.response.discount;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountValidationResponse {
    private Boolean isValid;

    private DiscountResult result;

    private String message;

    public static DiscountValidationResponse valid(DiscountResult result) {
        return DiscountValidationResponse.builder()
                .isValid(true)
                .result(result)
                .message("Mã giảm giá hợp lệ")
                .build();
    }

    public static DiscountValidationResponse invalid(String message) {
        return DiscountValidationResponse.builder()
                .isValid(false)
                .result(DiscountResult.noDiscount())
                .message(message)
                .build();
    }
}
