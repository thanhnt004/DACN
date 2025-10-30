# Hệ thống Quản lý Mã giảm giá (Discount Management)

## Tổng quan

Hệ thống quản lý mã giảm giá hoàn chỉnh với đầy đủ chức năng CRUD và các tính năng nâng cao.

## API Backend

### Endpoints chính

#### 1. Quản lý Discount
- **GET** `/api/v1/discounts` - Danh sách mã giảm giá (có phân trang và bộ lọc)
  - Query params: `code`, `active`, `page`, `size`
- **GET** `/api/v1/discounts/{id}` - Chi tiết 1 mã giảm giá
- **POST** `/api/v1/discounts` - Tạo mã giảm giá mới
- **PUT** `/api/v1/discounts/{id}` - Cập nhật mã giảm giá
- **DELETE** `/api/v1/discounts/{id}` - Xóa mã giảm giá

#### 2. Quản lý Sản phẩm áp dụng
- **POST** `/api/v1/discounts/{id}/products` - Gán sản phẩm vào discount
- **DELETE** `/api/v1/discounts/{id}/products` - Xóa sản phẩm khỏi discount

#### 3. Quản lý Danh mục áp dụng
- **POST** `/api/v1/discounts/{id}/categories` - Gán danh mục vào discount
- **DELETE** `/api/v1/discounts/{id}/categories` - Xóa danh mục khỏi discount

#### 4. Lịch sử sử dụng
- **GET** `/api/v1/discounts/{id}/redemptions` - Xem lịch sử sử dụng mã giảm giá

### Schema dữ liệu

#### DiscountCreateRequest / DiscountUpdateRequest
```typescript
{
    code: string                    // Mã giảm giá (bắt buộc, tối đa 100 ký tự, unique)
    name: string                    // Tên chiến dịch (bắt buộc, tối đa 255 ký tự)
    description?: string            // Mô tả chi tiết
    type: 'PERCENTAGE' | 'FIXED_AMOUNT'  // Loại giảm giá
    value: number                   // Giá trị (min: 1)
    startsAt?: string              // Thời gian bắt đầu (ISO datetime)
    endsAt?: string                // Thời gian kết thúc (ISO datetime)
    minOrderAmount?: number        // Giá trị đơn hàng tối thiểu (min: 0)
    maxRedemptions?: number        // Số lần sử dụng tối đa (min: 1)
    perUserLimit?: number          // Giới hạn mỗi người dùng (min: 1)
    active: boolean                // Trạng thái kích hoạt
}
```

#### DiscountResponse
```typescript
{
    id: string
    code: string
    name: string
    description?: string
    type: 'PERCENTAGE' | 'FIXED_AMOUNT'
    value: number
    startsAt?: string
    endsAt?: string
    minOrderAmount?: number
    maxRedemptions?: number
    perUserLimit?: number
    active: boolean
    createdAt: string
    updatedAt?: string
    productIds?: string[]          // Danh sách ID sản phẩm áp dụng
    categoryIds?: string[]         // Danh sách ID danh mục áp dụng
}
```

## Frontend Implementation

### Component: DiscountManager.tsx

**Vị trí:** `frontend/src/pages/admin/DiscountManager.tsx`

#### Chức năng đã triển khai

1. **Danh sách mã giảm giá**
   - Hiển thị dạng bảng với đầy đủ thông tin
   - Phân trang (20 items/trang)
   - Hiển thị: Mã, Tên, Loại, Giá trị, Thời gian, Giới hạn, Trạng thái

2. **Bộ lọc và Tìm kiếm**
   - Tìm kiếm theo mã giảm giá
   - Lọc theo trạng thái (Tất cả/Hoạt động/Đã tắt)
   - Nút "Xóa bộ lọc" để reset

3. **Tạo mã giảm giá mới**
   - Modal form với đầy đủ trường
   - Các trường bắt buộc: code, name, type, value
   - Các trường tùy chọn: description, thời gian, giới hạn
   - Validate dữ liệu trước khi submit
   - Auto uppercase cho code

4. **Sửa mã giảm giá**
   - Modal form giống Create
   - Pre-fill dữ liệu hiện tại
   - Format datetime-local cho date picker

5. **Xóa mã giảm giá**
   - Xác nhận trước khi xóa
   - Thông báo thành công/thất bại

