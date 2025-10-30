import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import * as ProductsApi from '../../api/admin/products'
import * as CatalogApi from '../../api/admin/catalog'
import * as BrandCategoryApi from '../../api/admin/brandCategory'
import ImageUploadZone from '../../components/layout/ImageUploadZone'
import { ArrowLeft, Plus, Edit, Trash2 } from 'lucide-react'

const slugify = (value: string) =>
    value
        .trim()
        .toLowerCase()
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/(^-|-$)+/g, '')

export default function ProductCreate() {
    const navigate = useNavigate()

    const [loading, setLoading] = useState(false)
    const [creating, setCreating] = useState(false)
    const [saving, setSaving] = useState(false)
    const [publishing, setPublishing] = useState(false)

    const [formData, setFormData] = useState({
        name: '',
        slug: '',
        priceAmount: '',
        brandId: '',
        categoryIds: [] as string[],
        description: '',
        material: '',
        gender: '' as '' | ProductsApi.Gender,
        seoTitle: '',
        seoDescription: '',
    })

    const [slugLocked, setSlugLocked] = useState(false)
    const [images, setImages] = useState<string[]>([])
    const [colorImages, setColorImages] = useState<Record<string, string[]>>({}) // colorId -> image URLs
    const [brands, setBrands] = useState<BrandCategoryApi.BrandDto[]>([])
    const [categories, setCategories] = useState<BrandCategoryApi.CategoryResponse[]>([])
    const [sizes, setSizes] = useState<CatalogApi.SizeDto[]>([])
    const [colors, setColors] = useState<CatalogApi.ColorDto[]>([])

    // Created product and variants
    const [createdProduct, setCreatedProduct] = useState<ProductsApi.ProductDetailResponse | null>(null)
    const [variants, setVariants] = useState<ProductsApi.VariantResponse[]>([])
    const [unsavedVariants, setUnsavedVariants] = useState<Array<{
        id: string // temporary local ID
        sku: string
        barcode?: string
        sizeId?: string
        colorId?: string
        priceAmount: number
        compareAtAmount?: number
        weightGrams?: number
        status: ProductsApi.VariantStatus
        inventory?: { quantityOnHand: number }
    }>>([])
    const [savingVariants, setSavingVariants] = useState(false)

    // Variant creation
    const [selectedSizes, setSelectedSizes] = useState<string[]>([])
    const [selectedColors, setSelectedColors] = useState<string[]>([])

    // Variant editing
    const [editingVariantId, setEditingVariantId] = useState<string | null>(null)
    const [editFormData, setEditFormData] = useState<{
        sku: string
        barcode: string
        priceAmount: string
        compareAtAmount: string
        weightGrams: string
        status: ProductsApi.VariantStatus
        quantityOnHand: string
    }>({
        sku: '',
        barcode: '',
        priceAmount: '',
        compareAtAmount: '',
        weightGrams: '',
        status: 'ACTIVE',
        quantityOnHand: '',
    })
    const [editLoading, setEditLoading] = useState(false)
    const [isEditingUnsaved, setIsEditingUnsaved] = useState(false) // Track if editing unsaved variant

    useEffect(() => {
        loadReferences()
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [])

    // Sync images when product is loaded
    useEffect(() => {
        if (createdProduct && createdProduct.images) {
            const imageUrls = createdProduct.images
                .sort((a, b) => (a.position || 0) - (b.position || 0))
                .map(img => img.imageUrl)
            setImages(imageUrls)
        }
    }, [createdProduct])

    const loadReferences = async () => {
        setLoading(true)
        try {
            const [brandsData, categoriesData, sizesData, colorsData] = await Promise.all([
                BrandCategoryApi.getBrands({ size: 100 }),
                BrandCategoryApi.getCategories({ size: 200 }),
                CatalogApi.getSizes(),
                CatalogApi.getColors(),
            ])
            setBrands(brandsData.content || [])
            setCategories(categoriesData.content || [])
            setSizes(sizesData || [])
            setColors(colorsData || [])
        } catch (error) {
            console.error('Error loading references:', error)
            alert(extractErrorMessage(error, 'Lỗi tải dữ liệu tham chiếu'))
        } finally {
            setLoading(false)
        }
    }

    const extractErrorMessage = (error: unknown, defaultMessage: string): string => {
        if (error && typeof error === 'object' && 'response' in error) {
            const axiosError = error as { response?: { data?: { message?: string; error?: string } } }
            if (axiosError.response?.data?.message) {
                return axiosError.response.data.message
            }
            if (axiosError.response?.data?.error) {
                return axiosError.response.data.error
            }
        }
        if (error instanceof Error) {
            return error.message
        }
        return defaultMessage
    }

    const handleNameChange = (value: string) => {
        setFormData(prev => ({
            ...prev,
            name: value,
            ...(!slugLocked ? { slug: slugify(value) } : {})
        }))
    }

    const handleSlugChange = (value: string) => {
        setSlugLocked(true)
        setFormData(prev => ({ ...prev, slug: value }))
    }

    const handleImageUploadComplete = (urls: string[]) => {
        setImages(prev => [...prev, ...urls])
    }

    const removeImage = (index: number) => {
        setImages(prev => prev.filter((_, i) => i !== index))
    }

    const handleCreateProduct = async (e: React.FormEvent) => {
        e.preventDefault()

        if (!formData.name || !formData.slug || !formData.priceAmount) {
            alert('Vui lòng điền đầy đủ thông tin: Tên sản phẩm, Slug và Giá bán')
            return
        }

        setCreating(true)
        try {
            const productData: ProductsApi.ProductCreateRequest = {
                name: formData.name,
                slug: formData.slug,
                priceAmount: parseInt(formData.priceAmount),
                brandId: formData.brandId || undefined,
                categoryId: formData.categoryIds.length > 0 ? formData.categoryIds : undefined,
                description: formData.description || undefined,
                material: formData.material || undefined,
                gender: formData.gender || undefined,
                seoTitle: formData.seoTitle || undefined,
                seoDescription: formData.seoDescription || undefined,
                images: [
                    // Main product images (no colorId)
                    ...images.map((url, index) => ({
                        imageUrl: url,
                        position: index,
                        isDefault: index === 0
                    })),
                    // Color-specific images
                    ...Object.entries(colorImages).flatMap(([colorId, urls]) =>
                        urls.map((url, index) => ({
                            imageUrl: url,
                            position: images.length + index,
                            colorId: colorId
                        }))
                    )
                ]
            }

            const product = await ProductsApi.createProduct(productData)
            setCreatedProduct(product)
            alert('Tạo bản nháp sản phẩm thành công! Bạn có thể tiếp tục chỉnh sửa và tạo biến thể.')
        } catch (error) {
            console.error('Error creating product:', error)
            alert(extractErrorMessage(error, 'Lỗi tạo sản phẩm'))
        } finally {
            setCreating(false)
        }
    }

    const handleSaveProduct = async () => {
        if (!createdProduct) {
            alert('Chưa có sản phẩm để lưu')
            return
        }

        if (!formData.name || !formData.slug || !formData.priceAmount) {
            alert('Vui lòng điền đầy đủ thông tin: Tên sản phẩm, Slug và Giá bán')
            return
        }

        setSaving(true)
        try {
            const updateData: ProductsApi.ProductUpdateRequest = {
                id: createdProduct.id,
                name: formData.name,
                slug: formData.slug,
                priceAmount: parseInt(formData.priceAmount),
                brandId: formData.brandId || undefined,
                description: formData.description || undefined,
                material: formData.material || undefined,
                gender: formData.gender || undefined,
                seoTitle: formData.seoTitle || undefined,
                seoDescription: formData.seoDescription || undefined,
                version: createdProduct.version || 0,
                images: [
                    // Main product images (no colorId)
                    ...images.map((url, index) => ({
                        imageUrl: url,
                        position: index,
                        isDefault: index === 0
                    })),
                    // Color-specific images
                    ...Object.entries(colorImages).flatMap(([colorId, urls]) =>
                        urls.map((url, index) => ({
                            imageUrl: url,
                            position: images.length + index,
                            colorId: colorId
                        }))
                    )
                ]
            }

            const updatedProduct = await ProductsApi.updateProduct(createdProduct.id, updateData)
            setCreatedProduct(updatedProduct)
            alert('Lưu thông tin sản phẩm thành công!')
        } catch (error) {
            console.error('Error saving product:', error)
            alert(extractErrorMessage(error, 'Lỗi lưu sản phẩm'))
        } finally {
            setSaving(false)
        }
    }

    const handlePublishProduct = async () => {
        if (!createdProduct) {
            alert('Chưa có sản phẩm để xuất bản')
            return
        }

        if (variants.length === 0) {
            alert('Vui lòng tạo ít nhất 1 biến thể trước khi xuất bản sản phẩm')
            return
        }

        const confirmPublish = confirm(
            'Bạn có chắc muốn xuất bản sản phẩm này?\n\n' +
            'Sản phẩm sẽ hiển thị công khai cho khách hàng.'
        )
        if (!confirmPublish) return

        setPublishing(true)
        try {
            await ProductsApi.changeProductStatus(createdProduct.id, 'ACTIVE')

            // Reload product
            const updatedProduct = await ProductsApi.getProductDetail(createdProduct.id)
            setCreatedProduct(updatedProduct)

            const productUrl = `${window.location.origin}/products/${createdProduct.slug}`

            alert(
                `✅ Xuất bản sản phẩm thành công!\n\n` +
                `Link sản phẩm:\n${productUrl}\n\n` +
                `Sản phẩm đã hiển thị công khai.`
            )
        } catch (error) {
            console.error('Error publishing product:', error)
            alert(extractErrorMessage(error, 'Lỗi xuất bản sản phẩm'))
        } finally {
            setPublishing(false)
        }
    }

    const handleGenerateVariants = () => {
        if (!createdProduct) {
            alert('Vui lòng tạo sản phẩm trước')
            return
        }

        if (selectedSizes.length === 0 || selectedColors.length === 0) {
            alert('Vui lòng chọn ít nhất 1 size và 1 màu')
            return
        }

        // Check if unsaved variants or saved variants already exist
        const totalExisting = variants.length + unsavedVariants.length
        if (totalExisting > 0) {
            const confirmReplace = confirm(
                `Đã có ${totalExisting} biến thể (${variants.length} đã lưu, ${unsavedVariants.length} chưa lưu). ` +
                `Bạn có muốn xóa hết và tạo lại không?`
            )
            if (!confirmReplace) return

            // Clear unsaved variants
            setUnsavedVariants([])
        }

        // Generate variants locally (not saved to backend yet)
        const newVariants: typeof unsavedVariants = []

        for (const sizeId of selectedSizes) {
            for (const colorId of selectedColors) {
                const size = sizes.find(s => s.id === sizeId)
                const color = colors.find(c => c.id === colorId)

                const sku = [
                    formData.slug.toUpperCase(),
                    size?.code || size?.name?.slice(0, 2),
                    color?.name?.slice(0, 3).toUpperCase()
                ].filter(Boolean).join('-')

                newVariants.push({
                    id: `temp-${Date.now()}-${sizeId}-${colorId}`, // temporary ID
                    sku,
                    sizeId,
                    colorId,
                    priceAmount: parseInt(formData.priceAmount),
                    status: 'ACTIVE',
                    inventory: {
                        quantityOnHand: 0
                    }
                })
            }
        }

        setUnsavedVariants(newVariants)
        alert(`Đã tạo ${newVariants.length} biến thể. Nhấn "Lưu biến thể" để lưu vào database.`)
    }

    const handleSaveVariants = async () => {
        if (!createdProduct) {
            alert('Chưa có sản phẩm để lưu biến thể')
            return
        }

        if (unsavedVariants.length === 0) {
            alert('Không có biến thể nào để lưu')
            return
        }

        setSavingVariants(true)
        try {
            const createdVariants: ProductsApi.VariantResponse[] = []

            for (const unsavedVariant of unsavedVariants) {
                const variantRequest: ProductsApi.VariantCreateRequest = {
                    sku: unsavedVariant.sku,
                    barcode: unsavedVariant.barcode,
                    sizeId: unsavedVariant.sizeId,
                    colorId: unsavedVariant.colorId,
                    priceAmount: unsavedVariant.priceAmount,
                    compareAtAmount: unsavedVariant.compareAtAmount,
                    weightGrams: unsavedVariant.weightGrams,
                    status: unsavedVariant.status,
                    inventory: {
                        quantityOnHand: unsavedVariant.inventory?.quantityOnHand || 0,
                        quantityReserved: 0,
                        reorderLevel: 5
                    }
                }

                const variant = await ProductsApi.createVariant(createdProduct.id, variantRequest)
                createdVariants.push(variant)
            }

            // Move unsaved to saved
            setVariants(prev => [...prev, ...createdVariants])
            setUnsavedVariants([])

            alert(`Đã lưu ${createdVariants.length} biến thể thành công!`)
        } catch (error: unknown) {
            console.error('Error saving variants:', error)
            alert(extractErrorMessage(error, 'Lỗi lưu biến thể'))
        } finally {
            setSavingVariants(false)
        }
    }

    const handleDeleteUnsavedVariant = (tempId: string) => {
        setUnsavedVariants(prev => prev.filter(v => v.id !== tempId))
    }

    const handleEditUnsavedVariant = (tempId: string) => {
        // Find the unsaved variant to edit
        const variant = unsavedVariants.find(v => v.id === tempId)
        if (!variant) return

        // Populate edit form with current values
        setEditFormData({
            sku: variant.sku || '',
            barcode: variant.barcode || '',
            priceAmount: variant.priceAmount?.toString() || '',
            compareAtAmount: variant.compareAtAmount?.toString() || '',
            weightGrams: variant.weightGrams?.toString() || '',
            status: variant.status || 'ACTIVE',
            quantityOnHand: variant.inventory?.quantityOnHand?.toString() || '0',
        })

        // Show inline form
        setEditingVariantId(tempId)
        setIsEditingUnsaved(true)
    }

    const handleSaveUnsavedEdit = (tempId: string) => {
        // Update the unsaved variant in local state
        setUnsavedVariants(prev => prev.map(v => {
            if (v.id === tempId) {
                return {
                    ...v,
                    sku: editFormData.sku,
                    barcode: editFormData.barcode || undefined,
                    priceAmount: parseFloat(editFormData.priceAmount),
                    compareAtAmount: editFormData.compareAtAmount ? parseFloat(editFormData.compareAtAmount) : undefined,
                    weightGrams: editFormData.weightGrams ? parseFloat(editFormData.weightGrams) : undefined,
                    status: editFormData.status,
                    inventory: {
                        quantityOnHand: parseInt(editFormData.quantityOnHand),
                    },
                }
            }
            return v
        }))

        // Close edit form
        setEditingVariantId(null)
        setIsEditingUnsaved(false)
    }

    const handleCancelEdit = () => {
        setEditingVariantId(null)
        setIsEditingUnsaved(false)
    }

    const handleDeleteVariant = async (variantId: string) => {
        if (!createdProduct || !confirm('Bạn có chắc muốn xóa biến thể này?')) return

        try {
            await ProductsApi.deleteVariant(createdProduct.id, variantId)
            setVariants(prev => prev.filter(v => v.id !== variantId))
            alert('Đã xóa biến thể')
        } catch (error) {
            console.error('Error deleting variant:', error)
            alert(extractErrorMessage(error, 'Lỗi xóa biến thể'))
        }
    }

    const handleSaveEdit = async (variantId: string) => {
        if (!createdProduct) return

        const variant = variants.find(v => v.id === variantId)
        if (!variant) return

        setEditLoading(true)
        try {
            const updateData: ProductsApi.VariantUpdateRequest = {
                sku: editFormData.sku,
                barcode: editFormData.barcode,
                priceAmount: parseFloat(editFormData.priceAmount),
                compareAtAmount: editFormData.compareAtAmount ? parseFloat(editFormData.compareAtAmount) : undefined,
                weightGrams: editFormData.weightGrams ? parseFloat(editFormData.weightGrams) : undefined,
                status: editFormData.status,
                version: variant.version || 0,
                inventory: {
                    quantityOnHand: parseInt(editFormData.quantityOnHand),
                },
            }

            await ProductsApi.updateVariant(createdProduct.id, variantId, updateData)

            // Reload variants
            const updatedVariants = await ProductsApi.getProductVariants(createdProduct.id)
            setVariants(updatedVariants)

            // Close edit form
            setEditingVariantId(null)
            setIsEditingUnsaved(false)
        } catch (error: unknown) {
            console.error('Error updating variant:', error)
            const message = error instanceof Error ? error.message : 'Failed to update variant'
            alert(message)
        } finally {
            setEditLoading(false)
        }
    }

    const handleEditVariant = (variantId: string) => {
        if (!createdProduct) return

        // Find the variant to edit
        const variant = variants.find(v => v.id === variantId)
        if (!variant) return

        // Populate edit form with current values
        setEditFormData({
            sku: variant.sku || '',
            barcode: variant.barcode || '',
            priceAmount: variant.priceAmount?.toString() || '',
            compareAtAmount: variant.compareAtAmount?.toString() || '',
            weightGrams: variant.weightGrams?.toString() || '',
            status: variant.status || 'ACTIVE',
            quantityOnHand: variant.inventory?.quantityOnHand?.toString() || '0',
        })

        // Show inline form
        setEditingVariantId(variantId)
        setIsEditingUnsaved(false)
    }

    if (loading) {
        return (
            <div className="flex items-center justify-center min-h-screen">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-red-600"></div>
            </div>
        )
    }

    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="flex items-center gap-4">
                <button
                    onClick={() => navigate('/admin/products')}
                    className="p-2 hover:bg-gray-100 rounded"
                >
                    <ArrowLeft className="w-5 h-5" />
                </button>
                <h1 className="text-2xl font-bold">Thêm sản phẩm mới</h1>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* Left Column - Product Info */}
                <div className="lg:col-span-2 space-y-6">
                    {/* Basic Info */}
                    <form onSubmit={handleCreateProduct} className="bg-white shadow rounded-lg p-6 space-y-4">
                        <h2 className="text-lg font-semibold">Thông tin cơ bản</h2>

                        <div>
                            <label className="block text-sm font-medium mb-1">Tên sản phẩm *</label>
                            <input
                                type="text"
                                value={formData.name}
                                onChange={(e) => handleNameChange(e.target.value)}
                                className="w-full border rounded px-3 py-2"
                                placeholder="Áo thun basic"
                                required
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium mb-1">Slug *</label>
                            <input
                                type="text"
                                value={formData.slug}
                                onChange={(e) => handleSlugChange(e.target.value)}
                                className="w-full border rounded px-3 py-2"
                                placeholder="ao-thun-basic"
                                required
                            />
                        </div>

                        <div className="grid grid-cols-2 gap-4">
                            <div>
                                <label className="block text-sm font-medium mb-1">Giá (VNĐ) *</label>
                                <input
                                    type="number"
                                    value={formData.priceAmount}
                                    onChange={(e) => setFormData(prev => ({ ...prev, priceAmount: e.target.value }))}
                                    className="w-full border rounded px-3 py-2"
                                    placeholder="350000"
                                    required
                                    min="0"
                                    step="1000"
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium mb-1">Giới tính</label>
                                <select
                                    value={formData.gender}
                                    onChange={(e) => setFormData(prev => ({ ...prev, gender: e.target.value as ProductsApi.Gender }))}
                                    className="w-full border rounded px-3 py-2"
                                >
                                    <option value="">-- Chọn --</option>
                                    <option value="men">Nam</option>
                                    <option value="women">Nữ</option>
                                    <option value="unisex">Unisex</option>
                                </select>
                            </div>
                        </div>

                        <div>
                            <label className="block text-sm font-medium mb-1">Thương hiệu</label>
                            <select
                                value={formData.brandId}
                                onChange={(e) => setFormData(prev => ({ ...prev, brandId: e.target.value }))}
                                className="w-full border rounded px-3 py-2"
                            >
                                <option value="">-- Chọn thương hiệu --</option>
                                {brands.map(brand => (
                                    <option key={brand.id} value={brand.id}>{brand.name}</option>
                                ))}
                            </select>
                        </div>

                        <div>
                            <label className="block text-sm font-medium mb-1">Danh mục</label>
                            <select
                                multiple
                                value={formData.categoryIds}
                                onChange={(e) => {
                                    const selected = Array.from(e.target.selectedOptions).map(opt => opt.value)
                                    setFormData(prev => ({ ...prev, categoryIds: selected }))
                                }}
                                className="w-full border rounded px-3 py-2"
                                size={5}
                            >
                                {categories.map(cat => (
                                    <option key={cat.id} value={cat.id}>
                                        {'  '.repeat(cat.level)}{cat.name}
                                    </option>
                                ))}
                            </select>
                            <p className="text-xs text-gray-500 mt-1">Giữ Ctrl để chọn nhiều</p>
                        </div>

                        <div>
                            <label className="block text-sm font-medium mb-1">Mô tả</label>
                            <textarea
                                value={formData.description}
                                onChange={(e) => setFormData(prev => ({ ...prev, description: e.target.value }))}
                                className="w-full border rounded px-3 py-2"
                                rows={4}
                                placeholder="Mô tả sản phẩm..."
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium mb-1">Chất liệu</label>
                            <input
                                type="text"
                                value={formData.material}
                                onChange={(e) => setFormData(prev => ({ ...prev, material: e.target.value }))}
                                className="w-full border rounded px-3 py-2"
                                placeholder="Cotton, Polyester..."
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium mb-1">SEO Title</label>
                            <input
                                type="text"
                                value={formData.seoTitle}
                                onChange={(e) => setFormData(prev => ({ ...prev, seoTitle: e.target.value }))}
                                className="w-full border rounded px-3 py-2"
                                placeholder="Tiêu đề SEO"
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium mb-1">SEO Description</label>
                            <textarea
                                value={formData.seoDescription}
                                onChange={(e) => setFormData(prev => ({ ...prev, seoDescription: e.target.value }))}
                                className="w-full border rounded px-3 py-2"
                                rows={2}
                                placeholder="Mô tả SEO"
                            />
                        </div>

                        {!createdProduct && (
                            <button
                                type="submit"
                                disabled={creating}
                                className="w-full bg-red-600 text-white px-4 py-2 rounded hover:bg-red-700 disabled:opacity-50"
                            >
                                {creating ? 'Đang tạo...' : 'Tạo sản phẩm'}
                            </button>
                        )}

                        {createdProduct && (
                            <div className="bg-green-50 border border-green-200 rounded p-4">
                                <p className="text-green-700 font-medium">
                                    ✓ Sản phẩm đã được tạo thành công!
                                </p>
                                <p className="text-sm text-green-600 mt-1">
                                    Bạn có thể tạo biến thể bên dưới
                                </p>
                            </div>
                        )}
                    </form>

                    {/* Variants Section */}
                    {createdProduct && (
                        <div className="bg-white shadow rounded-lg p-6 space-y-4">
                            <h2 className="text-lg font-semibold">Biến thể sản phẩm</h2>

                            {/* Generate Variants */}
                            {variants.length === 0 && (
                                <div className="space-y-4">
                                    <div>
                                        <label className="block text-sm font-medium mb-2">Chọn kích thước</label>
                                        <div className="flex flex-wrap gap-2">
                                            {sizes.map(size => (
                                                <label key={size.id} className="flex items-center gap-2 border rounded px-3 py-2 cursor-pointer hover:bg-gray-50">
                                                    <input
                                                        type="checkbox"
                                                        checked={selectedSizes.includes(size.id!)}
                                                        onChange={(e) => {
                                                            if (e.target.checked) {
                                                                setSelectedSizes(prev => [...prev, size.id!])
                                                            } else {
                                                                setSelectedSizes(prev => prev.filter(id => id !== size.id))
                                                            }
                                                        }}
                                                    />
                                                    <span>{size.name} ({size.code})</span>
                                                </label>
                                            ))}
                                        </div>
                                    </div>

                                    <div>
                                        <label className="block text-sm font-medium mb-2">Chọn màu sắc</label>
                                        <div className="flex flex-wrap gap-2">
                                            {colors.map(color => (
                                                <label key={color.id} className="flex items-center gap-2 border rounded px-3 py-2 cursor-pointer hover:bg-gray-50">
                                                    <input
                                                        type="checkbox"
                                                        checked={selectedColors.includes(color.id!)}
                                                        onChange={(e) => {
                                                            if (e.target.checked) {
                                                                setSelectedColors(prev => [...prev, color.id!])
                                                            } else {
                                                                setSelectedColors(prev => prev.filter(id => id !== color.id))
                                                            }
                                                        }}
                                                    />
                                                    <div
                                                        className="w-5 h-5 rounded border"
                                                        style={{ backgroundColor: color.hexCode }}
                                                    />
                                                    <span>{color.name}</span>
                                                </label>
                                            ))}
                                        </div>
                                    </div>

                                    <button
                                        type="button"
                                        onClick={handleGenerateVariants}
                                        disabled={selectedSizes.length === 0 || selectedColors.length === 0}
                                        className="w-full bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 disabled:opacity-50 flex items-center justify-center gap-2"
                                    >
                                        <Plus className="w-4 h-4" />
                                        Tạo {selectedSizes.length * selectedColors.length} biến thể
                                    </button>
                                </div>
                            )}

                            {/* Variants List */}
                            {(variants.length > 0 || unsavedVariants.length > 0) && (
                                <div className="space-y-4">
                                    <div className="flex items-center justify-between">
                                        <p className="text-sm text-gray-600">
                                            Tổng số: {variants.length + unsavedVariants.length} biến thể
                                            ({variants.length} đã lưu, {unsavedVariants.length} chưa lưu)
                                        </p>
                                        {unsavedVariants.length > 0 && (
                                            <button
                                                type="button"
                                                onClick={handleSaveVariants}
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
                                            // Combine and sort all variants by colorId
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
                                                const colorImages = (createdProduct?.images || []).filter(img => img.colorId === colorId)

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
                                                            <div className="flex items-center gap-2">
                                                                {colorImages.length > 0 && (
                                                                    <div className="flex gap-1">
                                                                        {colorImages.map((img, idx) => (
                                                                            <img
                                                                                key={idx}
                                                                                src={img.imageUrl}
                                                                                alt={`${color?.name} ${idx + 1}`}
                                                                                className="w-12 h-12 object-cover rounded border"
                                                                            />
                                                                        ))}
                                                                    </div>
                                                                )}
                                                                <button
                                                                    type="button"
                                                                    onClick={() => {
                                                                        const input = document.createElement('input')
                                                                        input.type = 'file'
                                                                        input.multiple = true
                                                                        input.accept = 'image/*'
                                                                        input.onchange = async (e: any) => {
                                                                            const files = Array.from(e.target.files || [])
                                                                            // TODO: Implement upload
                                                                            alert('Chức năng upload ảnh cho màu sẽ được hoàn thiện')
                                                                        }
                                                                        input.click()
                                                                    }}
                                                                    className="px-3 py-1 text-sm bg-blue-100 text-blue-700 rounded hover:bg-blue-200"
                                                                >
                                                                    📷 Thêm ảnh
                                                                </button>
                                                            </div>
                                                        </div>

                                                        {/* Variants for this color */}
                                                        <div className="space-y-2">
                                                            {colorVariants.map((variant: any) => {
                                                                const size = sizes.find(s => s.id === variant.sizeId)
                                                                const isEditing = editingVariantId === variant.id

                                                                return (
                                                                    <div key={variant.id}>
                                                                        {/* Render variant based on saved/unsaved */}
                                                                        {!variant.isSaved ? (
                                                                            // Unsaved variant rendering
                                                                            <>
                                                                                <div className="border-2 border-yellow-400 rounded p-3 flex items-center justify-between bg-yellow-50">
                                                                                    <div key={variant.id}>
                                                                                        {/* Unsaved Variant Display */}
                                                                                        <div className="border-2 border-yellow-400 rounded p-4 flex items-center justify-between bg-yellow-50">
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
                                                                                                        Tồn kho: {variant.inventory?.quantityOnHand ?? 0} |
                                                                                                        Giá: {variant.priceAmount?.toLocaleString()} VNĐ
                                                                                                    </p>
                                                                                                </div>
                                                                                            </div>
                                                                                            <div className="flex gap-2">
                                                                                                <button
                                                                                                    type="button"
                                                                                                    onClick={() => handleEditUnsavedVariant(variant.id)}
                                                                                                    className="p-2 text-blue-600 hover:bg-blue-50 rounded"
                                                                                                    title="Chỉnh sửa"
                                                                                                    disabled={isEditing}
                                                                                                >
                                                                                                    <Edit className="w-4 h-4" />
                                                                                                </button>
                                                                                                <button
                                                                                                    type="button"
                                                                                                    onClick={() => handleDeleteUnsavedVariant(variant.id)}
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
                                                                                                            onChange={(e) => setEditFormData({ ...editFormData, sku: e.target.value })}
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
                                                                                                            onChange={(e) => setEditFormData({ ...editFormData, barcode: e.target.value })}
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
                                                                                                            onChange={(e) => setEditFormData({ ...editFormData, priceAmount: e.target.value })}
                                                                                                            className="w-full border rounded px-3 py-2"
                                                                                                            required
                                                                                                        />
                                                                                                    </div>
                                                                                                    <div>
                                                                                                        <label className="block text-sm font-medium mb-1">
                                                                                                            Giá so sánh
                                                                                                        </label>
                                                                                                        <input
                                                                                                            type="number"
                                                                                                            value={editFormData.compareAtAmount}
                                                                                                            onChange={(e) => setEditFormData({ ...editFormData, compareAtAmount: e.target.value })}
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
                                                                                                            onChange={(e) => setEditFormData({ ...editFormData, weightGrams: e.target.value })}
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
                                                                                                            onChange={(e) => setEditFormData({ ...editFormData, quantityOnHand: e.target.value })}
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
                                                                                                            onChange={(e) => setEditFormData({ ...editFormData, status: e.target.value as ProductsApi.VariantStatus })}
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
                                                                                                        onClick={() => handleSaveUnsavedEdit(variant.id)}
                                                                                                        className="px-4 py-2 bg-yellow-600 text-white rounded hover:bg-yellow-700"
                                                                                                    >
                                                                                                        Lưu thay đổi
                                                                                                    </button>
                                                                                                    <button
                                                                                                        type="button"
                                                                                                        onClick={handleCancelEdit}
                                                                                                        className="px-4 py-2 bg-gray-200 text-gray-700 rounded hover:bg-gray-300"
                                                                                                    >
                                                                                                        Hủy
                                                                                                    </button>
                                                                                                </div>
                                                                                            </div>
                                                                                        )}
                                                                                    </div>
                                                                                    )
                                        })}

                                                                                    {/* Saved Variants */}
                                                                                    {variants.map(variant => {
                                                                                        const size = sizes.find(s => s.id === variant.sizeId)
                                                                                        const color = colors.find(c => c.id === variant.colorId)
                                                                                        const isEditing = editingVariantId === variant.id

                                                                                        return (
                                                                                            <div key={variant.id}>
                                                                                                {/* Variant Display */}
                                                                                                <div className="border rounded p-4 flex items-center justify-between hover:bg-gray-50">
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
                                                                                                                Tồn kho: {variant.inventory?.quantityOnHand ?? 0} |
                                                                                                                Giá: {variant.priceAmount?.toLocaleString()} VNĐ
                                                                                                            </p>
                                                                                                        </div>
                                                                                                    </div>
                                                                                                    <div className="flex gap-2">
                                                                                                        <button
                                                                                                            type="button"
                                                                                                            onClick={() => handleEditVariant(variant.id)}
                                                                                                            className="p-2 text-blue-600 hover:bg-blue-50 rounded"
                                                                                                            title="Chỉnh sửa"
                                                                                                            disabled={isEditing}
                                                                                                        >
                                                                                                            <Edit className="w-4 h-4" />
                                                                                                        </button>
                                                                                                        <button
                                                                                                            type="button"
                                                                                                            onClick={() => handleDeleteVariant(variant.id)}
                                                                                                            className="p-2 text-red-600 hover:bg-red-50 rounded"
                                                                                                            title="Xóa"
                                                                                                            disabled={isEditing}
                                                                                                        >
                                                                                                            <Trash2 className="w-4 h-4" />
                                                                                                        </button>
                                                                                                    </div>
                                                                                                </div>

                                                                                                {/* Inline Edit Form */}
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
                                                                                                                    onChange={(e) => setEditFormData({ ...editFormData, sku: e.target.value })}
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
                                                                                                                    onChange={(e) => setEditFormData({ ...editFormData, barcode: e.target.value })}
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
                                                                                                                    onChange={(e) => setEditFormData({ ...editFormData, priceAmount: e.target.value })}
                                                                                                                    className="w-full border rounded px-3 py-2"
                                                                                                                    required
                                                                                                                />
                                                                                                            </div>
                                                                                                            <div>
                                                                                                                <label className="block text-sm font-medium mb-1">
                                                                                                                    Giá so sánh
                                                                                                                </label>
                                                                                                                <input
                                                                                                                    type="number"
                                                                                                                    value={editFormData.compareAtAmount}
                                                                                                                    onChange={(e) => setEditFormData({ ...editFormData, compareAtAmount: e.target.value })}
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
                                                                                                                    onChange={(e) => setEditFormData({ ...editFormData, weightGrams: e.target.value })}
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
                                                                                                                    onChange={(e) => setEditFormData({ ...editFormData, quantityOnHand: e.target.value })}
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
                                                                                                                    onChange={(e) => setEditFormData({ ...editFormData, status: e.target.value as ProductsApi.VariantStatus })}
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
                                                                                                                onClick={() => handleSaveEdit(variant.id)}
                                                                                                                disabled={editLoading}
                                                                                                                className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:bg-gray-400"
                                                                                                            >
                                                                                                                {editLoading ? 'Đang lưu...' : 'Lưu thay đổi'}
                                                                                                            </button>
                                                                                                            <button
                                                                                                                type="button"
                                                                                                                onClick={handleCancelEdit}
                                                                                                                disabled={editLoading}
                                                                                                                className="px-4 py-2 bg-gray-200 text-gray-700 rounded hover:bg-gray-300 disabled:bg-gray-100"
                                                                                                            >
                                                                                                                Hủy
                                                                                                            </button>
                                                                                                        </div>
                                                                                                    </div>
                                                                                                )}
                                                                                            </div>
                                                                                        )
                                                                                    })}
                                                                                </div>

                                                                                {/* Action buttons */}
                                                                                <div className="flex gap-3 mt-4">
                                                                                    <button
                                                                                        type="button"
                                                                                        onClick={handleSaveProduct}
                                                                                        disabled={saving}
                                                                                        className="flex-1 bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed"
                                                                                    >
                                                                                        {saving ? 'Đang lưu...' : '💾 Lưu sản phẩm'}
                                                                                    </button>

                                                                                    {createdProduct?.status === 'DRAFT' && (
                                                                                        <button
                                                                                            type="button"
                                                                                            onClick={handlePublishProduct}
                                                                                            disabled={publishing || variants.length === 0 || unsavedVariants.length > 0}
                                                                                            className="flex-1 bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700 disabled:bg-gray-400 disabled:cursor-not-allowed"
                                                                                            title={
                                                                                                variants.length === 0
                                                                                                    ? 'Cần có ít nhất 1 biến thể để xuất bản'
                                                                                                    : unsavedVariants.length > 0
                                                                                                        ? 'Vui lòng lưu tất cả biến thể trước khi xuất bản'
                                                                                                        : ''
                                                                                            }
                                                                                        >
                                                                                            {publishing ? 'Đang xuất bản...' : '🚀 Xuất bản'}
                                                                                        </button>
                                                                                    )}

                                                                                    {createdProduct?.status === 'ACTIVE' && (
                                                                                        <a
                                                                                            href={`/products/${createdProduct.slug}`}
                                                                                            target="_blank"
                                                                                            rel="noopener noreferrer"
                                                                                            className="flex-1 bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700 text-center"
                                                                                        >
                                                                                            ✅ Đã xuất bản - Xem sản phẩm
                                                                                        </a>
                                                                                    )}

                                                                                    <button
                                                                                        type="button"
                                                                                        onClick={() => navigate('/admin/products')}
                                                                                        className="px-4 py-2 bg-gray-200 text-gray-700 rounded hover:bg-gray-300"
                                                                                    >
                                                                                        Quay lại
                                                                                    </button>
                                                                                </div>
                                                                            </div>
                            )}
                                                                    </div>
                                                                )
                                                            }
                </div>

                                                        {/* Right Column - Images */}
                                                        <div className="space-y-6">
                                                            <div className="bg-white shadow rounded-lg p-6 space-y-4">
                                                                <h2 className="text-lg font-semibold">Hình ảnh sản phẩm</h2>

                                                                <ImageUploadZone
                                                                    id="product-images"
                                                                    description="Kéo thả hoặc click để chọn ảnh"
                                                                    autoUpload={true}
                                                                    uploadType="product"
                                                                    onUploadComplete={handleImageUploadComplete}
                                                                />

                                                                {images.length > 0 && (
                                                                    <div className="space-y-2">
                                                                        <h3 className="text-sm font-medium">Ảnh đã chọn ({images.length})</h3>
                                                                        <div className="grid grid-cols-2 gap-2">
                                                                            {images.map((url, idx) => (
                                                                                <div key={idx} className="relative group">
                                                                                    <img
                                                                                        src={url}
                                                                                        alt={`Product ${idx + 1}`}
                                                                                        className="w-full h-32 object-cover rounded border"
                                                                                    />
                                                                                    {idx === 0 && (
                                                                                        <div className="absolute top-1 left-1 bg-green-600 text-white text-xs px-2 py-1 rounded">
                                                                                            Ảnh chính
                                                                                        </div>
                                                                                    )}
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

                                                                {createdProduct && (
                                                                    <div className="bg-blue-50 border border-blue-200 rounded p-3">
                                                                        <p className="text-xs text-blue-700">
                                                                            💡 Nhớ nhấn nút "Lưu sản phẩm" sau khi thêm/xóa ảnh để cập nhật thay đổi
                                                                        </p>
                                                                    </div>
                                                                )}
                                                            </div>
                                                        </div>
                                                    </div>
        </div>
                                    )
}

