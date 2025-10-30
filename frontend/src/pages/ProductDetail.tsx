import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import * as ProductsApi from '../api/admin/products'
import { Header } from '../components/layout/Header'
import Footer from '../components/layout/Footer'
import { ChevronRight, Share2, Minus, Plus, Package, Truck, RotateCcw } from 'lucide-react'
import { useCartStore } from '../store/cart'

export default function ProductDetail() {
    const { slug } = useParams()
    const [product, setProduct] = useState<ProductsApi.ProductDetailResponse | null>(null)
    const [loading, setLoading] = useState(false)
    const [selectedImage, setSelectedImage] = useState(0)
    const [selectedSize, setSelectedSize] = useState<string>('')
    const [selectedColor, setSelectedColor] = useState<string>('')
    const [quantity, setQuantity] = useState(1)
    const [expandedSections, setExpandedSections] = useState({
        description: false,
        material: false,
        guide: false
    })
    const [addingToCart, setAddingToCart] = useState(false)

    const { addToCart, fetchCart } = useCartStore()

    useEffect(() => {
        if (!slug) return
        let cancelled = false
        const load = async () => {
            setLoading(true)
            try {
                const res = await ProductsApi.getProductDetail(slug, ['images', 'variants', 'options'])
                if (!cancelled) {
                    setProduct(res)
                    // Auto-select first available size and color
                    if (res.options?.size && res.options.size.length > 0) {
                        const firstAvailableSize = res.options.size.find(size =>
                            res.variants?.some(v => v.sizeId === size.id && v.inventory?.available && v.inventory.available > 0)
                        )
                        if (firstAvailableSize) {
                            setSelectedSize(firstAvailableSize.id)
                        }
                    }
                    if (res.options?.color && res.options.color.length > 0) {
                        const firstAvailableColor = res.options.color.find(color =>
                            res.variants?.some(v => v.colorId === color.id && v.inventory?.available && v.inventory.available > 0)
                        )
                        if (firstAvailableColor) {
                            setSelectedColor(firstAvailableColor.id)
                        }
                    }
                }
            } catch (err) {
                console.error(err)
            } finally {
                if (!cancelled) setLoading(false)
            }
        }
        load()
        return () => { cancelled = true }
    }, [slug])

    // Reset quantity when variant changes
    useEffect(() => {
        const variant = getSelectedVariant()
        if (variant && variant.inventory?.available) {
            // If current quantity exceeds available stock, reset to max available
            setQuantity(prev => {
                if (prev > variant.inventory!.available!) {
                    return variant.inventory!.available!
                }
                return prev
            })
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [selectedSize, selectedColor])


    const handleQuantityChange = (delta: number) => {
        const variant = getSelectedVariant()
        const maxQuantity = variant?.inventory?.available || 99
        setQuantity(prev => Math.max(1, Math.min(maxQuantity, prev + delta)))
    }

    const toggleSection = (section: keyof typeof expandedSections) => {
        setExpandedSections(prev => ({ ...prev, [section]: !prev[section] }))
    }

    const calculateDiscount = () => {
        if (!product) return null
        // Assuming compareAtAmount is the original price
        const variant = product.variants?.find(v => v.sizeId === selectedSize && v.colorId === selectedColor)
        if (variant?.compareAtAmount && variant.compareAtAmount > variant.priceAmount) {
            const discount = Math.round(((variant.compareAtAmount - variant.priceAmount) / variant.compareAtAmount) * 100)
            return { original: variant.compareAtAmount, discount }
        }
        return null
    }

    const getCurrentPrice = () => {
        if (!product) return 0
        const variant = product.variants?.find(v => v.sizeId === selectedSize && v.colorId === selectedColor)
        return variant?.priceAmount || product.priceAmount
    }

    const getSelectedVariant = () => {
        if (!product || !product.variants) return null
        return product.variants.find(v => v.sizeId === selectedSize && v.colorId === selectedColor)
    }

    // Check if a specific variant is available
    const isVariantAvailable = (sizeId: string, colorId: string) => {
        const variant = product?.variants?.find(v => v.sizeId === sizeId && v.colorId === colorId)
        return variant?.inventory?.available && variant.inventory.available > 0
    }

    const handleAddToCart = async () => {
        // Validate selection
        if (!selectedSize || !selectedColor) {
            alert('Vui lòng chọn size và màu sắc')
            return
        }

        const variant = getSelectedVariant()
        if (!variant) {
            alert('Không tìm thấy biến thể sản phẩm')
            return
        }

        // Check if variant is available
        if (!variant.inventory?.available || variant.inventory.available < quantity) {
            alert('Sản phẩm không đủ số lượng trong kho')
            return
        }

        setAddingToCart(true)
        try {
            await addToCart(variant.id, quantity)
            alert('Đã thêm sản phẩm vào giỏ hàng!')
            // Optionally fetch cart to update count in header
            await fetchCart()
        } catch (error) {
            console.error('Error adding to cart:', error)
            alert('Có lỗi khi thêm vào giỏ hàng. Vui lòng thử lại.')
        } finally {
            setAddingToCart(false)
        }
    }

    const handleBuyNow = async () => {
        // Validate selection
        if (!selectedSize || !selectedColor) {
            alert('Vui lòng chọn size và màu sắc')
            return
        }

        const variant = getSelectedVariant()
        if (!variant) {
            alert('Không tìm thấy biến thể sản phẩm')
            return
        }

        // Check if variant is available
        if (!variant.inventory?.available || variant.inventory.available < quantity) {
            alert('Sản phẩm không đủ số lượng trong kho')
            return
        }

        // Add to cart first, then redirect to checkout
        setAddingToCart(true)
        try {
            await addToCart(variant.id, quantity)
            await fetchCart()
            // Redirect to checkout page
            window.location.href = '/checkout'
        } catch (error) {
            console.error('Error adding to cart:', error)
            alert('Có lỗi khi thêm vào giỏ hàng. Vui lòng thử lại.')
            setAddingToCart(false)
        }
    }

    if (loading) return (
        <div>
            <Header />
            <div className="flex items-center justify-center min-h-screen">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-red-600"></div>
            </div>
        </div>
    )

    if (!product) return (
        <div>
            <Header />
            <div className="max-w-7xl mx-auto px-4 py-12">
                <div className="text-center">
                    <h2 className="text-2xl font-bold text-gray-900 mb-2">Sản phẩm không tồn tại</h2>
                    <Link to="/products" className="text-red-600 hover:underline">
                        ← Quay lại danh sách sản phẩm
                    </Link>
                </div>
            </div>
            <Footer />
        </div>
    )

    const discount = calculateDiscount()
    const currentPrice = getCurrentPrice()
    const images = product.images || []
    const mainImage = images[selectedImage]?.imageUrl || (images[0]?.imageUrl ?? '')

    return (
        <div className="min-h-screen flex flex-col">
            <Header />

            {/* Breadcrumb */}
            <div className="bg-white border-b">
                <div className="max-w-7xl mx-auto px-4 py-3">
                    <div className="flex items-center gap-2 text-sm text-gray-600">
                        <Link to="/" className="hover:text-red-600">Trang chủ</Link>
                        <ChevronRight className="w-4 h-4" />
                        <Link to="/products" className="hover:text-red-600">Sản phẩm</Link>
                        {product.categories && product.categories.length > 0 && (
                            <>
                                <ChevronRight className="w-4 h-4" />
                                <Link
                                    to={`/products?category=${product.categories[0].slug}`}
                                    className="hover:text-red-600"
                                >
                                    {product.categories[0].name}
                                </Link>
                            </>
                        )}
                        <ChevronRight className="w-4 h-4" />
                        <span className="text-gray-900 font-medium">{product.name}</span>
                    </div>
                </div>
            </div>

            <div className="flex-1 bg-gray-50">
                <div className="max-w-7xl mx-auto px-4 py-8">
                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                        {/* Left Column - Images */}
                        <div className="space-y-4">
                            {/* Main Image */}
                            <div className="bg-white rounded-lg overflow-hidden relative aspect-square">
                                <img
                                    src={mainImage}
                                    alt={product.name}
                                    className="w-full h-full object-cover"
                                />
                                <div className="absolute bottom-4 right-4 bg-black/50 text-white px-3 py-1 rounded text-sm">
                                    {selectedImage + 1}/{images.length}
                                </div>
                            </div>

                            {/* Thumbnail Gallery */}
                            {images.length > 1 && (
                                <div className="grid grid-cols-6 gap-2">
                                    {images.map((img, idx) => (
                                        <button
                                            key={img.id}
                                            onClick={() => setSelectedImage(idx)}
                                            className={`aspect-square rounded-lg overflow-hidden border-2 transition ${selectedImage === idx
                                                    ? 'border-red-600'
                                                    : 'border-gray-200 hover:border-gray-400'
                                                }`}
                                        >
                                            <img
                                                src={img.imageUrl}
                                                alt={`${product.name} ${idx + 1}`}
                                                className="w-full h-full object-cover"
                                            />
                                        </button>
                                    ))}
                                </div>
                            )}
                        </div>

                        {/* Right Column - Product Info */}
                        <div className="space-y-6">
                            {/* Title and SKU */}
                            <div>
                                <h1 className="text-2xl lg:text-3xl font-bold text-gray-900 mb-2">
                                    {product.name}
                                </h1>
                                <div className="flex items-center gap-3">
                                    <span className="text-sm text-gray-600">SKU:</span>
                                    <span className="text-sm font-medium text-gray-900">{product.variants?.[0]?.sku || 'N/A'}</span>
                                    <button className="text-sm text-gray-500 hover:text-gray-700">
                                        <Share2 className="w-4 h-4 inline mr-1" />
                                        Copy
                                    </button>
                                </div>
                            </div>

                            {/* Price */}
                            <div className="border-t pt-4">
                                {discount && (
                                    <div className="flex items-center gap-3 mb-2">
                                        <span className="text-lg text-gray-400 line-through">
                                            {discount.original.toLocaleString('vi-VN')} ₫
                                        </span>
                                        <span className="bg-red-600 text-white text-sm px-2 py-1 rounded">
                                            -{discount.discount}%
                                        </span>
                                    </div>
                                )}
                                <div className="text-3xl font-bold text-red-600">
                                    {currentPrice.toLocaleString('vi-VN')} ₫
                                </div>
                            </div>

                            {/* Color Selection */}
                            {product.options?.color && product.options.color.length > 0 && (
                                <div>
                                    <div className="flex items-center justify-between mb-3">
                                        <label className="text-sm font-medium text-gray-900">
                                            Màu sắc:
                                            <span className="ml-2 text-gray-600">
                                                {product.options.color.find(c => c.id === selectedColor)?.name || ''}
                                            </span>
                                        </label>
                                        <button className="text-sm text-red-600 hover:underline flex items-center gap-1">
                                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
                                            </svg>
                                            Gợi ý tìm kích cỡ
                                        </button>
                                    </div>
                                    <div className="flex gap-2">
                                        {product.options.color.map(color => {
                                            // Check if this color has any available variants with selected size
                                            const isAvailable = selectedSize ? isVariantAvailable(selectedSize, color.id) : true
                                            return (
                                                <button
                                                    key={color.id}
                                                    onClick={() => isAvailable && setSelectedColor(color.id)}
                                                    disabled={!isAvailable}
                                                    className={`relative w-12 h-12 rounded-lg border-2 transition ${selectedColor === color.id
                                                            ? 'border-red-600'
                                                            : isAvailable
                                                                ? 'border-gray-300 hover:border-gray-400'
                                                                : 'border-gray-200 opacity-40 cursor-not-allowed'
                                                        }`}
                                                    title={isAvailable ? color.name : `${color.name} (Hết hàng)`}
                                                >
                                                    <div
                                                        className="w-full h-full rounded-md"
                                                        style={{ backgroundColor: color.hexCode }}
                                                    />
                                                    {!isAvailable && (
                                                        <div className="absolute inset-0 flex items-center justify-center">
                                                            <div className="w-full h-0.5 bg-gray-400 rotate-45" />
                                                        </div>
                                                    )}
                                                </button>
                                            )
                                        })}
                                    </div>
                                </div>
                            )}

                            {/* Size Selection */}
                            {product.options?.size && product.options.size.length > 0 && (
                                <div>
                                    <label className="block text-sm font-medium text-gray-900 mb-3">
                                        Kích cỡ:
                                    </label>
                                    <div className="flex gap-2 flex-wrap">
                                        {product.options.size.map(size => {
                                            // Check if this size has any available variants with selected color
                                            const isAvailable = selectedColor ? isVariantAvailable(size.id, selectedColor) : true
                                            return (
                                                <button
                                                    key={size.id}
                                                    onClick={() => isAvailable && setSelectedSize(size.id)}
                                                    disabled={!isAvailable}
                                                    className={`px-6 py-3 rounded-lg border-2 transition font-medium ${selectedSize === size.id
                                                            ? 'border-red-600 bg-red-50 text-red-600'
                                                            : isAvailable
                                                                ? 'border-gray-300 hover:border-gray-400 text-gray-900'
                                                                : 'border-gray-200 text-gray-400 opacity-50 cursor-not-allowed line-through'
                                                        }`}
                                                    title={isAvailable ? size.code : `${size.code} (Hết hàng)`}
                                                >
                                                    {size.code}
                                                </button>
                                            )
                                        })}
                                    </div>
                                </div>
                            )}

                            {/* Quantity & Add to Cart */}
                            <div className="border-t pt-6 space-y-4">
                                <div className="flex items-center gap-4">
                                    <div className="flex items-center border-2 border-gray-300 rounded-lg">
                                        <button
                                            onClick={() => handleQuantityChange(-1)}
                                            disabled={quantity <= 1}
                                            className="px-4 py-3 hover:bg-gray-100 transition disabled:opacity-50 disabled:cursor-not-allowed"
                                        >
                                            <Minus className="w-4 h-4" />
                                        </button>
                                        <input
                                            type="number"
                                            value={quantity}
                                            onChange={(e) => {
                                                const variant = getSelectedVariant()
                                                const maxQuantity = variant?.inventory?.available || 99
                                                const newVal = Math.max(1, Math.min(maxQuantity, parseInt(e.target.value) || 1))
                                                setQuantity(newVal)
                                            }}
                                            className="w-16 text-center border-x-2 border-gray-300 py-3 focus:outline-none"
                                        />
                                        <button
                                            onClick={() => handleQuantityChange(1)}
                                            disabled={(() => {
                                                const variant = getSelectedVariant()
                                                const maxQuantity = variant?.inventory?.available || 99
                                                return quantity >= maxQuantity
                                            })()}
                                            className="px-4 py-3 hover:bg-gray-100 transition disabled:opacity-50 disabled:cursor-not-allowed"
                                        >
                                            <Plus className="w-4 h-4" />
                                        </button>
                                    </div>
                                    {(() => {
                                        const variant = getSelectedVariant()
                                        const available = variant?.inventory?.available
                                        if (available !== undefined && available < 10 && available > 0) {
                                            return (
                                                <span className="text-sm text-orange-600">
                                                    Chỉ còn {available} sản phẩm
                                                </span>
                                            )
                                        }
                                        return null
                                    })()}
                                </div>

                                <div className="flex gap-3">
                                    <button
                                        onClick={handleAddToCart}
                                        disabled={addingToCart || !selectedSize || !selectedColor}
                                        className="flex-1 bg-red-600 text-white py-4 rounded-lg font-medium hover:bg-red-700 transition disabled:opacity-50 disabled:cursor-not-allowed"
                                    >
                                        {addingToCart ? 'ĐANG THÊM...' : 'THÊM VÀO GIỎ HÀNG'}
                                    </button>
                                    <button
                                        onClick={handleBuyNow}
                                        disabled={addingToCart || !selectedSize || !selectedColor}
                                        className="px-6 py-4 border-2 border-red-600 text-red-600 rounded-lg font-medium hover:bg-red-50 transition disabled:opacity-50 disabled:cursor-not-allowed"
                                    >
                                        MUA NGAY
                                    </button>
                                </div>
                            </div>

                            {/* Shipping & Return Info */}
                            <div className="bg-white rounded-lg p-4 space-y-3">
                                <div className="flex items-start gap-3">
                                    <Package className="w-5 h-5 text-gray-600 mt-0.5" />
                                    <div>
                                        <div className="font-medium text-gray-900">Thanh toán khi nhận hàng (COD)</div>
                                        <div className="text-sm text-gray-600">Giao hàng toàn quốc.</div>
                                    </div>
                                </div>
                                <div className="flex items-start gap-3">
                                    <Truck className="w-5 h-5 text-gray-600 mt-0.5" />
                                    <div>
                                        <div className="font-medium text-gray-900">Miễn phí giao hàng</div>
                                        <div className="text-sm text-gray-600">Với đơn hàng trên 599.000 ₫</div>
                                    </div>
                                </div>
                                <div className="flex items-start gap-3">
                                    <RotateCcw className="w-5 h-5 text-gray-600 mt-0.5" />
                                    <div>
                                        <div className="font-medium text-gray-900">Đổi hàng miễn phí</div>
                                        <div className="text-sm text-gray-600">Trong 30 ngày kể từ ngày mua.</div>
                                    </div>
                                </div>
                            </div>

                            {/* Expandable Sections */}
                            <div className="space-y-2">
                                {/* Description */}
                                <div className="border-b">
                                    <button
                                        onClick={() => toggleSection('description')}
                                        className="w-full flex items-center justify-between py-4 text-left"
                                    >
                                        <span className="font-medium text-gray-900">Mô tả</span>
                                        <Plus className={`w-5 h-5 transition-transform ${expandedSections.description ? 'rotate-45' : ''}`} />
                                    </button>
                                    {expandedSections.description && (
                                        <div className="pb-4 text-gray-600 text-sm leading-relaxed">
                                            {product.description || 'Chưa có mô tả'}
                                        </div>
                                    )}
                                </div>

                                {/* Material */}
                                <div className="border-b">
                                    <button
                                        onClick={() => toggleSection('material')}
                                        className="w-full flex items-center justify-between py-4 text-left"
                                    >
                                        <span className="font-medium text-gray-900">Chất liệu</span>
                                        <Plus className={`w-5 h-5 transition-transform ${expandedSections.material ? 'rotate-45' : ''}`} />
                                    </button>
                                    {expandedSections.material && (
                                        <div className="pb-4 text-gray-600 text-sm leading-relaxed">
                                            {product.material || 'Chưa cập nhật thông tin chất liệu'}
                                        </div>
                                    )}
                                </div>

                                {/* Usage Guide */}
                                <div className="border-b">
                                    <button
                                        onClick={() => toggleSection('guide')}
                                        className="w-full flex items-center justify-between py-4 text-left"
                                    >
                                        <span className="font-medium text-gray-900">Hướng dẫn sử dụng</span>
                                        <Plus className={`w-5 h-5 transition-transform ${expandedSections.guide ? 'rotate-45' : ''}`} />
                                    </button>
                                    {expandedSections.guide && (
                                        <div className="pb-4 text-gray-600 text-sm leading-relaxed">
                                            <p>• Giặt máy ở chế độ nhẹ, nhiệt độ thường.</p>
                                            <p>• Không sử dụng thuốc tẩy.</p>
                                            <p>• Phơi trong bóng mát.</p>
                                            <p>• Ủi ở nhiệt độ thấp.</p>
                                        </div>
                                    )}
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <Footer />
        </div>
    )
}
