import { useState, useEffect } from 'react'
import { useSearchParams } from 'react-router-dom'
import * as ProductsApi from '../api/admin/products'
import ProductCard from '../components/layout/ProductCard'
import { Header } from '../components/layout/Header'
import Footer from '../components/layout/Footer'
import ProductFilterSidebar from '../components/ProductFilterSidebar'
import type { ProductFilters } from '../components/ProductFilterSidebar'
import { ChevronDown } from 'lucide-react'

export default function ProductsPage() {
    const [searchParams] = useSearchParams()
    const [products, setProducts] = useState<ProductsApi.ProductSummaryResponse[]>([])
    const [loading, setLoading] = useState(true)
    const [page, setPage] = useState(0)
    const [totalPages, setTotalPages] = useState(1)
    const [totalElements, setTotalElements] = useState(0)

    // Sort state
    const [sortBy, setSortBy] = useState<string>('createdAt')
    const [sortDirection, setSortDirection] = useState<string>('desc')

    // Filter state
    const [filters, setFilters] = useState<ProductFilters>({
        minPriceAmount: undefined,
        maxPriceAmount: undefined,
        sizeIds: undefined,
        colorIds: undefined
    })

    const search = searchParams.get('search')
    const category = searchParams.get('category')
    const gender = searchParams.get('gender')

    useEffect(() => {
        const loadProducts = async () => {
            setLoading(true)
            try {
                const response = await ProductsApi.getProducts({
                    page,
                    size: 12,
                    status: 'ACTIVE',
                    gender: gender || undefined,
                    search: search || undefined,
                    categoryId: category || undefined,
                    minPriceAmount: filters.minPriceAmount,
                    maxPriceAmount: filters.maxPriceAmount,
                    sizeIds: filters.sizeIds,
                    colorIds: filters.colorIds,
                    sortBy: sortBy,
                    direction: sortDirection
                })
                console.log('Products API response:', response)
                setProducts(Array.isArray(response?.content) ? response.content : [])
                setTotalPages(response?.totalPages || 1)
                setTotalElements(response?.totalElements || 0)
            } catch (error) {
                console.error('Failed to load products:', error)
                setProducts([])
                setTotalPages(1)
                setTotalElements(0)
            } finally {
                setLoading(false)
            }
        }
        loadProducts()
    }, [page, search, category, gender, filters, sortBy, sortDirection])

    const handleFavoriteClick = (productId: string) => {
        console.log('Favorite clicked for product:', productId)
    }

    const handleFilterChange = (newFilters: ProductFilters) => {
        setFilters(newFilters)
        setPage(0) // Reset to first page when filters change
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

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <div className="text-gray-600">Đang tải sản phẩm...</div>
            </div>
        )
    }

    return (
        <div className="min-h-screen bg-gray-50">
            <Header />
            <div className="max-w-7xl mx-auto py-8 px-4">
                <div className="mb-8">
                    <h1 className="text-3xl font-bold text-gray-900">Sản phẩm</h1>
                    <p className="mt-2 text-gray-600">Khám phá bộ sưu tập của chúng tôi</p>
                </div>

                <div className="flex gap-6">
                    {/* Filter Sidebar */}
                    <aside className="hidden lg:block w-64 flex-shrink-0">
                        <ProductFilterSidebar filters={filters} onFilterChange={handleFilterChange} />
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
                        {products && products.length > 0 ? (
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
                                    Hiện tại không có sản phẩm nào phù hợp với bộ lọc của bạn.
                                    Vui lòng thử lại với các tiêu chí khác.
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
