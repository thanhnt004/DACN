package com.example.backend.dto.response.shipping;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingOption {

    /**
     * ID của phương thức vận chuyển
     */
    private String id;
    private String serviceLevel;
    /**
     * Tên hiển thị
     * VD: "Giao hàng tiêu chuẩn", "Giao hàng nhanh"
     */
    private String name;

    /**
     * Mô tả chi tiết
     * VD: "Giao trong 3-5 ngày làm việc"
     */
    private String description;

    /**
     * Nhà vận chuyển
     * VD: "Giao Hàng Nhanh", "Giao Hàng Tiết Kiệm", "Viettel Post"
     */
    private String carrier;

    /**
     * Phí vận chuyển (VND)
     */
    private Integer amount;

    /**
     * Thời gian giao hàng ước tính (ngày)
     */
    private Integer estimatedDays;

    /**
     * Có phải là tùy chọn mặc định không
     */
    private Boolean isDefault;

    /**
     * Có khả dụng với địa chỉ hiện tại không
     */
    private Boolean isAvailable;

    /**
     * Lý do không khả dụng (nếu isAvailable = false)
     */
    private String unavailableReason;
}