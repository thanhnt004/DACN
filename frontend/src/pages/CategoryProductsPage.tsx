import { useState, useEffect } from 'react'
import { useParams, useSearchParams } from 'react-router-dom'
import * as ProductsApi from '../api/admin/products'
import * as BrandCategoryApi from '../api/admin/brandCategory'
import ProductCard from '../components/layout/ProductCard'
import { Header } from '../components/layout/Header'
import Footer from '../components/layout/Footer'
import { ChevronDown, ChevronUp } from 'lucide-react'
import * as CatalogApi from '../api/admin/catalog'

interface CategoryFilters {
    gender?: string[]
    minPriceAmount?: number
    maxPriceAmount?: number
    sizeIds?: string[]
    colorIds?: string[]
}

export default function CategoryProductsPage() {
    const { slug } = useParams<{ slug: string }>()
    const [searchParams] = useSearchParams()
    const [products, setProducts] = useState<ProductsApi.ProductSummaryResponse[]>([])
    const [category, setCategory] = useState<BrandCategoryApi.CategoryResponse | null>(null)
    const [loading, setLoading] = useState(true)
    const [page, setPage] = useState(0)
    const [totalPages, setTotalPages] = useState(1)
    const [totalElements, setTotalElements] = useState(0)

    // Sort state
    const [sortBy, setSortBy] = useState<string>('createdAt')
    const [sortDirection, setSortDirection] = useState<string>('desc')

    // Filter state
    const [filters, setFilters] = useState<CategoryFilters>({
        gender: undefined,
        minPriceAmount: undefined,
        maxPriceAmount: undefined,
        sizeIds: undefined,
        colorIds: undefined
    })

    // Filter UI state
    const [sizes, setSizes] = useState<CatalogApi.SizeDto[]>([])
    const [colors, setColors] = useState<CatalogApi.ColorDto[]>([])
    const [expandedSections, setExpandedSections] = useState({
        price: true,
        gender: true,
        size: true,
        color: true
    })
    const [priceRange, setPriceRange] = useState({
        min: 29000,
        max: 1499000
    })

    const search = searchParams.get('search')
    const filterKey = JSON.stringify(filters);

    useEffect(() => {
        const loadData = async () => {
            if (!slug) return
            setLoading(true)
            try {
                const [categoryRes, sizesRes, colorsRes] = await Promise.all([
                    BrandCategoryApi.getCategory(slug),
                    !sizes.length ? CatalogApi.getSizes() : Promise.resolve(sizes),
                    !colors.length ? CatalogApi.getColors() : Promise.resolve(colors)
                ]);

                if (categoryRes) {
                    setCategory(categoryRes)
                }

                if (Array.isArray(sizesRes)) {
                    setSizes(sizesRes);
                }
                if (Array.isArray(colorsRes)) {
                    setColors(colorsRes);
                }

                if (categoryRes?.id) {
                    const genderParam = filters.gender && filters.gender.length > 0
                        ? filters.gender[0]
                        : undefined

                    const response = await ProductsApi.getProducts({
                        page,
                        size: 12,
                        status: 'ACTIVE',
                        categoryId: categoryRes.id,
                        gender: genderParam,
                        search: search || undefined,
                        minPriceAmount: filters.minPriceAmount,
                        maxPriceAmount: filters.maxPriceAmount,
                        sizeIds: filters.sizeIds,
                        colorIds: filters.colorIds,
                        sortBy: sortBy,
                        direction: sortDirection
                    })

                    setProducts(Array.isArray(response?.content) ? response.content : [])
                    setTotalPages(response?.totalPages || 1)
                    setTotalElements(response?.totalElements || 0)
                }
            } catch (error) {
                console.error('Failed to load category or products:', error)
                setProducts([])
                setTotalPages(1)
                setTotalElements(0)
            } finally {
                setLoading(false)
            }
        }
        loadData()
    }, [slug, page, filterKey, sortBy, sortDirection])

    const handleFavoriteClick = (productId: string) => {
        // console.log('Favorite clicked for product:', productId)
    }

    const handleSortChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
        const value = e.target.value
        if (value === 'price-asc') {
            setSortBy('priceAmount')
            setSortDirection('asc')
        } else if (value === 'price-desc') {
            setSortBy('priceAmount')
            setSortDirection('desc')
        } else if (value === 'newest') {
            setSortBy('createdAt')
            setSortDirection('desc')
        } else if (value === 'oldest') {
            setSortBy('createdAt')
            setSortDirection('asc')
        }
        setPage(0)
    }

    const toggleSection = (section: keyof typeof expandedSections) => {
        setExpandedSections(prev => ({ ...prev, [section]: !prev[section] }))
    }

    const handleGenderChange = (gender: string) => {
        const currentGenders = filters.gender || []
        const newGenders = currentGenders.includes(gender)
            ? currentGenders.filter(g => g !== gender)
            : [...currentGenders, gender]
        setFilters({ ...filters, gender: newGenders.length > 0 ? newGenders : undefined })
        setPage(0)
    }

    const handleSizeChange = (sizeId: string) => {
        const currentSizes = filters.sizeIds || []
        const newSizes = currentSizes.includes(sizeId)
            ? currentSizes.filter(s => s !== sizeId)
            : [...currentSizes, sizeId]
        setFilters({ ...filters, sizeIds: newSizes.length > 0 ? newSizes : undefined })
        setPage(0)
    }

    const handleColorChange = (colorId: string) => {
        const currentColors = filters.colorIds || []
        const newColors = currentColors.includes(colorId)
            ? currentColors.filter(c => c !== colorId)
            : [...currentColors, colorId]
        setFilters({ ...filters, colorIds: newColors.length > 0 ? newColors : undefined })
        setPage(0)
    }

    const handlePriceRangeChange = () => {
        setFilters({
            ...filters,
            minPriceAmount: priceRange.min,
            maxPriceAmount: priceRange.max
        })
        setPage(0)
    }

    if (loading && !category) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <div className="text-gray-600">Đang tải...</div>
            </div>
        )
    }

    return (
        <div className="min-h-screen bg-gray-50">
            <Header />
            <div className="max-w-7xl mx-auto py-8 px-4">
                {/* Breadcrumb & Title */}
                <div className="mb-8">
                    <h1 className="text-3xl font-bold text-gray-900">
                        {category?.name || 'Danh mục sản phẩm'}
                    </h1>
                    {category?.description && (
                        <p className="mt-2 text-gray-600">{category.description}</p>
                    )}
                </div>

                <div className="flex gap-6">
                    {/* Filter Sidebar */}
                    <aside className="hidden lg:block w-64 flex-shrink-0">
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

                            {/* Gender Filter */}
                            <div className="border-b border-gray-200 pb-4 mb-4">
                                <button
                                    onClick={() => toggleSection('gender')}
                                    className="flex items-center justify-between w-full text-left"
                                >
                                    <h3 className="font-semibold text-gray-900">Giới</h3>
                                    {expandedSections.gender ? <ChevronUp size={20} /> : <ChevronDown size={20} />}
                                </button>
                                {expandedSections.gender && (
                                    <div className="mt-3 space-y-2">
                                        {[
                                            { value: 'women', label: 'Nữ' },
                                            { value: 'men', label: 'Nam' },
                                            { value: 'unisex', label: 'Unisex' }
                                        ].map(({ value, label }) => (
                                            <label key={value} className="flex items-center gap-2 cursor-pointer">
                                                <input
                                                    type="checkbox"
                                                    checked={filters.gender?.includes(value)}
                                                    onChange={() => handleGenderChange(value)}
                                                    className="w-4 h-4 rounded border-gray-300"
                                                />
                                                <span className="text-sm text-gray-700">{label}</span>
                                            </label>
                                        ))}
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
                    </aside>

                    {/* Main Content */}
                    <div className="flex-1">
                        {/* Sort & Results Count */}
                        <div className="flex items-center justify-between mb-6">
                            <div className="text-sm text-gray-600">
                                {totalElements > 0 && (
                                    <span>{totalElements} sản phẩm</span>
                                )}
                            </div>
                            <div className="flex items-center gap-2">
                                <ChevronDown className="w-4 h-4 text-gray-500" />
                                <select
                                    onChange={handleSortChange}
                                    className="px-4 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-black"
                                    defaultValue="newest"
                                >
                                    <option value="newest">Mới nhất</option>
                                    <option value="oldest">Cũ nhất</option>
                                    <option value="price-asc">Giá: Thấp đến cao</option>
                                    <option value="price-desc">Giá: Cao đến thấp</option>
                                </select>
                            </div>
                        </div>

                        {/* Product Grid */}
                        {loading ? (
                            <div className="flex items-center justify-center py-16">
                                <div className="text-gray-600">Đang tải sản phẩm...</div>
                            </div>
                        ) : products && products.length > 0 ? (
                            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                                {products.filter(p => p && p.id).map((product) => (
                                    <ProductCard
                                        key={product.id}
                                        id={product.id}
                                        slug={product.slug}
                                        colors={product.colors}
                                        ratingAvg={product.ratingAvg}
                                        imageUrl={product.imageUrl}
                                        name={product.name}
                                        gender={product.gender}
                                        priceAmount={product.priceAmount}
                                        onFavoriteClick={handleFavoriteClick}
                                    />
                                ))}
                            </div>
                        ) : (
                            /* Empty State */
                            <div className="flex flex-col items-center justify-center py-16 px-4">
                                <svg
                                    className="w-24 h-24 text-gray-300 mb-4"
                                    fill="none"
                                    viewBox="0 0 24 24"
                                    stroke="currentColor"
                                >
                                    <path
                                        strokeLinecap="round"
                                        strokeLinejoin="round"
                                        strokeWidth={1.5}
                                        d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4"
                                    />
                                </svg>
                                <h3 className="text-xl font-semibold text-gray-700 mb-2">
                                    Không tìm thấy sản phẩm
                                </h3>
                                <p className="text-gray-500 text-center max-w-md">
                                    Hiện tại không có sản phẩm nào trong danh mục này phù hợp với bộ lọc của bạn.
                                </p>
                            </div>
                        )}

                        {/* Pagination */}
                        {totalPages > 1 && (
                            <div className="mt-8 flex justify-center gap-2">
                                <button
                                    onClick={() => setPage(Math.max(0, page - 1))}
                                    disabled={page === 0}
                                    className="px-4 py-2 border rounded-md disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
                                >
                                    Trước
                                </button>
                                <span className="px-4 py-2">
                                    Trang {page + 1} / {totalPages}
                                </span>
                                <button
                                    onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
                                    disabled={page >= totalPages - 1}
                                    className="px-4 py-2 border rounded-md disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
                                >
                                    Sau
                                </button>
                            </div>
                        )}
                    </div>
                </div>
            </div>
            <Footer />
        </div>
    )
}
