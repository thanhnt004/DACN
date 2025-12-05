import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import * as ProductsApi from '../../api/admin/products'
import * as CatalogApi from '../../api/admin/catalog'
import * as BrandCategoryApi from '../../api/admin/brandCategory'
import ImageUploadZone from '../../components/layout/ImageUploadZone'
import ProductVariantsList from './components/ProductVariantsList'
import { ArrowLeft, Plus } from 'lucide-react'
import { extractProblemMessage } from '../../lib/problemDetails'

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
    const [primaryImage, setPrimaryImage] = useState<string>('') // Single primary image
    const [galleryImages, setGalleryImages] = useState<string[]>([]) // Multiple gallery images
    const [images, setImages] = useState<string[]>([])
    const [colorImages, setColorImages] = useState<Record<string, string[]>>({}) // colorId -> image URLs
    const [publishSuccess, setPublishSuccess] = useState(false) // Show success notification
    const [brands, setBrands] = useState<BrandCategoryApi.BrandDto[]>([])
    const [categories, setCategories] = useState<BrandCategoryApi.CategoryResponse[]>([])

    // Category selection with multiple levels support
    const [categoryLevels, setCategoryLevels] = useState<Array<{
        level: number
        parentId: string | null
        selectedId: string
        options: BrandCategoryApi.CategoryResponse[]
    }>>([])

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
        historyCost?: number
        weightGrams?: number
        status: ProductsApi.VariantStatus
        inventory?: { quantityOnHand: number; reorderLevel: number }
    }>>([])
    const [savingVariants, setSavingVariants] = useState(false)

    // Variant creation
    const [selectedSizes, setSelectedSizes] = useState<string[]>([])
    const [selectedColors, setSelectedColors] = useState<string[]>([])
    const [defaultWeight, setDefaultWeight] = useState<string>('200')
    const [defaultInventory, setDefaultInventory] = useState<string>('10')
    const [defaultReorderLevel, setDefaultReorderLevel] = useState<string>('5')
    const [defaultHistoryCost, setDefaultHistoryCost] = useState<string>('0')

    // Variant editing
    const [editingVariantId, setEditingVariantId] = useState<string | null>(null)
    const [editFormData, setEditFormData] = useState<{
        sku: string
        barcode: string
        priceAmount: string
        compareAtAmount: string
        historyCost: string
        weightGrams: string
        status: ProductsApi.VariantStatus
        quantityOnHand: string
        reorderLevel: string
    }>({
        sku: '',
        barcode: '',
        priceAmount: '',
        compareAtAmount: '',
        historyCost: '',
        weightGrams: '',
        status: 'ACTIVE',
        quantityOnHand: '',
        reorderLevel: '',
    })
    const [editLoading, setEditLoading] = useState(false)

    useEffect(() => {
        loadReferences()
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [])

    // Sync images when product is loaded
    useEffect(() => {
        if (createdProduct && createdProduct.images) {
            const sortedImages = [...createdProduct.images].sort((a, b) => (a.position || 0) - (b.position || 0))

            // Find primary image (isDefault = true or first image without colorId)
            const primaryImg = sortedImages.find(img => img.isDefault && !img.colorId)
            if (primaryImg) {
                setPrimaryImage(primaryImg.imageUrl)
            }

            // Get gallery images (not primary, not color-specific)
            const galleryImgs = sortedImages
                .filter(img => !img.isDefault && !img.colorId)
                .map(img => img.imageUrl)
            setGalleryImages(galleryImgs)

            // Legacy: Keep old images array for backward compatibility
            const imageUrls = sortedImages.map(img => img.imageUrl)
            setImages(imageUrls)
        }
    }, [createdProduct])

    // Sync category selection when categories are loaded or formData changes
    useEffect(() => {
        if (categories.length === 0) return

        // If formData has categoryIds (e.g., when editing), rebuild category levels
        if (formData.categoryIds.length > 0) {
            const selectedIds = formData.categoryIds
            const newLevels: typeof categoryLevels = []

            // Build category levels from selected IDs
            for (let i = 0; i < selectedIds.length; i++) {
                const categoryId = selectedIds[i]
                const category = categories.find(c => c.id === categoryId)

                if (category) {
                    const parentId = category.parentId || null
                    const siblings = categories.filter(cat =>
                        (parentId === null && !cat.parentId) || cat.parentId === parentId
                    )

                    newLevels.push({
                        level: i,
                        parentId: parentId,
                        selectedId: categoryId,
                        options: siblings
                    })
                }
            }

            // Check if last selected category has children
            const lastSelectedId = selectedIds[selectedIds.length - 1]
            const children = categories.filter(cat => cat.parentId === lastSelectedId)
            if (children.length > 0) {
                newLevels.push({
                    level: selectedIds.length,
                    parentId: lastSelectedId,
                    selectedId: '',
                    options: children
                })
            }

            if (newLevels.length > 0) {
                setCategoryLevels(newLevels)
            }
        } else if (categoryLevels.length === 0) {
            // Initialize with root categories if no levels exist
            const roots = categories.filter(cat => !cat.parentId || cat.level === 0)
            if (roots.length > 0) {
                setCategoryLevels([{
                    level: 0,
                    parentId: null,
                    selectedId: '',
                    options: roots
                }])
            }
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [categories, formData.categoryIds])

    const loadReferences = async () => {
        setLoading(true)
        try {
            const [brandsData, rootCategoriesData, sizesData, colorsData] = await Promise.all([
                BrandCategoryApi.getBrands({ size: 100 }),
                // Get only root categories (parentId not specified = root only)
                BrandCategoryApi.getCategories({ size: 100 }),
                CatalogApi.getSizes(),
                CatalogApi.getColors(),
            ])

            setBrands(brandsData.content || [])
            const rootCategories = rootCategoriesData.content || []
            setCategories(rootCategories) // Initially only root categories

            // Initialize first level with root categories
            if (rootCategories.length > 0) {
                setCategoryLevels([{
                    level: 0,
                    parentId: null,
                    selectedId: '',
                    options: rootCategories
                }])
            }

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
        const responseData = error && typeof error === 'object' && 'response' in error
            ? (error as { response?: { data?: unknown } }).response?.data
            : undefined
        const fallback = error instanceof Error && error.message ? error.message : defaultMessage
        return extractProblemMessage(responseData, fallback)
    }

    // Handle category selection at any level
    const handleCategoryChange = async (levelIndex: number, categoryId: string) => {
        if (!categoryId) {
            // User selected "-- Chọn danh mục --" (empty value)
            // Reset to this level only, clear all subsequent levels
            const resetLevels = categoryLevels.slice(0, levelIndex + 1)
            resetLevels[levelIndex] = {
                ...resetLevels[levelIndex],
                selectedId: '' // Clear selection at this level
            }
            setCategoryLevels(resetLevels)
            setFormData(prev => ({ ...prev, categoryIds: [] }))
            return
        }

        // Update current level selection
        const newLevels = [...categoryLevels]
        newLevels[levelIndex] = {
            ...newLevels[levelIndex],
            selectedId: categoryId
        }

        // Remove all subsequent levels (important when changing selection)
        const updatedLevels = newLevels.slice(0, levelIndex + 1)

        try {
            // Fetch children of selected category using /flat API with parentId
            const childrenData = await BrandCategoryApi.getCategories({
                parentId: categoryId,
                size: 100
            })
            const children = childrenData.content || []

            // Add children to categories state for future reference
            setCategories(prev => {
                const newCats = [...prev]
                children.forEach(child => {
                    if (!newCats.find(c => c.id === child.id)) {
                        newCats.push(child)
                    }
                })
                return newCats
            })

            if (children.length > 0) {
                // Has children - add next level and DON'T set categoryIds yet
                updatedLevels.push({
                    level: levelIndex + 1,
                    parentId: categoryId,
                    selectedId: '',
                    options: children
                })
                // Clear categoryIds - user must select child
                setFormData(prev => ({ ...prev, categoryIds: [] }))
            } else {
                // Leaf node - collect all selected category IDs from root to leaf
                const allSelectedIds: string[] = []
                for (const level of updatedLevels) {
                    if (level.selectedId) {
                        allSelectedIds.push(level.selectedId)
                    }
                }
                setFormData(prev => ({ ...prev, categoryIds: allSelectedIds }))
            }

            setCategoryLevels(updatedLevels)
        } catch (error) {
            console.error('Error loading child categories:', error)
            alert('Lỗi khi tải danh mục con')
        }
    }

    // Get the path of selected categories for display
    const getSelectedCategoryPath = (): string => {
        const path: string[] = []
        for (const level of categoryLevels) {
            if (level.selectedId) {
                const cat = categories.find(c => c.id === level.selectedId)
                if (cat) path.push(cat.name)
            }
        }
        return path.join(' > ')
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

    const handlePrimaryImageUploadComplete = (urls: string[]) => {
        // Only take the first image
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
        // Only take the first image
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
                    // Primary image first
                    ...(primaryImage ? [{
                        imageUrl: primaryImage,
                        position: 0,
                        isDefault: true
                    }] : []),
                    // Gallery images
                    ...galleryImages.map((url, index) => ({
                        imageUrl: url,
                        position: (primaryImage ? 1 : 0) + index,
                        isDefault: false
                    })),
                    // Legacy: Main product images (no colorId)
                    ...images.map((url, index) => ({
                        imageUrl: url,
                        position: (primaryImage ? 1 : 0) + galleryImages.length + index,
                        isDefault: index === 0 && !primaryImage
                    })),
                    // Color-specific images
                    ...Object.entries(colorImages).flatMap(([colorId, urls]) =>
                        urls.map((url, index) => ({
                            imageUrl: url,
                            position: (primaryImage ? 1 : 0) + galleryImages.length + images.length + index,
                            colorId: colorId
                        }))
                    )
                ]
            }

            const product = await ProductsApi.createProduct(productData)
            setCreatedProduct(product)
            // Update slug in form data in case backend modified it (e.g. for uniqueness)
            setFormData(prev => ({ ...prev, slug: product.slug }))
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
                    // Primary image first
                    ...(primaryImage ? [{
                        imageUrl: primaryImage,
                        position: 0,
                        isDefault: true
                    }] : []),
                    // Gallery images
                    ...galleryImages.map((url, index) => ({
                        imageUrl: url,
                        position: (primaryImage ? 1 : 0) + index,
                        isDefault: false
                    })),
                    // Legacy: Main product images (no colorId)
                    ...images.map((url, index) => ({
                        imageUrl: url,
                        position: (primaryImage ? 1 : 0) + galleryImages.length + index,
                        isDefault: index === 0 && !primaryImage
                    })),
                    // Color-specific images
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

            // Show success notification
            setPublishSuccess(true)

            // Scroll to top to show notification
            window.scrollTo({ top: 0, behavior: 'smooth' })
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

                // Auto-generate barcode: 893 + timestamp (last 9 digits) + random (1 digit)
                const barcode = `893${Date.now().toString().slice(-9)}${Math.floor(Math.random() * 10)}`

                newVariants.push({
                    id: `temp-${Date.now()}-${sizeId}-${colorId}`, // temporary ID
                    sku,
                    barcode,
                    sizeId,
                    colorId,
                    priceAmount: parseInt(formData.priceAmount),
                    historyCost: parseInt(defaultHistoryCost) || 0,
                    weightGrams: parseInt(defaultWeight) || 200,
                    status: 'ACTIVE',
                    inventory: {
                        quantityOnHand: parseInt(defaultInventory) || 0,
                        reorderLevel: parseInt(defaultReorderLevel) || 5
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

        // Validate: Check for duplicate SKUs within unsaved variants
        const skuMap = new Map<string, number>()
        for (let i = 0; i < unsavedVariants.length; i++) {
            const sku = unsavedVariants[i].sku
            if (skuMap.has(sku)) {
                alert(`SKU trùng lặp: "${sku}" (biến thể #${skuMap.get(sku)! + 1} và #${i + 1})`)
                return
            }
            skuMap.set(sku, i)
        }

        // Validate: Check for duplicate size-color combinations
        const sizeColorMap = new Map<string, number>()
        for (let i = 0; i < unsavedVariants.length; i++) {
            const variant = unsavedVariants[i]
            const key = `${variant.sizeId}-${variant.colorId}`
            if (sizeColorMap.has(key)) {
                const size = sizes.find(s => s.id === variant.sizeId)
                const color = colors.find(c => c.id === variant.colorId)
                alert(`Cặp Size-Color trùng lặp: "${size?.name} - ${color?.name}" (biến thể #${sizeColorMap.get(key)! + 1} và #${i + 1})`)
                return
            }
            sizeColorMap.set(key, i)
        }

        // Validate: Check against existing saved variants for duplicate SKUs
        for (const unsavedVariant of unsavedVariants) {
            const existingVariant = variants.find(v => v.sku === unsavedVariant.sku)
            if (existingVariant) {
                alert(`SKU "${unsavedVariant.sku}" đã tồn tại trong các biến thể đã lưu`)
                return
            }
        }

        // Validate: Check against existing saved variants for duplicate size-color
        for (const unsavedVariant of unsavedVariants) {
            const existingVariant = variants.find(v =>
                v.sizeId === unsavedVariant.sizeId && v.colorId === unsavedVariant.colorId
            )
            if (existingVariant) {
                const size = sizes.find(s => s.id === unsavedVariant.sizeId)
                const color = colors.find(c => c.id === unsavedVariant.colorId)
                alert(`Cặp Size-Color "${size?.name} - ${color?.name}" đã tồn tại trong các biến thể đã lưu`)
                return
            }
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
                    historyCost: unsavedVariant.historyCost,
                    weightGrams: unsavedVariant.weightGrams,
                    status: unsavedVariant.status,
                    inventory: {
                        quantityOnHand: unsavedVariant.inventory?.quantityOnHand || 0,
                        quantityReserved: 0,
                        reorderLevel: unsavedVariant.inventory?.reorderLevel || 5
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
            historyCost: variant.historyCost?.toString() || '',
            weightGrams: variant.weightGrams?.toString() || '',
            status: variant.status || 'ACTIVE',
            quantityOnHand: variant.inventory?.quantityOnHand?.toString() || '0',
            reorderLevel: variant.inventory?.reorderLevel?.toString() || '5',
        })

        // Show inline form
        setEditingVariantId(tempId)
    }

    const handleCreateNewProduct = () => {
        // Confirm if there's unsaved work
        if (createdProduct && unsavedVariants.length > 0) {
            const confirmReset = confirm(
                `Bạn có ${unsavedVariants.length} biến thể chưa lưu. ` +
                `Bạn có chắc muốn tạo sản phẩm mới không? Dữ liệu hiện tại sẽ bị xóa.`
            )
            if (!confirmReset) return
        }

        // Reset all states
        setFormData({
            name: '',
            slug: '',
            priceAmount: '',
            brandId: '',
            categoryIds: [],
            description: '',
            material: '',
            gender: '',
            seoTitle: '',
            seoDescription: '',
        })
        setSlugLocked(false)

        // Reset category levels to root only
        const roots = categories.filter(cat => !cat.parentId || cat.level === 0)
        setCategoryLevels([{
            level: 0,
            parentId: null,
            selectedId: '',
            options: roots
        }])

        setPrimaryImage('')
        setGalleryImages([])
        setImages([])
        setColorImages({})
        setCreatedProduct(null)
        setVariants([])
        setUnsavedVariants([])
        setSelectedSizes([])
        setSelectedColors([])
        setEditingVariantId(null)
        setPublishSuccess(false)

        // Scroll to top
        window.scrollTo({ top: 0, behavior: 'smooth' })
    }

    const handleSaveUnsavedEdit = (tempId: string) => {
        // Validate SKU
        const newSku = editFormData.sku.trim()
        if (!newSku) {
            alert('SKU không được để trống')
            return
        }

        // Check for duplicate SKU in unsaved variants (excluding current one)
        const duplicateUnsaved = unsavedVariants.find(v => v.id !== tempId && v.sku === newSku)
        if (duplicateUnsaved) {
            alert(`SKU "${newSku}" đã tồn tại trong biến thể chưa lưu khác`)
            return
        }

        // Check for duplicate SKU in saved variants
        const duplicateSaved = variants.find(v => v.sku === newSku)
        if (duplicateSaved) {
            alert(`SKU "${newSku}" đã tồn tại trong biến thể đã lưu`)
            return
        }

        // Update the unsaved variant in local state
        setUnsavedVariants(prev => prev.map(v => {
            if (v.id === tempId) {
                return {
                    ...v,
                    sku: editFormData.sku,
                    barcode: editFormData.barcode || undefined,
                    priceAmount: parseFloat(editFormData.priceAmount),
                    compareAtAmount: editFormData.compareAtAmount ? parseFloat(editFormData.compareAtAmount) : undefined,
                    historyCost: editFormData.historyCost ? parseFloat(editFormData.historyCost) : undefined,
                    weightGrams: editFormData.weightGrams ? parseFloat(editFormData.weightGrams) : undefined,
                    status: editFormData.status,
                    inventory: {
                        quantityOnHand: parseInt(editFormData.quantityOnHand),
                        reorderLevel: parseInt(editFormData.reorderLevel),
                    },
                }
            }
            return v
        }))

        // Close edit form
        setEditingVariantId(null)
    }

    const handleCancelEdit = () => {
        setEditingVariantId(null)
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

        // Validate SKU
        const newSku = editFormData.sku.trim()
        if (!newSku) {
            alert('SKU không được để trống')
            return
        }

        // Check for duplicate SKU in saved variants (excluding current one)
        const duplicateSaved = variants.find(v => v.id !== variantId && v.sku === newSku)
        if (duplicateSaved) {
            alert(`SKU "${newSku}" đã tồn tại trong biến thể đã lưu khác`)
            return
        }

        // Check for duplicate SKU in unsaved variants
        const duplicateUnsaved = unsavedVariants.find(v => v.sku === newSku)
        if (duplicateUnsaved) {
            alert(`SKU "${newSku}" đã tồn tại trong biến thể chưa lưu`)
            return
        }

        setEditLoading(true)
        try {
            const updateData: ProductsApi.VariantUpdateRequest = {
                sku: editFormData.sku,
                barcode: editFormData.barcode,
                priceAmount: parseFloat(editFormData.priceAmount),
                compareAtAmount: editFormData.compareAtAmount ? parseFloat(editFormData.compareAtAmount) : undefined,
                historyCost: editFormData.historyCost ? parseFloat(editFormData.historyCost) : undefined,
                weightGrams: editFormData.weightGrams ? parseFloat(editFormData.weightGrams) : undefined,
                status: editFormData.status,
                version: variant.version || 0,
                inventory: {
                    quantityOnHand: parseInt(editFormData.quantityOnHand),
                    reorderLevel: parseInt(editFormData.reorderLevel),
                },
            }

            await ProductsApi.updateVariant(createdProduct.id, variantId, updateData)

            // Reload variants
            const updatedVariants = await ProductsApi.getProductVariants(createdProduct.id)
            setVariants(updatedVariants)

            // Close edit form
            setEditingVariantId(null)
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
            historyCost: variant.historyCost?.toString() || '',
            weightGrams: variant.weightGrams?.toString() || '',
            status: variant.status || 'ACTIVE',
            quantityOnHand: variant.inventory?.quantityOnHand?.toString() || '0',
            reorderLevel: variant.inventory?.reorderLevel?.toString() || '5',
        })

        // Show inline form
        setEditingVariantId(variantId)
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
            {/* Success Notification Bar */}
            {publishSuccess && createdProduct && (
                <div className="bg-green-50 border-l-4 border-green-500 p-4 shadow-md flex items-center justify-between">
                    <div className="flex items-center gap-3">
                        <div className="flex-shrink-0">
                            <svg className="h-5 w-5 text-green-500" viewBox="0 0 20 20" fill="currentColor">
                                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                            </svg>
                        </div>
                        <div>
                            <p className="text-sm font-medium text-green-800">
                                Product published.{' '}
                                <a
                                    href={`/products/${createdProduct.slug}`}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                    className="underline hover:text-green-900"
                                >
                                    View Product
                                </a>
                            </p>
                        </div>
                    </div>
                    <button
                        type="button"
                        onClick={() => setPublishSuccess(false)}
                        className="flex-shrink-0 ml-4 inline-flex text-green-500 hover:text-green-700"
                    >
                        <span className="sr-only">Close</span>
                        <svg className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                            <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" />
                        </svg>
                    </button>
                </div>
            )}

            {/* Header */}
            <div className="flex items-center justify-between gap-4">
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

                {createdProduct && (
                    <button
                        type="button"
                        onClick={handleCreateNewProduct}
                        className="flex items-center gap-2 bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700"
                    >
                        <Plus className="w-4 h-4" />
                        Thêm sản phẩm mới
                    </button>
                )}
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
                            <div className="flex items-center justify-between mb-1">
                                <label className="block text-sm font-medium">Danh mục</label>
                                {/* Reset button - only show when a category is selected */}
                                {(categoryLevels.length > 1 || categoryLevels[0]?.selectedId) && (
                                    <button
                                        type="button"
                                        onClick={() => {
                                            // Reset to initial state - only root categories
                                            const roots = categories.filter(cat => !cat.parentId || cat.level === 0)
                                            setCategoryLevels([{
                                                level: 0,
                                                parentId: null,
                                                selectedId: '',
                                                options: roots
                                            }])
                                            setFormData(prev => ({ ...prev, categoryIds: [] }))
                                        }}
                                        className="text-xs text-red-600 hover:text-red-700 underline"
                                    >
                                        Đổi danh mục
                                    </button>
                                )}
                            </div>

                            {/* Dynamic Category Levels */}
                            <div className="space-y-2">
                                {categoryLevels.map((level, index) => (
                                    <div key={`category-level-${index}`}>
                                        <label className="block text-xs text-gray-600 mb-1">
                                            {index === 0 ? 'Danh mục chính' : `Danh mục con cấp ${index}`}
                                        </label>
                                        <select
                                            value={level.selectedId}
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
                            </div>

                            {/* Show selected category path (only for leaf nodes) */}
                            {formData.categoryIds.length > 0 && (
                                <p className="text-xs text-green-600 mt-2">
                                    ✓ Đã chọn: {getSelectedCategoryPath()}
                                </p>
                            )}
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

                                    <div className="grid grid-cols-2 gap-4 p-4 bg-gray-50 rounded border border-gray-200">
                                        <div className="col-span-2 text-sm font-medium text-gray-700 mb-2">
                                            Thiết lập mặc định cho các biến thể
                                        </div>
                                        <div>
                                            <label className="block text-xs text-gray-600 mb-1">Giá gốc (VNĐ)</label>
                                            <input
                                                type="number"
                                                value={defaultHistoryCost}
                                                onChange={(e) => setDefaultHistoryCost(e.target.value)}
                                                className="w-full border rounded px-3 py-2 text-sm"
                                                placeholder="0"
                                                min="0"
                                            />
                                        </div>
                                        <div>
                                            <label className="block text-xs text-gray-600 mb-1">Khối lượng (gram)</label>
                                            <input
                                                type="number"
                                                value={defaultWeight}
                                                onChange={(e) => setDefaultWeight(e.target.value)}
                                                className="w-full border rounded px-3 py-2 text-sm"
                                                placeholder="200"
                                                min="0"
                                            />
                                        </div>
                                        <div>
                                            <label className="block text-xs text-gray-600 mb-1">Tồn kho ban đầu</label>
                                            <input
                                                type="number"
                                                value={defaultInventory}
                                                onChange={(e) => setDefaultInventory(e.target.value)}
                                                className="w-full border rounded px-3 py-2 text-sm"
                                                placeholder="10"
                                                min="0"
                                            />
                                        </div>
                                        <div>
                                            <label className="block text-xs text-gray-600 mb-1">Mức đặt hàng lại</label>
                                            <input
                                                type="number"
                                                value={defaultReorderLevel}
                                                onChange={(e) => setDefaultReorderLevel(e.target.value)}
                                                className="w-full border rounded px-3 py-2 text-sm"
                                                placeholder="5"
                                                min="0"
                                            />
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
                                onSaveVariants={handleSaveVariants}
                                onEditVariant={handleEditVariant}
                                onDeleteVariant={handleDeleteVariant}
                                onEditUnsavedVariant={handleEditUnsavedVariant}
                                onDeleteUnsavedVariant={handleDeleteUnsavedVariant}
                                onSaveEdit={handleSaveEdit}
                                onSaveUnsavedEdit={handleSaveUnsavedEdit}
                                onCancelEdit={handleCancelEdit}
                                onColorImageUpload={handleColorImageUploadComplete}
                                onRemoveColorImage={removeColorImage}
                                onEditFormDataChange={setEditFormData}
                            />

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

                        {createdProduct && (
                            <div className="bg-blue-50 border border-blue-200 rounded p-3">
                                <p className="text-xs text-blue-700">
                                    💡 Nhớ nhấn nút "Lưu sản phẩm" sau khi thêm/xóa ảnh để cập nhật thay đổi
                                </p>
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

                        {createdProduct && (
                            <div className="bg-blue-50 border border-blue-200 rounded p-3">
                                <p className="text-xs text-blue-700">
                                    💡 Nhớ nhấn nút "Lưu sản phẩm" sau khi thêm/xóa ảnh để cập nhật thay đổi
                                </p>
                            </div>
                        )}
                    </div>

                    {/* Legacy Images (kept for backwards compatibility) */}
                    {images.length > 0 && (
                        <div className="bg-white shadow rounded-lg p-6 space-y-4">
                            <h2 className="text-lg font-semibold">Ảnh cũ (Legacy)</h2>

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
                        </div>
                    )}
                </div>
            </div>
        </div>
    )
}

