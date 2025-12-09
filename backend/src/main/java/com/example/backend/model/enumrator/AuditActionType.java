package com.example.backend.model.enumrator;

/**
 * Enum định nghĩa các loại hành động được audit trong hệ thống
 */
public enum AuditActionType {
    // Order Status Actions
    UPDATE_ORDER_STATUS("Cập nhật trạng thái đơn hàng"),
    CANCEL_ORDER("Hủy đơn hàng"),
    RETURN_ORDER("Trả đơn hàng"),
    CONFIRM_ORDER("Xác nhận đơn hàng"),
    SHIP_ORDER("Giao đơn hàng"),

    // Product & Inventory Actions
    UPDATE_PRODUCT_PRICE("Cập nhật giá sản phẩm"),
    UPDATE_VARIANT_PRICE("Cập nhật giá phiên bản"),
    ADJUST_STOCK_MANUAL("Điều chỉnh tồn kho thủ công"),
    CREATE_PRODUCT("Tạo sản phẩm mới"),
    DELETE_PRODUCT("Xóa sản phẩm"),
    UPDATE_PRODUCT_STATUS("Cập nhật trạng thái sản phẩm"),

    // User & Permission Actions
    CHANGE_USER_ROLE("Thay đổi vai trò người dùng"),
    BAN_USER("Khóa tài khoản người dùng"),
    RESTORE_USER("Khôi phục tài khoản người dùng"),

    // Payment Actions
    APPROVE_REFUND("Duyệt hoàn tiền"),
    REJECT_REFUND("Từ chối hoàn tiền"),

    // Change Request Actions
    APPROVE_CHANGE_REQUEST("Duyệt yêu cầu thay đổi"),
    REJECT_CHANGE_REQUEST("Từ chối yêu cầu thay đổi");

    private final String description;

    AuditActionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

