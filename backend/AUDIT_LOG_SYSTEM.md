# Hệ Thống Audit Log

## Tổng Quan

Hệ thống audit log được thiết kế để ghi lại tất cả các hoạt động quan trọng trong hệ thống, đặc biệt là:
- Thay đổi trạng thái đơn hàng
- Thay đổi giá sản phẩm và tồn kho
- Thay đổi quyền và trạng thái người dùng

## Cấu Trúc

### 1. Entity: AuditLog
**File**: `model/AuditLog.java`

Cấu trúc bảng:
```java
- id: UUID (Primary Key)
- actor: User (người thực hiện hành động)
- action: String (loại hành động)
- entityType: String (loại entity bị tác động)
- entityId: UUID (ID của entity)
- metadata: JSONB (thông tin chi tiết)
- traceId: String (để trace request)
- createdAt: Instant (thời gian tạo)
```

### 2. Enums

#### AuditActionType
**File**: `model/enumrator/AuditActionType.java`

Các action được audit:
- **Order Actions**: 
  - `UPDATE_ORDER_STATUS`: Cập nhật trạng thái đơn hàng
  - `CANCEL_ORDER`: Hủy đơn hàng
  - `RETURN_ORDER`: Trả đơn hàng
  - `CONFIRM_ORDER`: Xác nhận đơn hàng
  - `SHIP_ORDER`: Giao đơn hàng

- **Product & Inventory Actions**:
  - `UPDATE_PRODUCT_PRICE`: Cập nhật giá sản phẩm
  - `UPDATE_VARIANT_PRICE`: Cập nhật giá phiên bản
  - `ADJUST_STOCK_MANUAL`: Điều chỉnh tồn kho thủ công

- **User & Permission Actions**:
  - `CHANGE_USER_ROLE`: Thay đổi vai trò người dùng
  - `BAN_USER`: Khóa tài khoản
  - `RESTORE_USER`: Khôi phục tài khoản

- **Change Request Actions**:
  - `APPROVE_CHANGE_REQUEST`: Duyệt yêu cầu thay đổi
  - `REJECT_CHANGE_REQUEST`: Từ chối yêu cầu thay đổi

#### AuditEntityType
**File**: `model/enumrator/AuditEntityType.java`

Các entity được audit:
- `ORDER`: Đơn hàng
- `PRODUCT`: Sản phẩm
- `PRODUCT_VARIANT`: Phiên bản sản phẩm
- `INVENTORY`: Kho hàng
- `USER`: Người dùng
- `ORDER_CHANGE_REQUEST`: Yêu cầu thay đổi đơn hàng

### 3. Repository: AuditLogRepository
**File**: `repository/AuditLogRepository.java`

Các query methods:
```java
// Tìm theo entity ID
Page<AuditLog> findByEntityId(UUID entityId, Pageable pageable)

// Tìm theo actor
Page<AuditLog> findByActor(User actor, Pageable pageable)

// Query phức tạp với nhiều filter
Page<AuditLog> findByFilters(...)

// Lấy 10 thay đổi gần nhất
List<AuditLog> findTop10ByEntityIdOrderByCreatedAtDesc(UUID entityId)
```

### 4. Service: AuditLogService
**File**: `service/audit/AuditLogService.java`

#### Đặc điểm quan trọng:
- **Async**: Sử dụng `@Async` để không ảnh hưởng performance
- **REQUIRES_NEW**: Transaction riêng, đảm bảo log được lưu ngay cả khi business transaction rollback
- **Error Handling**: Catch all exceptions để không ảnh hưởng business logic

#### Methods:
```java
// Ghi log với actor tự động (lấy từ SecurityContext)
void logAction(AuditActionType action, AuditEntityType entityType, 
               UUID entityId, Map<String, Object> metadata)

// Ghi log với actor được chỉ định rõ
void logAction(User actor, AuditActionType action, AuditEntityType entityType,
               UUID entityId, Map<String, Object> metadata)

// Query logs
Page<AuditLog> getLogsByEntityId(UUID entityId, Pageable pageable)
Page<AuditLog> getLogsByFilters(...)
List<AuditLog> getRecentLogsByEntityId(UUID entityId)
```

### 5. Controller: AuditLogController
**File**: `controller/audit/AuditLogController.java`

#### Endpoints (Chỉ dành cho ADMIN):

**GET /api/v1/admin/audit-logs**
- Lấy danh sách audit logs với filter
- Params: entityType, action, entityId, actorId, startDate, endDate
- Phân trang

