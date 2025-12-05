package com.example.backend.dto.request.order;

import com.example.backend.dto.response.user.UserAddress;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnOrderRequest {
    @Schema(description = "Địa chỉ nhận hàng trả lại")
    @NotNull
    private UserAddress returnAddress;

    @Schema(description = "Phương thức trả hàng", example = "PICKUP", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Phương thức trả hàng không được để trống")
    private ReturnOption returnOption;

    @Schema(description = "Lý do trả hàng", example = "Sản phẩm bị lỗi, không giống mô tả", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Lý do trả hàng không được để trống")
    @Size(min = 10, max = 1000, message = "Lý do phải từ 10 đến 1000 ký tự")
    private String reason;

    @Schema(description = "Danh sách URL hình ảnh/video bằng chứng tình trạng sản phẩm", example = "[\"https://s3.aws.com/img1.jpg\", \"https://s3.aws.com/img2.jpg\"]")
    private List<String> images;

    private PaymentRefundOption paymentRefundOption;
}
