import { useState, useEffect } from 'react'
import { ChevronDown, ChevronUp } from 'lucide-react'
import * as CatalogApi from '../api/admin/catalog'

interface FilterSidebarProps {
    filters: ProductFilters
    onFilterChange: (filters: ProductFilters) => void
}

export interface ProductFilters {
    brandId?: string
    minPriceAmount?: number
    maxPriceAmount?: number
    sizeIds?: string[]
    colorIds?: string[]
}

export default function ProductFilterSidebar({ filters, onFilterChange }: FilterSidebarProps) {
    const [sizes, setSizes] = useState<CatalogApi.SizeDto[]>([])
    const [colors, setColors] = useState<CatalogApi.ColorDto[]>([])

    const [expandedSections, setExpandedSections] = useState({
        price: true,
        size: true,
        color: true
    })

    // Price range state
    const [priceRange, setPriceRange] = useState({
        min: filters.minPriceAmount || 29000,
        max: filters.maxPriceAmount || 1499000
    })

    useEffect(() => {
        loadFilterData()
    }, [])

    const loadFilterData = async () => {
        try {
            const [sizesRes, colorsRes] = await Promise.all([
                CatalogApi.getSizes(),
                CatalogApi.getColors()
            ])
            setSizes(sizesRes || [])
            setColors(colorsRes || [])
        } catch (error) {
            console.error('Failed to load filter data:', error)
        }
    }

    const toggleSection = (section: keyof typeof expandedSections) => {
        setExpandedSections(prev => ({ ...prev, [section]: !prev[section] }))
    }

    const handleSizeChange = (sizeId: string) => {
        const currentSizes = filters.sizeIds || []
        const newSizes = currentSizes.includes(sizeId)
            ? currentSizes.filter(s => s !== sizeId)
            : [...currentSizes, sizeId]
        onFilterChange({ ...filters, sizeIds: newSizes.length > 0 ? newSizes : undefined })
    }

    const handleColorChange = (colorId: string) => {
        const currentColors = filters.colorIds || []
        const newColors = currentColors.includes(colorId)
            ? currentColors.filter(c => c !== colorId)
            : [...currentColors, colorId]
        onFilterChange({ ...filters, colorIds: newColors.length > 0 ? newColors : undefined })
    }

    const handlePriceRangeChange = () => {
        onFilterChange({
            ...filters,
            minPriceAmount: priceRange.min,
            maxPriceAmount: priceRange.max
        })
    }

    return (
        <div className="w-full bg-white rounded-lg border border-gray-200 p-4">
            {/* Price Range Filter */}
            <div className="border-b border-gray-200 pb-4 mb-4">
                <button
                    onClick={() => toggleSection('price')}
                    className="flex items-center justify-between w-full text-left"
                >
                    <h3 className="font-semibold text-gray-900">Khoảng giá</h3>
                    {expandedSections.price ? <ChevronUp size={20} /> : <ChevronDown size={20} />}
                </button>
                {expandedSections.price && (
                    <div className="mt-4 space-y-4">
                        <div className="flex items-center gap-2">
                            <input
                                type="number"
                                value={priceRange.min}
                                onChange={(e) => setPriceRange({ ...priceRange, min: Number(e.target.value) })}
                                className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
                                placeholder="Từ"
                            />
                            <span className="text-gray-500">—</span>
                            <input
                                type="number"
                                value={priceRange.max}
                                onChange={(e) => setPriceRange({ ...priceRange, max: Number(e.target.value) })}
                                className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
                                placeholder="Đến"
                            />
                        </div>
                        <button
                            onClick={handlePriceRangeChange}
                            className="w-full px-4 py-2 bg-black text-white rounded-md hover:bg-gray-800 transition-colors text-sm"
                        >
                            Áp dụng
                        </button>
                    </div>
                )}
            </div>

            {/* Size Filter */}
            <div className="border-b border-gray-200 pb-4 mb-4">
                <button
                    onClick={() => toggleSection('size')}
                    className="flex items-center justify-between w-full text-left"
                >
                    <h3 className="font-semibold text-gray-900">Kích cỡ</h3>
                    {expandedSections.size ? <ChevronUp size={20} /> : <ChevronDown size={20} />}
                </button>
                {expandedSections.size && (
                    <div className="mt-3 grid grid-cols-3 gap-2">
                        {sizes.map((size) => (
                            <button
                                key={size.id}
                                onClick={() => handleSizeChange(size.id!)}
                                className={`px-3 py-2 text-sm border rounded-md transition-colors ${filters.sizeIds?.includes(size.id!)
                                        ? 'bg-black text-white border-black'
                                        : 'bg-white text-gray-700 border-gray-300 hover:border-black'
                                    }`}
                            >
                                {size.code || size.name}
                            </button>
                        ))}
                    </div>
                )}
            </div>

            {/* Color Filter */}
            <div className="pb-4">
                <button
                    onClick={() => toggleSection('color')}
                    className="flex items-center justify-between w-full text-left"
                >
                    <h3 className="font-semibold text-gray-900">Màu sắc</h3>
                    {expandedSections.color ? <ChevronUp size={20} /> : <ChevronDown size={20} />}
                </button>
                {expandedSections.color && (
                    <div className="mt-3 grid grid-cols-5 gap-3">
                        {colors.map((color) => (
                            <button
                                key={color.id}
                                onClick={() => handleColorChange(color.id!)}
                                className={`relative w-10 h-10 rounded-full border-2 transition-all ${filters.colorIds?.includes(color.id!)
                                        ? 'border-black scale-110'
                                        : 'border-gray-300 hover:border-gray-400'
                                    }`}
                                title={color.name}
                                style={{ backgroundColor: color.hexCode }}
                            >
                                {filters.colorIds?.includes(color.id!) && (
                                    <div className="absolute inset-0 flex items-center justify-center">
                                        <svg className="w-5 h-5 text-white drop-shadow" fill="currentColor" viewBox="0 0 20 20">
                                            <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                                        </svg>
                                    </div>
                                )}
                            </button>
                        ))}
                    </div>
                )}
            </div>
        </div>
    )
}
