package com.example.backend.model.enumrator;

/**
 * Enum định nghĩa các loại entity được audit trong hệ thống
 */
public enum AuditEntityType {
    ORDER("Đơn hàng"),
    PRODUCT("Sản phẩm"),
    PRODUCT_VARIANT("Phiên bản sản phẩm"),
    INVENTORY("Kho hàng"),
    USER("Người dùng"),
    PAYMENT("Thanh toán"),
    ORDER_CHANGE_REQUEST("Yêu cầu thay đổi đơn hàng");

    private final String description;

    AuditEntityType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

