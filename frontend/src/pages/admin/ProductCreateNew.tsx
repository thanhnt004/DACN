import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Upload, X, Plus, Image as ImageIcon, Edit2, Save, Package, Send } from 'lucide-react';
import { toast } from 'react-toastify';
import { createProduct, createVariantBulk, changeProductStatus, updateProduct, updateVariant as updateVariantApi } from '../../api/admin/products';
import { getBrands, getCategories, type CategoryResponse } from '../../api/admin/brandCategory';
import { getSizes, getColors } from '../../api/admin/catalog';
import { uploadImagesToCloudinary } from '../../api/media';
import type { 
    ProductCreateRequest, 
    ProductImagePayload,
    Gender
} from '../../api/admin/products';
import type { BrandDto } from '../../api/admin/brandCategory';
import type { SizeDto, ColorDto } from '../../api/admin/catalog';

interface VariantRow {
    tempId: string;
    variantId?: string; // ID of existing variant if already created
    version?: number; // Version for optimistic locking
    colorId: string;
    colorName: string;
    sizeId: string;
    sizeName: string;
    sku: string;
    barcode: string;
    priceAmount: string;
    compareAtAmount: string;
    historyCost: string;
    weightGrams: string;
    quantityOnHand: string;
    reorderLevel: string;
    imageFile?: File;
    imagePreview?: string;
}

interface BulkApplyValues {
    priceAmount: string;
    compareAtAmount: string;
    quantityOnHand: string;
    reorderLevel: string;
    historyCost: string;
}

