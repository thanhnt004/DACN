import { Edit, Trash2 } from 'lucide-react'
import { toast } from 'react-toastify'
import * as ProductsApi from '../../../api/admin/products'
import * as CatalogApi from '../../../api/admin/catalog'
import { uploadImagesToCloudinary } from '../../../api/media'

interface ProductVariantsListProps {
    variants: ProductsApi.VariantResponse[]
    unsavedVariants: Array<{
        id: string
        sku: string
        barcode?: string
        sizeId?: string
        colorId?: string
        priceAmount: number
        compareAtAmount?: number
        historyCost?: number
        weightGrams?: number
        status: ProductsApi.VariantStatus
        inventory?: { quantityOnHand: number; reorderLevel: number }
    }>
    savingVariants: boolean
    sizes: CatalogApi.SizeDto[]
    colors: CatalogApi.ColorDto[]
    createdProduct: ProductsApi.ProductDetailResponse | null
    editingVariantId: string | null
    editFormData: {
        sku: string
        barcode: string
        priceAmount: string
        compareAtAmount: string
        historyCost: string
        weightGrams: string
        status: ProductsApi.VariantStatus
        quantityOnHand: string
        reorderLevel: string
    }
    editLoading: boolean
    colorImages: Record<string, string[]>
    onSaveVariants: () => void
    onEditVariant: (variantId: string) => void
    onDeleteVariant: (variantId: string) => void
    onEditUnsavedVariant: (variantId: string) => void
    onDeleteUnsavedVariant: (variantId: string) => void
    onSaveEdit: (variantId: string) => void
    onSaveUnsavedEdit: (variantId: string) => void
    onCancelEdit: () => void
    onColorImageUpload: (colorId: string, urls: string[]) => void
    onRemoveColorImage: (colorId: string, index: number) => void
    onEditFormDataChange: (data: {
        sku: string
        barcode: string
        priceAmount: string
        compareAtAmount: string
        historyCost: string
        weightGrams: string
        status: ProductsApi.VariantStatus
        quantityOnHand: string
        reorderLevel: string
    }) => void
}

