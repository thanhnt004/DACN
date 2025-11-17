package com.example.backend.dto.request.checkout;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateDiscountRequest {
    @Pattern(
            regexp = "^[A-Z0-9]{4,20}$",
            message = "Mã giảm giá phải từ 4-20 ký tự chữ hoa và số"
    )
    private String code;
}