const ProductCreateNew: React.FC = () => {
    const navigate = useNavigate();
    
    // Product state
    const [productId, setProductId] = useState<string | null>(null);
    const [productVersion, setProductVersion] = useState<number>(0);
    const [name, setName] = useState('');
    const [slug, setSlug] = useState('');
    const [description, setDescription] = useState('');
    const [material, setMaterial] = useState('');
    const [gender, setGender] = useState<Gender>('unisex');
    const [brandId, setBrandId] = useState('');
    const [categoryIds, setCategoryIds] = useState<string[]>([]);
    const [seoTitle, setSeoTitle] = useState('');
    const [seoDescription, setSeoDescription] = useState('');
    
    // Images
    const [primaryImage, setPrimaryImage] = useState<{ file: File; preview: string } | null>(null);
    const [galleryImages, setGalleryImages] = useState<Array<{ file: File; preview: string }>>([]);
    const [isDraggingPrimary, setIsDraggingPrimary] = useState(false);
    const [isDraggingGallery, setIsDraggingGallery] = useState(false);
    
    // Variants
    const [selectedColors, setSelectedColors] = useState<string[]>([]);
    const [selectedSizes, setSelectedSizes] = useState<string[]>([]);
    const [variants, setVariants] = useState<VariantRow[]>([]);
    const [editingVariantId, setEditingVariantId] = useState<string | null>(null);
    
    // Bulk apply
    const [bulkValues, setBulkValues] = useState<BulkApplyValues>({
        priceAmount: '',
        compareAtAmount: '',
        quantityOnHand: '',
        reorderLevel: '10',
        historyCost: ''
    });
    
    // Reference data
    const [brands, setBrands] = useState<BrandDto[]>([]);
    const [categories, setCategories] = useState<CategoryResponse[]>([]);
    const [sizes, setSizesData] = useState<SizeDto[]>([]);
    const [colors, setColorsData] = useState<ColorDto[]>([]);
    
    // Category tree
    const [expandedCategories, setExpandedCategories] = useState<Set<string>>(new Set());
    const [showCategoryModal, setShowCategoryModal] = useState(false);
    
    const [loading, setLoading] = useState(false);
    const [errors, setErrors] = useState<Record<string, string>>({});

    // Load reference data
    useEffect(() => {
        const loadData = async () => {
            try {
                const [brandsRes, sizesRes, colorsRes] = await Promise.all([
                    getBrands({ size: 1000 }),
                    getSizes(),
                    getColors()
                ]);
                
                // Get all categories using flat API (more reliable)
                const categoriesRes = await getCategories({ size: 10000, all: true });
                const allCategories = categoriesRes.content || [];
                
                setBrands(brandsRes.content || []);
                setCategories(allCategories);
                setSizesData(sizesRes);
                setColorsData(colorsRes);
                
                console.log('Loaded categories:', allCategories.length, allCategories);
            } catch (err) {
                console.error('Failed to load reference data:', err);
                toast.error('Không thể tải dữ liệu danh mục');
            }
        };
        loadData();
    }, []);

    // Auto-generate slug
    useEffect(() => {
        if (name && !productId) {
            const generatedSlug = name
                .toLowerCase()
                .normalize('NFD')
                .replace(/[\u0300-\u036f]/g, '')
                .replace(/đ/g, 'd')
                .replace(/[^a-z0-9]+/g, '-')
                .replace(/^-+|-+$/g, '');
            setSlug(generatedSlug);
        }
    }, [name, productId]);

    // Auto-generate SEO title
    useEffect(() => {
        if (name && !productId) {
            setSeoTitle(name);
        }
    }, [name, productId]);

    // Generate variants when colors/sizes change
    useEffect(() => {
        if (productId && selectedColors.length > 0 && selectedSizes.length > 0) {
            const newVariants: VariantRow[] = [];
            selectedColors.forEach(colorId => {
                const color = colors.find(c => c.id === colorId);
                selectedSizes.forEach(sizeId => {
                    const size = sizes.find(s => s.id === sizeId);
                    if (color && size) {
                        const existing = variants.find(v => v.colorId === colorId && v.sizeId === sizeId);
                        if (existing) {
                            newVariants.push(existing);
                        } else {
                            // Auto-generate SKU: PRODUCTNAME-SIZE-COLOR
                            const productPrefix = slug.toUpperCase().substring(0, 10).replace(/-/g, '');
                            const sku = `${productPrefix}-${size.code || size.name}-${color.name}`
                                .toUpperCase()
                                .replace(/\s/g, '-')
                                .replace(/[^A-Z0-9-]/g, '');
                            
                            newVariants.push({
                                tempId: `${colorId}-${sizeId}`,
                                colorId,
                                colorName: color.name || '',
                                sizeId,
                                sizeName: size.name || '',
                                sku,
                                barcode: '',
                                priceAmount: '',
                                compareAtAmount: '',
                                historyCost: '',
                                weightGrams: '200',
                                quantityOnHand: '',
                                reorderLevel: '10',
                            });
                        }
                    }
                });
            });
            setVariants(newVariants);
        } else {
            setVariants([]);
        }
    }, [productId, selectedColors, selectedSizes, colors, sizes, slug]);

    // Primary Image handlers
    const handlePrimaryImageSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (file && file.type.startsWith('image/')) {
            const preview = URL.createObjectURL(file);
            setPrimaryImage({ file, preview });
        }
    };

    const handlePrimaryImageDrop = (e: React.DragEvent) => {
        e.preventDefault();
        setIsDraggingPrimary(false);
        const file = e.dataTransfer.files[0];
        if (file && file.type.startsWith('image/')) {
            const preview = URL.createObjectURL(file);
            setPrimaryImage({ file, preview });
        }
    };

    const removePrimaryImage = () => {
        if (primaryImage) {
            URL.revokeObjectURL(primaryImage.preview);
            setPrimaryImage(null);
        }
    };

    // Gallery Images handlers
    const handleGalleryImageSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
        const files = Array.from(e.target.files || []);
        files.forEach(file => {
            if (file.type.startsWith('image/')) {
                const preview = URL.createObjectURL(file);
                setGalleryImages(prev => [...prev, { file, preview }]);
            }
        });
    };

    const handleGalleryImageDrop = (e: React.DragEvent) => {
        e.preventDefault();
        setIsDraggingGallery(false);
        const files = Array.from(e.dataTransfer.files);
        files.forEach(file => {
            if (file.type.startsWith('image/')) {
                const preview = URL.createObjectURL(file);
                setGalleryImages(prev => [...prev, { file, preview }]);
            }
        });
    };

    const removeGalleryImage = (index: number) => {
        setGalleryImages(prev => {
            const newImages = [...prev];
            URL.revokeObjectURL(newImages[index].preview);
            newImages.splice(index, 1);
            return newImages;
        });
    };

    // Variant image handlers
    const handleVariantImageSelect = (tempId: string, e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (file && file.type.startsWith('image/')) {
            const preview = URL.createObjectURL(file);
            setVariants(prev => prev.map(v => 
                v.tempId === tempId ? { ...v, imageFile: file, imagePreview: preview } : v
            ));
        }
    };

    const removeVariantImage = (tempId: string) => {
        setVariants(prev => prev.map(v => {
            if (v.tempId === tempId && v.imagePreview) {
                URL.revokeObjectURL(v.imagePreview);
                return { ...v, imageFile: undefined, imagePreview: undefined };
            }
            return v;
        }));
    };

    // Variant update handlers
    const updateVariantField = (tempId: string, field: keyof VariantRow, value: string) => {
        setVariants(prev => prev.map(v => v.tempId === tempId ? { ...v, [field]: value } : v));
    };

    // Bulk apply handler
    const handleBulkApply = () => {
        setVariants(prev => prev.map(v => ({
            ...v,
            priceAmount: bulkValues.priceAmount || v.priceAmount,
            compareAtAmount: bulkValues.compareAtAmount || v.compareAtAmount,
            quantityOnHand: bulkValues.quantityOnHand || v.quantityOnHand,
            reorderLevel: bulkValues.reorderLevel || v.reorderLevel,
            historyCost: bulkValues.historyCost || v.historyCost,
        })));
        toast.success('Đã áp dụng giá trị cho tất cả biến thể');
    };

    // Category tree helpers
    const getCategoryTree = () => {
        return categories.filter(c => !c.parentId || c.level === 0);
    };

    const getChildCategories = (parentId: string) => {
        return categories.filter(c => c.parentId === parentId);
    };

    const hasChildren = (categoryId: string) => {
        return categories.some(c => c.parentId === categoryId);
    };

    const toggleCategoryExpanded = (categoryId: string) => {
        setExpandedCategories(prev => {
            const next = new Set(prev);
            if (next.has(categoryId)) {
                next.delete(categoryId);
            } else {
                next.add(categoryId);
            }
            return next;
        });
    };

    const expandAllCategories = () => {
        const allCategoryIds = categories.map(c => c.id);
        setExpandedCategories(new Set(allCategoryIds));
    };

    const collapseAllCategories = () => {
        setExpandedCategories(new Set());
    };

    const toggleAllCategories = () => {
        if (expandedCategories.size > 0) {
            collapseAllCategories();
        } else {
            expandAllCategories();
        }
    };

    const toggleCategory = (categoryId: string) => {
        setCategoryIds(prev => 
            prev.includes(categoryId) 
                ? prev.filter(id => id !== categoryId)
                : [...prev, categoryId]
        );
    };

    // Render all categories in columns (flat list)
    const renderCategoryGrid = (): React.ReactElement => {
        const allCategories = categories; // All categories flat
        const isSelected = (categoryId: string) => categoryIds.includes(categoryId);

        return (
            <div className="grid grid-cols-3 gap-3">
                {allCategories.map(category => (
                    <label 
                        key={category.id}
                        className="flex items-center gap-2 p-3 border rounded-lg hover:bg-gray-50 cursor-pointer transition-colors"
                    >
                        <input
                            type="checkbox"
                            checked={isSelected(category.id)}
                            onChange={() => toggleCategory(category.id)}
                            className="w-4 h-4 text-red-600 border-gray-300 rounded focus:ring-red-500"
                        />
                        <span className={`text-sm flex-1 ${
                            isSelected(category.id) ? 'font-medium text-red-700' : 'text-gray-700'
                        }`}>
                            {category.name}
                        </span>
                    </label>
                ))}
            </div>
        );
    };

    // Save Draft (Create or Update Product)
    const handleSaveDraft = async () => {
        // Clear previous errors
        setErrors({});
        const newErrors: Record<string, string> = {};

        // Validate required fields
        if (!name.trim()) {
            newErrors.name = 'Tên sản phẩm là bắt buộc';
        }
        if (!slug.trim()) {
            newErrors.slug = 'Slug là bắt buộc';
        }
        if (!brandId) {
            newErrors.brand = 'Vui lòng chọn thương hiệu';
        }
        if (!primaryImage && !productId) {
            newErrors.primaryImage = 'Ảnh chính là bắt buộc';
        }

        if (Object.keys(newErrors).length > 0) {
            setErrors(newErrors);
            toast.error('Vui lòng điền đầy đủ thông tin bắt buộc');
            return;
        }

        try {
            setLoading(true);

            // Upload images if there are new ones
            let imagePayloads: ProductImagePayload[] = [];
            
            if (primaryImage || galleryImages.length > 0) {
                try {
                    const allFiles = [];
                    if (primaryImage) allFiles.push(primaryImage.file);
                    allFiles.push(...galleryImages.map(img => img.file));

                    toast.info('Đang tải ảnh lên...');
                    const uploadedUrls = await uploadImagesToCloudinary(allFiles, 'product', productId || undefined);
                    
                    imagePayloads = uploadedUrls.map((url, index) => ({
                        imageUrl: url,
                        position: index,
                        isDefault: index === 0, // First image is primary
                        alt: name
                    }));
                } catch (uploadError) {
                    console.error('Failed to upload images:', uploadError);
                    toast.error('Không thể tải ảnh lên. Vui lòng thử lại.');
                    throw uploadError;
                }
            }

            if (productId) {
                // Update existing product
                const request = {
                    id: productId,
                    name: name.trim(),
                    slug: slug.trim(),
                    brandId: brandId || undefined,
                    categoryId: categoryIds.length > 0 ? categoryIds : undefined,
                    description: description.trim() || undefined,
                    material: material.trim() || undefined,
                    gender,
                    seoTitle: seoTitle.trim() || undefined,
                    seoDescription: seoDescription.trim() || undefined,
                    images: imagePayloads.length > 0 ? imagePayloads : undefined,
                    version: productVersion
                };

                const response = await updateProduct(productId, request);
                setProductVersion(response.version || 0);
                toast.success('Đã cập nhật thông tin sản phẩm thành công!');
            } else {
                // Create new product
                const request: ProductCreateRequest = {
                    name: name.trim(),
                    slug: slug.trim(),
                    priceAmount: 0, // Will be set in variants
                    brandId: brandId || undefined,
                    categoryId: categoryIds.length > 0 ? categoryIds : undefined,
                    description: description.trim() || undefined,
                    material: material.trim() || undefined,
                    gender,
                    seoTitle: seoTitle.trim() || undefined,
                    seoDescription: seoDescription.trim() || undefined,
                    images: imagePayloads.length > 0 ? imagePayloads : undefined
                };

                const response = await createProduct(request);
                setProductId(response.id);
                setProductVersion(response.version || 0);
                toast.success('Đã lưu bản nháp sản phẩm. Bạn có thể tạo biến thể ngay bây giờ.');
            }
        } catch (err) {
            console.error('Failed to save product:', err);
            const errorMessage = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
            toast.error(errorMessage || 'Không thể lưu sản phẩm');
        } finally {
            setLoading(false);
        }
    };

    // Publish Product
    const handlePublishProduct = async () => {
        if (!productId) {
            toast.error('Không tìm thấy sản phẩm');
            return;
        }

        const confirmed = window.confirm('Bạn có chắc chắn muốn xuất bản sản phẩm này? Sản phẩm sẽ hiển thị trên cửa hàng.');
        if (!confirmed) return;

        try {
            setLoading(true);
            await changeProductStatus(productId, 'ACTIVE');
            toast.success('Đã xuất bản sản phẩm thành công!');
            navigate('/admin/products');
        } catch (err) {
            console.error('Failed to publish product:', err);
            const errorMessage = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
            toast.error(errorMessage || 'Không thể xuất bản sản phẩm');
        } finally {
            setLoading(false);
        }
    };

    const handleSaveVariants = async () => {
        if (!productId) {
            toast.error('Vui lòng lưu sản phẩm trước khi tạo biến thể');
            return;
        }

        if (variants.length === 0) {
            toast.error('Vui lòng chọn màu sắc và kích thước');
            return;
        }

        // Validate variants
        for (const v of variants) {
            if (!v.sku || !v.sku.trim()) {
                toast.error(`Biến thể ${v.sizeName}-${v.colorName} chưa có SKU`);
                return;
            }
            if (!v.priceAmount || parseFloat(v.priceAmount) <= 0) {
                toast.error(`Biến thể ${v.sizeName}-${v.colorName} chưa có giá bán hợp lệ`);
                return;
            }
            if (v.priceAmount && parseFloat(v.priceAmount) > 1000000000) {
                toast.error(`Biến thể ${v.sizeName}-${v.colorName} có giá bán quá cao`);
                return;
            }
            if (!v.quantityOnHand || parseInt(v.quantityOnHand) < 0) {
                toast.error(`Biến thể ${v.sizeName}-${v.colorName} chưa có số lượng hợp lệ`);
                return;
            }
            if (v.compareAtAmount && parseFloat(v.compareAtAmount) < parseFloat(v.priceAmount)) {
                toast.error(`Biến thể ${v.sizeName}-${v.colorName}: Giá so sánh phải lớn hơn giá bán`);
                return;
            }
        }

        try {
            setLoading(true);
            
            // Separate new variants and existing variants
            const newVariants = variants.filter(v => !v.variantId);
            const existingVariants = variants.filter(v => v.variantId);
            
            // Upload variant images if any
            const variantsWithImages = variants.filter(v => v.imageFile);
            const variantImageUrls: Record<string, string> = {};
            
            for (const v of variantsWithImages) {
                if (v.imageFile) {
                    try {
                        const [url] = await uploadImagesToCloudinary([v.imageFile], 'product', productId);
                        variantImageUrls[v.tempId] = url;
                    } catch (uploadError) {
                        console.error(`Failed to upload image for variant ${v.sku}:`, uploadError);
                        toast.warning(`Không thể tải ảnh cho biến thể ${v.sizeName}-${v.colorName}`);
                    }
                }
            }

            // Create new variants
            if (newVariants.length > 0) {
                const variantRequests = newVariants.map(v => ({
                    sku: v.sku,
                    barcode: v.barcode || undefined,
                    sizeId: v.sizeId,
                    colorId: v.colorId,
                    priceAmount: Math.round(parseFloat(v.priceAmount) * 100),
                    compareAtAmount: v.compareAtAmount ? Math.round(parseFloat(v.compareAtAmount) * 100) : undefined,
                    historyCost: v.historyCost ? Math.round(parseFloat(v.historyCost) * 100) : undefined,
                    weightGrams: parseInt(v.weightGrams) || 200,
                    status: 'ACTIVE' as const,
                    inventory: {
                        quantityOnHand: parseInt(v.quantityOnHand),
                        quantityReserved: 0,
                        reorderLevel: parseInt(v.reorderLevel) || 10
                    },
                    image: variantImageUrls[v.tempId] ? {
                        imageUrl: variantImageUrls[v.tempId],
                        isDefault: false
                    } : undefined
                }));

                await createVariantBulk(productId, variantRequests);
            }
            
            // Update existing variants
            if (existingVariants.length > 0) {
                
                for (const v of existingVariants) {
                    if (!v.variantId || v.version === undefined) continue;
                    
                    const updateRequest = {
                        sku: v.sku,
                        barcode: v.barcode || undefined,
                        sizeId: v.sizeId,
                        colorId: v.colorId,
                        priceAmount: Math.round(parseFloat(v.priceAmount) * 100),
                        compareAtAmount: v.compareAtAmount ? Math.round(parseFloat(v.compareAtAmount) * 100) : undefined,
                        historyCost: v.historyCost ? Math.round(parseFloat(v.historyCost) * 100) : undefined,
                        weightGrams: parseInt(v.weightGrams) || 200,
                        status: 'ACTIVE' as const,
                        version: v.version,
                        inventory: {
                            quantityOnHand: parseInt(v.quantityOnHand),
                            quantityReserved: 0,
                            reorderLevel: parseInt(v.reorderLevel) || 10
                        },
                        image: variantImageUrls[v.tempId] ? {
                            imageUrl: variantImageUrls[v.tempId],
                            isDefault: false
                        } : undefined
                    };
                    
                    await updateVariantApi(productId, v.variantId, updateRequest);
                }
            }
            
            // After saving, reload product details to get updated variant info
            const { getProductDetail } = await import('../../api/admin/products');
            const updatedProduct = await getProductDetail(productId, ['variants']);
            
            // Update variants state with the saved variants (now they have IDs and versions)
            if (updatedProduct.variants && updatedProduct.variants.length > 0) {
                const updatedVariantRows: VariantRow[] = updatedProduct.variants.map(v => {
                    const colorInfo = colors.find(c => c.id === v.colorId);
                    const sizeInfo = sizes.find(s => s.id === v.sizeId);
                    
                    return {
                        tempId: `${v.sizeId}-${v.colorId}`,
                        variantId: v.id,
                        version: v.version,
                        colorId: v.colorId || '',
                        colorName: colorInfo?.name || v.color?.name || '',
                        sizeId: v.sizeId || '',
                        sizeName: sizeInfo?.code || v.size?.code || '',
                        sku: v.sku,
                        barcode: v.barcode || '',
                        priceAmount: (v.priceAmount / 100).toString(),
                        compareAtAmount: v.compareAtAmount ? (v.compareAtAmount / 100).toString() : '',
                        historyCost: v.historyCost ? (v.historyCost / 100).toString() : '',
                        weightGrams: v.weightGrams?.toString() || '200',
                        quantityOnHand: v.inventory?.quantityOnHand?.toString() || '0',
                        reorderLevel: v.inventory?.reorderLevel?.toString() || '10',
                        imagePreview: v.image?.imageUrl
                    };
                });
                
                setVariants(updatedVariantRows);
            }
            
            // Show single success message
            const totalVariants = newVariants.length + existingVariants.length;
            toast.success(`Đã lưu thành công ${totalVariants} biến thể!`);
        } catch (err) {
            console.error('Failed to save variants:', err);
            const errorMessage = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
            toast.error(errorMessage || 'Không thể lưu biến thể');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="px-6 py-6">
            <div className="mb-6">
                <h1 className="text-2xl font-bold">Tạo sản phẩm mới</h1>
                <p className="text-sm text-gray-500">Điền thông tin sản phẩm và lưu bản nháp trước khi tạo biến thể</p>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* Left Column - Main Info */}
                <div className="lg:col-span-2 space-y-6">
                    {/* Basic Info */}
                    <div className="bg-white rounded-lg shadow p-6 space-y-4">
                        <h2 className="text-lg font-semibold">Thông tin cơ bản</h2>
                        
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">
                                Tên sản phẩm <span className="text-red-500">*</span>
                            </label>
                            <input
                                type="text"
                                value={name}
                                onChange={(e) => {
                                    setName(e.target.value);
                                    if (errors.name) {
                                        setErrors(prev => {
                                            const newErrors = { ...prev };
                                            delete newErrors.name;
                                            return newErrors;
                                        });
                                    }
                                }}
                                className={`w-full px-3 py-2 border rounded-md focus:ring-red-500 focus:border-red-500 ${
                                    errors.name ? 'border-red-500 bg-red-50' : 'border-gray-300'
                                }`}
                                placeholder="VD: Áo thun nam basic"
                                disabled={loading}
                            />
                            {errors.name && (
                                <p className="text-red-500 text-sm mt-1">{errors.name}</p>
                            )}
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">
                                Slug <span className="text-red-500">*</span>
                            </label>
                            <input
                                type="text"
                                value={slug}
                                onChange={(e) => {
                                    setSlug(e.target.value);
                                    if (errors.slug) {
                                        setErrors(prev => {
                                            const newErrors = { ...prev };
                                            delete newErrors.slug;
                                            return newErrors;
                                        });
                                    }
                                }}
                                className={`w-full px-3 py-2 border rounded-md focus:ring-red-500 focus:border-red-500 ${
                                    errors.slug ? 'border-red-500 bg-red-50' : 'border-gray-300'
                                }`}
                                placeholder="ao-thun-nam-basic"
                                disabled={loading}
                            />
                            {errors.slug && (
                                <p className="text-red-500 text-sm mt-1">{errors.slug}</p>
                            )}
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">Mô tả</label>
                            <textarea
                                value={description}
                                onChange={(e) => setDescription(e.target.value)}
                                rows={5}
                                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-red-500 focus:border-red-500"
                                placeholder="Mô tả chi tiết sản phẩm..."
                                disabled={loading}
                            />
                        </div>

                        <div className="grid grid-cols-2 gap-4">
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Chất liệu</label>
                                <input
                                    type="text"
                                    value={material}
                                    onChange={(e) => setMaterial(e.target.value)}
                                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-red-500 focus:border-red-500"
                                    placeholder="VD: Cotton 100%"
                                    disabled={loading}
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Giới tính</label>
                                <select
                                    value={gender}
                                    onChange={(e) => setGender(e.target.value as Gender)}
                                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-red-500 focus:border-red-500"
                                    disabled={loading}
                                >
                                    <option value="men">Nam</option>
                                    <option value="women">Nữ</option>
                                    <option value="unisex">Unisex</option>
                                </select>
                            </div>
                        </div>

                        {/* SEO Fields */}
                        <div className="pt-4 border-t">
                            <h3 className="text-sm font-semibold text-gray-700 mb-3">Tối ưu SEO</h3>
                            <div className="space-y-4">
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">SEO Title</label>
                                    <input
                                        type="text"
                                        value={seoTitle}
                                        onChange={(e) => setSeoTitle(e.target.value)}
                                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-red-500 focus:border-red-500"
                                        placeholder="Tiêu đề hiển thị trên Google"
                                        disabled={loading}
                                    />
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">SEO Description</label>
                                    <textarea
                                        value={seoDescription}
                                        onChange={(e) => setSeoDescription(e.target.value)}
                                        rows={3}
                                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-red-500 focus:border-red-500"
                                        placeholder="Mô tả ngắn gọn để hiển thị trên kết quả tìm kiếm"
                                        disabled={loading}
                                    />
                                </div>
                            </div>
                        </div>

                        <div className="pt-4 border-t">
                            <button
                                onClick={handleSaveDraft}
                                disabled={loading}
                                className="flex items-center gap-2 px-6 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50"
                            >
                                <Save className="w-4 h-4" />
                                {loading ? 'Đang lưu...' : (productId ? 'Cập nhật thông tin' : 'Lưu bản nháp')}
                            </button>
                        </div>
                    </div>

                    {/* Variants Section - Only after product is saved */}
                    {productId && (
                        <div className="bg-white rounded-lg shadow p-6 space-y-4">
                            <div className="flex items-center justify-between">
                                <h2 className="text-lg font-semibold flex items-center gap-2">
                                    <Package className="w-5 h-5" />
                                    Biến thể sản phẩm
                                </h2>
                            </div>

                            {/* Color & Size Selection */}
                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-2">Màu sắc</label>
                                    <div className="max-h-48 overflow-y-auto border border-gray-300 rounded-md p-2 space-y-1">
                                        {colors.map(color => color.id && (
                                            <label key={color.id} className="flex items-center gap-2 p-2 hover:bg-gray-50 rounded cursor-pointer">
                                                <input
                                                    type="checkbox"
                                                    checked={selectedColors.includes(color.id)}
                                                    onChange={(e) => {
                                                        if (e.target.checked && color.id) {
                                                            setSelectedColors(prev => [...prev, color.id!]);
                                                        } else if (color.id) {
                                                            setSelectedColors(prev => prev.filter(id => id !== color.id));
                                                        }
                                                    }}
                                                    className="w-4 h-4 text-red-600 border-gray-300 rounded focus:ring-red-500"
                                                />
                                                <div 
                                                    className="w-5 h-5 rounded border border-gray-300" 
                                                    style={{ backgroundColor: color.hexCode || '#ccc' }}
                                                />
                                                <span className="text-sm">{color.name}</span>
                                            </label>
                                        ))}
                                    </div>
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-2">Kích thước</label>
                                    <div className="max-h-48 overflow-y-auto border border-gray-300 rounded-md p-2 space-y-1">
                                        {sizes.map(size => size.id && (
                                            <label key={size.id} className="flex items-center gap-2 p-2 hover:bg-gray-50 rounded cursor-pointer">
                                                <input
                                                    type="checkbox"
                                                    checked={selectedSizes.includes(size.id)}
                                                    onChange={(e) => {
                                                        if (e.target.checked && size.id) {
                                                            setSelectedSizes(prev => [...prev, size.id!]);
                                                        } else if (size.id) {
                                                            setSelectedSizes(prev => prev.filter(id => id !== size.id));
                                                        }
                                                    }}
                                                    className="w-4 h-4 text-red-600 border-gray-300 rounded focus:ring-red-500"
                                                />
                                                <span className="text-sm font-medium">{size.name}</span>
                                            </label>
                                        ))}
                                    </div>
                                </div>
                            </div>

                            {/* Bulk Apply Section */}
                            {variants.length > 0 && (
                                <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 space-y-3">
                                    <div className="flex items-center justify-between">
                                        <h3 className="text-sm font-semibold text-blue-900">Áp dụng hàng loạt</h3>
                                        <button
                                            onClick={handleBulkApply}
                                            className="px-4 py-1.5 bg-blue-600 text-white text-sm rounded-md hover:bg-blue-700"
                                        >
                                            Áp dụng cho tất cả
                                        </button>
                                    </div>
                                    <div className="grid grid-cols-5 gap-3">
                                        <div>
                                            <label className="block text-xs font-medium text-gray-700 mb-1">Giá bán (VNĐ)</label>
                                            <input
                                                type="number"
                                                value={bulkValues.priceAmount}
                                                onChange={(e) => setBulkValues(prev => ({ ...prev, priceAmount: e.target.value }))}
                                                className="w-full px-2 py-1.5 text-sm border border-gray-300 rounded-md"
                                                placeholder="0"
                                            />
                                        </div>
                                        <div>
                                            <label className="block text-xs font-medium text-gray-700 mb-1">Giá so sánh (VNĐ)</label>
                                            <input
                                                type="number"
                                                value={bulkValues.compareAtAmount}
                                                onChange={(e) => setBulkValues(prev => ({ ...prev, compareAtAmount: e.target.value }))}
                                                className="w-full px-2 py-1.5 text-sm border border-gray-300 rounded-md"
                                                placeholder="0"
                                            />
                                        </div>
                                        <div>
                                            <label className="block text-xs font-medium text-gray-700 mb-1">Số lượng</label>
                                            <input
                                                type="number"
                                                value={bulkValues.quantityOnHand}
                                                onChange={(e) => setBulkValues(prev => ({ ...prev, quantityOnHand: e.target.value }))}
                                                className="w-full px-2 py-1.5 text-sm border border-gray-300 rounded-md"
                                                placeholder="0"
                                            />
                                        </div>
                                        <div>
                                            <label className="block text-xs font-medium text-gray-700 mb-1">Reorder Level</label>
                                            <input
                                                type="number"
                                                value={bulkValues.reorderLevel}
                                                onChange={(e) => setBulkValues(prev => ({ ...prev, reorderLevel: e.target.value }))}
                                                className="w-full px-2 py-1.5 text-sm border border-gray-300 rounded-md"
                                                placeholder="10"
                                            />
                                        </div>
                                        <div>
                                            <label className="block text-xs font-medium text-gray-700 mb-1">Giá gốc (VNĐ)</label>
                                            <input
                                                type="number"
                                                value={bulkValues.historyCost}
                                                onChange={(e) => setBulkValues(prev => ({ ...prev, historyCost: e.target.value }))}
                                                className="w-full px-2 py-1.5 text-sm border border-gray-300 rounded-md"
                                                placeholder="0"
                                            />
                                        </div>
                                    </div>
                                </div>
                            )}

                            {/* Variants Table */}
                            {variants.length > 0 && (
                                <div className="overflow-x-auto">
                                    <table className="min-w-full divide-y divide-gray-200">
                                        <thead className="bg-gray-50">
                                            <tr>
                                                <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">Hình ảnh</th>
                                                <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">Kích thước</th>
                                                <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">Màu sắc</th>
                                                <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">SKU</th>
                                                <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">Giá bán</th>
                                                <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">Số lượng</th>
                                                <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">Thao tác</th>
                                            </tr>
                                        </thead>
                                        <tbody className="bg-white divide-y divide-gray-200">
                                            {variants.map(variant => {
                                                const isEditing = editingVariantId === variant.tempId;
                                                return (
                                                    <tr key={variant.tempId} className={isEditing ? 'bg-yellow-50' : ''}>
                                                        <td className="px-3 py-2">
                                                            {variant.imagePreview ? (
                                                                <div className="relative w-16 h-16 group">
                                                                    <img src={variant.imagePreview} alt="" className="w-full h-full object-cover rounded" />
                                                                    <button
                                                                        onClick={() => removeVariantImage(variant.tempId)}
                                                                        className="absolute -top-1 -right-1 bg-red-500 text-white rounded-full p-1 opacity-0 group-hover:opacity-100"
                                                                    >
                                                                        <X className="w-3 h-3" />
                                                                    </button>
                                                                </div>
                                                            ) : (
                                                                <label className="w-16 h-16 border-2 border-dashed border-gray-300 rounded flex items-center justify-center cursor-pointer hover:border-red-500">
                                                                    <input
                                                                        type="file"
                                                                        accept="image/*"
                                                                        className="hidden"
                                                                        onChange={(e) => handleVariantImageSelect(variant.tempId, e)}
                                                                    />
                                                                    <ImageIcon className="w-6 h-6 text-gray-400" />
                                                                </label>
                                                            )}
                                                        </td>
                                                        <td className="px-3 py-2 text-sm">{variant.sizeName}</td>
                                                        <td className="px-3 py-2 text-sm">{variant.colorName}</td>
                                                        <td className="px-3 py-2">
                                                            {isEditing ? (
                                                                <input
                                                                    type="text"
                                                                    value={variant.sku}
                                                                    onChange={(e) => updateVariantField(variant.tempId, 'sku', e.target.value)}
                                                                    className="w-full px-2 py-1 text-sm border rounded"
                                                                />
                                                            ) : (
                                                                <span className="text-sm font-mono">{variant.sku}</span>
                                                            )}
                                                        </td>
                                                        <td className="px-3 py-2">
                                                            {isEditing ? (
                                                                <input
                                                                    type="number"
                                                                    value={variant.priceAmount}
                                                                    onChange={(e) => updateVariantField(variant.tempId, 'priceAmount', e.target.value)}
                                                                    className="w-24 px-2 py-1 text-sm border rounded"
                                                                />
                                                            ) : (
                                                                <span className="text-sm">{variant.priceAmount ? `${parseFloat(variant.priceAmount).toLocaleString('vi-VN')} ₫` : '-'}</span>
                                                            )}
                                                        </td>
                                                        <td className="px-3 py-2">
                                                            {isEditing ? (
                                                                <input
                                                                    type="number"
                                                                    value={variant.quantityOnHand}
                                                                    onChange={(e) => updateVariantField(variant.tempId, 'quantityOnHand', e.target.value)}
                                                                    className="w-20 px-2 py-1 text-sm border rounded"
                                                                />
                                                            ) : (
                                                                <span className="text-sm">{variant.quantityOnHand || '0'}</span>
                                                            )}
                                                        </td>
                                                        <td className="px-3 py-2">
                                                            <button
                                                                onClick={() => setEditingVariantId(isEditing ? null : variant.tempId)}
                                                                className="text-blue-600 hover:text-blue-800"
                                                            >
                                                                <Edit2 className="w-4 h-4" />
                                                            </button>
                                                        </td>
                                                    </tr>
                                                );
                                            })}
                                        </tbody>
                                    </table>
                                </div>
                            )}

                            {/* Expanded Edit Form */}
                            {editingVariantId && (
                                <div className="bg-gray-50 border border-gray-200 rounded-lg p-4 space-y-3">
                                    {(() => {
                                        const variant = variants.find(v => v.tempId === editingVariantId);
                                        if (!variant) return null;
                                        return (
                                            <>
                                                <div className="flex items-center justify-between">
                                                    <h3 className="text-sm font-semibold">
                                                        Chỉnh sửa: {variant.sizeName} - {variant.colorName}
                                                    </h3>
                                                    <button
                                                        onClick={() => setEditingVariantId(null)}
                                                        className="text-gray-500 hover:text-gray-700"
                                                    >
                                                        <X className="w-5 h-5" />
                                                    </button>
                                                </div>
                                                <div className="grid grid-cols-3 gap-3">
                                                    <div>
                                                        <label className="block text-xs font-medium text-gray-700 mb-1">Barcode</label>
                                                        <input
                                                            type="text"
                                                            value={variant.barcode}
                                                            onChange={(e) => updateVariantField(variant.tempId, 'barcode', e.target.value)}
                                                            className="w-full px-2 py-1.5 text-sm border rounded"
                                                        />
                                                    </div>
                                                    <div>
                                                        <label className="block text-xs font-medium text-gray-700 mb-1">Giá so sánh (VNĐ)</label>
                                                        <input
                                                            type="number"
                                                            value={variant.compareAtAmount}
                                                            onChange={(e) => updateVariantField(variant.tempId, 'compareAtAmount', e.target.value)}
                                                            className="w-full px-2 py-1.5 text-sm border rounded"
                                                        />
                                                    </div>
                                                    <div>
                                                        <label className="block text-xs font-medium text-gray-700 mb-1">Giá gốc (VNĐ)</label>
                                                        <input
                                                            type="number"
                                                            value={variant.historyCost}
                                                            onChange={(e) => updateVariantField(variant.tempId, 'historyCost', e.target.value)}
                                                            className="w-full px-2 py-1.5 text-sm border rounded"
                                                        />
                                                    </div>
                                                    <div>
                                                        <label className="block text-xs font-medium text-gray-700 mb-1">Cân nặng (grams)</label>
                                                        <input
                                                            type="number"
                                                            value={variant.weightGrams}
                                                            onChange={(e) => updateVariantField(variant.tempId, 'weightGrams', e.target.value)}
                                                            className="w-full px-2 py-1.5 text-sm border rounded"
                                                        />
                                                    </div>
                                                    <div>
                                                        <label className="block text-xs font-medium text-gray-700 mb-1">Reorder Level</label>
                                                        <input
                                                            type="number"
                                                            value={variant.reorderLevel}
                                                            onChange={(e) => updateVariantField(variant.tempId, 'reorderLevel', e.target.value)}
                                                            className="w-full px-2 py-1.5 text-sm border rounded"
                                                        />
                                                    </div>
                                                </div>
                                            </>
                                        );
                                    })()}
                                </div>
                            )}

                            {/* No Variants Message */}
                            {variants.length === 0 && selectedColors.length === 0 && selectedSizes.length === 0 && (
                                <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 text-center">
                                    <p className="text-sm text-yellow-800">
                                        Chưa có biến thể. Vui lòng chọn màu sắc và kích thước để tạo biến thể sản phẩm.
                                    </p>
                                </div>
                            )}

                            {/* Save Variants Button */}
                            {variants.length > 0 && (
                                <div className="pt-4 border-t">
                                    <button
                                        onClick={handleSaveVariants}
                                        disabled={loading}
                                        className="w-full flex items-center justify-center gap-2 px-6 py-3 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50 font-medium"
                                    >
                                        <Save className="w-5 h-5" />
                                        {loading ? 'Đang lưu...' : `Lưu ${variants.length} biến thể`}
                                    </button>
                                </div>
                            )}

                            {/* Publish Product Button */}
                            {productId && (
                                <div className="pt-4 border-t">
                                    <button
                                        onClick={handlePublishProduct}
                                        disabled={loading}
                                        className="w-full flex items-center justify-center gap-2 px-6 py-3 bg-green-600 text-white rounded-md hover:bg-green-700 disabled:opacity-50 font-medium"
                                    >
                                        <Send className="w-5 h-5" />
                                        {loading ? 'Đang xuất bản...' : 'Xuất bản sản phẩm'}
                                    </button>
                                </div>
                            )}
                        </div>
                    )}
                </div>

                {/* Right Column - Images & Categories */}
                <div className="space-y-6">
                    {/* Primary Image */}
                    <div className="bg-white rounded-lg shadow p-6 space-y-4">
                        <h2 className="text-lg font-semibold">
                            Ảnh chính <span className="text-red-500">*</span>
                        </h2>
                        {errors.primaryImage && (
                            <p className="text-red-500 text-sm">{errors.primaryImage}</p>
                        )}
                        {primaryImage ? (
                            <div className="relative group">
                                <img 
                                    src={primaryImage.preview} 
                                    alt="Primary" 
                                    className="w-full h-64 object-cover rounded-lg"
                                />
                                <button
                                    onClick={removePrimaryImage}
                                    className="absolute top-2 right-2 bg-red-500 text-white p-2 rounded-full opacity-0 group-hover:opacity-100 transition-opacity hover:bg-red-600"
                                >
                                    <X className="w-4 h-4" />
                                </button>
                            </div>
                        ) : (
                            <div
                                onDragOver={(e) => { e.preventDefault(); setIsDraggingPrimary(true); }}
                                onDragLeave={() => setIsDraggingPrimary(false)}
                                onDrop={handlePrimaryImageDrop}
                                className={`border-2 border-dashed rounded-lg p-8 text-center cursor-pointer ${
                                    isDraggingPrimary ? 'border-red-500 bg-red-50' : 'border-gray-300'
                                }`}
                            >
                                <label className="cursor-pointer">
                                    <Upload className="w-12 h-12 mx-auto text-gray-400 mb-2" />
                                    <p className="text-sm text-gray-600">Kéo thả hoặc click để chọn ảnh</p>
                                    <input
                                        type="file"
                                        accept="image/*"
                                        className="hidden"
                                        onChange={handlePrimaryImageSelect}
                                    />
                                </label>
                            </div>
                        )}
                    </div>

                    {/* Gallery Images */}
                    <div className="bg-white rounded-lg shadow p-6 space-y-4">
                        <h2 className="text-lg font-semibold">Ảnh Gallery</h2>
                        <div className="grid grid-cols-2 gap-3">
                            {galleryImages.map((img, index) => (
                                <div key={index} className="relative group">
                                    <img 
                                        src={img.preview} 
                                        alt={`Gallery ${index + 1}`}
                                        className="w-full h-32 object-cover rounded-lg"
                                    />
                                    <button
                                        onClick={() => removeGalleryImage(index)}
                                        className="absolute top-1 right-1 bg-red-500 text-white p-1 rounded-full opacity-0 group-hover:opacity-100 transition-opacity hover:bg-red-600"
                                    >
                                        <X className="w-3 h-3" />
                                    </button>
                                </div>
                            ))}
                            <div
                                onDragOver={(e) => { e.preventDefault(); setIsDraggingGallery(true); }}
                                onDragLeave={() => setIsDraggingGallery(false)}
                                onDrop={handleGalleryImageDrop}
                                className={`border-2 border-dashed rounded-lg h-32 flex items-center justify-center cursor-pointer ${
                                    isDraggingGallery ? 'border-red-500 bg-red-50' : 'border-gray-300'
                                }`}
                            >
                                <label className="cursor-pointer text-center">
                                    <Plus className="w-8 h-8 mx-auto text-gray-400 mb-1" />
                                    <p className="text-xs text-gray-500">Thêm ảnh</p>
                                    <input
                                        type="file"
                                        accept="image/*"
                                        multiple
                                        className="hidden"
                                        onChange={handleGalleryImageSelect}
                                    />
                                </label>
                            </div>
                        </div>
                    </div>

                    {/* Brand */}
                    <div className="bg-white rounded-lg shadow p-6 space-y-4">
                        <h2 className="text-lg font-semibold">
                            Thương hiệu <span className="text-red-500">*</span>
                        </h2>
                        <div>
                            <select
                                value={brandId}
                                onChange={(e) => {
                                    setBrandId(e.target.value);
                                    if (errors.brand) {
                                        setErrors(prev => {
                                            const newErrors = { ...prev };
                                            delete newErrors.brand;
                                            return newErrors;
                                        });
                                    }
                                }}
                                className={`w-full px-3 py-2 border rounded-md focus:ring-red-500 focus:border-red-500 ${
                                    errors.brand ? 'border-red-500 bg-red-50' : 'border-gray-300'
                                }`}
                                disabled={loading}
                            >
                                <option value="">-- Chọn thương hiệu --</option>
                                {brands.map(brand => (
                                    <option key={brand.id} value={brand.id}>{brand.name}</option>
                                ))}
                            </select>
                            {errors.brand && (
                                <p className="text-red-500 text-sm mt-1">{errors.brand}</p>
                            )}
                        </div>
                    </div>

                    {/* Categories */}
                    <div className="bg-white rounded-lg shadow p-6 space-y-4">
                        <h2 className="text-lg font-semibold">Danh mục</h2>
                        <button
                            onClick={() => {
                                expandAllCategories();
                                setShowCategoryModal(true);
                            }}
                            className="w-full px-4 py-2 text-left border border-gray-300 rounded-md hover:border-red-500 focus:ring-red-500 focus:border-red-500"
                        >
                            {categoryIds.length > 0 ? (
                                <span className="text-gray-900">
                                    Đã chọn {categoryIds.length} danh mục
                                </span>
                            ) : (
                                <span className="text-gray-500">-- Chọn danh mục --</span>
                            )}
                        </button>
                        {categoryIds.length > 0 && (
                            <div className="flex flex-wrap gap-2">
                                {categoryIds.map(catId => {
                                    const cat = categories.find(c => c.id === catId);
                                    return cat ? (
                                        <span key={catId} className="inline-flex items-center gap-1 px-2 py-1 bg-red-50 text-red-700 text-xs rounded-full">
                                            {cat.name}
                                            <button
                                                onClick={() => toggleCategory(catId)}
                                                className="hover:bg-red-200 rounded-full p-0.5"
                                            >
                                                <X className="w-3 h-3" />
                                            </button>
                                        </span>
                                    ) : null;
                                })}
                            </div>
                        )}
                    </div>
                </div>
            </div>

            {/* Category Selection Modal */}
            {showCategoryModal && (
                <div 
                    className="fixed inset-0 bg-transparent flex items-center justify-center z-[9999] p-4"
                    onClick={() => setShowCategoryModal(false)}
                >
                    <div 
                        className="bg-white rounded-lg shadow-xl max-w-3xl w-full max-h-[85vh] flex flex-col"
                        onClick={(e) => e.stopPropagation()}
                    >
                        {/* Modal Header */}
                        <div className="flex items-center justify-between p-6 border-b">
                            <h3 className="text-lg font-semibold">Chọn danh mục sản phẩm</h3>
                            <button
                                onClick={() => setShowCategoryModal(false)}
                                className="text-gray-400 hover:text-gray-600"
                            >
                                <X className="w-6 h-6" />
                            </button>
                        </div>

                        {/* Modal Body */}
                        <div className="flex-1 overflow-y-auto p-6 min-h-[400px]">
                            {renderCategoryGrid()}
                        </div>

                        {/* Modal Footer */}
                        <div className="flex items-center justify-between p-6 border-t bg-gray-50">
                            <p className="text-sm text-gray-600">
                                Đã chọn: <strong>{categoryIds.length}</strong> danh mục
                            </p>
                            <div className="flex gap-3">
                                <button
                                    onClick={() => {
                                        setCategoryIds([]);
                                    }}
                                    className="px-4 py-2 text-sm text-gray-700 border border-gray-300 rounded-md hover:bg-gray-50"
                                >
                                    Bỏ chọn tất cả
                                </button>
                                <button
                                    onClick={() => setShowCategoryModal(false)}
                                    className="px-4 py-2 text-sm bg-red-600 text-white rounded-md hover:bg-red-700"
                                >
                                    Xong
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default ProductCreateNew;
