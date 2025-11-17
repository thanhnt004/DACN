package com.example.backend.dto.response.discount;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountResult {
    private Boolean isValid;
    //discount code
    private String code;

    private UUID discountId;

    private String type;

    private Long value;

    private Long discountAmount;

    private String errorMessage;

    public static DiscountResult noDiscount() {
        return DiscountResult.builder()
                .isValid(false)
                .discountAmount(0L)
                .build();
    }

    public static DiscountResult invalid(String errorMessage) {
        return DiscountResult.builder()
                .isValid(false)
                .discountAmount(0L)
                .errorMessage(errorMessage)
                .build();
    }
}
