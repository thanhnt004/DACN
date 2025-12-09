# Hướng dẫn cập nhật Modal cho các file còn lại

## Đã tạo:
✅ ErrorModal component - `frontend/src/components/common/ErrorModal.tsx`
✅ ConfirmModal component - `frontend/src/components/common/ConfirmModal.tsx`  
✅ useModals hook - `frontend/src/hooks/useModals.ts`

## Đã cập nhật:
✅ ColorManager.tsx
✅ CartPage.tsx

## Các file cần cập nhật còn lại:

### Admin pages:
- [ ] OrderManager.tsx (3 confirm)
- [ ] ProductCreateNew.tsx (1 window.confirm)
- [ ] ProductManager.tsx (1 confirm)
- [ ] UserManager.tsx (3 confirm)
- [ ] SizeManager.tsx (1 confirm)
- [ ] SizeAndColorManager.tsx (2 confirm)
- [ ] ProductEdit.tsx (1 confirm)
- [ ] DiscountManager.tsx (1 confirm)
- [ ] CategoryManager.tsx (1 confirm)
- [ ] BrandManager.tsx (1 confirm)
- [ ] BannerManager.tsx (1 window.confirm)

### Member pages:
- [ ] ProfilePage.tsx (2 confirm)
- [ ] DeactivateAccount.tsx (1 confirm)
- [ ] components/OrderDetailModal.tsx (1 confirm)

### Components:
- [ ] layout/MemberLayout.tsx (2 confirm)
- [ ] admin/components/VariantMatrixEditor.tsx (1 confirm)

## Pattern áp dụng cho MỖI file:

### 1. Thêm imports (điều chỉnh path tùy vị trí file):

```typescript
// Cho file trong pages/admin/
import ErrorModal from '../../components/common/ErrorModal'
import ConfirmModal from '../../components/common/ConfirmModal'
import { useModals } from '../../hooks/useModals'

// Cho file trong pages/member/
import ErrorModal from '../../components/common/ErrorModal'
import ConfirmModal from '../../components/common/ConfirmModal'
import { useModals } from '../../hooks/useModals'

// Cho file trong pages/
import ErrorModal from '../components/common/ErrorModal'
import ConfirmModal from '../components/common/ConfirmModal'
import { useModals } from '../hooks/useModals'

// Cho file trong components/
import ErrorModal from '../common/ErrorModal' // hoặc './ErrorModal'
import ConfirmModal from '../common/ConfirmModal'
import { useModals } from '../../hooks/useModals'
```

### 2. Thêm hook vào component:

```typescript
export default function YourComponent() {
    // ... existing states
    const { errorModal, showError, closeError, confirmModal, showConfirm, closeConfirm } = useModals()
    // ... rest of code
}
```

### 3. Thay thế toast.error() thành showError():

```typescript
// TRƯỚC:
catch (error) {
    toast.error('Message lỗi')
}

// SAU:
catch (error: any) {
    showError(error?.response?.data?.message || 'Message lỗi')
}
```

### 4. Thay thế confirm() thành showConfirm():

```typescript
// TRƯỚC:
const handleDelete = async (id: string) => {
    if (!confirm('Bạn có chắc muốn xóa?')) return
    try {
        await deleteItem(id)
    } catch (error) {
        toast.error('Xóa thất bại')
    }
}

// SAU:
const handleDelete = async (id: string) => {
    showConfirm('Bạn có chắc muốn xóa?', async () => {
        try {
            await deleteItem(id)
            toast.success('Xóa thành công')
        } catch (error: any) {
            showError(error?.response?.data?.message || 'Xóa thất bại')
        }
    })
}
```

### 5. Thêm Modal components trước closing tag cuối:

```typescript
return (
    <div>
        {/* ... existing JSX */}
        
        <ErrorModal
            isOpen={errorModal.isOpen}
            onClose={closeError}
            title={errorModal.title}
            message={errorModal.message}
        />

        <ConfirmModal
            isOpen={confirmModal.isOpen}
            onClose={closeConfirm}
            onConfirm={confirmModal.onConfirm}
            title={confirmModal.title}
            message={confirmModal.message}
        />
    </div>
)
```

## LƯU Ý QUAN TRỌNG:

- ❌ KHÔNG thay đổi toast.success(), toast.warning(), toast.info()
- ✅ CHỈ thay toast.error() thành showError()
- ✅ CHỈ thay confirm() / window.confirm() thành showConfirm()
- ✅ Giữ nguyên ALL logic code khác
- ✅ Đảm bảo đúng relative path cho imports tùy vị trí file
- ✅ Thêm toast.success() sau khi action thành công trong callback của showConfirm

## Ví dụ hoàn chỉnh (xem ColorManager.tsx hoặc CartPage.tsx):

ColorManager.tsx đã được cập nhật đầy đủ, tham khảo file này làm mẫu!