6. **Hiển thị thông tin**
   - Format giá trị: 10% hoặc 100,000đ
   - Format ngày tháng: dd/MM/yyyy HH:mm
   - Badge màu cho loại và trạng thái
   - Hiển thị ∞ cho giới hạn không giới hạn

7. **Quản lý sản phẩm áp dụng** ✨ NEW
   - Modal với danh sách tất cả sản phẩm
   - Search sản phẩm theo tên
   - Multi-select checkbox
   - Nút "Thêm đã chọn" và "Xóa đã chọn"
   - Hiển thị số lượng sản phẩm đã chọn
   - Hiển thị badge số lượng SP đã gán (📦 SP (5))

8. **Quản lý danh mục áp dụng** ✨ NEW
   - Modal với danh sách tất cả danh mục
   - Search danh mục theo tên
   - Multi-select checkbox
   - Nút "Thêm đã chọn" và "Xóa đã chọn"
   - Hiển thị số lượng danh mục đã chọn
   - Hiển thị badge số lượng DM đã gán (📁 DM (3))

#### UI/UX Features

- **Loading states:** Hiển thị "Đang tải..." khi fetch dữ liệu
- **Empty state:** Thông báo khi không có dữ liệu
- **Responsive design:** Grid layout responsive cho form
- **Modal scrollable:** Hỗ trợ scroll cho form dài
- **Emoji alerts:** ✅ thành công, ❌ lỗi
- **Disabled pagination:** Nút phân trang disabled khi không thể điều hướng

### API Client: discounts.ts

**Vị trí:** `frontend/src/api/admin/discounts.ts`

#### Functions

```typescript
// Quản lý Discount
getDiscounts(params?: GetDiscountsParams): Promise<PageResponse<DiscountResponse>>
getDiscount(id: string): Promise<DiscountResponse>
createDiscount(data: DiscountCreateRequest): Promise<DiscountResponse>
updateDiscount(id: string, data: DiscountUpdateRequest): Promise<DiscountResponse>
deleteDiscount(id: string): Promise<void>

// Quản lý Sản phẩm
addProductsToDiscount(id: string, data: ProductAssignmentRequest): Promise<void>
removeProductsFromDiscount(id: string, data: ProductAssignmentRequest): Promise<void>

// Quản lý Danh mục
addCategoriesToDiscount(id: string, data: CategoryAssignmentRequest): Promise<void>
removeCategoriesFromDiscount(id: string, data: CategoryAssignmentRequest): Promise<void>
```

## Routing

**URL:** `/admin/discounts`

Cấu hình trong `App.tsx`:
```tsx
<Route path="discounts" element={<DiscountManager />} />
```

## Validation Rules

### Backend Validation (từ @Valid annotation)

1. **code**
   - @NotBlank
   - @Size(max = 100)
   - Unique constraint

2. **name**
   - @NotBlank
   - @Size(max = 255)

3. **type**
   - @NotBlank
   - Enum: PERCENTAGE | FIXED_AMOUNT

4. **value**
   - @NotNull
   - @Min(1)

5. **maxRedemptions**
   - @Min(1)

6. **perUserLimit**
   - @Min(1)

7. **minOrderAmount**
   - @Min(0)

### Frontend Validation

- Code và Name không được rỗng
- Value phải > 0
- Tự động uppercase cho code
- Number inputs có min attribute

## Database Schema

```sql
CREATE TABLE discounts (
    id UUID PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(50) NOT NULL,
    value INTEGER NOT NULL,
    starts_at TIMESTAMP,
    ends_at TIMESTAMP,
    max_redemptions INTEGER,
    per_user_limit INTEGER,
    min_order_amount BIGINT,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

-- Relations
product_discounts (product_id, discount_id) - ManyToMany
category_discounts (category_id, discount_id) - ManyToMany
```

## Tính năng chưa triển khai (có thể mở rộng)

1. ~~**Quản lý sản phẩm/danh mục áp dụng**~~ ✅ **ĐÃ TRIỂN KHAI**
   - ✅ Modal chọn sản phẩm với search
   - ✅ Modal chọn danh mục với search
   - ✅ Thêm/Xóa sản phẩm và danh mục
   - ✅ Hiển thị số lượng SP/DM đã gán
   - ✅ Multi-select checkbox

2. **Xem lịch sử sử dụng**
   - Tab "Lịch sử sử dụng" trong detail view
   - Bảng hiển thị: User, Order, Thời gian, Số tiền giảm