**GET /api/v1/admin/audit-logs/entity/{entityId}**
- Lấy 10 thay đổi gần nhất của một entity

**GET /api/v1/admin/audit-logs/entity/{entityId}/paginated**
- Lấy lịch sử thay đổi của entity với phân trang

## Tích Hợp Vào Các Service

### 1. OrderService
**File**: `service/order/OrderService.java`

#### updateStatus()
Ghi log mỗi khi trạng thái đơn hàng thay đổi:
```java
Map<String, Object> metadata = new HashMap<>();
metadata.put("order_number", order.getOrderNumber());
metadata.put("old_status", oldStatus.name());
metadata.put("new_status", status.name());
metadata.put("total_amount", order.getTotalAmount());

auditLogService.logAction(
    AuditActionType.UPDATE_ORDER_STATUS,
    AuditEntityType.ORDER,
    order.getId(),
    metadata
);
```

### 2. OrderFacadeService
**File**: `service/facade/OrderFacadeService.java`

#### cancelOrderByAdmin()
Ghi log khi admin hủy đơn:
```java
metadata.put("order_number", order.getOrderNumber());
metadata.put("reason", adminReason);
metadata.put("is_paid", order.getPaidAt() != null);

auditLogService.logAction(
    AuditActionType.CANCEL_ORDER,
    AuditEntityType.ORDER,
    order.getId(),
    metadata
);
```

#### returnOrderByAdmin()
Ghi log khi admin xử lý trả hàng:
```java
auditLogService.logAction(
    AuditActionType.RETURN_ORDER,
    AuditEntityType.ORDER,
    order.getId(),
    metadata
);
```

#### reviewChangeRequest()
Ghi log khi admin duyệt/từ chối yêu cầu:
```java
metadata.put("request_type", request.getType());
metadata.put("review_status", dto.getStatus());
metadata.put("customer_reason", request.getReason());

auditLogService.logAction(
    AuditActionType.APPROVE_CHANGE_REQUEST, // hoặc REJECT_CHANGE_REQUEST
    AuditEntityType.ORDER_CHANGE_REQUEST,
    requestId,
    metadata
);
```

### 3. ProductVariantService
**File**: `service/product/ProductVariantService.java`

#### update()
Ghi log khi giá variant thay đổi:
```java
if (request.getPriceAmount() != null && !request.getPriceAmount().equals(oldPriceAmount)) {
    metadata.put("variant_sku", productVariant.getSku());
    metadata.put("old_price", oldPriceAmount);
    metadata.put("new_price", request.getPriceAmount());
    
    auditLogService.logAction(
        AuditActionType.UPDATE_VARIANT_PRICE,
        AuditEntityType.PRODUCT_VARIANT,
        variantId,
        metadata
    );
}
```

### 4. ProductInventoryService
**File**: `service/product/ProductInventoryService.java`

#### update()
Ghi log khi điều chỉnh tồn kho thủ công:
```java
metadata.put("old_quantity", oldQuantity);
metadata.put("new_quantity", request.getQuantityOnHand());
metadata.put("difference", request.getQuantityOnHand() - oldQuantity);

auditLogService.logAction(
    AuditActionType.ADJUST_STOCK_MANUAL,
    AuditEntityType.INVENTORY,
    variantId,
    metadata
);
```

### 5. UserManagerService
**File**: `service/user/UserManagerService.java`

#### band()
Ghi log khi khóa tài khoản:
```java
metadata.put("target_user_email", user.getEmail());
metadata.put("old_status", user.getStatus().name());
metadata.put("new_status", UserStatus.DISABLED.name());

auditLogService.logAction(
    AuditActionType.BAN_USER,
    AuditEntityType.USER,
    userId,
    metadata
);
```

#### grantAdminRole() / revokeAdminRole()
Ghi log khi thay đổi quyền:
```java
metadata.put("target_user_email", user.getEmail());
metadata.put("old_role", user.getRole().name());
metadata.put("new_role", Role.ADMIN.name());

auditLogService.logAction(
    AuditActionType.CHANGE_USER_ROLE,
    AuditEntityType.USER,
    userId,
    metadata
);
```

## Use Cases

### 1. Tranh Chấp Đơn Hàng
**Tình huống**: Khách hàng khiếu nại chưa nhận hàng nhưng đơn đã chuyển sang "Đã giao"

**Xử lý**:
```
GET /api/v1/admin/audit-logs/entity/{orderId}
```

Kết quả sẽ cho thấy:
- Ai đã chuyển trạng thái (actor)
- Thời gian chính xác (createdAt)
- Trạng thái cũ và mới (metadata)

