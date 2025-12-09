import { Link } from 'react-router-dom'
import { ShoppingCart } from 'lucide-react'
import { useState } from 'react'
import { createPortal } from 'react-dom'
import QuickAddModal from '../QuickAddModal'

type ProductCardProps = {
    id: string
    slug: string
    colors?: string[]
    sizes?: string[]
    ratingAvg?: number
    imageUrl?: string
    name: string
    gender?: string
    priceAmount: number
    ageRange?: string
    isInStock?: boolean
}

export default function ProductCard({
    id,
    slug,
    colors = [],
    sizes = [],
    ratingAvg,
    imageUrl,
    name,
    gender,
    priceAmount,
    ageRange,
    isInStock = true
}: ProductCardProps) {
    const [showQuickAdd, setShowQuickAdd] = useState(false)

    const formatPrice = (price: number) => {
        return new Intl.NumberFormat('vi-VN', {
            style: 'currency',
            currency: 'VND'
        }).format(price)
    }

    const handleQuickAdd = (e: React.MouseEvent) => {
        e.preventDefault()
        e.stopPropagation()
        setShowQuickAdd(true)
    }

    return (
        <>
        <Link
            to={`/products/${slug}`}
            className="group block"
        >
            <div className="relative rounded-lg border-2 border-black overflow-hidden bg-white transition-shadow hover:shadow-lg">
                {/* Image Container */}
                <div className="relative aspect-[3/4] overflow-hidden bg-gray-100">
                    {imageUrl ? (
                        <img
                            src={imageUrl}
                            alt={name}
                            className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
                        />
                    ) : (
                        <div className="flex h-full w-full items-center justify-center text-gray-400">
                            <span className="text-sm">Không có ảnh</span>
                        </div>
                    )}

                    {/* Out of Stock Badge */}
                    {!isInStock && (
                        <div className="absolute inset-0 bg-black bg-opacity-40 flex items-center justify-center">
                            <span className="bg-red-600 text-white px-4 py-2 rounded-lg font-semibold text-sm">
                                Hết hàng
                            </span>
                        </div>
                    )}

                    {/* Quick Add Button Icon - Bottom right */}
                    {isInStock && (
                        <button
                            onClick={handleQuickAdd}
                            className="absolute bottom-3 right-3 !w-14 !h-14 bg-white !rounded-full shadow-lg border-2 border-gray-200 !flex !items-center !justify-center !p-0 transition-all duration-200 hover:bg-red-600 hover:border-red-600 hover:scale-110 z-10 group/cart cursor-pointer"
                            title="Thêm vào giỏ hàng"
                            aria-label="Thêm vào giỏ hàng"
                            type="button"
                        >
                            <ShoppingCart 
                                className="!w-6 !h-6 text-gray-900 transition-colors group-hover/cart:text-white" 
                                strokeWidth={2.5}
                            />
                        </button>
                    )}
                </div>

                {/* Content */}
                <div className="p-3 space-y-1.5">
                    {/* Product Name */}
                    <h3 className="text-sm font-medium text-gray-900 line-clamp-2 h-10 leading-5">
                        {name}
                    </h3>

                    {/* Price */}
                    <div className="text-base font-bold text-red-600">
                        {formatPrice(priceAmount)}
                    </div>

                    {/* Sizes */}
                    {sizes && sizes.length > 0 && (
                        <div className="flex items-center gap-1 flex-wrap">
                            <span className="text-xs text-gray-500">Size:</span>
                            {sizes.slice(0, 5).map((size, index) => (
                                <span
                                    key={index}
                                    className="text-xs px-1.5 py-0.5 border border-gray-300 rounded text-gray-700"
                                >
                                    {size}
                                </span>
                            ))}
                            {sizes.length > 5 && (
                                <span className="text-xs text-gray-500">+{sizes.length - 5}</span>
                            )}
                        </div>
                    )}

                    {/* Colors */}
                    {colors && colors.length > 0 && (
                        <div className="flex items-center gap-1">
                            <span className="text-xs text-gray-500">Màu:</span>
                            <div className="flex items-center gap-1">
                                {colors.slice(0, 5).map((color, index) => (
                                    <div
                                        key={index}
                                        className="h-4 w-4 rounded-full border border-gray-300"
                                        style={{ backgroundColor: color }}
                                        title={color}
                                    />
                                ))}
                                {colors.length > 5 && (
                                    <span className="text-xs text-gray-500">+{colors.length - 5}</span>
                                )}
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </Link>
        
        {/* Quick Add Modal - Rendered outside Link using Portal */}
        {showQuickAdd && createPortal(
            <QuickAddModal
                productId={id}
                onClose={() => setShowQuickAdd(false)}
            />,
            document.body
        )}
    </>
    )
}
