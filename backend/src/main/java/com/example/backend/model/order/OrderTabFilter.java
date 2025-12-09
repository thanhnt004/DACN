package com.example.backend.model.order;

import lombok.Getter;

/**
 * Enum định nghĩa các tab lọc đơn hàng theo yêu cầu nghiệp vụ
 * Kết hợp trạng thái đơn hàng và phương thức thanh toán
 */
@Getter
public enum OrderTabFilter {
    /**
     * Tab 1: Tất cả - Hiển thị tất cả đơn hàng
     */
    ALL("Tất cả", "No filter"),
    
    /**
     * Tab 2: Chưa thanh toán
     * Logic: Status == PENDING AND PaymentMethod != COD AND Payment Status != SUCCESS
     * Đơn hàng thanh toán online đã khởi tạo nhưng chưa thanh toán/verify (VNPAY, MOMO chưa thanh toán)
     */
    UNPAID("Chưa thanh toán", "Status == PENDING AND PaymentMethod != COD AND Payment Status != SUCCESS"),
    
    /**
     * Tab 3: Chờ xác nhận
     * Logic: (Status == CONFIRMED) OR (Status == PENDING AND PaymentMethod == COD)
     * Bao gồm: Đơn hàng đã thanh toán online (CONFIRMED) HOẶC đơn COD mới chờ merchant duyệt (PENDING + COD)
     */
    TO_CONFIRM("Chờ xác nhận", "(Status == CONFIRMED) OR (Status == PENDING AND PaymentMethod == COD)"),
    
    /**
     * Tab 4: Chuẩn bị hàng
     * Logic: Status == PROCESSING
     * Đơn hàng đang được đóng gói
     */
    PROCESSING("Chuẩn bị hàng", "Status == PROCESSING"),
    
    /**
     * Tab 5: Đang giao
     * Logic: Status == SHIPPED
     * Đơn hàng đang được vận chuyển
     */
    SHIPPING("Đang giao", "Status == SHIPPED"),
    
    /**
     * Tab 6: Đã giao
     * Logic: Status == DELIVERED
     * Đơn hàng đã giao thành công
     */
    COMPLETED("Đã giao", "Status == DELIVERED"),
    
    /**
     * Tab 7: Yêu cầu hủy
     * Logic: Status == CANCELING
     * Khách hàng yêu cầu hủy, chờ duyệt
     */
    CANCEL_REQ("Yêu cầu hủy", "Status == CANCELING"),
    
    /**
     * Tab 8: Đã hủy
     * Logic: Status == CANCELLED
     * Đơn hàng đã bị hủy
     */
    CANCELLED("Đã hủy", "Status == CANCELLED"),
    
    /**
     * Tab 9: Yêu cầu trả hàng
     * Logic: Status == RETURNING
     * Khách yêu cầu trả hàng / Đang trả hàng
     */
    RETURN_REQ("Yêu cầu trả hàng", "Status == RETURNING"),
    
    /**
     * Tab 10: Đã trả hàng
     * Logic: Status == REFUNDED OR Status == RETURNED
     * Hàng đã được trả và hoàn tiền
     */
    REFUNDED("Đã trả hàng", "Status == REFUNDED OR Status == RETURNED"),

    /**
     * Tab 11: Chờ hoàn tiền
     * Logic: ChangeRequest.Status == WAITING_REFUND
     * Đơn hàng đã hủy/trả và đang chờ hoàn tiền
     */
    WAITING_REFUND("Chờ hoàn tiền", "ChangeRequest.Status == WAITING_REFUND");

    private final String displayName;
    private final String description;

    OrderTabFilter(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Chuyển đổi từ string sang enum, case-insensitive
     */
    public static OrderTabFilter fromString(String tab) {
        if (tab == null || tab.trim().isEmpty()) {
            return ALL;
        }
        
        try {
            return OrderTabFilter.valueOf(tab.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ALL; // Default to ALL if invalid
        }
    }
}
