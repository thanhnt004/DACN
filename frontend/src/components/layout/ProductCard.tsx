import { Link } from 'react-router-dom'
import { Heart } from 'lucide-react'
import { useState } from 'react'

type ProductCardProps = {
    id: string
    slug: string
    colors?: string[]
    ratingAvg?: number
    imageUrl?: string
    name: string
    gender?: string
    priceAmount: number
    ageRange?: string
    onFavoriteClick?: (id: string) => void
    isFavorite?: boolean
}

export default function ProductCard({
    id,
    slug,
    colors = [],
    ratingAvg,
    imageUrl,
    name,
    gender,
    priceAmount,
    ageRange,
    onFavoriteClick,
    isFavorite = false
}: ProductCardProps) {
    const [isFavorited, setIsFavorited] = useState(isFavorite)

    const handleFavoriteClick = (e: React.MouseEvent) => {
        e.preventDefault()
        e.stopPropagation()
        setIsFavorited(!isFavorited)
        if (onFavoriteClick) {
            onFavoriteClick(id)
        }
    }

    const formatPrice = (price: number) => {
        return new Intl.NumberFormat('vi-VN', {
            style: 'currency',
            currency: 'VND'
        }).format(price)
    }

    const getGenderLabel = (genderValue?: string) => {
        switch (genderValue?.toLowerCase()) {
            case 'men':
                return 'NAM'
            case 'women':
                return 'NỮ'
            case 'unisex':
                return 'UNISEX'
            default:
                return 'TRẺ EM'
        }
    }

    return (
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

                    {/* Favorite Button */}
                    <button
                        onClick={handleFavoriteClick}
                        className="absolute top-3 right-3 rounded-full bg-white p-2 shadow-md transition-all hover:scale-110"
                        aria-label="Yêu thích"
                    >
                        <Heart
                            size={20}
                            className={`transition-colors ${isFavorited ? 'fill-red-500 text-red-500' : 'text-gray-600'
                                }`}
                        />
                    </button>
                </div>

                {/* Content */}
                <div className="p-4 space-y-2">
                    {/* Gender & Age Range */}
                    <div className="flex items-center gap-2 text-xs font-semibold text-gray-600">
                        <span className="uppercase">{getGenderLabel(gender)}</span>
                        {ageRange && (
                            <>
                                <span>•</span>
                                <span>{ageRange}</span>
                            </>
                        )}
                    </div>

                    {/* Product Name */}
                    <h3 className="text-sm font-medium text-gray-900 line-clamp-2 min-h-[2.5rem]">
                        {name}
                    </h3>

                    {/* Price */}
                    <div className="text-base font-bold text-gray-900">
                        {formatPrice(priceAmount)}
                    </div>

                    {/* Rating */}
                    {ratingAvg !== undefined && ratingAvg > 0 && (
                        <div className="flex items-center gap-1">
                            <span className="text-yellow-500">★</span>
                            <span className="text-sm font-medium text-gray-900">
                                {ratingAvg.toFixed(1)}
                            </span>
                            <span className="text-sm text-gray-500">
                                ({Math.floor(Math.random() * 50 + 10)})
                            </span>
                        </div>
                    )}

                    {/* Color Options */}
                    {colors && Array.isArray(colors) && colors.length > 0 && (
                        <div className="flex items-center gap-1.5 pt-1">
                            {colors.slice(0, 4).map((color, index) => (
                                <div
                                    key={index}
                                    className="h-5 w-5 rounded-full border border-gray-300"
                                    style={{ backgroundColor: color }}
                                    title={color}
                                />
                            ))}
                            {colors.length > 4 && (
                                <span className="text-xs text-gray-500">+{colors.length - 4}</span>
                            )}
                        </div>
                    )}
                </div>
            </div>
        </Link>
    )
}
