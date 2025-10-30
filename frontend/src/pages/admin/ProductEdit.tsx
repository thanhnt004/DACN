import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import * as ProductsApi from '../../api/admin/products'
import * as CatalogApi from '../../api/admin/catalog'
import * as BrandCategoryApi from '../../api/admin/brandCategory'
import ImageUploadZone from '../../components/layout/ImageUploadZone'
import ProductVariantsList from './components/ProductVariantsList'
import { ArrowLeft } from 'lucide-react'

const slugify = (value: string) =>
    value
        .trim()
        .toLowerCase()
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/(^-|-$)+/g, '')

export default function ProductEdit() {
    const navigate = useNavigate()
    const { productId } = useParams<{ productId: string }>()

    const [loading, setLoading] = useState(true)
    const [saving, setSaving] = useState(false)

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

    const [slugLocked, setSlugLocked] = useState(true) // Auto-lock slug when editing
    const [primaryImage, setPrimaryImage] = useState<string>('')
    const [galleryImages, setGalleryImages] = useState<string[]>([])
    const [images, setImages] = useState<string[]>([])
    const [colorImages, setColorImages] = useState<Record<string, string[]>>({})
    const [brands, setBrands] = useState<BrandCategoryApi.BrandDto[]>([])
    const [categoryLevels, setCategoryLevels] = useState<Array<{
        level: number
        parentId: string | null
        selectedId: string | null
        options: BrandCategoryApi.CategoryResponse[]
    }>>([])
    const [sizes, setSizes] = useState<CatalogApi.SizeDto[]>([])
    const [colors, setColors] = useState<CatalogApi.ColorDto[]>([])

    const [createdProduct, setCreatedProduct] = useState<ProductsApi.ProductDetailResponse | null>(null)
    const [variants, setVariants] = useState<ProductsApi.VariantResponse[]>([])
    const [unsavedVariants, setUnsavedVariants] = useState<Array<{
        id: string
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
    const [savingVariants, setSavingVariants] = useState(false)

    useEffect(() => {
        loadProduct()
        loadReferences()
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [productId])

    const loadProduct = async () => {
        if (!productId) {
            alert('Product ID không hợp lệ')
            navigate('/admin/products')
            return
        }

        setLoading(true)
        try {
            const product = await ProductsApi.getProductDetail(productId, ['images', 'variants', 'categories'])
            setCreatedProduct(product)

            // Populate form
            setFormData({
                name: product.name || '',
                slug: product.slug || '',
                priceAmount: product.priceAmount?.toString() || '',
                brandId: product.brandId || '',
                categoryIds: product.categories?.map(c => c.id) || [],
                description: product.description || '',
                material: product.material || '',
                gender: product.gender || '',
                seoTitle: product.seoTitle || '',
                seoDescription: product.seoDescription || '',
            })

            // Load variants
            if (product.variants) {
                setVariants(product.variants)
            }

            // Rebuild category levels from product categories
            if (product.categories && product.categories.length > 0) {
                await rebuildCategoryLevels(product.categories)
            }
        } catch (error) {
            console.error('Error loading product:', error)
            alert('Lỗi tải sản phẩm')
            navigate('/admin/products')
        } finally {
            setLoading(false)
        }
    }

    const rebuildCategoryLevels = async (productCategories: BrandCategoryApi.CategoryResponse[]) => {
        try {
            // Sort by level to build hierarchy
            const sortedCategories = [...productCategories].sort((a, b) => a.level - b.level)
            const newLevels: typeof categoryLevels = []

            for (const category of sortedCategories) {
                if (category.level === 0) {
                    // Root level
                    const rootCategoriesData = await BrandCategoryApi.getCategories({ size: 100 })
                    newLevels.push({
                        level: 0,
                        parentId: null,
                        selectedId: category.id,
                        options: rootCategoriesData.content || []
                    })
                } else {
                    // Child level
                    const parentCategory = sortedCategories.find(c => c.id === category.parentId)
                    if (parentCategory) {
                        const childrenData = await BrandCategoryApi.getCategories({
                            parentId: parentCategory.id,
                            size: 100
                        })
                        newLevels.push({
                            level: category.level,
                            parentId: category.parentId || null,
                            selectedId: category.id,
                            options: childrenData.content || []
                        })
                    }
                }
            }

            setCategoryLevels(newLevels)
        } catch (error) {
            console.error('Error rebuilding category levels:', error)
        }
    }

    // Sync images when product is loaded
    useEffect(() => {
        if (createdProduct && createdProduct.images) {
            const sortedImages = [...createdProduct.images].sort((a, b) => (a.position || 0) - (b.position || 0))

            const primaryImg = sortedImages.find(img => img.isDefault && !img.colorId)
            if (primaryImg) {
                setPrimaryImage(primaryImg.imageUrl)
            }

            const galleryImgs = sortedImages
                .filter(img => !img.isDefault && !img.colorId)
                .map(img => img.imageUrl)
            setGalleryImages(galleryImgs)

            const imageUrls = sortedImages.map(img => img.imageUrl)
            setImages(imageUrls)
        }
    }, [createdProduct])

    const loadReferences = async () => {
        try {
            const [brandsData, sizesData, colorsData] = await Promise.all([
                BrandCategoryApi.getBrands({ size: 100 }),
                CatalogApi.getSizes(),
                CatalogApi.getColors(),
            ])
            setBrands(brandsData.content || [])
            setSizes(sizesData || [])
            setColors(colorsData || [])

            // Load root categories for initial dropdown
            const rootCategoriesData = await BrandCategoryApi.getCategories({ size: 100 })
            setCategoryLevels([{
                level: 0,
                parentId: null,
                selectedId: null,
                options: rootCategoriesData.content || []
            }])
        } catch (error) {
            console.error('Error loading references:', error)
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

    const handleCategoryChange = async (levelIndex: number, categoryId: string) => {
        if (!categoryId) {
            // Reset selection - clear this level and all subsequent levels
            const resetLevels = categoryLevels.slice(0, levelIndex + 1)
            resetLevels[levelIndex] = {
                ...resetLevels[levelIndex],
                selectedId: null
            }
            setCategoryLevels(resetLevels)
            setFormData(prev => ({ ...prev, categoryIds: [] }))
            return
        }

        const newLevels = [...categoryLevels]
        newLevels[levelIndex] = {
            ...newLevels[levelIndex],
            selectedId: categoryId
        }

        try {
            // Fetch children for this category
            const childrenResponse = await BrandCategoryApi.getCategories({
                parentId: categoryId,
                size: 100
            })

            const children = childrenResponse.content || []

            if (children.length > 0) {
                // Has children - add next level dropdown
                const nextLevel = {
                    level: levelIndex + 1,
                    parentId: categoryId,
                    selectedId: null,
                    options: children
                }

                // Remove any levels after current, then add the new one
                const updatedLevels = [...newLevels.slice(0, levelIndex + 1), nextLevel]
                setCategoryLevels(updatedLevels)
                setFormData(prev => ({ ...prev, categoryIds: [] })) // Clear until final selection
            } else {
                // Leaf node - collect all selected category IDs
                const updatedLevels = newLevels.slice(0, levelIndex + 1)
                setCategoryLevels(updatedLevels)

                const selectedIds = updatedLevels
                    .map(level => level.selectedId)
                    .filter((id): id is string => id !== null)

                setFormData(prev => ({ ...prev, categoryIds: selectedIds }))
            }
        } catch (error) {
            console.error('Failed to load child categories:', error)
        }
    }

    const handleResetCategory = () => {
        // Reset to initial state with root categories only
        setCategoryLevels(prev => [{
            level: 0,
            parentId: null,
            selectedId: null,
            options: prev[0]?.options || []
        }])
        setFormData(prev => ({ ...prev, categoryIds: [] }))
    }

    const handlePrimaryImageUploadComplete = (urls: string[]) => {
        if (urls.length > 0) {
            setPrimaryImage(urls[0])
        }
    }

    const handleGalleryImageUploadComplete = (urls: string[]) => {
        setGalleryImages(prev => [...prev, ...urls])
    }

    const removePrimaryImage = () => {
        setPrimaryImage('')
    }

    const removeGalleryImage = (index: number) => {
        setGalleryImages(prev => prev.filter((_, i) => i !== index))
    }

    const handleColorImageUploadComplete = (colorId: string, urls: string[]) => {
        setColorImages(prev => ({
            ...prev,
            [colorId]: urls.length > 0 ? [urls[0]] : []
        }))
    }

    const removeColorImage = (colorId: string, index: number) => {
        setColorImages(prev => ({
            ...prev,
            [colorId]: (prev[colorId] || []).filter((_, i) => i !== index)
        }))
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
                categoryId: formData.categoryIds.length > 0 ? formData.categoryIds : undefined,
                description: formData.description || undefined,
                material: formData.material || undefined,
                gender: formData.gender || undefined,
                seoTitle: formData.seoTitle || undefined,
                seoDescription: formData.seoDescription || undefined,
                version: createdProduct.version || 0,
                images: [
                    ...(primaryImage ? [{
                        imageUrl: primaryImage,
                        position: 0,
                        isDefault: true
                    }] : []),
                    ...galleryImages.map((url, index) => ({
                        imageUrl: url,
                        position: (primaryImage ? 1 : 0) + index,
                        isDefault: false
                    })),
                    ...images.map((url, index) => ({
                        imageUrl: url,
                        position: (primaryImage ? 1 : 0) + galleryImages.length + index,
                        isDefault: index === 0 && !primaryImage
                    })),
                    ...Object.entries(colorImages).flatMap(([colorId, urls]) =>
                        urls.map((url, index) => ({
                            imageUrl: url,
                            position: (primaryImage ? 1 : 0) + galleryImages.length + images.length + index,
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

    // Reuse variant management from ProductCreate
    const handleDeleteUnsavedVariant = (tempId: string) => {
        setUnsavedVariants(prev => prev.filter(v => v.id !== tempId))
    }

    const handleDeleteVariant = async (variantId: string) => {
        if (!createdProduct) return
        if (!confirm('Bạn có chắc muốn xóa biến thể này?')) return

        try {
            await ProductsApi.deleteVariant(createdProduct.id, variantId)
            setVariants(prev => prev.filter(v => v.id !== variantId))
            alert('Xóa biến thể thành công!')
        } catch (error) {
            console.error('Error deleting variant:', error)
            alert('Lỗi xóa biến thể')
        }
    }

    const handleEditVariant = (variantId: string) => {
        const variant = variants.find(v => v.id === variantId)
        if (!variant) return

        setEditFormData({
            sku: variant.sku || '',
            barcode: variant.barcode || '',
            priceAmount: variant.priceAmount?.toString() || '',
            compareAtAmount: variant.compareAtAmount?.toString() || '',
            weightGrams: variant.weightGrams?.toString() || '',
            status: variant.status || 'ACTIVE',
            quantityOnHand: variant.inventory?.quantityOnHand?.toString() || '0',
        })

        setEditingVariantId(variantId)
    }

    const handleSaveEdit = async (variantId: string) => {
        if (!createdProduct) return

        const variant = variants.find(v => v.id === variantId)
        if (!variant) return

        // Validate SKU
        const newSku = editFormData.sku.trim()
        if (!newSku) {
            alert('SKU không được để trống')
            return
        }

        const duplicateSaved = variants.find(v => v.id !== variantId && v.sku === newSku)
        if (duplicateSaved) {
            alert(`SKU "${newSku}" đã tồn tại trong biến thể đã lưu khác`)
            return
        }

        const duplicateUnsaved = unsavedVariants.find(v => v.sku === newSku)
        if (duplicateUnsaved) {
            alert(`SKU "${newSku}" đã tồn tại trong biến thể chưa lưu`)
            return
        }

        setEditLoading(true)
        try {
            const updateData: ProductsApi.VariantUpdateRequest = {
                sku: editFormData.sku,
                barcode: editFormData.barcode || undefined,
                priceAmount: parseFloat(editFormData.priceAmount),
                compareAtAmount: editFormData.compareAtAmount ? parseFloat(editFormData.compareAtAmount) : undefined,
                weightGrams: editFormData.weightGrams ? parseFloat(editFormData.weightGrams) : undefined,
                status: editFormData.status,
                version: variant.version || 0,
                inventory: {
                    quantityOnHand: parseInt(editFormData.quantityOnHand || '0'),
                },
            }

            await ProductsApi.updateVariant(createdProduct.id, variantId, updateData)

            const updatedVariants = await ProductsApi.getProductVariants(createdProduct.id)
            setVariants(updatedVariants)
            setEditingVariantId(null)
            alert('Cập nhật biến thể thành công!')
        } catch (error) {
            console.error('Error updating variant:', error)
            alert(extractErrorMessage(error, 'Lỗi cập nhật biến thể'))
        } finally {
            setEditLoading(false)
        }
    }

    if (loading) {
        return (
            <div className="flex items-center justify-center min-h-screen">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-red-600"></div>
            </div>
        )
    }

    if (!createdProduct) {
        return <div>Không tìm thấy sản phẩm</div>
    }

    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="flex items-center justify-between gap-4">
                <div className="flex items-center gap-4">
                    <button
                        onClick={() => navigate('/admin/products')}
                        className="p-2 hover:bg-gray-100 rounded"
                    >
                        <ArrowLeft className="w-5 h-5" />
                    </button>
                    <h1 className="text-2xl font-bold">Chỉnh sửa sản phẩm: {createdProduct.name}</h1>
                </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* Left Column - Product Info */}
                <div className="lg:col-span-2 space-y-6">
                    {/* Basic Info */}
                    <form onSubmit={(e) => { e.preventDefault(); handleSaveProduct(); }} className="bg-white shadow rounded-lg p-6 space-y-4">
                        <h2 className="text-lg font-semibold">Thông tin cơ bản</h2>

                        <div>
                            <label className="block text-sm font-medium mb-1">Tên sản phẩm *</label>
                            <input
                                type="text"
                                value={formData.name}
                                onChange={(e) => handleNameChange(e.target.value)}
                                className="w-full border rounded px-3 py-2"
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
                            <div className="space-y-2">
                                {(categoryLevels.length > 1 || categoryLevels[0]?.selectedId) && (
                                    <button
                                        type="button"
                                        onClick={handleResetCategory}
                                        className="text-sm text-blue-600 hover:text-blue-700 underline"
                                    >
                                        Đổi danh mục
                                    </button>
                                )}

                                {categoryLevels.map((level, index) => (
                                    <div key={level.level}>
                                        <label className="block text-xs text-gray-600 mb-1">
                                            {level.level === 0 ? 'Danh mục gốc' : `Danh mục cấp ${level.level}`}
                                        </label>
                                        <select
                                            value={level.selectedId || ''}
                                            onChange={(e) => handleCategoryChange(index, e.target.value)}
                                            className="w-full border rounded px-3 py-2"
                                        >
                                            <option value="">-- Chọn danh mục --</option>
                                            {level.options.map(cat => (
                                                <option key={cat.id} value={cat.id}>
                                                    {cat.name}
                                                </option>
                                            ))}
                                        </select>
                                    </div>
                                ))}

                                {formData.categoryIds.length > 0 && (
                                    <div className="text-xs text-green-600 mt-2">
                                        ✓ Đã chọn {formData.categoryIds.length} danh mục
                                    </div>
                                )}
                            </div>
                        </div>

                        <div>
                            <label className="block text-sm font-medium mb-1">Mô tả</label>
                            <textarea
                                value={formData.description}
                                onChange={(e) => setFormData(prev => ({ ...prev, description: e.target.value }))}
                                className="w-full border rounded px-3 py-2"
                                rows={4}
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium mb-1">Chất liệu</label>
                            <input
                                type="text"
                                value={formData.material}
                                onChange={(e) => setFormData(prev => ({ ...prev, material: e.target.value }))}
                                className="w-full border rounded px-3 py-2"
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium mb-1">SEO Title</label>
                            <input
                                type="text"
                                value={formData.seoTitle}
                                onChange={(e) => setFormData(prev => ({ ...prev, seoTitle: e.target.value }))}
                                className="w-full border rounded px-3 py-2"
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium mb-1">SEO Description</label>
                            <textarea
                                value={formData.seoDescription}
                                onChange={(e) => setFormData(prev => ({ ...prev, seoDescription: e.target.value }))}
                                className="w-full border rounded px-3 py-2"
                                rows={3}
                            />
                        </div>

                        <button
                            type="submit"
                            disabled={saving}
                            className="w-full bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 disabled:opacity-50"
                        >
                            {saving ? 'Đang lưu...' : 'Lưu thay đổi'}
                        </button>
                    </form>

                    {/* Variants Section */}
                    <div className="bg-white shadow rounded-lg p-6 space-y-4">
                        <h2 className="text-lg font-semibold">Biến thể sản phẩm</h2>

                        <ProductVariantsList
                            variants={variants}
                            unsavedVariants={unsavedVariants}
                            savingVariants={savingVariants}
                            sizes={sizes}
                            colors={colors}
                            createdProduct={createdProduct}
                            editingVariantId={editingVariantId}
                            editFormData={editFormData}
                            editLoading={editLoading}
                            colorImages={colorImages}
                            onColorImageUpload={handleColorImageUploadComplete}
                            onRemoveColorImage={removeColorImage}
                            onDeleteUnsavedVariant={handleDeleteUnsavedVariant}
                            onDeleteVariant={handleDeleteVariant}
                            onEditVariant={handleEditVariant}
                            onEditUnsavedVariant={() => { }}
                            onSaveEdit={handleSaveEdit}
                            onSaveUnsavedEdit={() => { }}
                            onSaveVariants={async () => { }}
                            onCancelEdit={() => setEditingVariantId(null)}
                            onEditFormDataChange={setEditFormData}
                        />
                    </div>
                </div>

                {/* Right Column - Images */}
                <div className="space-y-6">
                    {/* Primary Image */}
                    <div className="bg-white shadow rounded-lg p-6 space-y-4">
                        <h2 className="text-lg font-semibold">Ảnh chính (Primary Image)</h2>
                        <p className="text-sm text-gray-600">Chỉ chọn 1 ảnh làm ảnh đại diện chính cho sản phẩm</p>

                        {!primaryImage && (
                            <ImageUploadZone
                                id="primary-image"
                                description="Kéo thả hoặc click để chọn ảnh chính"
                                autoUpload={true}
                                uploadType="product"
                                onUploadComplete={handlePrimaryImageUploadComplete}
                            />
                        )}

                        {primaryImage && (
                            <div className="space-y-2">
                                <div className="relative group">
                                    <img
                                        src={primaryImage}
                                        alt="Primary"
                                        className="w-full h-64 object-cover rounded border"
                                    />
                                    <div className="absolute top-2 left-2 bg-blue-600 text-white text-xs px-2 py-1 rounded">
                                        Ảnh chính
                                    </div>
                                    <button
                                        type="button"
                                        onClick={removePrimaryImage}
                                        className="absolute top-2 right-2 bg-red-600 text-white px-3 py-1 rounded opacity-0 group-hover:opacity-100 transition-opacity"
                                    >
                                        ✕ Xóa
                                    </button>
                                </div>
                            </div>
                        )}
                    </div>

                    {/* Gallery Images */}
                    <div className="bg-white shadow rounded-lg p-6 space-y-4">
                        <h2 className="text-lg font-semibold">Thư viện ảnh (Gallery Images)</h2>
                        <p className="text-sm text-gray-600">Thêm nhiều ảnh để hiển thị trong thư viện sản phẩm</p>

                        <ImageUploadZone
                            id="gallery-images"
                            description="Kéo thả hoặc click để chọn nhiều ảnh"
                            autoUpload={true}
                            uploadType="product"
                            onUploadComplete={handleGalleryImageUploadComplete}
                        />

                        {galleryImages.length > 0 && (
                            <div className="space-y-2">
                                <h3 className="text-sm font-medium">Ảnh thư viện ({galleryImages.length})</h3>
                                <div className="grid grid-cols-2 gap-2">
                                    {galleryImages.map((url, idx) => (
                                        <div key={idx} className="relative group">
                                            <img
                                                src={url}
                                                alt={`Gallery ${idx + 1}`}
                                                className="w-full h-32 object-cover rounded border"
                                            />
                                            <button
                                                type="button"
                                                onClick={() => removeGalleryImage(idx)}
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
                </div>
            </div>
        </div>
    )
}
