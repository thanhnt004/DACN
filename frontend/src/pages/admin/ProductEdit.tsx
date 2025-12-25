import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Upload, X, Plus, Image as ImageIcon, Edit2, Save, Package, Send, ArrowLeft } from 'lucide-react';
import { toast } from 'react-toastify';
import { getProductDetail, updateProduct, createVariantBulk, changeProductStatus, updateVariant as updateVariantApi, deleteVariant } from '../../api/admin/products';
import { getBrands, getCategories, type CategoryResponse } from '../../api/admin/brandCategory';
import { getSizes, getColors } from '../../api/admin/catalog';
import { uploadImagesToCloudinary } from '../../api/media';
import type { 
    ProductImagePayload,
    Gender
} from '../../api/admin/products';
import type { BrandDto } from '../../api/admin/brandCategory';
import type { SizeDto, ColorDto } from '../../api/admin/catalog';

interface VariantRow {
    tempId: string;
    variantId?: string;
    version?: number;
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
    weightGrams: string;
    barcode: string;
}

const ProductEdit: React.FC = () => {
    const navigate = useNavigate();
    const { productId } = useParams<{ productId: string }>();
    
    // Product state
    const [productVersion, setProductVersion] = useState<number>(0);
    const [productStatus, setProductStatus] = useState<'DRAFT' | 'ACTIVE' | 'ARCHIVED'>('DRAFT');
    const [name, setName] = useState('');
    const [slug, setSlug] = useState('');
    const [brandId, setBrandId] = useState('');
    const [categoryIds, setCategoryIds] = useState<string[]>([]);
    const [description, setDescription] = useState('');
    const [material, setMaterial] = useState('');
    const [gender, setGender] = useState<Gender>('unisex');
    const [seoTitle, setSeoTitle] = useState('');
    const [seoDescription, setSeoDescription] = useState('');
    const [priceAmount, setPriceAmount] = useState('');
    
    // Images
    const [primaryImage, setPrimaryImage] = useState<{ file: File; preview: string } | null>(null);
    const [galleryImages, setGalleryImages] = useState<{ file: File; preview: string }[]>([]);
    const [existingPrimaryImage, setExistingPrimaryImage] = useState<ProductImagePayload | null>(null);
    const [existingGalleryImages, setExistingGalleryImages] = useState<ProductImagePayload[]>([]);
    
    // Variants
    const [variants, setVariants] = useState<VariantRow[]>([]);
    const [selectedSizes, setSelectedSizes] = useState<string[]>([]);
    const [selectedColors, setSelectedColors] = useState<string[]>([]);
    // Track which sizes/colors are locked (in use by variants)
    const [lockedSizes, setLockedSizes] = useState<string[]>([]);
    const [lockedColors, setLockedColors] = useState<string[]>([]);
    const [bulkValues, setBulkValues] = useState<BulkApplyValues>({
        priceAmount: '',
        compareAtAmount: '',
        quantityOnHand: '',
        reorderLevel: '10',
        historyCost: '',
        weightGrams: '200',
        barcode: ''
    });
    
    // Reference data
    const [brands, setBrands] = useState<BrandDto[]>([]);
    const [categories, setCategories] = useState<CategoryResponse[]>([]);
    const [sizesData, setSizesData] = useState<SizeDto[]>([]);
    const [colorsData, setColorsData] = useState<ColorDto[]>([]);
    
    // UI state
    const [loading, setLoading] = useState(false);
    const [loadingProduct, setLoadingProduct] = useState(true);
    const [errors, setErrors] = useState<Record<string, string>>({});
    const [activeTab, setActiveTab] = useState<'basic' | 'variants'>('basic');
    const [showCategoryModal, setShowCategoryModal] = useState(false);
    const [expandedCategories, setExpandedCategories] = useState<Set<string>>(new Set());

    // Load product data - only after reference data is loaded
    useEffect(() => {
        if (!productId) {
            toast.error('ID sản phẩm không hợp lệ');
            navigate('/admin/products');
            return;
        }
        // Only load product after we have sizes and colors data
        if (sizesData.length > 0 && colorsData.length > 0) {
            loadProduct();
        }
    }, [productId, navigate, sizesData, colorsData]);

    // Load reference data
    useEffect(() => {
        const loadData = async () => {
            try {
                const [brandsRes, sizesRes, colorsRes] = await Promise.all([
                    getBrands({ size: 1000 }),
                    getSizes(),
                    getColors()
                ]);
                
                const categoriesRes = await getCategories({ size: 10000, all: true });
                const allCategories = categoriesRes.content || [];
                
                setBrands(brandsRes.content || []);
                setCategories(allCategories);
                setSizesData(sizesRes);
                setColorsData(colorsRes);
            } catch (err) {
                console.error('Failed to load reference data:', err);
                toast.error('Không thể tải dữ liệu danh mục');
            }
        };
        loadData();
    }, []);

    const loadProduct = useCallback(async () => {
        if (!productId) return;
        
        setLoadingProduct(true);
        try {
            const product = await getProductDetail(productId, ['images', 'variants', 'categories']);
            
            console.log('📦 Loaded product:', product);
            console.log('🏷️ Brand ID from product:', product.brandId);
            console.log('🖼️ All images:', product.images);
            
            // Set basic info
            setName(product.name || '');
            setSlug(product.slug || '');
            setBrandId(product.brandId || '');
            setCategoryIds(product.categories?.map(c => c.id) || []);
            setDescription(product.description || '');
            setMaterial(product.material || '');
            setGender(product.gender || 'unisex');
            setSeoTitle(product.seoTitle || '');
            setSeoDescription(product.seoDescription || '');
            setPriceAmount(product.priceAmount ? (product.priceAmount / 100).toString() : '');
            setProductVersion(product.version || 0);
            setProductStatus(product.status || 'DRAFT');
            
            // Set existing images
            if (product.images && product.images.length > 0) {
                // Use all images, don't filter by colorId
                const productImages = product.images;
                
                console.log('🖼️ Product images:', productImages);
                
                // Find primary image (isDefault = true)
                const primaryImage = productImages.find(img => img.isDefault);
                console.log('🎯 Primary image:', primaryImage);
                
                if (primaryImage) {
                    setExistingPrimaryImage(primaryImage);
                }
                
                // Gallery images are non-default images, sorted by position
                const galleryImages = productImages
                    .filter(img => !img.isDefault)
                    .sort((a, b) => (a.position || 0) - (b.position || 0));
                
                console.log('🖼️ Gallery images:', galleryImages);
                
                if (galleryImages.length > 0) {
                    setExistingGalleryImages(galleryImages);
                }
            }
            
            // Set variants
            if (product.variants && product.variants.length > 0) {
                const variantRows: VariantRow[] = product.variants.map(v => {
                    const colorInfo = colorsData.find(c => c.id === v.colorId) || v.color;
                    const sizeInfo = sizesData.find(s => s.id === v.sizeId) || v.size;
                    
                    return {
                        tempId: `${v.sizeId}-${v.colorId}`,
                        variantId: v.id,
                        version: v.version,
                        colorId: v.colorId || '',
                        colorName: colorInfo?.name || '',
                        sizeId: v.sizeId || '',
                        sizeName: sizeInfo?.code || '',
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
                
                setVariants(variantRows);
                
                // Extract selected sizes and colors
                const uniqueSizes = [...new Set(variantRows.map(v => v.sizeId))];
                const uniqueColors = [...new Set(variantRows.map(v => v.colorId))];
                setSelectedSizes(uniqueSizes);
                setSelectedColors(uniqueColors);
                setLockedSizes(uniqueSizes);
                setLockedColors(uniqueColors);
            }
            
        } catch (err) {
            console.error('Failed to load product:', err);
            toast.error('Không thể tải thông tin sản phẩm');
            navigate('/admin/products');
        } finally {
            setLoadingProduct(false);
        }
    }, [productId, colorsData, sizesData, navigate]);

    // Category helper functions
    const toggleCategory = (categoryId: string) => {
        setCategoryIds(prev => 
            prev.includes(categoryId) 
                ? prev.filter(id => id !== categoryId)
                : [...prev, categoryId]
        );
    };

    const expandAllCategories = () => {
        const allCategoryIds = categories.map(c => c.id);
        setExpandedCategories(new Set(allCategoryIds));
    };

    const renderCategoryGrid = (): React.ReactElement => {
        const allCategories = categories;
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

    // Handle primary image
    const handlePrimaryImageSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (file) {
            setPrimaryImage({
                file,
                preview: URL.createObjectURL(file)
            });
            setExistingPrimaryImage(null);
        }
    };

    const removePrimaryImage = () => {
        if (primaryImage) {
            URL.revokeObjectURL(primaryImage.preview);
            setPrimaryImage(null);
        }
        setExistingPrimaryImage(null);
    };

    // Handle gallery images
    const handleGalleryImageSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
        const files = Array.from(e.target.files || []);
        const newImages = files.map(file => ({
            file,
            preview: URL.createObjectURL(file)
        }));
        setGalleryImages(prev => [...prev, ...newImages]);
    };

    const removeGalleryImage = (index: number) => {
        setGalleryImages(prev => {
            const updated = [...prev];
            URL.revokeObjectURL(updated[index].preview);
            updated.splice(index, 1);
            return updated;
        });
    };

    const removeExistingGalleryImage = (index: number) => {
        setExistingGalleryImages(prev => {
            const updated = [...prev];
            updated.splice(index, 1);
            return updated;
        });
    };

    // Generate variants
    const generateVariants = () => {
        if (selectedSizes.length === 0 || selectedColors.length === 0) {
            toast.error('Vui lòng chọn ít nhất 1 size và 1 màu');
            return;
        }

        const newVariants: VariantRow[] = [];
        
        selectedSizes.forEach(sizeId => {
            selectedColors.forEach(colorId => {
                const tempId = `${sizeId}-${colorId}`;
                const existing = variants.find(v => v.tempId === tempId);
                
                if (existing) {
                    newVariants.push(existing);
                } else {
                    const size = sizesData.find(s => s.id === sizeId);
                    const color = colorsData.find(c => c.id === colorId);
                    
                    newVariants.push({
                        tempId,
                        colorId,
                        colorName: color?.name || '',
                        sizeId,
                        sizeName: size?.code || '',
                        sku: `${slug}-${size?.code}-${color?.name}`.toUpperCase(),
                        barcode: '',
                        priceAmount: bulkValues.priceAmount || '',
                        compareAtAmount: bulkValues.compareAtAmount || '',
                        historyCost: bulkValues.historyCost || '',
                        weightGrams: '200',
                        quantityOnHand: bulkValues.quantityOnHand || '0',
                        reorderLevel: bulkValues.reorderLevel || '10'
                    });
                }
            });
        });
        
        setVariants(newVariants);
        toast.success(`Đã tạo ${newVariants.length} biến thể`);
    };

    // Apply bulk values
    const applyBulkValues = () => {
        setVariants(prev => prev.map(v => ({
            ...v,
            barcode: bulkValues.barcode || v.barcode,
            priceAmount: bulkValues.priceAmount || v.priceAmount,
            compareAtAmount: bulkValues.compareAtAmount || v.compareAtAmount,
            historyCost: bulkValues.historyCost || v.historyCost,
            weightGrams: bulkValues.weightGrams || v.weightGrams,
            quantityOnHand: bulkValues.quantityOnHand || v.quantityOnHand,
            reorderLevel: bulkValues.reorderLevel || v.reorderLevel
        })));
        toast.success('Đã áp dụng giá trị cho tất cả biến thể');
    };

    // Update variant field
    const updateVariantField = (tempId: string, field: keyof VariantRow, value: string) => {
        setVariants(prev => prev.map(v => 
            v.tempId === tempId ? { ...v, [field]: value } : v
        ));
    };

    // Handle variant image
    const handleVariantImageSelect = (tempId: string, e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (file) {
            const preview = URL.createObjectURL(file);
            setVariants(prev => prev.map(v => 
                v.tempId === tempId ? { ...v, imageFile: file, imagePreview: preview } : v
            ));
        }
    };

    const removeVariantImage = (tempId: string) => {
        setVariants(prev => prev.map(v => {
            if (v.tempId === tempId && v.imagePreview && v.imageFile) {
                URL.revokeObjectURL(v.imagePreview);
                const { imageFile, imagePreview, ...rest } = v;
                return rest;
            }
            if (v.tempId === tempId) {
                const { imagePreview, ...rest } = v;
                return rest;
            }
            return v;
        }));
    };

    // Delete variant
    const handleDeleteVariant = async (tempId: string) => {
        const variant = variants.find(v => v.tempId === tempId);
        if (!variant) return;

        let deletedVariant: VariantRow | undefined = undefined;
        if (variant.variantId && productId) {
            if (!window.confirm('Xóa biến thể này? Hành động không thể hoàn tác.')) return;
            
            try {
                setLoading(true);
                await deleteVariant(productId, variant.variantId);
                setVariants(prev => prev.filter(v => v.tempId !== tempId));
                deletedVariant = variant;
                toast.success('Đã xóa biến thể');
            } catch (err) {
                console.error('Failed to delete variant:', err);
                toast.error('Không thể xóa biến thể');
            } finally {
                setLoading(false);
            }
        } else {
            setVariants(prev => prev.filter(v => v.tempId !== tempId));
            deletedVariant = variant;
        }
        // After deletion, check if any color/size is no longer used
        setTimeout(() => {
            if (deletedVariant) {
                const stillUsedColors = variants.filter(v => v.tempId !== tempId).map(v => v.colorId);
                const stillUsedSizes = variants.filter(v => v.tempId !== tempId).map(v => v.sizeId);
                // If deleted color is not in any variant, unlock and uncheck
                if (!stillUsedColors.includes(deletedVariant.colorId)) {
                    setLockedColors(prev => prev.filter(id => id !== deletedVariant!.colorId));
                    setSelectedColors(prev => prev.filter(id => id !== deletedVariant!.colorId));
                }
                // If deleted size is not in any variant, unlock and uncheck
                if (!stillUsedSizes.includes(deletedVariant.sizeId)) {
                    setLockedSizes(prev => prev.filter(id => id !== deletedVariant!.sizeId));
                    setSelectedSizes(prev => prev.filter(id => id !== deletedVariant!.sizeId));
                }
            }
        }, 100);
    };

    // Save product
    const handleSaveDraft = async () => {
        setErrors({});
        const newErrors: Record<string, string> = {};

        if (!name.trim()) newErrors.name = 'Tên sản phẩm là bắt buộc';
        if (!slug.trim()) newErrors.slug = 'Slug là bắt buộc';
        if (!priceAmount || parseFloat(priceAmount) <= 0) newErrors.priceAmount = 'Giá bán phải lớn hơn 0';
        if (!brandId) newErrors.brand = 'Vui lòng chọn thương hiệu';
        if (!existingPrimaryImage && !primaryImage) newErrors.primaryImage = 'Ảnh chính là bắt buộc';

        if (Object.keys(newErrors).length > 0) {
            setErrors(newErrors);
            toast.error('Vui lòng điền đầy đủ thông tin bắt buộc');
            return;
        }

        try {
            setLoading(true);

            let imagePayloads: ProductImagePayload[] = [];
            let currentPosition = 0;

            try {
                // 1. Handle Primary Image
                if (primaryImage) {
                    // Upload new primary image
                    const [url] = await uploadImagesToCloudinary([primaryImage.file], 'product', productId || undefined);
                    imagePayloads.push({
                        imageUrl: url,
                        position: currentPosition++,
                        isDefault: true,
                        alt: name
                    });
                } else if (existingPrimaryImage) {
                    // Use existing primary image
                    imagePayloads.push({
                        ...existingPrimaryImage,
                        position: currentPosition++,
                        isDefault: true,
                        alt: name
                    });
                }

                // 2. Handle Existing Gallery Images
                existingGalleryImages.forEach(img => {
                    imagePayloads.push({
                        ...img,
                        position: currentPosition++,
                        isDefault: false,
                        alt: name
                    });
                });

                // 3. Handle New Gallery Images
                if (galleryImages.length > 0) {
                    const newGalleryFiles = galleryImages.map(img => img.file);
                    const newGalleryUrls = await uploadImagesToCloudinary(newGalleryFiles, 'product', productId || undefined);
                    
                    newGalleryUrls.forEach(url => {
                        imagePayloads.push({
                            imageUrl: url,
                            position: currentPosition++,
                            isDefault: false,
                            alt: name
                        });
                    });
                }
            } catch (uploadError) {
                console.error('Failed to upload images:', uploadError);
                toast.error('Không thể tải ảnh lên');
                throw uploadError;
            }

            if (productId) {
                const request = {
                    id: productId,
                    name: name.trim(),
                    slug: slug.trim(),
                    brandId: brandId || undefined,  // Chuyển empty string thành undefined
                    categoryId: categoryIds.length > 0 ? categoryIds : undefined,
                    priceAmount: Math.round(parseFloat(priceAmount) * 100),
                    description: description.trim() || undefined,
                    material: material.trim() || undefined,
                    gender,
                    seoTitle: seoTitle.trim() || undefined,
                    seoDescription: seoDescription.trim() || undefined,
                    images: imagePayloads.length > 0 ? imagePayloads : undefined,
                    version: productVersion
                };

                console.log('🔍 Saving product with request:', request);
                const response = await updateProduct(productId, request);
                setProductVersion(response.version || 0);
                toast.success('Đã cập nhật thông tin sản phẩm thành công!');
            }
        } catch (err) {
            console.error('Failed to save product:', err);
            const errorMessage = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
            toast.error(errorMessage || 'Không thể lưu sản phẩm');
        } finally {
            setLoading(false);
        }
    };

    // Publish product
    const handlePublishProduct = async () => {
        if (!productId) {
            toast.error('Không tìm thấy sản phẩm');
            return;
        }

        // Validate trước khi xuất bản
        if (variants.length === 0) {
            toast.error('Sản phẩm phải có ít nhất 1 biến thể trước khi xuất bản!');
            setActiveTab('variants'); // Chuyển sang tab variants
            return;
        }

        try {
            setLoading(true);
            await changeProductStatus(productId, 'ACTIVE');
            setProductStatus('ACTIVE');
            toast.success('Đã xuất bản sản phẩm thành công!');
        } catch (err) {
            console.error('Failed to publish product:', err);
            const errorMessage = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
            toast.error(errorMessage || 'Không thể xuất bản sản phẩm');
        } finally {
            setLoading(false);
        }
    };

    // Archive product
    const handleArchiveProduct = async () => {
        if (!productId) {
            toast.error('Không tìm thấy sản phẩm');
            return;
        }

        try {
            setLoading(true);
            await changeProductStatus(productId, 'ARCHIVED');
            setProductStatus('ARCHIVED');
            toast.success('Đã lưu trữ sản phẩm thành công!');
        } catch (err) {
            console.error('Failed to archive product:', err);
            const errorMessage = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
            toast.error(errorMessage || 'Không thể lưu trữ sản phẩm');
        } finally {
            setLoading(false);
        }
    };

    // Save variants
    const handleSaveVariants = async () => {
        if (!productId) {
            toast.error('Vui lòng lưu sản phẩm trước');
            return;
        }

        if (variants.length === 0) {
            toast.error('Vui lòng tạo biến thể');
            return;
        }

        // Validate
        const skuSet = new Set<string>();
        for (const v of variants) {
            if (!v.sku?.trim()) {
                toast.error(`Biến thể ${v.sizeName}-${v.colorName} chưa có SKU`);
                return;
            }
            
            // Kiểm tra SKU trùng lặp
            const skuUpper = v.sku.trim().toUpperCase();
            if (skuSet.has(skuUpper)) {
                toast.error(`SKU "${v.sku}" bị trùng lặp!`);
                return;
            }
            skuSet.add(skuUpper);
            
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
            
            const newVariants = variants.filter(v => !v.variantId);
            const existingVariants = variants.filter(v => v.variantId);
            
            // Upload variant images
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
                console.log('Creating new variants:', newVariants.length);
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

                console.log('Variant requests:', variantRequests);
                try {
                    await createVariantBulk(productId, variantRequests);
                } catch (bulkError) {
                    console.error('Failed to create variants:', bulkError);
                    const errorMessage = (bulkError as any)?.response?.data?.message;
                    if (errorMessage?.includes('Sku is existed') || errorMessage?.includes('SKU')) {
                        toast.error('Một số SKU đã tồn tại trong hệ thống. Vui lòng kiểm tra lại!');
                    } else {
                        toast.error(errorMessage || 'Không thể tạo biến thể');
                    }
                    throw bulkError;
                }
            }
            
            // Update existing variants
            if (existingVariants.length > 0) {
                for (const v of existingVariants) {
                    if (!v.variantId || v.version === undefined) continue;
                    
                    const updateRequest: any = {
                        sku: v.sku,
                        sizeId: v.sizeId,
                        colorId: v.colorId,
                        priceAmount: Math.round(parseFloat(v.priceAmount) * 100),
                        weightGrams: parseInt(v.weightGrams) || 200,
                        status: 'ACTIVE' as const,
                        version: v.version,
                        inventory: {
                            quantityOnHand: parseInt(v.quantityOnHand),
                            quantityReserved: 0,
                            reorderLevel: parseInt(v.reorderLevel) || 10
                        }
                    };
                    
                    // Add optional fields only if they have values
                    if (v.barcode && v.barcode.trim()) {
                        updateRequest.barcode = v.barcode;
                    }
                    if (v.compareAtAmount && v.compareAtAmount.trim()) {
                        updateRequest.compareAtAmount = Math.round(parseFloat(v.compareAtAmount) * 100);
                    }
                    if (v.historyCost && v.historyCost.trim()) {
                        updateRequest.historyCost = Math.round(parseFloat(v.historyCost) * 100);
                    }
                    if (variantImageUrls[v.tempId]) {
                        updateRequest.image = {
                            imageUrl: variantImageUrls[v.tempId],
                            isDefault: false
                        };
                    }
                    
                    const updateResponse = await updateVariantApi(productId, v.variantId, updateRequest);
                    console.log('Update variant response:', updateResponse);
                }
            }
            
            // Reload variants
            console.log('Reloading variants for product:', productId);
            const updatedProduct = await getProductDetail(productId, ['variants']);
            console.log('Updated product variants:', updatedProduct.variants);
            
            if (updatedProduct.variants && updatedProduct.variants.length > 0) {
                const updatedVariantRows: VariantRow[] = updatedProduct.variants.map(v => {
                    const colorInfo = colorsData.find(c => c.id === v.colorId);
                    const sizeInfo = sizesData.find(s => s.id === v.sizeId);
                    
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

    if (loadingProduct) {
        return (
            <div className="flex justify-center items-center h-screen">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-red-600"></div>
            </div>
        );
    }

    return (
        <div className="px-6 py-6">
            {/* Header */}
            <div className="mb-6 flex items-center justify-between">
                <div className="flex items-center gap-4">
                    <button
                        onClick={() => navigate('/admin/products')}
                        className="flex items-center gap-2 text-gray-600 hover:text-gray-900"
                    >
                        <ArrowLeft className="w-5 h-5" />
                        Quay lại
                    </button>
                    <div>
                        <h1 className="text-2xl font-bold">Chỉnh sửa sản phẩm</h1>
                        <p className="text-sm text-gray-500">Cập nhật thông tin sản phẩm và biến thể</p>
                    </div>
                </div>
                <div className="flex gap-3">
                    <button
                        onClick={handleSaveDraft}
                        disabled={loading}
                        className="flex items-center gap-2 px-4 py-2 bg-gray-600 text-white rounded-lg hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        <Save className="w-4 h-4" />
                        Lưu thay đổi
                    </button>
                    
                    {/* Hiển thị nút phù hợp với status */}
                    {productStatus === 'ACTIVE' ? (
                        // Khi đang ACTIVE, hiển thị nút Lưu trữ
                        <button
                            onClick={handleArchiveProduct}
                            disabled={loading}
                            className="flex items-center gap-2 px-4 py-2 bg-orange-600 text-white rounded-lg hover:bg-orange-700 disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                            <Package className="w-4 h-4" />
                            Lưu trữ
                        </button>
                    ) : (
                        // Khi đang DRAFT hoặc ARCHIVED, hiển thị nút Xuất bản
                        <div className="relative group">
                            <button
                                onClick={handlePublishProduct}
                                disabled={loading || variants.length === 0}
                                className="flex items-center gap-2 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed"
                                title={variants.length === 0 ? 'Sản phẩm phải có ít nhất 1 biến thể để xuất bản' : ''}
                            >
                                <Send className="w-4 h-4" />
                                Xuất bản
                            </button>
                            {variants.length === 0 && (
                                <div className="hidden group-hover:block absolute bottom-full mb-2 left-1/2 -translate-x-1/2 px-3 py-2 bg-gray-900 text-white text-sm rounded whitespace-nowrap z-10">
                                    Cần ít nhất 1 biến thể để xuất bản
                                    <div className="absolute top-full left-1/2 -translate-x-1/2 border-4 border-transparent border-t-gray-900"></div>
                                </div>
                            )}
                        </div>
                    )}
                </div>
            </div>

            {/* Tabs */}
            <div className="mb-6 border-b border-gray-200">
                <div className="flex gap-8">
                    <button
                        onClick={() => setActiveTab('basic')}
                        className={`pb-4 px-1 border-b-2 font-medium transition-colors ${
                            activeTab === 'basic'
                                ? 'border-red-600 text-red-600'
                                : 'border-transparent text-gray-500 hover:text-gray-700'
                        }`}
                    >
                        Thông tin cơ bản
                    </button>
                    <button
                        onClick={() => setActiveTab('variants')}
                        className={`pb-4 px-1 border-b-2 font-medium transition-colors ${
                            activeTab === 'variants'
                                ? 'border-red-600 text-red-600'
                                : 'border-transparent text-gray-500 hover:text-gray-700'
                        }`}
                    >
                        Biến thể ({variants.length})
                    </button>
                </div>
            </div>

            {/* Content */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {activeTab === 'basic' && (
                    <>
                        {/* Left Column - Basic Info */}
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
                                            if (errors.name) setErrors(prev => ({ ...prev, name: '' }));
                                        }}
                                        className={`w-full px-3 py-2 border rounded-md focus:ring-red-500 focus:border-red-500 ${
                                            errors.name ? 'border-red-500 bg-red-50' : 'border-gray-300'
                                        }`}
                                        placeholder="Nhập tên sản phẩm"
                                        disabled={loading}
                                    />
                                    {errors.name && <p className="text-xs text-red-500 mt-1">{errors.name}</p>}
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
                                            if (errors.slug) setErrors(prev => ({ ...prev, slug: '' }));
                                        }}
                                        className={`w-full px-3 py-2 border rounded-md focus:ring-red-500 focus:border-red-500 ${
                                            errors.slug ? 'border-red-500 bg-red-50' : 'border-gray-300'
                                        }`}
                                        placeholder="slug-san-pham"
                                        disabled={loading}
                                    />
                                    {errors.slug && <p className="text-xs text-red-500 mt-1">{errors.slug}</p>}
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Giá bán (VNĐ) <span className="text-red-500">*</span>
                                    </label>
                                    <input
                                        type="number"
                                        value={priceAmount}
                                        onChange={(e) => {
                                            setPriceAmount(e.target.value);
                                            if (errors.priceAmount) setErrors(prev => ({ ...prev, priceAmount: '' }));
                                        }}
                                        className={`w-full px-3 py-2 border rounded-md focus:ring-red-500 focus:border-red-500 ${
                                            errors.priceAmount ? 'border-red-500 bg-red-50' : 'border-gray-300'
                                        }`}
                                        placeholder="100000"
                                        min="0"
                                        disabled={loading}
                                    />
                                    {errors.priceAmount && <p className="text-xs text-red-500 mt-1">{errors.priceAmount}</p>}
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Mô tả
                                    </label>
                                    <textarea
                                        value={description}
                                        onChange={(e) => setDescription(e.target.value)}
                                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-red-500 focus:border-red-500"
                                        rows={4}
                                        placeholder="Mô tả chi tiết sản phẩm"
                                        disabled={loading}
                                    />
                                </div>

                                <div className="grid grid-cols-2 gap-4">
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-1">
                                            Chất liệu
                                        </label>
                                        <input
                                            type="text"
                                            value={material}
                                            onChange={(e) => setMaterial(e.target.value)}
                                            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-red-500 focus:border-red-500"
                                            placeholder="VD: Cotton, Polyester..."
                                            disabled={loading}
                                        />
                                    </div>

                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-1">
                                            Giới tính
                                        </label>
                                        <select
                                            value={gender}
                                            onChange={(e) => setGender(e.target.value as Gender)}
                                            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-red-500 focus:border-red-500"
                                            disabled={loading}
                                        >
                                            <option value="UNISEX">Unisex</option>
                                            <option value="MALE">Nam</option>
                                            <option value="FEMALE">Nữ</option>
                                            <option value="KIDS">Trẻ em</option>
                                        </select>
                                    </div>
                                </div>
                            </div>

                            {/* SEO */}
                            <div className="bg-white rounded-lg shadow p-6 space-y-4">
                                <h2 className="text-lg font-semibold">SEO</h2>
                                
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        SEO Title
                                    </label>
                                    <input
                                        type="text"
                                        value={seoTitle}
                                        onChange={(e) => setSeoTitle(e.target.value)}
                                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-red-500 focus:border-red-500"
                                        placeholder="Tiêu đề SEO"
                                        disabled={loading}
                                    />
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        SEO Description
                                    </label>
                                    <textarea
                                        value={seoDescription}
                                        onChange={(e) => setSeoDescription(e.target.value)}
                                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-red-500 focus:border-red-500"
                                        rows={3}
                                        placeholder="Mô tả SEO"
                                        disabled={loading}
                                    />
                                </div>
                            </div>
                        </div>

                        {/* Right Column - Images & Brand */}
                        <div className="space-y-6">
                            {/* Images */}
                            <div className="bg-white rounded-lg shadow p-6 space-y-4">
                                <h2 className="text-lg font-semibold">
                                    Ảnh sản phẩm <span className="text-red-500">*</span>
                                </h2>
                                
                                {/* Primary Image */}
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-2">Ảnh chính</label>
                                    {!primaryImage && !existingPrimaryImage && (
                                        <label className="flex flex-col items-center justify-center w-full h-48 border-2 border-dashed border-gray-300 rounded-lg cursor-pointer hover:border-red-500 transition-colors">
                                            <Upload className="w-10 h-10 text-gray-400 mb-2" />
                                            <p className="text-sm text-gray-500">Click để chọn ảnh chính</p>
                                            <input
                                                type="file"
                                                accept="image/*"
                                                className="hidden"
                                                onChange={handlePrimaryImageSelect}
                                            />
                                        </label>
                                    )}
                                    {(primaryImage || existingPrimaryImage) && (
                                        <div className="relative">
                                            <img
                                                src={primaryImage?.preview || existingPrimaryImage?.imageUrl}
                                                alt="Primary"
                                                className="w-full h-48 object-cover rounded-lg"
                                            />
                                            <button
                                                onClick={removePrimaryImage}
                                                className="absolute top-2 right-2 p-1 bg-red-600 text-white rounded-full hover:bg-red-700"
                                            >
                                                <X className="w-4 h-4" />
                                            </button>
                                        </div>
                                    )}
                                    {errors.primaryImage && <p className="text-xs text-red-500 mt-1">{errors.primaryImage}</p>}
                                </div>

                                {/* Gallery */}
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-2">Ảnh thư viện</label>
                                    <div className="grid grid-cols-2 gap-2 mb-2">
                                        {existingGalleryImages.map((img, idx) => (
                                            <div key={`existing-${idx}`} className="relative">
                                                <img
                                                    src={img.imageUrl}
                                                    alt={`Existing ${idx}`}
                                                    className="w-full h-32 object-cover rounded-lg"
                                                />
                                                <button
                                                    onClick={() => removeExistingGalleryImage(idx)}
                                                    className="absolute top-1 right-1 p-1 bg-red-600 text-white rounded-full hover:bg-red-700"
                                                >
                                                    <X className="w-3 h-3" />
                                                </button>
                                            </div>
                                        ))}
                                        {galleryImages.map((img, idx) => (
                                            <div key={`new-${idx}`} className="relative">
                                                <img
                                                    src={img.preview}
                                                    alt={`New ${idx}`}
                                                    className="w-full h-32 object-cover rounded-lg"
                                                />
                                                <button
                                                    onClick={() => removeGalleryImage(idx)}
                                                    className="absolute top-1 right-1 p-1 bg-red-600 text-white rounded-full hover:bg-red-700"
                                                >
                                                    <X className="w-3 h-3" />
                                                </button>
                                            </div>
                                        ))}
                                        <label className="flex flex-col items-center justify-center h-32 border-2 border-dashed border-gray-300 rounded-lg cursor-pointer hover:border-red-500 transition-colors">
                                            <ImageIcon className="w-8 h-8 text-gray-400" />
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
                                <select
                                    value={brandId}
                                    onChange={(e) => {
                                        setBrandId(e.target.value);
                                        if (errors.brand) setErrors(prev => ({ ...prev, brand: '' }));
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
                                {errors.brand && <p className="text-xs text-red-500 mt-1">{errors.brand}</p>}
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
                                        <span className="text-gray-400">Chọn danh mục sản phẩm</span>
                                    )}
                                </button>
                                
                                {/* Selected Categories List */}
                                {categoryIds.length > 0 && (
                                    <div className="flex flex-wrap gap-2 mt-2">
                                        {categoryIds.map(id => {
                                            const category = categories.find(c => c.id === id);
                                            return category ? (
                                                <span key={id} className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-800">
                                                    {category.name}
                                                    <button
                                                        type="button"
                                                        onClick={(e) => {
                                                            e.stopPropagation();
                                                            toggleCategory(id);
                                                        }}
                                                        className="ml-1.5 inline-flex items-center justify-center w-4 h-4 rounded-full text-red-400 hover:bg-red-200 hover:text-red-500 focus:outline-none"
                                                    >
                                                        <span className="sr-only">Remove {category.name}</span>
                                                        <X className="w-3 h-3" />
                                                    </button>
                                                </span>
                                            ) : null;
                                        })}
                                    </div>
                                )}
                            </div>
                        </div>
                    </>
                )}

                {activeTab === 'variants' && (
                    <div className="lg:col-span-3 space-y-6">
                        {/* Loading State */}
                        {loadingProduct && (
                            <div className="bg-white rounded-lg shadow p-6">
                                <p className="text-center text-gray-500">Đang tải thông tin biến thể...</p>
                            </div>
                        )}

                        {/* Variant Generator */}
                        {!loadingProduct && (
                            <div className="bg-white rounded-lg shadow p-6 space-y-4">
                                <h2 className="text-lg font-semibold">Tạo biến thể</h2>
                            
                            {/* Color & Size Selection Grid */}
                            <div className="grid grid-cols-2 gap-4">
                                {/* Colors */}
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-2">Màu sắc</label>
                                    <div className="max-h-48 overflow-y-auto border border-gray-300 rounded-md p-2 space-y-1">
                                        {colorsData.map(color => {
                                            const isLocked = lockedColors.includes(color.id);
                                            return (
                                                <label key={color.id} className="flex items-center gap-2 p-2 hover:bg-gray-50 rounded cursor-pointer">
                                                    <input
                                                        type="checkbox"
                                                        checked={selectedColors.includes(color.id)}
                                                        disabled={isLocked}
                                                        onChange={(e) => {
                                                            if (e.target.checked) {
                                                                setSelectedColors(prev => [...prev, color.id]);
                                                            } else {
                                                                if (!isLocked) {
                                                                    setSelectedColors(prev => prev.filter(id => id !== color.id));
                                                                }
                                                            }
                                                        }}
                                                        className="w-4 h-4 text-red-600 border-gray-300 rounded focus:ring-red-500"
                                                    />
                                                    <div
                                                        className="w-5 h-5 rounded border border-gray-300"
                                                        style={{ backgroundColor: color.hexCode || '#ccc' }}
                                                    />
                                                    <span className="text-sm">{color.name}</span>
                                                    {isLocked && (
                                                        <span className="ml-2 text-xs text-gray-400">(đang dùng)</span>
                                                    )}
                                                </label>
                                            );
                                        })}
                                    </div>
                                </div>

                                {/* Sizes */}
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-2">Kích thước</label>
                                    <div className="max-h-48 overflow-y-auto border border-gray-300 rounded-md p-2 space-y-1">
                                        {sizesData.map(size => {
                                            const isLocked = lockedSizes.includes(size.id);
                                            return (
                                                <label key={size.id} className="flex items-center gap-2 p-2 hover:bg-gray-50 rounded cursor-pointer">
                                                    <input
                                                        type="checkbox"
                                                        checked={selectedSizes.includes(size.id)}
                                                        disabled={isLocked}
                                                        onChange={(e) => {
                                                            if (e.target.checked) {
                                                                setSelectedSizes(prev => [...prev, size.id]);
                                                            } else {
                                                                if (!isLocked) {
                                                                    setSelectedSizes(prev => prev.filter(id => id !== size.id));
                                                                }
                                                            }
                                                        }}
                                                        className="w-4 h-4 text-red-600 border-gray-300 rounded focus:ring-red-500"
                                                    />
                                                    <span className="text-sm font-medium">{size.name || size.code}</span>
                                                    {isLocked && (
                                                        <span className="ml-2 text-xs text-gray-400">(đang dùng)</span>
                                                    )}
                                                </label>
                                            );
                                        })}
                                    </div>
                                </div>
                            </div>

                            <button
                                onClick={generateVariants}
                                className="w-full flex items-center justify-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 font-medium"
                            >
                                <Plus className="w-5 h-5" />
                                Tạo biến thể từ màu sắc và kích thước đã chọn
                            </button>
                            </div>
                        )}

                        {/* Bulk Apply */}
                        {!loadingProduct && variants.length > 0 && (
                            <div className="bg-white rounded-lg shadow p-6 space-y-4">
                                <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 space-y-3">
                                    <div className="flex items-center justify-between">
                                        <h3 className="text-sm font-semibold text-blue-900">Áp dụng hàng loạt cho tất cả biến thể</h3>
                                        <button
                                            onClick={applyBulkValues}
                                            className="px-4 py-1.5 bg-blue-600 text-white text-sm rounded-md hover:bg-blue-700 font-medium"
                                        >
                                            Áp dụng cho tất cả
                                        </button>
                                    </div>
                                    <div className="grid grid-cols-7 gap-3">
                                        <div>
                                            <label className="block text-xs font-medium text-gray-700 mb-1">SKU Prefix</label>
                                            <input
                                                type="text"
                                                value={bulkValues.barcode}
                                                onChange={(e) => setBulkValues(prev => ({ ...prev, barcode: e.target.value }))}
                                                className="w-full px-2 py-1.5 text-sm border border-gray-300 rounded-md"
                                                placeholder="Barcode"
                                            />
                                        </div>
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
                                            <label className="block text-xs font-medium text-gray-700 mb-1">Giá so sánh</label>
                                            <input
                                                type="number"
                                                value={bulkValues.compareAtAmount}
                                                onChange={(e) => setBulkValues(prev => ({ ...prev, compareAtAmount: e.target.value }))}
                                                className="w-full px-2 py-1.5 text-sm border border-gray-300 rounded-md"
                                                placeholder="0"
                                            />
                                        </div>
                                        <div>
                                            <label className="block text-xs font-medium text-gray-700 mb-1">Giá gốc</label>
                                            <input
                                                type="number"
                                                value={bulkValues.historyCost}
                                                onChange={(e) => setBulkValues(prev => ({ ...prev, historyCost: e.target.value }))}
                                                className="w-full px-2 py-1.5 text-sm border border-gray-300 rounded-md"
                                                placeholder="0"
                                            />
                                        </div>
                                        <div>
                                            <label className="block text-xs font-medium text-gray-700 mb-1">Trọng lượng (g)</label>
                                            <input
                                                type="number"
                                                value={bulkValues.weightGrams}
                                                onChange={(e) => setBulkValues(prev => ({ ...prev, weightGrams: e.target.value }))}
                                                className="w-full px-2 py-1.5 text-sm border border-gray-300 rounded-md"
                                                placeholder="200"
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
                                            <label className="block text-xs font-medium text-gray-700 mb-1">Reorder</label>
                                            <input
                                                type="number"
                                                value={bulkValues.reorderLevel}
                                                onChange={(e) => setBulkValues(prev => ({ ...prev, reorderLevel: e.target.value }))}
                                                className="w-full px-2 py-1.5 text-sm border border-gray-300 rounded-md"
                                                placeholder="10"
                                            />
                                        </div>
                                    </div>
                                </div>
                            </div>
                        )}

                        {/* Variants Table */}
                        {!loadingProduct && variants.length > 0 && (
                            <div className="bg-white rounded-lg shadow p-6 space-y-4">
                                <div className="flex items-center justify-between">
                                    <h2 className="text-lg font-semibold">Danh sách biến thể ({variants.length})</h2>
                                    <button
                                        onClick={handleSaveVariants}
                                        disabled={loading}
                                        className="flex items-center gap-2 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50"
                                    >
                                        <Save className="w-4 h-4" />
                                        Lưu tất cả biến thể
                                    </button>
                                </div>

                                <div className="overflow-x-auto">
                                    <table className="w-full text-sm">
                                        <thead className="bg-gray-50">
                                            <tr>
                                                <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">Ảnh</th>
                                                <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">Kích thước</th>
                                                <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">Màu sắc</th>
                                                <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">SKU</th>
                                                <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">Barcode</th>
                                                <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">Giá bán</th>
                                                <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">Giá so sánh</th>
                                                <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">Giá gốc</th>
                                                <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">Trọng lượng (g)</th>
                                                <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">Số lượng</th>
                                                <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">Reorder</th>
                                                <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">Thao tác</th>
                                            </tr>
                                        </thead>
                                        <tbody className="bg-white divide-y divide-gray-200">
                                            {variants.map((variant) => (
                                                <tr key={variant.tempId} className="hover:bg-gray-50">
                                                    <td className="px-3 py-3">
                                                        <div className="relative w-16 h-16">
                                                            {variant.imagePreview ? (
                                                                <div className="relative w-full h-full">
                                                                    <img
                                                                        src={variant.imagePreview}
                                                                        alt="Variant"
                                                                        className="w-full h-full object-cover rounded border"
                                                                    />
                                                                    <button
                                                                        onClick={() => removeVariantImage(variant.tempId)}
                                                                        className="absolute -top-1 -right-1 p-0.5 bg-red-600 text-white rounded-full hover:bg-red-700"
                                                                    >
                                                                        <X className="w-3 h-3" />
                                                                    </button>
                                                                </div>
                                                            ) : (
                                                                <label className="flex items-center justify-center w-full h-full border-2 border-dashed border-gray-300 rounded cursor-pointer hover:border-red-500">
                                                                    <ImageIcon className="w-6 h-6 text-gray-400" />
                                                                    <input
                                                                        type="file"
                                                                        accept="image/*"
                                                                        className="hidden"
                                                                        onChange={(e) => handleVariantImageSelect(variant.tempId, e)}
                                                                    />
                                                                </label>
                                                            )}
                                                        </div>
                                                    </td>
                                                    <td className="px-3 py-3">
                                                        <span className="font-medium text-gray-900">{variant.sizeName}</span>
                                                    </td>
                                                    <td className="px-3 py-3">
                                                        <span className="text-gray-900">{variant.colorName}</span>
                                                    </td>
                                                    <td className="px-3 py-3">
                                                        <input
                                                            type="text"
                                                            value={variant.sku}
                                                            onChange={(e) => updateVariantField(variant.tempId, 'sku', e.target.value)}
                                                            className="w-32 px-2 py-1.5 text-sm border border-gray-300 rounded focus:ring-red-500 focus:border-red-500"
                                                            placeholder="SKU"
                                                        />
                                                    </td>
                                                    <td className="px-3 py-3">
                                                        <input
                                                            type="text"
                                                            value={variant.barcode}
                                                            onChange={(e) => updateVariantField(variant.tempId, 'barcode', e.target.value)}
                                                            className="w-32 px-2 py-1.5 text-sm border border-gray-300 rounded focus:ring-red-500 focus:border-red-500"
                                                            placeholder="Barcode"
                                                        />
                                                    </td>
                                                    <td className="px-3 py-3">
                                                        <input
                                                            type="number"
                                                            value={variant.priceAmount}
                                                            onChange={(e) => updateVariantField(variant.tempId, 'priceAmount', e.target.value)}
                                                            className="w-24 px-2 py-1.5 text-sm border border-gray-300 rounded focus:ring-red-500 focus:border-red-500"
                                                            placeholder="0"
                                                        />
                                                    </td>
                                                    <td className="px-3 py-3">
                                                        <input
                                                            type="number"
                                                            value={variant.compareAtAmount}
                                                            onChange={(e) => updateVariantField(variant.tempId, 'compareAtAmount', e.target.value)}
                                                            className="w-24 px-2 py-1.5 text-sm border border-gray-300 rounded focus:ring-red-500 focus:border-red-500"
                                                            placeholder="0"
                                                        />
                                                    </td>
                                                    <td className="px-3 py-3">
                                                        <input
                                                            type="number"
                                                            value={variant.historyCost}
                                                            onChange={(e) => updateVariantField(variant.tempId, 'historyCost', e.target.value)}
                                                            className="w-24 px-2 py-1.5 text-sm border border-gray-300 rounded focus:ring-red-500 focus:border-red-500"
                                                            placeholder="0"
                                                        />
                                                    </td>
                                                    <td className="px-3 py-3">
                                                        <input
                                                            type="number"
                                                            value={variant.weightGrams}
                                                            onChange={(e) => updateVariantField(variant.tempId, 'weightGrams', e.target.value)}
                                                            className="w-20 px-2 py-1.5 text-sm border border-gray-300 rounded focus:ring-red-500 focus:border-red-500"
                                                            placeholder="200"
                                                        />
                                                    </td>
                                                    <td className="px-3 py-3">
                                                        <input
                                                            type="number"
                                                            value={variant.quantityOnHand}
                                                            onChange={(e) => updateVariantField(variant.tempId, 'quantityOnHand', e.target.value)}
                                                            className="w-20 px-2 py-1.5 text-sm border border-gray-300 rounded focus:ring-red-500 focus:border-red-500"
                                                            placeholder="0"
                                                        />
                                                    </td>
                                                    <td className="px-3 py-3">
                                                        <input
                                                            type="number"
                                                            value={variant.reorderLevel}
                                                            onChange={(e) => updateVariantField(variant.tempId, 'reorderLevel', e.target.value)}
                                                            className="w-20 px-2 py-1.5 text-sm border border-gray-300 rounded focus:ring-red-500 focus:border-red-500"
                                                            placeholder="10"
                                                        />
                                                    </td>
                                                    <td className="px-3 py-3">
                                                        <button
                                                            onClick={() => handleDeleteVariant(variant.tempId)}
                                                            className="p-1.5 text-red-600 hover:bg-red-50 rounded transition-colors"
                                                            title="Xóa biến thể"
                                                        >
                                                            <X className="w-5 h-5" />
                                                        </button>
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        )}

                        {/* No variants message */}
                        {!loadingProduct && variants.length === 0 && (
                            <div className="bg-white rounded-lg shadow p-6">
                                <div className="text-center text-gray-500 py-8">
                                    <p className="text-lg font-medium mb-2">Chưa có biến thể nào</p>
                                    <p className="text-sm">Chọn kích thước và màu sắc ở trên, sau đó nhấn "Tạo biến thể"</p>
                                </div>
                            </div>
                        )}
                    </div>
                )}
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

export default ProductEdit;
