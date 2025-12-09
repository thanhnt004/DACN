# Audit Log - Quick Reference

## 🎯 Mục Đích
Ghi lại tất cả hoạt động quan trọng trong hệ thống để:
- Giải quyết tranh chấp
- Phát hiện gian lận
- Audit compliance
- Debug issues

## 📁 Files Created

```
model/enumrator/
  ├── AuditActionType.java        # 15+ action types
  └── AuditEntityType.java        # 7 entity types

repository/
  └── AuditLogRepository.java     # Query methods

service/audit/
  └── AuditLogService.java        # Async logging service

dto/response/audit/
  └── AuditLogResponse.java       # Response DTO

mapper/
  └── AuditLogMapper.java         # Mapper

controller/audit/
  └── AuditLogController.java     # REST API
```

## 🔧 Modified Services

```
✅ OrderService.java              # updateStatus()
✅ OrderFacadeService.java        # cancelOrderByAdmin(), returnOrderByAdmin(), reviewChangeRequest()
✅ ProductVariantService.java     # update() - price changes
✅ ProductInventoryService.java   # update() - stock adjustments
✅ UserManagerService.java        # band(), restoreUser(), grantAdminRole(), revokeAdminRole()
```

## 📊 Action Types

### Order Actions
- `UPDATE_ORDER_STATUS` - Thay đổi trạng thái
- `CANCEL_ORDER` - Hủy đơn
- `RETURN_ORDER` - Trả đơn
- `CONFIRM_ORDER` - Xác nhận
- `SHIP_ORDER` - Giao hàng

### Product Actions
- `UPDATE_PRODUCT_PRICE` - Đổi giá sản phẩm
- `UPDATE_VARIANT_PRICE` - Đổi giá variant
- `ADJUST_STOCK_MANUAL` - Điều chỉnh kho

### User Actions
- `CHANGE_USER_ROLE` - Đổi role
- `BAN_USER` - Khóa user
- `RESTORE_USER` - Khôi phục user

### Request Actions
- `APPROVE_CHANGE_REQUEST` - Duyệt yêu cầu
- `REJECT_CHANGE_REQUEST` - Từ chối yêu cầu

## 🌐 API Endpoints (Admin Only)

### 1. Get All Logs (with filters)
```
GET /api/v1/admin/audit-logs
```
**Query Params:**
- `entityType` - ORDER, PRODUCT_VARIANT, USER, etc.
- `action` - UPDATE_ORDER_STATUS, CANCEL_ORDER, etc.
- `entityId` - UUID
- `actorId` - UUID
- `startDate` - ISO datetime
- `endDate` - ISO datetime
- `page`, `size`, `sort`

**Example:**
```bash
GET /api/v1/admin/audit-logs?entityType=ORDER&action=CANCEL_ORDER&startDate=2025-01-01T00:00:00Z&page=0&size=20
```

### 2. Get Entity History (10 recent)
```
GET /api/v1/admin/audit-logs/entity/{entityId}
```

### 3. Get Entity History (paginated)
```
GET /api/v1/admin/audit-logs/entity/{entityId}/paginated?page=0&size=20
```

## 💻 Code Usage

### In Service Layer
```java
// Inject service
private final AuditLogService auditLogService;

// Log an action
Map<String, Object> metadata = new HashMap<>();
metadata.put("order_number", order.getOrderNumber());
metadata.put("old_status", oldStatus.name());
metadata.put("new_status", newStatus.name());
metadata.put("reason", reason);

auditLogService.logAction(
    AuditActionType.UPDATE_ORDER_STATUS,
    AuditEntityType.ORDER,
    order.getId(),
    metadata
);
```

## 🔍 Common Use Cases

### 1. Điều tra đơn hàng bị tranh chấp
```bash
# Xem lịch sử thay đổi trạng thái
GET /api/v1/admin/audit-logs/entity/{orderId}

# Hoặc filter theo action
GET /api/v1/admin/audit-logs?entityType=ORDER&action=UPDATE_ORDER_STATUS&entityId={orderId}
```

### 2. Tìm ai đã thay đổi giá
```bash
GET /api/v1/admin/audit-logs?entityType=PRODUCT_VARIANT&action=UPDATE_VARIANT_PRICE&startDate=2025-01-01T00:00:00Z
```

### 3. Kiểm tra điều chỉnh kho
```bash
GET /api/v1/admin/audit-logs?entityType=INVENTORY&action=ADJUST_STOCK_MANUAL&entityId={variantId}
```

### 4. Audit thay đổi quyền
```bash
GET /api/v1/admin/audit-logs?entityType=USER&action=CHANGE_USER_ROLE&entityId={userId}
```

### 5. Xem hoạt động của một admin
```bash
GET /api/v1/admin/audit-logs?actorId={adminUserId}&startDate=2025-01-01T00:00:00Z
```

## 📝 Response Format

```json
{
  "content": [
    {
      "id": "uuid",
      "actor": {
        "id": "uuid",
        "email": "admin@example.com",
        "fullName": "Admin Name",
        "role": "ADMIN"
      },
      "action": "UPDATE_ORDER_STATUS",
      "actionDescription": "Cập nhật trạng thái đơn hàng",
      "entityType": "ORDER",
      "entityTypeDescription": "Đơn hàng",
      "entityId": "uuid",
      "metadata": {
        "order_number": "ORD-20250101-000001",
        "old_status": "PENDING",
        "new_status": "CONFIRMED",
        "total_amount": 500000
      },
      "traceId": null,
      "createdAt": "2025-01-01T10:00:00Z"
    }
  ],
  "totalElements": 100,
  "totalPages": 5,
  "currentPage": 0,
  "pageSize": 20
}
```

## ⚡ Key Features

✅ **Async Logging** - Không ảnh hưởng performance
✅ **REQUIRES_NEW** - Log được lưu ngay cả khi rollback
✅ **Auto Actor** - Tự động lấy user từ SecurityContext
✅ **Rich Metadata** - JSONB cho thông tin chi tiết
✅ **Flexible Query** - Filter theo nhiều điều kiện
✅ **Pagination** - Hỗ trợ phân trang
✅ **Immutable** - Không có update/delete

## 🛡️ Security

- ✅ Chỉ ADMIN được xem logs
- ✅ Logs không thể sửa/xóa
- ✅ Actor được track tự động
- ✅ Metadata có thể chứa sensitive data (cẩn thận!)

## 📚 Documentation Files

1. `AUDIT_LOG_SYSTEM.md` - Chi tiết đầy đủ (500+ dòng)
2. `AUDIT_LOG_IMPLEMENTATION_SUMMARY.md` - Tóm tắt triển khai
3. `AUDIT_LOG_QUICK_REFERENCE.md` - This file

## 🎓 Tips

1. **Metadata Design**: Luôn include old/new values
2. **Performance**: Logs chạy async, không lo performance
3. **Debugging**: Dùng traceId để trace request
4. **Archival**: Cân nhắc archive logs cũ hơn 90 ngày
5. **Monitoring**: Setup alerts cho actions bất thường

## 🚨 Troubleshooting

### Logs không được tạo?
1. Check `@EnableAsync` configuration
2. Check transaction propagation
3. Check exception handling

### Missing actor?
1. Ensure SecurityContext is set
2. For system actions, pass actor=null explicitly

### Performance issues?
1. Check database indexes
2. Consider archiving old logs
3. Use pagination

## 📞 Support

Đọc thêm tài liệu chi tiết: `AUDIT_LOG_SYSTEM.md`