3. **Bulk actions**
   - Chọn nhiều discount để xóa/kích hoạt/vô hiệu hóa cùng lúc

4. **Export/Import**
   - Export danh sách ra Excel/CSV
   - Import từ file

5. **Analytics**
   - Thống kê số lần sử dụng
   - Tổng số tiền đã giảm
   - Biểu đồ hiệu quả chiến dịch

## Testing

### Hướng dẫn sử dụng chức năng Sản phẩm/Danh mục

#### Thêm sản phẩm vào discount:
1. Trong bảng danh sách, click vào nút **"📦 SP (0)"** ở cột Hành động
2. Modal hiện lên với danh sách tất cả sản phẩm
3. Dùng ô search để tìm sản phẩm cần thêm
4. Check vào checkbox của các sản phẩm muốn thêm
5. Click nút **"Thêm đã chọn"** (màu xanh)
6. Xác nhận thông báo thành công
7. Số lượng SP trên badge sẽ tự động cập nhật

#### Xóa sản phẩm khỏi discount:
1. Click vào nút **"📦 SP (5)"** (số hiện tại)
2. Các sản phẩm đã được gán sẽ được check sẵn
3. Uncheck các sản phẩm muốn xóa (hoặc giữ nguyên các cái muốn xóa)
4. Click nút **"Xóa đã chọn"** (màu đỏ)
5. Xác nhận thông báo thành công

#### Thêm/Xóa danh mục:
- Tương tự như sản phẩm, nhưng click vào nút **"📁 DM (0)"**

**Lưu ý:**
- Có thể chọn nhiều items cùng lúc
- Search hoạt động real-time
- Modal có scroll khi danh sách dài
- Badge hiển thị số lượng hiện tại

### Manual Testing Checklist

- [ ] Tạo discount với đầy đủ trường
- [ ] Tạo discount chỉ với trường bắt buộc
- [ ] Sửa discount
- [ ] Xóa discount
- [ ] Tìm kiếm theo code
- [ ] Lọc theo active status
- [ ] Phân trang (trang trước/sau)
- [ ] Validate code trùng lặp
- [ ] Validate các trường bắt buộc
- [ ] Test với PERCENTAGE type
- [ ] Test với FIXED_AMOUNT type
- [ ] Test datetime picker
- [ ] Test các giới hạn (min order, max redemptions, per user)
- [ ] **Thêm sản phẩm vào discount** ✨
- [ ] **Xóa sản phẩm khỏi discount** ✨
- [ ] **Thêm danh mục vào discount** ✨
- [ ] **Xóa danh mục khỏi discount** ✨
- [ ] **Search sản phẩm trong modal** ✨
- [ ] **Search danh mục trong modal** ✨
- [ ] **Multi-select với checkbox** ✨
- [ ] **Badge hiển thị số lượng SP/DM** ✨

### Expected Behaviors

1. **Code trùng lặp:** Backend trả về error, frontend hiển thị alert
2. **Trường bắt buộc trống:** Frontend validate trước khi gửi
3. **Datetime:** Convert sang ISO string khi gửi lên backend
4. **Empty fields:** `undefined` được gửi cho optional fields
5. **Pagination:** Disable nút khi ở trang đầu/cuối

## Troubleshooting

### Common Issues

1. **"Failed to fetch discounts"**
   - Kiểm tra backend đang chạy
   - Kiểm tra token authentication
   - Check console cho error chi tiết

2. **"Discount code is existed"**
   - Code đã tồn tại trong database
   - Thử code khác hoặc xóa discount cũ

3. **Datetime không hiển thị đúng**
   - Backend trả về ISO string
   - Frontend cần slice(0, 16) cho datetime-local input

4. **Create modal không reset**
   - Đảm bảo gọi resetForm() khi đóng modal

## Future Enhancements

1. Thêm rich text editor cho description
2. Upload ảnh banner cho campaign
3. Schedule tự động active/inactive theo thời gian
4. Integration với email marketing
5. A/B testing cho các chiến dịch
6. Real-time notification khi discount được sử dụng
7. Duplicate discount feature
8. Template discount để tạo nhanh

## Dependencies

- React 18+
- TypeScript
- Axios (http client)
- Tailwind CSS (styling)

## License

Internal project - All rights reserved
