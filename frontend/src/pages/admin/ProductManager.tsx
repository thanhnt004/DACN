import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import * as ProductsApi from '../../api/admin/products'
import * as BrandCategoryApi from '../../api/admin/brandCategory'
import * as CatalogApi from '../../api/admin/catalog'
import type { ProductSummaryResponse } from '../../api/admin/products'
import { Eye, Edit2, Trash2, Filter, X } from 'lucide-react'

interface FilterState {
    categoryId?: string
    brandId?: string
    gender?: '' | 'men' | 'women' | 'unisex'
    minPriceAmount?: string
    maxPriceAmount?: string
    sizeIds?: string[]
    colorIds?: string[]
    sortBy?: string
    direction?: 'asc' | 'desc'
}

export default function ProductManager() {
    const navigate = useNavigate()
    const [products, setProducts] = useState<ProductSummaryResponse[]>([])
    const [loading, setLoading] = useState(false)
    const [page, setPage] = useState(0)
    const [totalPages, setTotalPages] = useState(0)
    const [totalElements, setTotalElements] = useState(0)

    // Filter states
    const [showFilters, setShowFilters] = useState(false)
    const [filters, setFilters] = useState<FilterState>({})
    const [brands, setBrands] = useState<BrandCategoryApi.BrandDto[]>([])
    const [categories, setCategories] = useState<BrandCategoryApi.CategoryResponse[]>([])
    const [sizes, setSizes] = useState<CatalogApi.SizeDto[]>([])
    const [colors, setColors] = useState<CatalogApi.ColorDto[]>([])

    useEffect(() => {
        loadReferences()
    }, [])

    const loadReferences = async () => {
        try {
            const [brandsData, categoriesData, sizesData, colorsData] = await Promise.all([
                BrandCategoryApi.getBrands({ size: 100 }),
                BrandCategoryApi.getCategories({ size: 200 }),
                CatalogApi.getSizes(),
                CatalogApi.getColors(),
            ])
            setBrands(brandsData.content || [])
            setCategories(categoriesData.content || [])
            setSizes(sizesData || [])
            setColors(colorsData || [])
        } catch (error) {
            console.error('Error loading references:', error)
        }
    }

    const fetchProducts = async () => {
        setLoading(true)
        try {
            // Build query params
            const params: any = { page, size: 20 }

            if (filters.categoryId) params.categoryId = filters.categoryId
            if (filters.brandId) params.brandId = filters.brandId
            if (filters.gender) params.gender = filters.gender
            if (filters.minPriceAmount) params.minPriceAmount = parseInt(filters.minPriceAmount)
            if (filters.maxPriceAmount) params.maxPriceAmount = parseInt(filters.maxPriceAmount)
            if (filters.sizeIds && filters.sizeIds.length > 0) params.sizeIds = filters.sizeIds
            if (filters.colorIds && filters.colorIds.length > 0) params.colorIds = filters.colorIds
            if (filters.sortBy) params.sortBy = filters.sortBy
            if (filters.direction) params.direction = filters.direction

            const res = await ProductsApi.getProducts(params)
            setProducts(res?.content ?? [])
            setTotalPages(Math.max(res?.totalPages ?? 1, 1))
            setTotalElements(res?.totalElements ?? 0)
        } catch (error) {
            console.error('Error fetching products:', error)
            alert('Lỗi tải sản phẩm')
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        fetchProducts()
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [page, filters])

    const handleDelete = async (id: string) => {
        if (!confirm('Bạn có chắc muốn xóa sản phẩm này?')) return
        try {
            await ProductsApi.deleteProduct(id)
            alert('Xóa sản phẩm thành công!')
            fetchProducts()
        } catch (error) {
            console.error('Error deleting product:', error)
            alert('Lỗi xóa sản phẩm')
        }
    }

    const handleClearFilters = () => {
        setFilters({})
        setPage(0)
    }

    const handleApplyFilters = () => {
        setPage(0)
        setShowFilters(false)
    }

    const formatPrice = (price: number) => {
        return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price)
    }

    const formatDate = (dateString?: string) => {
        if (!dateString) return '—'
        return new Date(dateString).toLocaleDateString('vi-VN', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit'
        })
    }

    return (
        <div className="space-y-4">
            <div className="flex justify-between items-center">
                <h1 className="text-2xl font-bold">Quản lý sản phẩm</h1>
                <div className="flex gap-2">
                    <button
                        onClick={() => setShowFilters(!showFilters)}
                        className="flex items-center gap-2 bg-gray-100 text-gray-700 px-4 py-2 rounded hover:bg-gray-200"
                    >
                        <Filter className="w-4 h-4" />
                        Bộ lọc
                    </button>
                    <button
                        onClick={() => navigate('/admin/products/create')}
                        className="bg-red-600 text-white px-4 py-2 rounded hover:bg-red-700"
                    >
                        + Tạo sản phẩm mới
                    </button>
                </div>
            </div>

            {/* Filter Panel */}
            {showFilters && (
                <div className="bg-white shadow rounded-lg p-6 space-y-4">
                    <div className="flex justify-between items-center">
                        <h2 className="text-lg font-semibold">Bộ lọc</h2>
                        <button
                            onClick={() => setShowFilters(false)}
                            className="text-gray-500 hover:text-gray-700"
                        >
                            <X className="w-5 h-5" />
                        </button>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                        {/* Category Filter */}
                        <div>
                            <label className="block text-sm font-medium mb-1">Danh mục</label>
                            <select
                                value={filters.categoryId || ''}
                                onChange={(e) => setFilters({ ...filters, categoryId: e.target.value || undefined })}
                                className="w-full border rounded px-3 py-2"
                            >
                                <option value="">Tất cả</option>
                                {categories.map(cat => (
                                    <option key={cat.id} value={cat.id}>{cat.name}</option>
                                ))}
                            </select>
                        </div>

                        {/* Brand Filter */}
                        <div>
                            <label className="block text-sm font-medium mb-1">Thương hiệu</label>
                            <select
                                value={filters.brandId || ''}
                                onChange={(e) => setFilters({ ...filters, brandId: e.target.value || undefined })}
                                className="w-full border rounded px-3 py-2"
                            >
                                <option value="">Tất cả</option>
                                {brands.map(brand => (
                                    <option key={brand.id} value={brand.id}>{brand.name}</option>
                                ))}
                            </select>
                        </div>

                        {/* Gender Filter */}
                        <div>
                            <label className="block text-sm font-medium mb-1">Giới tính</label>
                            <select
                                value={filters.gender || ''}
                                onChange={(e) => setFilters({ ...filters, gender: e.target.value as any || undefined })}
                                className="w-full border rounded px-3 py-2"
                            >
                                <option value="">Tất cả</option>
                                <option value="men">Nam</option>
                                <option value="women">Nữ</option>
                                <option value="unisex">Unisex</option>
                            </select>
                        </div>

                        {/* Min Price */}
                        <div>
                            <label className="block text-sm font-medium mb-1">Giá từ (VNĐ)</label>
                            <input
                                type="number"
                                value={filters.minPriceAmount || ''}
                                onChange={(e) => setFilters({ ...filters, minPriceAmount: e.target.value || undefined })}
                                className="w-full border rounded px-3 py-2"
                                placeholder="0"
                                min="0"
                            />
                        </div>

                        {/* Max Price */}
                        <div>
                            <label className="block text-sm font-medium mb-1">Giá đến (VNĐ)</label>
                            <input
                                type="number"
                                value={filters.maxPriceAmount || ''}
                                onChange={(e) => setFilters({ ...filters, maxPriceAmount: e.target.value || undefined })}
                                className="w-full border rounded px-3 py-2"
                                placeholder="10000000"
                                min="0"
                            />
                        </div>

                        {/* Sort By */}
                        <div>
                            <label className="block text-sm font-medium mb-1">Sắp xếp theo</label>
                            <select
                                value={filters.sortBy || ''}
                                onChange={(e) => setFilters({ ...filters, sortBy: e.target.value || undefined })}
                                className="w-full border rounded px-3 py-2"
                            >
                                <option value="">Mặc định</option>
                                <option value="priceAmount">Giá</option>
                                <option value="createdAt">Ngày tạo</option>
                            </select>
                        </div>

                        {/* Direction */}
                        {filters.sortBy && (
                            <div>
                                <label className="block text-sm font-medium mb-1">Thứ tự</label>
                                <select
                                    value={filters.direction || 'asc'}
                                    onChange={(e) => setFilters({ ...filters, direction: e.target.value as 'asc' | 'desc' })}
                                    className="w-full border rounded px-3 py-2"
                                >
                                    <option value="asc">Tăng dần</option>
                                    <option value="desc">Giảm dần</option>
                                </select>
                            </div>
                        )}
                    </div>

                    {/* Sizes Filter */}
                    <div>
                        <label className="block text-sm font-medium mb-2">Kích thước</label>
                        <div className="flex flex-wrap gap-2">
                            {sizes.map(size => (
                                <label key={size.id} className="flex items-center gap-2 border rounded px-3 py-2 cursor-pointer hover:bg-gray-50">
                                    <input
                                        type="checkbox"
                                        checked={filters.sizeIds?.includes(size.id!) || false}
                                        onChange={(e) => {
                                            const sizeIds = filters.sizeIds || []
                                            if (e.target.checked) {
                                                setFilters({ ...filters, sizeIds: [...sizeIds, size.id!] })
                                            } else {
                                                setFilters({ ...filters, sizeIds: sizeIds.filter(id => id !== size.id) })
                                            }
                                        }}
                                    />
                                    <span>{size.name} ({size.code})</span>
                                </label>
                            ))}
                        </div>
                    </div>

                    {/* Colors Filter */}
                    <div>
                        <label className="block text-sm font-medium mb-2">Màu sắc</label>
                        <div className="flex flex-wrap gap-2">
                            {colors.map(color => (
                                <label key={color.id} className="flex items-center gap-2 border rounded px-3 py-2 cursor-pointer hover:bg-gray-50">
                                    <input
                                        type="checkbox"
                                        checked={filters.colorIds?.includes(color.id!) || false}
                                        onChange={(e) => {
                                            const colorIds = filters.colorIds || []
                                            if (e.target.checked) {
                                                setFilters({ ...filters, colorIds: [...colorIds, color.id!] })
                                            } else {
                                                setFilters({ ...filters, colorIds: colorIds.filter(id => id !== color.id) })
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

                    <div className="flex gap-2">
                        <button
                            onClick={handleApplyFilters}
                            className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700"
                        >
                            Áp dụng
                        </button>
                        <button
                            onClick={handleClearFilters}
                            className="bg-gray-200 text-gray-700 px-4 py-2 rounded hover:bg-gray-300"
                        >
                            Xóa bộ lọc
                        </button>
                    </div>
                </div>
            )}

            {loading ? (
                <div className="flex justify-center items-center py-12">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-red-600"></div>
                </div>
            ) : products.length === 0 ? (
                <div className="bg-white shadow rounded p-6 text-center text-gray-600">
                    Không tìm thấy sản phẩm nào.
                </div>
            ) : (
                <>
                    <div className="bg-white shadow rounded-lg overflow-hidden">
                        <div className="overflow-x-auto">
                            <table className="w-full">
                                <thead className="bg-gray-50 border-b">
                                    <tr>
                                        <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                            Ảnh
                                        </th>
                                        <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                            Sản phẩm
                                        </th>
                                        <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                            Danh mục
                                        </th>
                                        <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                            Giá
                                        </th>
                                        <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                            Giới tính
                                        </th>
                                        <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                            Đánh giá
                                        </th>
                                        <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                                            Hành động
                                        </th>
                                    </tr>
                                </thead>
                                <tbody className="bg-white divide-y divide-gray-200">
                                    {products.map((product) => (
                                        <tr key={product.id} className="hover:bg-gray-50">
                                            <td className="px-4 py-3">
                                                {product.imageUrl ? (
                                                    <img
                                                        src={product.imageUrl}
                                                        alt={product.name}
                                                        className="w-16 h-16 object-cover rounded"
                                                    />
                                                ) : (
                                                    <div className="w-16 h-16 bg-gray-200 rounded flex items-center justify-center text-gray-400 text-xs">
                                                        No Image
                                                    </div>
                                                )}
                                            </td>
                                            <td className="px-4 py-3">
                                                <button
                                                    onClick={() => navigate(`/products/${product.slug}`)}
                                                    className="text-blue-600 hover:underline font-medium text-left"
                                                >
                                                    {product.name}
                                                </button>
                                                <div className="text-sm text-gray-500 mt-1">
                                                    {product.slug}
                                                </div>
                                                {product.colors && product.colors.length > 0 && (
                                                    <div className="flex gap-1 mt-1">
                                                        {product.colors.slice(0, 5).map((color, idx) => (
                                                            <div
                                                                key={idx}
                                                                className="w-4 h-4 rounded-full border border-gray-300"
                                                                style={{ backgroundColor: color }}
                                                                title={color}
                                                            />
                                                        ))}
                                                        {product.colors.length > 5 && (
                                                            <span className="text-xs text-gray-500 ml-1">
                                                                +{product.colors.length - 5}
                                                            </span>
                                                        )}
                                                    </div>
                                                )}
                                            </td>
                                            <td className="px-4 py-3 text-sm text-gray-900">
                                                —
                                            </td>
                                            <td className="px-4 py-3 text-sm font-medium text-gray-900">
                                                {formatPrice(product.priceAmount)}
                                            </td>
                                            <td className="px-4 py-3 text-sm text-gray-900">
                                                {product.gender === 'men' && 'Nam'}
                                                {product.gender === 'women' && 'Nữ'}
                                                {product.gender === 'unisex' && 'Unisex'}
                                                {!product.gender && '—'}
                                            </td>
                                            <td className="px-4 py-3 text-sm text-gray-900">
                                                {product.ratingAvg ? (
                                                    <div className="flex items-center gap-1">
                                                        <span className="text-yellow-500">★</span>
                                                        <span>{product.ratingAvg.toFixed(1)}</span>
                                                    </div>
                                                ) : (
                                                    '—'
                                                )}
                                            </td>
                                            <td className="px-4 py-3 text-right text-sm font-medium">
                                                <div className="flex items-center justify-end gap-2">
                                                    <button
                                                        onClick={() => navigate(`/products/${product.slug}`)}
                                                        className="text-blue-600 hover:text-blue-900"
                                                        title="Xem chi tiết"
                                                    >
                                                        <Eye className="w-4 h-4" />
                                                    </button>
                                                    <button
                                                        onClick={() => navigate(`/admin/products/${product.id}/edit`)}
                                                        className="text-green-600 hover:text-green-900"
                                                        title="Chỉnh sửa"
                                                    >
                                                        <Edit2 className="w-4 h-4" />
                                                    </button>
                                                    <button
                                                        onClick={() => handleDelete(product.id)}
                                                        className="text-red-600 hover:text-red-900"
                                                        title="Xóa"
                                                    >
                                                        <Trash2 className="w-4 h-4" />
                                                    </button>
                                                </div>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    </div>

                    {/* Pagination */}
                    <div className="flex items-center justify-between bg-white px-4 py-3 rounded-lg shadow">
                        <div className="text-sm text-gray-700">
                            Hiển thị <span className="font-medium">{products.length}</span> sản phẩm
                            {totalElements > 0 && (
                                <span> / Tổng <span className="font-medium">{totalElements}</span></span>
                            )}
                        </div>
                        <div className="flex gap-2">
                            <button
                                disabled={page === 0}
                                onClick={() => setPage(page - 1)}
                                className="px-4 py-2 border rounded disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
                            >
                                Trước
                            </button>
                            <span className="px-4 py-2 text-sm">
                                Trang {page + 1} / {totalPages}
                            </span>
                            <button
                                disabled={page >= totalPages - 1}
                                onClick={() => setPage(page + 1)}
                                className="px-4 py-2 border rounded disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
                            >
                                Sau
                            </button>
                        </div>
                    </div>
                </>
            )}
        </div>
    )
}
