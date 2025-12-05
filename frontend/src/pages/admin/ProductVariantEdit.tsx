import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import * as ProductsApi from '../../api/admin/products'
import * as CatalogApi from '../../api/admin/catalog'
import ImageUploadZone from '../../components/layout/ImageUploadZone'
import { ArrowLeft, Save } from 'lucide-react'

export default function ProductVariantEdit() {
    const { productId, variantId } = useParams<{ productId: string; variantId: string }>()
    const navigate = useNavigate()

    const [loading, setLoading] = useState(true)
    const [saving, setSaving] = useState(false)
    const [product, setProduct] = useState<ProductsApi.ProductDetailResponse | null>(null)
    const [variant, setVariant] = useState<ProductsApi.VariantResponse | null>(null)
    const [sizes, setSizes] = useState<CatalogApi.SizeDto[]>([])
    const [colors, setColors] = useState<CatalogApi.ColorDto[]>([])

    const [formData, setFormData] = useState({
        sku: '',
        barcode: '',
        sizeId: '',
        colorId: '',
        priceAmount: '',
        compareAtAmount: '',
        historyCost: '',
        weightGrams: '',
        status: 'ACTIVE' as ProductsApi.VariantStatus,
        quantityOnHand: '',
        quantityReserved: '',
        reorderLevel: ''
    })

    const [variantImages, setVariantImages] = useState<ProductsApi.ProductImagePayload[]>([])

    useEffect(() => {
        loadData()
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [productId, variantId])

    const loadData = async () => {
        if (!productId || !variantId) return

        setLoading(true)
        try {
            const [productData, sizesData, colorsData] = await Promise.all([
                ProductsApi.getProductDetail(productId, ['variants', 'images', 'inventory']),
                CatalogApi.getSizes(),
                CatalogApi.getColors()
            ])

            setProduct(productData)
            setSizes(sizesData)
            setColors(colorsData)

            const currentVariant = productData.variants?.find((v: ProductsApi.VariantResponse) => v.id === variantId)
            if (currentVariant) {
                setVariant(currentVariant)
                setFormData({
                    sku: currentVariant.sku,
                    barcode: currentVariant.barcode || '',
                    sizeId: currentVariant.sizeId || '',
                    colorId: currentVariant.colorId || '',
                    priceAmount: currentVariant.priceAmount?.toString() || productData.priceAmount.toString(),
                    compareAtAmount: currentVariant.compareAtAmount?.toString() || '',
                    historyCost: currentVariant.historyCost?.toString() || '',
                    weightGrams: currentVariant.weightGrams?.toString() || '',
                    status: currentVariant.status || 'ACTIVE',
                    quantityOnHand: currentVariant.inventory?.quantityOnHand?.toString() || '0',
                    quantityReserved: currentVariant.inventory?.quantityReserved?.toString() || '0',
                    reorderLevel: currentVariant.inventory?.reorderLevel?.toString() || '0'
                })

                // Load variant images (images with matching colorId)
                const variantImgs = productData.images?.filter((img: ProductsApi.ProductImageResponse) =>
                    img.colorId === currentVariant.colorId
                ) || []
                setVariantImages(variantImgs)
            }
        } catch (error) {
            console.error('Error loading data:', error)
            alert('Lỗi tải dữ liệu')
        } finally {
            setLoading(false)
        }
    }

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault()

        if (!productId || !variantId || !variant) return

        setSaving(true)
        try {
            const updateData: ProductsApi.VariantUpdateRequest = {
                sku: formData.sku,
                barcode: formData.barcode || undefined,
                priceAmount: parseFloat(formData.priceAmount),
                compareAtAmount: formData.compareAtAmount ? parseFloat(formData.compareAtAmount) : undefined,
                historyCost: formData.historyCost ? parseFloat(formData.historyCost) : undefined,
                weightGrams: formData.weightGrams ? parseInt(formData.weightGrams) : undefined,
                status: formData.status,
                version: variant.version || 0,
                inventory: {
                    quantityOnHand: formData.quantityOnHand ? parseInt(formData.quantityOnHand) : undefined,
                    quantityReserved: formData.quantityReserved ? parseInt(formData.quantityReserved) : undefined,
                    reorderLevel: formData.reorderLevel ? parseInt(formData.reorderLevel) : undefined
                }
            }

            await ProductsApi.updateVariant(productId, variantId, updateData)

            alert('Cập nhật biến thể thành công!')
            navigate(`/admin/products/${productId}/edit`)
        } catch (error) {
            console.error('Error saving variant:', error)
            alert('Lỗi lưu biến thể')
        } finally {
            setSaving(false)
        }
    }

    const handleImageUploadComplete = (urls: string[]) => {
        const newImages: ProductsApi.ProductImagePayload[] = urls.map(url => ({
            imageUrl: url,
            colorId: formData.colorId || undefined,
            position: variantImages.length
        }))
        setVariantImages([...variantImages, ...newImages])
    }

    const removeImage = (index: number) => {
        setVariantImages(variantImages.filter((_, i) => i !== index))
    }

    if (loading) {
        return (
            <div className="flex items-center justify-center min-h-screen">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-red-600"></div>
            </div>
        )
    }

    if (!product || !variant) {
        return (
            <div className="p-6">
                <div className="bg-red-50 border border-red-200 rounded p-4 text-red-700">
                    Không tìm thấy sản phẩm hoặc biến thể
                </div>
            </div>
        )
    }

    const selectedSize = sizes.find(s => s.id === formData.sizeId)
    const selectedColor = colors.find(c => c.id === formData.colorId)

    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="flex items-center gap-4">
                <button
                    onClick={() => navigate(`/admin/products/${productId}/edit`)}
                    className="p-2 hover:bg-gray-100 rounded"
                >
                    <ArrowLeft className="w-5 h-5" />
                </button>
                <div>
                    <h1 className="text-2xl font-bold">Chỉnh sửa biến thể</h1>
                    <p className="text-gray-600">{product.name}</p>
                </div>
            </div>

            <form onSubmit={handleSubmit} className="space-y-6">
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                    {/* Left column - Main info */}
                    <div className="lg:col-span-2 space-y-6">
                        {/* Variant attributes */}
                        <div className="bg-white shadow rounded-lg p-6 space-y-4">
                            <h2 className="text-lg font-semibold">Thuộc tính biến thể</h2>

                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-sm font-medium mb-1">
                                        Kích thước *
                                    </label>
                                    <select
                                        value={formData.sizeId}
                                        onChange={(e) => setFormData({ ...formData, sizeId: e.target.value })}
                                        className="w-full border rounded px-3 py-2"
                                        required
                                    >
                                        <option value="">-- Chọn kích thước --</option>
                                        {sizes.map(size => (
                                            <option key={size.id} value={size.id}>
                                                {size.name} ({size.code})
                                            </option>
                                        ))}
                                    </select>
                                </div>

                                <div>
                                    <label className="block text-sm font-medium mb-1">
                                        Màu sắc *
                                    </label>
                                    <select
                                        value={formData.colorId}
                                        onChange={(e) => setFormData({ ...formData, colorId: e.target.value })}
                                        className="w-full border rounded px-3 py-2"
                                        required
                                    >
                                        <option value="">-- Chọn màu --</option>
                                        {colors.map(color => (
                                            <option key={color.id} value={color.id}>
                                                {color.name}
                                            </option>
                                        ))}
                                    </select>
                                    {selectedColor && (
                                        <div className="mt-2 flex items-center gap-2">
                                            <div
                                                className="w-6 h-6 rounded border"
                                                style={{ backgroundColor: selectedColor.hexCode }}
                                            />
                                            <span className="text-sm text-gray-600">{selectedColor.hexCode}</span>
                                        </div>
                                    )}
                                </div>
                            </div>

                            <div>
                                <label className="block text-sm font-medium mb-1">SKU *</label>
                                <input
                                    type="text"
                                    value={formData.sku}
                                    onChange={(e) => setFormData({ ...formData, sku: e.target.value })}
                                    className="w-full border rounded px-3 py-2"
                                    placeholder="Mã SKU tự động"
                                    required
                                />
                                <p className="text-xs text-gray-500 mt-1">
                                    Ví dụ: {product.slug.toUpperCase()}-{selectedSize?.code || 'M'}-{selectedColor?.name?.slice(0, 3).toUpperCase() || 'RED'}
                                </p>
                            </div>

                            <div>
                                <label className="block text-sm font-medium mb-1">Barcode</label>
                                <input
                                    type="text"
                                    value={formData.barcode}
                                    onChange={(e) => setFormData({ ...formData, barcode: e.target.value })}
                                    className="w-full border rounded px-3 py-2"
                                    placeholder="Mã vạch (tùy chọn)"
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium mb-1">Trạng thái</label>
                                <select
                                    value={formData.status}
                                    onChange={(e) => setFormData({ ...formData, status: e.target.value as ProductsApi.VariantStatus })}
                                    className="w-full border rounded px-3 py-2"
                                >
                                    <option value="ACTIVE">Đang bán</option>
                                    <option value="DISCONTINUED">Ngưng bán</option>
                                </select>
                            </div>
                        </div>

                        {/* Pricing */}
                        <div className="bg-white shadow rounded-lg p-6 space-y-4">
                            <h2 className="text-lg font-semibold">Giá bán</h2>

                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-sm font-medium mb-1">
                                        Giá bán * (VNĐ)
                                    </label>
                                    <input
                                        type="number"
                                        value={formData.priceAmount}
                                        onChange={(e) => setFormData({ ...formData, priceAmount: e.target.value })}
                                        className="w-full border rounded px-3 py-2"
                                        placeholder={`Mặc định: ${product.priceAmount.toLocaleString()}`}
                                        required
                                        min="0"
                                        step="1000"
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium mb-1">
                                        Giá gốc (VNĐ)
                                    </label>
                                    <input
                                        type="number"
                                        value={formData.historyCost}
                                        onChange={(e) => setFormData({ ...formData, historyCost: e.target.value })}
                                        className="w-full border rounded px-3 py-2"
                                        placeholder="Giá gốc (tùy chọn)"
                                        min="0"
                                        step="1000"
                                    />
                                </div>
                            </div>
                            <div>
                                    <label className="block text-sm font-medium mb-1">
                                        Giá thị trường (VNĐ)
                                    </label>
                                    <input
                                        type="number"
                                        value={formData.compareAtAmount}
                                        onChange={(e) => setFormData({ ...formData, compareAtAmount: e.target.value })}
                                        className="w-full border rounded px-3 py-2"
                                        placeholder="Giá thị trường (tùy chọn)"
                                        min="0"
                                        step="1000"
                                    />
                                </div>

                            <div>
                                <label className="block text-sm font-medium mb-1">
                                    Trọng lượng (gram)
                                </label>
                                <input
                                    type="number"
                                    value={formData.weightGrams}
                                    onChange={(e) => setFormData({ ...formData, weightGrams: e.target.value })}
                                    className="w-full border rounded px-3 py-2"
                                    placeholder="Trọng lượng"
                                    min="0"
                                />
                            </div>
                        </div>

                        {/* Inventory */}
                        <div className="bg-white shadow rounded-lg p-6 space-y-4">
                            <h2 className="text-lg font-semibold">Tồn kho</h2>

                            <div className="grid grid-cols-3 gap-4">
                                <div>
                                    <label className="block text-sm font-medium mb-1">
                                        Số lượng trong kho
                                    </label>
                                    <input
                                        type="number"
                                        value={formData.quantityOnHand}
                                        onChange={(e) => setFormData({ ...formData, quantityOnHand: e.target.value })}
                                        className="w-full border rounded px-3 py-2"
                                        placeholder="0"
                                        min="0"
                                    />
                                </div>

                                <div>
                                    <label className="block text-sm font-medium mb-1">
                                        Số lượng đã đặt
                                    </label>
                                    <input
                                        type="number"
                                        value={formData.quantityReserved}
                                        onChange={(e) => setFormData({ ...formData, quantityReserved: e.target.value })}
                                        className="w-full border rounded px-3 py-2"
                                        placeholder="0"
                                        min="0"
                                    />
                                </div>

                                <div>
                                    <label className="block text-sm font-medium mb-1">
                                        Mức đặt hàng lại
                                    </label>
                                    <input
                                        type="number"
                                        value={formData.reorderLevel}
                                        onChange={(e) => setFormData({ ...formData, reorderLevel: e.target.value })}
                                        className="w-full border rounded px-3 py-2"
                                        placeholder="0"
                                        min="0"
                                    />
                                </div>
                            </div>

                            <div className="bg-blue-50 border border-blue-200 rounded p-3">
                                <p className="text-sm text-blue-700">
                                    <strong>Có thể bán:</strong> {' '}
                                    {parseInt(formData.quantityOnHand || '0') - parseInt(formData.quantityReserved || '0')} sản phẩm
                                </p>
                            </div>
                        </div>
                    </div>

                    {/* Right column - Images */}
                    <div className="space-y-6">
                        <div className="bg-white shadow rounded-lg p-6 space-y-4">
                            <h2 className="text-lg font-semibold">Ảnh biến thể</h2>
                            <p className="text-sm text-gray-600">
                                Upload ảnh cho biến thể này (màu: {selectedColor?.name || 'Chưa chọn'})
                            </p>

                            <ImageUploadZone
                                id="variant-images"
                                description="Kéo thả hoặc click để chọn ảnh"
                                autoUpload={true}
                                uploadType="product"
                                targetId={productId}
                                onUploadComplete={handleImageUploadComplete}
                            />

                            {/* Current images */}
                            {variantImages.length > 0 && (
                                <div className="space-y-2">
                                    <h3 className="text-sm font-medium">Ảnh hiện tại</h3>
                                    <div className="grid grid-cols-2 gap-2">
                                        {variantImages.map((img, idx) => (
                                            <div key={idx} className="relative group">
                                                <img
                                                    src={img.imageUrl}
                                                    alt={img.alt || `Variant ${idx + 1}`}
                                                    className="w-full h-32 object-cover rounded border"
                                                />
                                                <button
                                                    type="button"
                                                    onClick={() => removeImage(idx)}
                                                    className="absolute top-1 right-1 bg-red-600 text-white p-1 rounded opacity-0 group-hover:opacity-100 transition-opacity"
                                                >
                                                    ✕
                                                </button>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            )}
                        </div>

                        {/* Actions */}
                        <div className="bg-white shadow rounded-lg p-6 space-y-4">
                            <button
                                type="submit"
                                disabled={saving}
                                className="w-full bg-red-600 text-white px-4 py-2 rounded hover:bg-red-700 disabled:opacity-50 flex items-center justify-center gap-2"
                            >
                                <Save className="w-4 h-4" />
                                {saving ? 'Đang lưu...' : 'Lưu thay đổi'}
                            </button>

                            <button
                                type="button"
                                onClick={() => navigate(`/admin/products/${productId}/edit`)}
                                className="w-full border border-gray-300 px-4 py-2 rounded hover:bg-gray-50"
                            >
                                Hủy
                            </button>
                        </div>
                    </div>
                </div>
            </form>
        </div>
    )
}