### 2. Gian Lận Giá
**Tình huống**: Phát hiện sản phẩm có giá bán bất thường

**Xử lý**:
```
GET /api/v1/admin/audit-logs?entityType=PRODUCT_VARIANT&action=UPDATE_VARIANT_PRICE&startDate=...&endDate=...
```

Kết quả cho thấy:
- Ai đã thay đổi giá
- Giá cũ và giá mới
- Thời gian thay đổi

### 3. Điều Tra Kho
**Tình huống**: Số lượng tồn kho không khớp

**Xử lý**:
```
GET /api/v1/admin/audit-logs?entityType=INVENTORY&action=ADJUST_STOCK_MANUAL&entityId={variantId}
```

Kết quả cho thấy:
- Lịch sử điều chỉnh kho
- Số lượng thay đổi
- Người thực hiện

### 4. Kiểm Tra Quyền Truy Cập
**Tình huống**: Cần biết ai đã cấp quyền Admin cho một user

**Xử lý**:
```
GET /api/v1/admin/audit-logs?entityType=USER&action=CHANGE_USER_ROLE&entityId={userId}
```

Kết quả cho thấy:
- Ai đã cấp quyền
- Thời gian cấp quyền
- Role cũ và mới

## Best Practices

### 1. Metadata Structure
Luôn bao gồm các thông tin cần thiết trong metadata:
```java
Map<String, Object> metadata = new HashMap<>();
metadata.put("old_value", oldValue);
metadata.put("new_value", newValue);
metadata.put("reason", reason);  // nếu có
metadata.put("entity_identifier", identifier);  // để dễ tìm
```

### 2. Performance
- Audit log chạy async nên không ảnh hưởng business logic
- Sử dụng pagination khi query logs
- Có thể cân nhắc archive logs cũ

### 3. Security
- Chỉ ADMIN mới được xem audit logs
- Không log sensitive data (password, token, etc.)
- Metadata có thể chứa thông tin cần thiết để điều tra

### 4. Monitoring
- Theo dõi số lượng audit logs tạo ra
- Alert khi có nhiều thay đổi bất thường
- Định kỳ review logs của các hành động quan trọng

## Testing

### Test Scenarios

1. **Test Order Status Change Logging**
```java
// When: Admin changes order status
// Then: Audit log should be created with correct action type
// And: Metadata should contain old and new status
```

2. **Test Price Change Logging**
```java
// When: Admin updates variant price
// Then: Audit log should be created
// And: Old and new prices should be in metadata
```

3. **Test User Role Change Logging**
```java
// When: Admin grants admin role to user
// Then: Audit log should contain actor info
// And: Target user info and role change should be logged
```

4. **Test Query Filters**
```java
// When: Query logs by filters
// Then: Should return correct filtered results
// And: Pagination should work correctly
```

## Database Indexes

Đã có indexes để tối ưu query:
```sql
CREATE INDEX idx_audit_logs_entity_id ON audit_logs(entity_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_entity_type ON audit_logs(entity_type);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
CREATE INDEX idx_audit_logs_actor ON audit_logs(actor_user_id);
```

## Maintenance

### Log Retention
Cân nhắc chiến lược lưu trữ:
- Hot data: 90 ngày (trong database chính)
- Warm data: 1 năm (có thể archive)
- Cold data: >1 năm (backup, có thể xóa)

### Archive Strategy
```sql
-- Ví dụ archive logs cũ hơn 90 ngày
INSERT INTO audit_logs_archive 
SELECT * FROM audit_logs 
WHERE created_at < NOW() - INTERVAL '90 days';

DELETE FROM audit_logs 
WHERE created_at < NOW() - INTERVAL '90 days';
```

## Troubleshooting

### Logs không được tạo?
1. Check `@Async` configuration
2. Check transaction propagation (REQUIRES_NEW)
3. Check exception handling trong AuditLogService

### Performance issue?
1. Check database indexes
2. Consider archiving old logs
3. Review query patterns

### Missing actor information?
1. Ensure SecurityContext is properly set
2. For system actions, explicitly pass actor=null
3. Check authentication flow

## Future Enhancements

1. **Real-time Monitoring Dashboard**
   - WebSocket cho real-time updates
   - Charts và statistics

2. **Advanced Analytics**
   - Pattern detection
   - Anomaly detection
   - Risk scoring

3. **Export & Reporting**
   - Export to CSV/PDF
   - Scheduled reports
   - Custom report templates

4. **Integration**
   - Send critical events to external monitoring
   - Slack/Email notifications
   - SIEM integration

