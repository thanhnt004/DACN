package com.example.backend.dto.request.checkout;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutSessionCreateRequest  {
    //Optional
    private UUID cartId;

    @NotEmpty(message = "Danh sách sản phẩm không được để trống!")
    @Valid
    private List<CheckOutItem> items;
    @Pattern(
            regexp = "^[A-Z0-9]{4,20}$",
            message = "Mã giảm giá phải từ 4-20 ký tự chữ hoa và số"
    )
    private String discountCode;
}
