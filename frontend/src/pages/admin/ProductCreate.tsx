import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { toast } from 'react-toastify'
import * as ProductsApi from '../../api/admin/products'
import * as CatalogApi from '../../api/admin/catalog'
import * as BrandCategoryApi from '../../api/admin/brandCategory'
import ImageUploadZone from '../../components/layout/ImageUploadZone'
import ProductStepper from './components/ProductStepper'
import VariantMatrixEditor from './components/VariantMatrixEditor'
import { ArrowLeft, ArrowRight, Save, Eye, Search, Globe } from 'lucide-react'
import { extractProblemMessage } from '../../lib/problemDetails'

const slugify = (value: string) =>
    value
        .trim()
        .toLowerCase()
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/(^-|-$)+/g, '')

interface VariantMatrixRow {
    id: string
    sizeId?: string
    sizeName?: string
    colorId?: string
    colorName?: string
    colorHex?: string
    sku: string
    priceAmount: string
    historyCost: string
    compareAtAmount: string
    quantityOnHand: string
    weightGrams: string
    reorderLevel: string
}

export default function ProductCreate() {
    const navigate = useNavigate()
    const { id: productId } = useParams()

    // Step management
    const [currentStep, setCurrentStep] = useState(1)
    const [completedSteps, setCompletedSteps] = useState<number[]>([])

    // Loading states
    const [loading, setLoading] = useState(false)
    const [saving, setSaving] = useState(false)

    // Step 1: General Info
    const [formData, setFormData] = useState({
        name: '',
        slug: '',
        brandId: '',
        description: '',
        material: '',
        gender: '' as '' | ProductsApi.Gender,
    })
    const [slugLocked, setSlugLocked] = useState(false)
    const [brands, setBrands] = useState<BrandCategoryApi.BrandDto[]>([])

    // Step 2: Categorization
    const [categories, setCategories] = useState<BrandCategoryApi.CategoryResponse[]>([])
    const [categoryLevels, setCategoryLevels] = useState<Array<{
        level: number
        parentId: string | null
        selectedId: string
        options: BrandCategoryApi.CategoryResponse[]
    }>>([])
    const [selectedCategoryIds, setSelectedCategoryIds] = useState<string[]>([])

    // Step 3: Variants & Inventory
    const [sizes, setSizes] = useState<CatalogApi.SizeDto[]>([])
    const [colors, setColors] = useState<CatalogApi.ColorDto[]>([])
    const [variantRows, setVariantRows] = useState<VariantMatrixRow[]>([])

    // Step 4: Images
    const [primaryImage, setPrimaryImage] = useState<string>('')
    const [galleryImages, setGalleryImages] = useState<string[]>([])
    const [colorImageMap, setColorImageMap] = useState<Record<string, string[]>>({})

    // Step 5: SEO & Finalize
    const [seoData, setSeoData] = useState({
        seoTitle: '',
        seoDescription: '',
    })
    const [publishNow, setPublishNow] = useState(false)

    // Created product
    const [createdProduct, setCreatedProduct] = useState<ProductsApi.ProductDetailResponse | null>(null)

    useEffect(() => {
        loadReferences()
    }, [])

    const loadReferences = async () => {
        setLoading(true)
        try {
            const [brandsRes, categoriesRes, sizesRes, colorsRes] = await Promise.all([
                BrandCategoryApi.getBrands({ size: 1000 }),
                BrandCategoryApi.getCategories({ size: 1000 }),
                CatalogApi.getSizes(),
                CatalogApi.getColors(),
            ])

            const brandsList = brandsRes.content || []
            const categoriesList = categoriesRes.content || []
            
            setBrands(brandsList)
            setCategories(categoriesList)
            setSizes(sizesRes)
            setColors(colorsRes)

            // Initialize category levels
            const roots = categoriesList.filter((cat: any) => !cat.parentId || cat.level === 0)
            setCategoryLevels([{
                level: 0,
                parentId: null,
                selectedId: '',
                options: roots
            }])
        } catch (error) {
            console.error('Error loading references:', error)
            toast.error('Không thể tải dữ liệu. Vui lòng thử lại.')
        } finally {
            setLoading(false)
        }
    }

    const handleNameChange = (value: string) => {
        setFormData(prev => ({ ...prev, name: value }))
        if (!slugLocked) {
            setFormData(prev => ({ ...prev, slug: slugify(value) }))
        }
    }

    const handleSlugChange = (value: string) => {
        setFormData(prev => ({ ...prev, slug: value }))
        setSlugLocked(true)
    }

    const handleCategoryChange = (levelIndex: number, categoryId: string) => {
        if (!categoryId) {
            // Clear selection
            setCategoryLevels(prev => prev.slice(0, levelIndex + 1))
            setSelectedCategoryIds([])
            return
        }

        const selectedCategory = categories.find(c => c.id === categoryId)
        if (!selectedCategory) return

        // Update current level
        const updatedLevels = [...categoryLevels]
        updatedLevels[levelIndex] = { ...updatedLevels[levelIndex], selectedId: categoryId }

        // Find children
        const children = categories.filter(cat => cat.parentId === categoryId)

        if (children.length > 0) {
            // Has children - add next level
            updatedLevels.splice(levelIndex + 1, updatedLevels.length, {
                level: levelIndex + 1,
                parentId: categoryId,
                selectedId: '',
                options: children
            })
            setCategoryLevels(updatedLevels)
            setSelectedCategoryIds([]) // Not a leaf yet
        } else {
            // Leaf node - save as selected
            setCategoryLevels(updatedLevels.slice(0, levelIndex + 1))
            setSelectedCategoryIds([categoryId])
        }
    }

    const getSelectedCategoryPath = () => {
        const selectedIds = categoryLevels.map(level => level.selectedId).filter(Boolean)
        return selectedIds
            .map(id => categories.find(c => c.id === id)?.name)
            .filter(Boolean)
            .join(' > ')
    }

    // STEP 1: Create product draft
    const handleStep1Submit = async () => {
        if (!formData.name.trim() || !formData.slug.trim()) {
            toast.warning('Vui lòng nhập tên sản phẩm và slug')
            return
        }

        setSaving(true)
        try {
            const response = await ProductsApi.createProduct({
                name: formData.name,
                slug: formData.slug,
                brandId: formData.brandId || undefined,
                description: formData.description || undefined,
                material: formData.material || undefined,
                gender: formData.gender || undefined,
                priceAmount: undefined, // Will be set later with variants
                seoTitle: formData.name,
                seoDescription: formData.description || formData.name,
            })

            setCreatedProduct(response.data)
            setCompletedSteps([...completedSteps, 1])
            setCurrentStep(2)
            toast.success('Đã tạo sản phẩm nháp thành công!')
        } catch (error) {
            console.error('Error creating product:', error)
            toast.error(extractProblemMessage(error) || 'Không thể tạo sản phẩm')
        } finally {
            setSaving(false)
        }
    }

    // STEP 2: Update categories
    const handleStep2Submit = async () => {
        if (!createdProduct) return

        if (selectedCategoryIds.length === 0) {
            toast.warning('Vui lòng chọn ít nhất một danh mục')
            return
        }

        setSaving(true)
        try {
            await ProductsApi.updateProduct(createdProduct.id, {
                name: createdProduct.name,
                priceAmount: createdProduct.price || 0,
                categoryIds: selectedCategoryIds,
                brandId: createdProduct.brandId,
                description: createdProduct.description,
                material: createdProduct.material,
                gender: createdProduct.gender,
                seoTitle: createdProduct.seoTitle,
                seoDescription: createdProduct.seoDescription,
            })

            setCompletedSteps([...completedSteps, 2])
            setCurrentStep(3)
        } catch (error) {
            console.error('Error updating categories:', error)
            toast.error(extractProblemMessage(error) || 'Không thể cập nhật danh mục')
        } finally {
            setSaving(false)
        }
    }

    // STEP 3: Create variants
    const handleStep3Submit = async () => {
        if (!createdProduct) return

        if (variantRows.length === 0) {
            toast.warning('Vui lòng tạo ít nhất một biến thể')
            return
        }

        // Validate all variants have required data
        const invalidVariants = variantRows.filter(v => !v.sku || !v.priceAmount || !v.quantityOnHand)
        if (invalidVariants.length > 0) {
            toast.warning('Vui lòng điền đầy đủ SKU, giá bán và số lượng tồn kho cho tất cả biến thể')
            return
        }

        setSaving(true)
        try {
            // Create all variants
            for (const row of variantRows) {
                await ProductsApi.createVariant(createdProduct.id, {
                    sku: row.sku,
                    sizeId: row.sizeId,
                    colorId: row.colorId,
                    priceAmount: parseInt(row.priceAmount),
                    compareAtAmount: row.compareAtAmount ? parseInt(row.compareAtAmount) : undefined,
                    historyCost: row.historyCost ? parseInt(row.historyCost) : undefined,
                    weightGrams: row.weightGrams ? parseInt(row.weightGrams) : 200,
                    inventory: {
                        quantityOnHand: parseInt(row.quantityOnHand),
                        reorderLevel: parseInt(row.reorderLevel) || 5,
                    },
                })
            }

            setCompletedSteps([...completedSteps, 3])
            setCurrentStep(4)
            toast.success('Đã tạo tất cả biến thể thành công!')
        } catch (error) {
            console.error('Error creating variants:', error)
            toast.error(extractProblemMessage(error) || 'Không thể tạo biến thể')
        } finally {
            setSaving(false)
        }
    }

    // STEP 4: Upload images
    const handleStep4Submit = async () => {
        if (!createdProduct) return

        if (!primaryImage && galleryImages.length === 0) {
            toast.warning('Vui lòng tải lên ít nhất một hình ảnh')
            return
        }

        setSaving(true)
        try {
            const imageRequests: ProductsApi.ProductImageRequest[] = []

            // Primary image
            if (primaryImage) {
                imageRequests.push({
                    imageUrl: primaryImage,
                    isDefault: true,
                    position: 0,
                })
            }

            // Gallery images
            galleryImages.forEach((url, index) => {
                imageRequests.push({
                    imageUrl: url,
                    isDefault: false,
                    position: (primaryImage ? 1 : 0) + index,
                })
            })

            // Color-specific images
            Object.entries(colorImageMap).forEach(([colorId, urls]) => {
                urls.forEach((url, index) => {
                    imageRequests.push({
                        imageUrl: url,
                        colorId,
                        isDefault: false,
                        position: imageRequests.length + index,
                    })
                })
            })

            await ProductsApi.updateProduct(createdProduct.id, {
                name: createdProduct.name,
                priceAmount: createdProduct.price || 0,
                categoryIds: selectedCategoryIds,
                images: imageRequests,
                brandId: createdProduct.brandId,
                description: createdProduct.description,
                material: createdProduct.material,
                gender: createdProduct.gender,
                seoTitle: createdProduct.seoTitle,
                seoDescription: createdProduct.seoDescription,
            })

            setCompletedSteps([...completedSteps, 4])
            setCurrentStep(5)
        } catch (error) {
            console.error('Error uploading images:', error)
            toast.error(extractProblemMessage(error) || 'Không thể tải lên hình ảnh')
        } finally {
            setSaving(false)
        }
    }

    // STEP 5: Finalize and publish
    const handleStep5Submit = async () => {
        if (!createdProduct) return

        setSaving(true)
        try {
            await ProductsApi.updateProduct(createdProduct.id, {
                name: createdProduct.name,
                priceAmount: createdProduct.price || 0,
                categoryIds: selectedCategoryIds,
                seoTitle: seoData.seoTitle || createdProduct.name,
                seoDescription: seoData.seoDescription || createdProduct.description,
                brandId: createdProduct.brandId,
                description: createdProduct.description,
                material: createdProduct.material,
                gender: createdProduct.gender,
            })

            if (publishNow) {
                await ProductsApi.publishProduct(createdProduct.id)
                toast.success('Sản phẩm đã được xuất bản thành công!')
            } else {
                toast.success('Sản phẩm đã được lưu dưới dạng nháp!')
            }

            navigate('/admin/products')
        } catch (error) {
            console.error('Error finalizing product:', error)
            toast.error(extractProblemMessage(error) || 'Không thể hoàn tất sản phẩm')
        } finally {
            setSaving(false)
        }
    }

    const handlePrimaryImageUpload = (urls: string[]) => {
        if (urls.length > 0) {
            setPrimaryImage(urls[0])
        }
    }

    const handleGalleryImageUpload = (urls: string[]) => {
        setGalleryImages(prev => [...prev, ...urls])
    }

    const handleColorImageUpload = (colorId: string, urls: string[]) => {
        setColorImageMap(prev => ({
            ...prev,
            [colorId]: [...(prev[colorId] || []), ...urls],
        }))
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
            <div className="flex items-center justify-between">
                <div className="flex items-center gap-4">
                    <button
                        onClick={() => navigate('/admin/products')}
                        className="p-2 hover:bg-gray-100 rounded"
                    >
                        <ArrowLeft className="w-5 h-5" />
                    </button>
                    <h1 className="text-2xl font-bold">
                        {createdProduct ? `Chỉnh sửa: ${createdProduct.name}` : 'Thêm sản phẩm mới'}
                    </h1>
                </div>
            </div>

            {/* Stepper */}
            <ProductStepper currentStep={currentStep} completedSteps={completedSteps} />

            {/* Step Content */}
            <div className="bg-white shadow rounded-lg p-6">
                {/* STEP 1: General Info */}
                {currentStep === 1 && (
                    <div className="space-y-6">
                        <div>
                            <h2 className="text-xl font-semibold mb-4">Bước 1: Thông tin chung</h2>
                            <p className="text-gray-600 mb-6">Nhập thông tin cơ bản về sản phẩm của bạn</p>
                        </div>

                        <div>
                            <label className="block text-sm font-medium mb-2">Tên sản phẩm *</label>
                            <input
                                type="text"
                                value={formData.name}
                                onChange={(e) => handleNameChange(e.target.value)}
                                className="w-full border rounded-lg px-4 py-3 text-lg"
                                placeholder="VD: Áo thun cotton cao cấp"
                                required
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium mb-2">Slug (URL thân thiện) *</label>
                            <input
                                type="text"
                                value={formData.slug}
                                onChange={(e) => handleSlugChange(e.target.value)}
                                className="w-full border rounded-lg px-4 py-3"
                                placeholder="ao-thun-cotton-cao-cap"
                                required
                            />
                            <p className="text-xs text-gray-500 mt-1">
                                URL sẽ là: /products/{formData.slug || 'slug-cua-san-pham'}
                            </p>
                        </div>

                        <div>
                            <label className="block text-sm font-medium mb-2">Thương hiệu</label>
                            <select
                                value={formData.brandId}
                                onChange={(e) => setFormData(prev => ({ ...prev, brandId: e.target.value }))}
                                className="w-full border rounded-lg px-4 py-3"
                            >
                                <option value="">-- Chọn thương hiệu --</option>
                                {brands.map(brand => (
                                    <option key={brand.id} value={brand.id}>{brand.name}</option>
                                ))}
                            </select>
                        </div>

                        <div>
                            <label className="block text-sm font-medium mb-2">Mô tả sản phẩm</label>
                            <textarea
                                value={formData.description}
                                onChange={(e) => setFormData(prev => ({ ...prev, description: e.target.value }))}
                                className="w-full border rounded-lg px-4 py-3"
                                rows={6}
                                placeholder="Nhập mô tả chi tiết về sản phẩm..."
                            />
                        </div>

                        <div className="grid grid-cols-2 gap-4">
                            <div>
                                <label className="block text-sm font-medium mb-2">Chất liệu</label>
                                <input
                                    type="text"
                                    value={formData.material}
                                    onChange={(e) => setFormData(prev => ({ ...prev, material: e.target.value }))}
                                    className="w-full border rounded-lg px-4 py-3"
                                    placeholder="VD: Cotton 100%"
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium mb-2">Giới tính</label>
                                <div className="flex gap-4 pt-2">
                                    <label className="flex items-center gap-2 cursor-pointer">
                                        <input
                                            type="radio"
                                            name="gender"
                                            value="men"
                                            checked={formData.gender === 'men'}
                                            onChange={(e) => setFormData(prev => ({ ...prev, gender: e.target.value as ProductsApi.Gender }))}
                                            className="w-4 h-4"
                                        />
                                        <span>Nam</span>
                                    </label>
                                    <label className="flex items-center gap-2 cursor-pointer">
                                        <input
                                            type="radio"
                                            name="gender"
                                            value="women"
                                            checked={formData.gender === 'women'}
                                            onChange={(e) => setFormData(prev => ({ ...prev, gender: e.target.value as ProductsApi.Gender }))}
                                            className="w-4 h-4"
                                        />
                                        <span>Nữ</span>
                                    </label>
                                    <label className="flex items-center gap-2 cursor-pointer">
                                        <input
                                            type="radio"
                                            name="gender"
                                            value="unisex"
                                            checked={formData.gender === 'unisex'}
                                            onChange={(e) => setFormData(prev => ({ ...prev, gender: e.target.value as ProductsApi.Gender }))}
                                            className="w-4 h-4"
                                        />
                                        <span>Unisex</span>
                                    </label>
                                </div>
                            </div>
                        </div>

                        <div className="flex justify-end gap-3 pt-6 border-t">
                            <button
                                type="button"
                                onClick={handleStep1Submit}
                                disabled={saving || !formData.name || !formData.slug}
                                className="flex items-center gap-2 bg-red-600 text-white px-6 py-3 rounded-lg font-medium hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                {saving ? 'Đang lưu...' : 'Tạo & Tiếp tục'}
                                <ArrowRight className="w-5 h-5" />
                            </button>
                        </div>
                    </div>
                )}

                {/* STEP 2: Categorization */}
                {currentStep === 2 && (
                    <div className="space-y-6">
                        <div>
                            <h2 className="text-xl font-semibold mb-4">Bước 2: Phân loại sản phẩm</h2>
                            <p className="text-gray-600 mb-6">Chọn danh mục phù hợp để khách hàng dễ dàng tìm kiếm</p>
                        </div>

                        <div className="space-y-4">
                            {categoryLevels.map((level, index) => (
                                <div key={`category-level-${index}`}>
                                    <label className="block text-sm font-medium mb-2">
                                        {index === 0 ? 'Danh mục chính' : `Danh mục con cấp ${index}`}
                                    </label>
                                    <select
                                        value={level.selectedId}
                                        onChange={(e) => handleCategoryChange(index, e.target.value)}
                                        className="w-full border rounded-lg px-4 py-3"
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

                            {selectedCategoryIds.length > 0 && (
                                <div className="bg-green-50 border border-green-200 rounded-lg p-4">
                                    <p className="text-sm text-green-700">
                                        ✓ Đã chọn: <strong>{getSelectedCategoryPath()}</strong>
                                    </p>
                                </div>
                            )}
                        </div>

                        <div className="flex justify-between gap-3 pt-6 border-t">
                            <button
                                type="button"
                                onClick={() => setCurrentStep(1)}
                                className="flex items-center gap-2 border border-gray-300 px-6 py-3 rounded-lg font-medium hover:bg-gray-50"
                            >
                                <ArrowLeft className="w-5 h-5" />
                                Quay lại
                            </button>
                            <button
                                type="button"
                                onClick={handleStep2Submit}
                                disabled={saving || selectedCategoryIds.length === 0}
                                className="flex items-center gap-2 bg-red-600 text-white px-6 py-3 rounded-lg font-medium hover:bg-red-700 disabled:opacity-50"
                            >
                                {saving ? 'Đang lưu...' : 'Lưu & Tiếp tục'}
                                <ArrowRight className="w-5 h-5" />
                            </button>
                        </div>
                    </div>
                )}

                {/* STEP 3: Variants & Inventory */}
                {currentStep === 3 && (
                    <div className="space-y-6">
                        <div>
                            <h2 className="text-xl font-semibold mb-4">Bước 3: Biến thể & Kho hàng</h2>
                            <p className="text-gray-600 mb-6">Tạo các biến thể sản phẩm (kích thước, màu sắc) và quản lý tồn kho</p>
                        </div>

                        <VariantMatrixEditor
                            sizes={sizes}
                            colors={colors}
                            variants={variantRows}
                            onVariantsChange={setVariantRows}
                            productName={formData.name}
                        />

                        <div className="flex justify-between gap-3 pt-6 border-t">
                            <button
                                type="button"
                                onClick={() => setCurrentStep(2)}
                                className="flex items-center gap-2 border border-gray-300 px-6 py-3 rounded-lg font-medium hover:bg-gray-50"
                            >
                                <ArrowLeft className="w-5 h-5" />
                                Quay lại
                            </button>
                            <button
                                type="button"
                                onClick={handleStep3Submit}
                                disabled={saving || variantRows.length === 0}
                                className="flex items-center gap-2 bg-red-600 text-white px-6 py-3 rounded-lg font-medium hover:bg-red-700 disabled:opacity-50"
                            >
                                {saving ? 'Đang lưu...' : 'Lưu cấu hình & Tiếp tục'}
                                <ArrowRight className="w-5 h-5" />
                            </button>
                        </div>
                    </div>
                )}

                {/* STEP 4: Media Gallery */}
                {currentStep === 4 && (
                    <div className="space-y-6">
                        <div>
                            <h2 className="text-xl font-semibold mb-4">Bước 4: Hình ảnh sản phẩm</h2>
                            <p className="text-gray-600 mb-6">Thêm hình ảnh để sản phẩm hấp dẫn hơn</p>
                        </div>

                        {/* Primary Image */}
                        <div className="border rounded-lg p-6">
                            <h3 className="font-semibold mb-4">Ảnh đại diện ⭐</h3>
                            {!primaryImage ? (
                                <ImageUploadZone
                                    id="primary-image"
                                    description="Kéo thả hoặc click để chọn ảnh đại diện"
                                    autoUpload={true}
                                    uploadType="product"
                                    onUploadComplete={handlePrimaryImageUpload}
                                />
                            ) : (
                                <div className="relative inline-block">
                                    <img src={primaryImage} alt="Primary" className="w-48 h-48 object-cover rounded-lg border-4 border-yellow-400" />
                                    <button
                                        onClick={() => setPrimaryImage('')}
                                        className="absolute -top-2 -right-2 bg-red-600 text-white rounded-full w-8 h-8 flex items-center justify-center hover:bg-red-700"
                                    >
                                        ✕
                                    </button>
                                </div>
                            )}
                        </div>

                        {/* Gallery Images */}
                        <div className="border rounded-lg p-6">
                            <h3 className="font-semibold mb-4">Thư viện ảnh (Gallery)</h3>
                            <ImageUploadZone
                                id="gallery-images"
                                description="Kéo thả hoặc click để chọn nhiều ảnh"
                                autoUpload={true}
                                uploadType="product"
                                onUploadComplete={handleGalleryImageUpload}
                            />
                            {galleryImages.length > 0 && (
                                <div className="grid grid-cols-4 gap-4 mt-4">
                                    {galleryImages.map((url, idx) => (
                                        <div key={idx} className="relative group">
                                            <img src={url} alt={`Gallery ${idx}`} className="w-full h-32 object-cover rounded-lg border" />
                                            <button
                                                onClick={() => setGalleryImages(prev => prev.filter((_, i) => i !== idx))}
                                                className="absolute top-1 right-1 bg-red-600 text-white rounded-full w-6 h-6 opacity-0 group-hover:opacity-100 transition-opacity"
                                            >
                                                ✕
                                            </button>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>

                        {/* Color-specific Images */}
                        {variantRows.some(v => v.colorId) && (
                            <div className="border rounded-lg p-6">
                                <h3 className="font-semibold mb-4">Ảnh theo màu sắc</h3>
                                <p className="text-sm text-gray-600 mb-4">Tải lên ảnh riêng cho từng màu sắc</p>
                                
                                <div className="space-y-4">
                                    {Array.from(new Set(variantRows.map(v => v.colorId).filter(Boolean))).map(colorId => {
                                        const color = colors.find(c => c.id === colorId)
                                        if (!color) return null

                                        return (
                                            <div key={colorId} className="border rounded p-4">
                                                <div className="flex items-center gap-3 mb-3">
                                                    <div
                                                        className="w-8 h-8 rounded border-2 border-gray-300"
                                                        style={{ backgroundColor: color.hexCode }}
                                                    />
                                                    <span className="font-medium">{color.name}</span>
                                                </div>
                                                <ImageUploadZone
                                                    id={`color-${colorId}`}
                                                    description={`Ảnh cho màu ${color.name}`}
                                                    autoUpload={true}
                                                    uploadType="product"
                                                    onUploadComplete={(urls) => handleColorImageUpload(colorId!, urls)}
                                                />
                                                {colorImageMap[colorId!]?.length > 0 && (
                                                    <div className="grid grid-cols-4 gap-2 mt-3">
                                                        {colorImageMap[colorId!].map((url, idx) => (
                                                            <div key={idx} className="relative group">
                                                                <img src={url} alt={`${color.name} ${idx}`} className="w-full h-20 object-cover rounded border" />
                                                            </div>
                                                        ))}
                                                    </div>
                                                )}
                                            </div>
                                        )
                                    })}
                                </div>
                            </div>
                        )}

                        <div className="flex justify-between gap-3 pt-6 border-t">
                            <button
                                type="button"
                                onClick={() => setCurrentStep(3)}
                                className="flex items-center gap-2 border border-gray-300 px-6 py-3 rounded-lg font-medium hover:bg-gray-50"
                            >
                                <ArrowLeft className="w-5 h-5" />
                                Quay lại
                            </button>
                            <button
                                type="button"
                                onClick={handleStep4Submit}
                                disabled={saving}
                                className="flex items-center gap-2 bg-red-600 text-white px-6 py-3 rounded-lg font-medium hover:bg-red-700 disabled:opacity-50"
                            >
                                {saving ? 'Đang lưu...' : 'Lưu hình ảnh & Tiếp tục'}
                                <ArrowRight className="w-5 h-5" />
                            </button>
                        </div>
                    </div>
                )}

                {/* STEP 5: SEO & Finalize */}
                {currentStep === 5 && (
                    <div className="space-y-6">
                        <div>
                            <h2 className="text-xl font-semibold mb-4">Bước 5: SEO & Hoàn tất</h2>
                            <p className="text-gray-600 mb-6">Tối ưu hóa SEO và quyết định xuất bản sản phẩm</p>
                        </div>

                        {/* SEO Preview */}
                        <div className="border rounded-lg p-6 bg-gray-50">
                            <h3 className="font-semibold mb-4 flex items-center gap-2">
                                <Globe className="w-5 h-5" />
                                Xem trước trên Google
                            </h3>
                            <div className="bg-white p-4 rounded border">
                                <div className="text-blue-600 text-lg hover:underline cursor-pointer">
                                    {seoData.seoTitle || formData.name}
                                </div>
                                <div className="text-green-700 text-sm mt-1">
                                    https://yourdomain.com/products/{formData.slug}
                                </div>
                                <div className="text-gray-600 text-sm mt-2">
                                    {seoData.seoDescription || formData.description || 'Mô tả sản phẩm sẽ hiển thị ở đây'}
                                </div>
                            </div>
                        </div>

                        {/* SEO Fields */}
                        <div className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium mb-2">SEO Title</label>
                                <input
                                    type="text"
                                    value={seoData.seoTitle}
                                    onChange={(e) => setSeoData(prev => ({ ...prev, seoTitle: e.target.value }))}
                                    className="w-full border rounded-lg px-4 py-3"
                                    placeholder={formData.name}
                                    maxLength={60}
                                />
                                <p className="text-xs text-gray-500 mt-1">{seoData.seoTitle.length}/60 ký tự</p>
                            </div>

                            <div>
                                <label className="block text-sm font-medium mb-2">SEO Description</label>
                                <textarea
                                    value={seoData.seoDescription}
                                    onChange={(e) => setSeoData(prev => ({ ...prev, seoDescription: e.target.value }))}
                                    className="w-full border rounded-lg px-4 py-3"
                                    rows={3}
                                    placeholder={formData.description}
                                    maxLength={160}
                                />
                                <p className="text-xs text-gray-500 mt-1">{seoData.seoDescription.length}/160 ký tự</p>
                            </div>
                        </div>

                        {/* Summary Stats */}
                        <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
                            <h3 className="font-semibold mb-4">Tóm tắt sản phẩm</h3>
                            <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
                                <div className="bg-white rounded-lg p-4">
                                    <div className="text-2xl font-bold text-red-600">{variantRows.length}</div>
                                    <div className="text-sm text-gray-600">Biến thể</div>
                                </div>
                                <div className="bg-white rounded-lg p-4">
                                    <div className="text-2xl font-bold text-green-600">
                                        {variantRows.reduce((sum, v) => sum + parseInt(v.quantityOnHand || '0'), 0)}
                                    </div>
                                    <div className="text-sm text-gray-600">Tổng tồn kho</div>
                                </div>
                                <div className="bg-white rounded-lg p-4">
                                    <div className="text-2xl font-bold text-blue-600">
                                        {primaryImage ? 1 : 0} + {galleryImages.length}
                                    </div>
                                    <div className="text-sm text-gray-600">Hình ảnh</div>
                                </div>
                                <div className="bg-white rounded-lg p-4">
                                    <div className="text-2xl font-bold text-purple-600">
                                        {selectedCategoryIds.length}
                                    </div>
                                    <div className="text-sm text-gray-600">Danh mục</div>
                                </div>
                            </div>
                        </div>

                        {/* Publish Toggle */}
                        <div className="border rounded-lg p-6">
                            <label className="flex items-center justify-between cursor-pointer">
                                <div>
                                    <div className="font-semibold">Xuất bản ngay lập tức</div>
                                    <div className="text-sm text-gray-600">
                                        {publishNow 
                                            ? 'Sản phẩm sẽ hiển thị công khai trên website' 
                                            : 'Sản phẩm sẽ được lưu dưới dạng nháp (Draft)'}
                                    </div>
                                </div>
                                <div className="relative">
                                    <input
                                        type="checkbox"
                                        checked={publishNow}
                                        onChange={(e) => setPublishNow(e.target.checked)}
                                        className="sr-only peer"
                                    />
                                    <div className="w-14 h-8 bg-gray-300 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-red-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-1 after:left-1 after:bg-white after:border-gray-300 after:border after:rounded-full after:h-6 after:w-6 after:transition-all peer-checked:bg-red-600"></div>
                                </div>
                            </label>
                        </div>

                        <div className="flex justify-between gap-3 pt-6 border-t">
                            <button
                                type="button"
                                onClick={() => setCurrentStep(4)}
                                className="flex items-center gap-2 border border-gray-300 px-6 py-3 rounded-lg font-medium hover:bg-gray-50"
                            >
                                <ArrowLeft className="w-5 h-5" />
                                Quay lại
                            </button>
                            <button
                                type="button"
                                onClick={handleStep5Submit}
                                disabled={saving}
                                className="flex items-center gap-2 bg-green-600 text-white px-8 py-3 rounded-lg font-medium text-lg hover:bg-green-700 disabled:opacity-50"
                            >
                                {saving ? 'Đang xử lý...' : publishNow ? '🚀 Hoàn tất & Đăng bán' : '💾 Lưu nháp'}
                            </button>
                        </div>
                    </div>
                )}
            </div>
        </div>
    )
}