export default function ProductVariantsList({
    variants,
    unsavedVariants,
    savingVariants,
    sizes,
    colors,
    createdProduct,
    editingVariantId,
    editFormData,
    editLoading,
    colorImages,
    onSaveVariants,
    onEditVariant,
    onDeleteVariant,
    onEditUnsavedVariant,
    onDeleteUnsavedVariant,
    onSaveEdit,
    onSaveUnsavedEdit,
    onCancelEdit,
    onColorImageUpload,
    onRemoveColorImage,
    onEditFormDataChange,
}: ProductVariantsListProps) {
    if (variants.length === 0 && unsavedVariants.length === 0) {
        return null
    }

    return (
        <div className="space-y-4">
            <div className="flex items-center justify-between">
                <p className="text-sm text-gray-600">
                    Tổng số: {variants.length + unsavedVariants.length} biến thể
                    ({variants.length} đã lưu, {unsavedVariants.length} chưa lưu)
                </p>
                {unsavedVariants.length > 0 && (
                    <button
                        type="button"
                        onClick={onSaveVariants}
                        disabled={savingVariants}
                        className="px-4 py-2 bg-orange-600 text-white rounded hover:bg-orange-700 disabled:bg-gray-400 text-sm"
                    >
                        {savingVariants ? 'Đang lưu...' : `💾 Lưu ${unsavedVariants.length} biến thể`}
                    </button>
                )}
            </div>

            <div className="space-y-4">
                {/* Group variants by color and render */}
                {(() => {
                    // Combine all variants with isSaved flag
                    const allVariants = [
                        ...unsavedVariants.map(v => ({ ...v, isSaved: false })),
                        ...variants.map(v => ({ ...v, isSaved: true }))
                    ]

                    // Group by colorId
                    const variantsByColor = allVariants.reduce((acc, variant) => {
                        const colorId = variant.colorId || 'no-color'
                        if (!acc[colorId]) acc[colorId] = []
                        acc[colorId].push(variant)
                        return acc
                    }, {} as Record<string, typeof allVariants>)

                    return Object.entries(variantsByColor).map(([colorId, colorVariants]) => {
                        const color = colors.find(c => c.id === colorId)
                        // Combine images from createdProduct and colorImages state
                        const productColorImages = (createdProduct?.images || []).filter(img => img.colorId === colorId)
                        const localColorImages = colorImages[colorId] || []
                        const allColorImages = [
                            ...productColorImages.map(img => ({ url: img.imageUrl, fromProduct: true, productImg: img })),
                            ...localColorImages.map(url => ({ url, fromProduct: false, productImg: null }))
                        ]

                        return (
                            <div key={colorId} className="border-2 border-gray-300 rounded-lg p-4 space-y-3">
                                {/* Color Header */}
                                <div className="flex items-center justify-between pb-3 border-b">
                                    <div className="flex items-center gap-3">
                                        {color && (
                                            <div
                                                className="w-10 h-10 rounded border-2 border-gray-400"
                                                style={{ backgroundColor: color.hexCode }}
                                            />
                                        )}
                                        <div>
                                            <h3 className="font-semibold text-lg">{color?.name || 'Không có màu'}</h3>
                                            <p className="text-sm text-gray-600">{colorVariants.length} biến thể</p>
                                        </div>
                                    </div>

                                    {/* Color Images */}
                                    <div className="flex items-center gap-2 flex-wrap">
                                        {allColorImages.length > 0 && (
                                            <div className="flex gap-1 flex-wrap">
                                                {allColorImages.map((img, idx) => (
                                                    <div key={idx} className="relative group">
                                                        <img
                                                            src={img.url}
                                                            alt={`${color?.name} ${idx + 1}`}
                                                            className="w-12 h-12 object-cover rounded border"
                                                        />
                                                        {!img.fromProduct && (
                                                            <button
                                                                type="button"
                                                                onClick={() => onRemoveColorImage(colorId, idx - productColorImages.length)}
                                                                className="absolute -top-1 -right-1 bg-red-600 text-white text-xs w-4 h-4 rounded-full opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center"
                                                                title="Xóa ảnh"
                                                            >
                                                                ✕
                                                            </button>
                                                        )}
                                                    </div>
                                                ))}
                                            </div>
                                        )}

                                        {/* Upload button - Only show if no image exists for this color */}
                                        {allColorImages.length === 0 && (
                                            <button
                                                type="button"
                                                onClick={() => {
                                                    const input = document.createElement('input')
                                                    input.type = 'file'
                                                    input.multiple = false
                                                    input.accept = 'image/*'
                                                    input.onchange = async (e: Event) => {
                                                        const target = e.target as HTMLInputElement
                                                        const files = Array.from(target.files || [])
                                                        if (files.length > 0) {
                                                            try {
                                                                const urls = await uploadImagesToCloudinary(files, 'product')
                                                                onColorImageUpload(colorId, urls)
                                                            } catch (error) {
                                                                console.error('Upload failed:', error)
                                                                toast.error('Upload ảnh thất bại. Vui lòng thử lại.')
                                                            }
                                                        }
                                                    }
                                                    input.click()
                                                }}
                                                className="px-3 py-1 text-sm bg-blue-100 text-blue-700 rounded hover:bg-blue-200"
                                            >
                                                📷 Thêm ảnh
                                            </button>
                                        )}
                                    </div>
                                </div>

                                {/* Variants for this color */}
                                <div className="space-y-2">
                                    {colorVariants.map((variant) => {
                                        const size = sizes.find(s => s.id === variant.sizeId)
                                        const isEditing = editingVariantId === variant.id

                                        return (
                                            <div key={variant.id}>
                                                {!variant.isSaved ? (
                                                    // Unsaved Variant
                                                    <>
                                                        <div className="border-2 border-yellow-400 rounded p-3 flex items-center justify-between bg-yellow-50">
                                                            <div className="flex items-center gap-4">
                                                                {color && (
                                                                    <div
                                                                        className="w-8 h-8 rounded border"
                                                                        style={{ backgroundColor: color.hexCode }}
                                                                    />
                                                                )}
                                                                <div>
                                                                    <div className="flex items-center gap-2">
                                                                        <p className="font-medium">
                                                                            {size?.name} - {color?.name}
                                                                        </p>
                                                                        <span className="text-xs bg-yellow-600 text-white px-2 py-0.5 rounded">
                                                                            CHƯA LƯU
                                                                        </span>
                                                                    </div>
                                                                    <p className="text-sm text-gray-600">SKU: {variant.sku}</p>
                                                                    <p className="text-sm text-gray-600">
                                                                        Tồn kho: {variant.inventory?.quantityOnHand ?? 0} (Min: {variant.inventory?.reorderLevel ?? 5}) |
                                                                        Giá: {variant.priceAmount?.toLocaleString()} VNĐ
                                                                    </p>
                                                                </div>
                                                            </div>
                                                            <div className="flex gap-2">
                                                                <button
                                                                    type="button"
                                                                    onClick={() => onEditUnsavedVariant(variant.id)}
                                                                    className="p-2 text-blue-600 hover:bg-blue-50 rounded"
                                                                    title="Chỉnh sửa"
                                                                    disabled={isEditing}
                                                                >
                                                                    <Edit className="w-4 h-4" />
                                                                </button>
                                                                <button
                                                                    type="button"
                                                                    onClick={() => onDeleteUnsavedVariant(variant.id)}
                                                                    className="p-2 text-red-600 hover:bg-red-50 rounded"
                                                                    title="Xóa"
                                                                    disabled={isEditing}
                                                                >
                                                                    <Trash2 className="w-4 h-4" />
                                                                </button>
                                                            </div>
                                                        </div>

                                                        {/* Inline Edit Form for Unsaved Variant */}
                                                        {isEditing && (
                                                            <div className="border-2 border-yellow-400 rounded p-4 mt-2 bg-yellow-50">
                                                                <h3 className="font-semibold mb-3 text-yellow-900">
                                                                    Chỉnh sửa (chưa lưu): {size?.name} - {color?.name}
                                                                </h3>
                                                                <div className="grid grid-cols-2 gap-4">
                                                                    <div>
                                                                        <label className="block text-sm font-medium mb-1">
                                                                            SKU *
                                                                        </label>
                                                                        <input
                                                                            type="text"
                                                                            value={editFormData.sku}
                                                                            onChange={(e) => onEditFormDataChange({ ...editFormData, sku: e.target.value })}
                                                                            className="w-full border rounded px-3 py-2"
                                                                            required
                                                                        />
                                                                    </div>
                                                                    <div>
                                                                        <label className="block text-sm font-medium mb-1">
                                                                            Barcode
                                                                        </label>
                                                                        <input
                                                                            type="text"
                                                                            value={editFormData.barcode}
                                                                            onChange={(e) => onEditFormDataChange({ ...editFormData, barcode: e.target.value })}
                                                                            className="w-full border rounded px-3 py-2"
                                                                        />
                                                                    </div>
                                                                    <div>
                                                                        <label className="block text-sm font-medium mb-1">
                                                                            Giá bán *
                                                                        </label>
                                                                        <input
                                                                            type="number"
                                                                            value={editFormData.priceAmount}
                                                                            onChange={(e) => onEditFormDataChange({ ...editFormData, priceAmount: e.target.value })}
                                                                            className="w-full border rounded px-3 py-2"
                                                                            required
                                                                        />
                                                                    </div>
                                                                    <div>
                                                                        <label className="block text-sm font-medium mb-1">
                                                                            Giá gốc
                                                                        </label>
                                                                        <input
                                                                            type="number"
                                                                            value={editFormData.historyCost}
                                                                            onChange={(e) => onEditFormDataChange({ ...editFormData, historyCost: e.target.value })}
                                                                            className="w-full border rounded px-3 py-2"
                                                                        />
                                                                    </div>
                                                                    <div>
                                                                        <label className="block text-sm font-medium mb-1">
                                                                            Giá so sánh
                                                                        </label>
                                                                        <input
                                                                            type="number"
                                                                            value={editFormData.compareAtAmount}
                                                                            onChange={(e) => onEditFormDataChange({ ...editFormData, compareAtAmount: e.target.value })}
                                                                            className="w-full border rounded px-3 py-2"
                                                                        />
                                                                    </div>
                                                                    <div>
                                                                        <label className="block text-sm font-medium mb-1">
                                                                            Khối lượng (g)
                                                                        </label>
                                                                        <input
                                                                            type="number"
                                                                            value={editFormData.weightGrams}
                                                                            onChange={(e) => onEditFormDataChange({ ...editFormData, weightGrams: e.target.value })}
                                                                            className="w-full border rounded px-3 py-2"
                                                                        />
                                                                    </div>
                                                                    <div>
                                                                        <label className="block text-sm font-medium mb-1">
                                                                            Số lượng tồn kho *
                                                                        </label>
                                                                        <input
                                                                            type="number"
                                                                            value={editFormData.quantityOnHand}
                                                                            onChange={(e) => onEditFormDataChange({ ...editFormData, quantityOnHand: e.target.value })}
                                                                            className="w-full border rounded px-3 py-2"
                                                                            required
                                                                        />
                                                                    </div>
                                                                    <div>
                                                                        <label className="block text-sm font-medium mb-1">
                                                                            Mức đặt hàng lại *
                                                                        </label>
                                                                        <input
                                                                            type="number"
                                                                            value={editFormData.reorderLevel}
                                                                            onChange={(e) => onEditFormDataChange({ ...editFormData, reorderLevel: e.target.value })}
                                                                            className="w-full border rounded px-3 py-2"
                                                                            required
                                                                        />
                                                                    </div>
                                                                    <div className="col-span-2">
                                                                        <label className="block text-sm font-medium mb-1">
                                                                            Trạng thái
                                                                        </label>
                                                                        <select
                                                                            value={editFormData.status}
                                                                            onChange={(e) => onEditFormDataChange({ ...editFormData, status: e.target.value as ProductsApi.VariantStatus })}
                                                                            className="w-full border rounded px-3 py-2"
                                                                        >
                                                                            <option value="ACTIVE">Hoạt động</option>
                                                                            <option value="DISCONTINUED">Ngừng bán</option>
                                                                        </select>
                                                                    </div>
                                                                </div>
                                                                <div className="flex gap-2 mt-4">
                                                                    <button
                                                                        type="button"
                                                                        onClick={() => onSaveUnsavedEdit(variant.id)}
                                                                        className="px-4 py-2 bg-yellow-600 text-white rounded hover:bg-yellow-700"
                                                                    >
                                                                        Lưu thay đổi
                                                                    </button>
                                                                    <button
                                                                        type="button"
                                                                        onClick={onCancelEdit}
                                                                        className="px-4 py-2 bg-gray-200 text-gray-700 rounded hover:bg-gray-300"
                                                                    >
                                                                        Hủy
                                                                    </button>
                                                                </div>
                                                            </div>
                                                        )}
                                                    </>
                                                ) : (
                                                    // Saved Variant
                                                    <>
                                                        <div className="border rounded p-3 flex items-center justify-between hover:bg-gray-50">
                                                            <div className="flex items-center gap-4">
                                                                {color && (
                                                                    <div
                                                                        className="w-8 h-8 rounded border"
                                                                        style={{ backgroundColor: color.hexCode }}
                                                                    />
                                                                )}
                                                                <div>
                                                                    <p className="font-medium">
                                                                        {size?.name} - {color?.name}
                                                                    </p>
                                                                    <p className="text-sm text-gray-600">SKU: {variant.sku}</p>
                                                                    <p className="text-sm text-gray-600">
                                                                        Tồn kho: {variant.inventory?.quantityOnHand ?? 0} (Min: {variant.inventory?.reorderLevel ?? 5}) |
                                                                        Giá: {variant.priceAmount?.toLocaleString()} VNĐ
                                                                    </p>
                                                                </div>
                                                            </div>
                                                            <div className="flex gap-2">
                                                                <button
                                                                    type="button"
                                                                    onClick={() => onEditVariant(variant.id)}
                                                                    className="p-2 text-blue-600 hover:bg-blue-50 rounded"
                                                                    title="Chỉnh sửa"
                                                                    disabled={isEditing}
                                                                >
                                                                    <Edit className="w-4 h-4" />
                                                                </button>
                                                                <button
                                                                    type="button"
                                                                    onClick={() => onDeleteVariant(variant.id)}
                                                                    className="p-2 text-red-600 hover:bg-red-50 rounded"
                                                                    title="Xóa"
                                                                    disabled={isEditing}
                                                                >
                                                                    <Trash2 className="w-4 h-4" />
                                                                </button>
                                                            </div>
                                                        </div>

                                                        {/* Inline Edit Form for Saved Variant */}
                                                        {isEditing && (
                                                            <div className="border border-blue-300 rounded p-4 mt-2 bg-blue-50">
                                                                <h3 className="font-semibold mb-3 text-blue-900">
                                                                    Chỉnh sửa: {size?.name} - {color?.name}
                                                                </h3>
                                                                <div className="grid grid-cols-2 gap-4">
                                                                    <div>
                                                                        <label className="block text-sm font-medium mb-1">
                                                                            SKU *
                                                                        </label>
                                                                        <input
                                                                            type="text"
                                                                            value={editFormData.sku}
                                                                            onChange={(e) => onEditFormDataChange({ ...editFormData, sku: e.target.value })}
                                                                            className="w-full border rounded px-3 py-2"
                                                                            required
                                                                        />
                                                                    </div>
                                                                    <div>
                                                                        <label className="block text-sm font-medium mb-1">
                                                                            Barcode
                                                                        </label>
                                                                        <input
                                                                            type="text"
                                                                            value={editFormData.barcode}
                                                                            onChange={(e) => onEditFormDataChange({ ...editFormData, barcode: e.target.value })}
                                                                            className="w-full border rounded px-3 py-2"
                                                                        />
                                                                    </div>
                                                                    <div>
                                                                        <label className="block text-sm font-medium mb-1">
                                                                            Giá bán *
                                                                        </label>
                                                                        <input
                                                                            type="number"
                                                                            value={editFormData.priceAmount}
                                                                            onChange={(e) => onEditFormDataChange({ ...editFormData, priceAmount: e.target.value })}
                                                                            className="w-full border rounded px-3 py-2"
                                                                            required
                                                                        />
                                                                    </div>
                                                                    <div>
                                                                        <label className="block text-sm font-medium mb-1">
                                                                            Giá gốc
                                                                        </label>
                                                                        <input
                                                                            type="number"
                                                                            value={editFormData.historyCost}
                                                                            onChange={(e) => onEditFormDataChange({ ...editFormData, historyCost: e.target.value })}
                                                                            className="w-full border rounded px-3 py-2"
                                                                        />
                                                                    </div>
                                                                    <div>
                                                                        <label className="block text-sm font-medium mb-1">
                                                                            Giá so sánh
                                                                        </label>
                                                                        <input
                                                                            type="number"
                                                                            value={editFormData.compareAtAmount}
                                                                            onChange={(e) => onEditFormDataChange({ ...editFormData, compareAtAmount: e.target.value })}
                                                                            className="w-full border rounded px-3 py-2"
                                                                        />
                                                                    </div>
                                                                    <div>
                                                                        <label className="block text-sm font-medium mb-1">
                                                                            Khối lượng (g)
                                                                        </label>
                                                                        <input
                                                                            type="number"
                                                                            value={editFormData.weightGrams}
                                                                            onChange={(e) => onEditFormDataChange({ ...editFormData, weightGrams: e.target.value })}
                                                                            className="w-full border rounded px-3 py-2"
                                                                        />
                                                                    </div>
                                                                    <div>
                                                                        <label className="block text-sm font-medium mb-1">
                                                                            Số lượng tồn kho *
                                                                        </label>
                                                                        <input
                                                                            type="number"
                                                                            value={editFormData.quantityOnHand}
                                                                            onChange={(e) => onEditFormDataChange({ ...editFormData, quantityOnHand: e.target.value })}
                                                                            className="w-full border rounded px-3 py-2"
                                                                            required
                                                                        />
                                                                    </div>
                                                                    <div>
                                                                        <label className="block text-sm font-medium mb-1">
                                                                            Mức đặt hàng lại *
                                                                        </label>
                                                                        <input
                                                                            type="number"
                                                                            value={editFormData.reorderLevel}
                                                                            onChange={(e) => onEditFormDataChange({ ...editFormData, reorderLevel: e.target.value })}
                                                                            className="w-full border rounded px-3 py-2"
                                                                            required
                                                                        />
                                                                    </div>
                                                                    <div className="col-span-2">
                                                                        <label className="block text-sm font-medium mb-1">
                                                                            Trạng thái
                                                                        </label>
                                                                        <select
                                                                            value={editFormData.status}
                                                                            onChange={(e) => onEditFormDataChange({ ...editFormData, status: e.target.value as ProductsApi.VariantStatus })}
                                                                            className="w-full border rounded px-3 py-2"
                                                                        >
                                                                            <option value="ACTIVE">Hoạt động</option>
                                                                            <option value="DISCONTINUED">Ngừng bán</option>
                                                                        </select>
                                                                    </div>
                                                                </div>
                                                                <div className="flex gap-2 mt-4">
                                                                    <button
                                                                        type="button"
                                                                        onClick={() => onSaveEdit(variant.id)}
                                                                        disabled={editLoading}
                                                                        className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:bg-gray-400"
                                                                    >
                                                                        {editLoading ? 'Đang lưu...' : 'Lưu thay đổi'}
                                                                    </button>
                                                                    <button
                                                                        type="button"
                                                                        onClick={onCancelEdit}
                                                                        disabled={editLoading}
                                                                        className="px-4 py-2 bg-gray-200 text-gray-700 rounded hover:bg-gray-300 disabled:bg-gray-100"
                                                                    >
                                                                        Hủy
                                                                    </button>
                                                                </div>
                                                            </div>
                                                        )}
                                                    </>
                                                )}
                                            </div>
                                        )
                                    })}
                                </div>
                            </div>
                        )
                    })
                })()}
            </div>
        </div>
    )
}
