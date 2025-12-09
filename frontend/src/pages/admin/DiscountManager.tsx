import { useEffect, useState } from 'react'
import { toast } from 'react-toastify'
import * as DiscountsApi from '../../api/admin/discounts'
import type { DiscountResponse, DiscountCreateRequest, DiscountUpdateRequest } from '../../api/admin/discounts'
import { getProducts } from '../../api/admin/products'
import { getCategories } from '../../api/admin/brandCategory'
import { resolveErrorMessage } from '../../lib/problemDetails'
import { instantToDateTimeLocal, formatInstant } from '../../lib/dateUtils'

export default function DiscountManager() {
    const [discounts, setDiscounts] = useState<DiscountResponse[]>([])
    const [loading, setLoading] = useState(false)
    const [page, setPage] = useState(0)
    const [totalPages, setTotalPages] = useState(0)
    const [searchCode, setSearchCode] = useState('')
    const [filterActive, setFilterActive] = useState<boolean | undefined>(undefined)

    const [showCreateModal, setShowCreateModal] = useState(false)
    const [showEditModal, setShowEditModal] = useState(false)
    const [editingDiscount, setEditingDiscount] = useState<DiscountResponse | null>(null)

    // Modal quản lý sản phẩm và danh mục
    const [showProductModal, setShowProductModal] = useState(false)
    const [showCategoryModal, setShowCategoryModal] = useState(false)
    const [managingDiscount, setManagingDiscount] = useState<DiscountResponse | null>(null)
    const [availableProducts, setAvailableProducts] = useState<Array<{ id: string; name: string }>>([])
    const [availableCategories, setAvailableCategories] = useState<Array<{ id: string; name: string }>>([])
    const [selectedProductIds, setSelectedProductIds] = useState<string[]>([])
    const [selectedCategoryIds, setSelectedCategoryIds] = useState<string[]>([])
    const [searchProduct, setSearchProduct] = useState('')
    const [searchCategory, setSearchCategory] = useState('')

    const [formData, setFormData] = useState<DiscountCreateRequest>({
        code: '',
        name: '',
        description: '',
        type: 'PERCENTAGE',
        value: 0,
        startsAt: '',
        endsAt: '',
        minOrderAmount: undefined,
        maxRedemptions: undefined,
        perUserLimit: undefined,
        active: true,
    })

    const fetchDiscounts = async () => {
        setLoading(true)
        try {
            const res = await DiscountsApi.getDiscounts({
                page,
                size: 20,
                code: searchCode || undefined,
                active: filterActive,
            })
            setDiscounts(res.content)
            setTotalPages(res.totalPages)
        } catch (error) {
            console.error('Failed to fetch discounts:', error)
            toast.error('Lỗi tải danh sách khuyến mãi')
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        fetchDiscounts()
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [page, searchCode, filterActive])

    const resetForm = () => {
        setFormData({
            code: '',
            name: '',
            description: '',
            type: 'PERCENTAGE',
            value: 0,
            startsAt: '',
            endsAt: '',
            minOrderAmount: undefined,
            maxRedemptions: undefined,
            perUserLimit: undefined,
            active: true,
        })
    }

    const handleCreate = async () => {
        if (!formData.code || !formData.name || !formData.value) {
            toast.warning('Vui lòng điền đầy đủ: Mã, Tên, và Giá trị')
            return
        }

        // Convert datetime-local format to ISO-8601
        const createData = {
            ...formData,
            startsAt: formData.startsAt ? new Date(formData.startsAt).toISOString() : undefined,
            endsAt: formData.endsAt ? new Date(formData.endsAt).toISOString() : undefined,
        }

        try {
            const newDiscount = await DiscountsApi.createDiscount(createData)
            // Optimistic update - thêm vào đầu danh sách
            setDiscounts(prev => [newDiscount, ...prev])
            setShowCreateModal(false)
            resetForm()
            toast.success('Tạo mã giảm giá thành công!')
        } catch (error) {
            console.error('Failed to create discount:', error)
            const message = resolveErrorMessage(error, 'Lỗi tạo mã giảm giá')
            toast.error(message)
        }
    }

    const handleEdit = async () => {
        if (!editingDiscount) return

        const updateData: DiscountUpdateRequest = {
            code: formData.code,
            name: formData.name,
            description: formData.description,
            type: formData.type,
            value: formData.value,
            // Convert datetime-local format to ISO-8601 with seconds and timezone
            startsAt: formData.startsAt ? new Date(formData.startsAt).toISOString() : undefined,
            endsAt: formData.endsAt ? new Date(formData.endsAt).toISOString() : undefined,
            minOrderAmount: formData.minOrderAmount,
            maxRedemptions: formData.maxRedemptions,
            perUserLimit: formData.perUserLimit,
            active: formData.active,
        }

        try {
            const updated = await DiscountsApi.updateDiscount(editingDiscount.id, updateData)
            // Optimistic update - cập nhật trong danh sách hiện tại
            setDiscounts(prev => prev.map(d => d.id === editingDiscount.id ? updated : d))
            setShowEditModal(false)
            setEditingDiscount(null)
            resetForm()
            toast.success('Cập nhật mã giảm giá thành công!')
        } catch (error) {
            console.error('Failed to update discount:', error)
            const message = resolveErrorMessage(error, 'Lỗi cập nhật mã giảm giá')
            toast.error(message)
        }
    }

    const handleDelete = async (id: string) => {
        if (!confirm('Bạn có chắc muốn xóa mã giảm giá này?')) return

        try {
            // Optimistic update - xóa khỏi UI ngay lập tức
            setDiscounts(prev => prev.filter(d => d.id !== id))
            await DiscountsApi.deleteDiscount(id)
            toast.success('Xóa mã giảm giá thành công!')
        } catch (error) {
            console.error('Failed to delete discount:', error)
            // Rollback nếu lỗi
            fetchDiscounts()
            toast.error('Lỗi xóa mã giảm giá')
        }
    }

    const openEditModal = (discount: DiscountResponse) => {
        setEditingDiscount(discount)
        setFormData({
            code: discount.code,
            name: discount.name,
            description: discount.description || '',
            type: discount.type,
            value: discount.value,
            startsAt: discount.startsAt ? instantToDateTimeLocal(discount.startsAt) : '',
            endsAt: discount.endsAt ? instantToDateTimeLocal(discount.endsAt) : '',
            minOrderAmount: discount.minOrderAmount,
            maxRedemptions: discount.maxRedemptions,
            perUserLimit: discount.perUserLimit,
            active: discount.active,
        })
        setShowEditModal(true)
    }

    const formatValue = (discount: DiscountResponse) => {
        if (discount.type === 'PERCENTAGE') {
            return `${discount.value}%`
        }
        return `${discount.value.toLocaleString('vi-VN')}đ`
    }

    const formatDate = (dateString?: string) => {
        if (!dateString) return '-'
        return formatInstant(dateString, 'vi-VN', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit',
        })
    }

    // Quản lý sản phẩm
    const openProductModal = async (discount: DiscountResponse) => {
        setManagingDiscount(discount)
        setSelectedProductIds(discount.productIds || [])
        setShowProductModal(true)

        try {
            const res = await getProducts({ size: 100 })
            setAvailableProducts(res.content.map(p => ({ id: p.id, name: p.name })))
        } catch (error) {
            console.error('Failed to fetch products:', error)
            toast.error('Lỗi tải danh sách sản phẩm')
        }
    }

    const handleAddProducts = async () => {
        if (!managingDiscount || selectedProductIds.length === 0) return

        try {
            await DiscountsApi.addProductsToDiscount(managingDiscount.id, { productIds: selectedProductIds })
            // Fetch lại discount đang quản lý và cập nhật trong danh sách
            const updated = await DiscountsApi.getDiscount(managingDiscount.id)
            setDiscounts(prev => prev.map(d => d.id === managingDiscount.id ? updated : d))
            setShowProductModal(false)
            setManagingDiscount(null)
            setSelectedProductIds([])
            toast.success('Thêm sản phẩm thành công!')
        } catch (error) {
            console.error('Failed to add products:', error)
            toast.error('Lỗi thêm sản phẩm')
        }
    }

    const handleRemoveProducts = async () => {
        if (!managingDiscount || selectedProductIds.length === 0) return

        try {
            await DiscountsApi.removeProductsFromDiscount(managingDiscount.id, { productIds: selectedProductIds })
            // Fetch lại discount đang quản lý và cập nhật trong danh sách
            const updated = await DiscountsApi.getDiscount(managingDiscount.id)
            setDiscounts(prev => prev.map(d => d.id === managingDiscount.id ? updated : d))
            setShowProductModal(false)
            setManagingDiscount(null)
            setSelectedProductIds([])
            toast.success('Xóa sản phẩm thành công!')
        } catch (error) {
            console.error('Failed to remove products:', error)
            toast.error('Lỗi xóa sản phẩm')
        }
    }

    // Quản lý danh mục
    const openCategoryModal = async (discount: DiscountResponse) => {
        setManagingDiscount(discount)
        setSelectedCategoryIds(discount.categoryIds || [])
        setShowCategoryModal(true)

        try {
            const res = await getCategories({ size: 100 })
            setAvailableCategories(res.content.map(c => ({ id: c.id, name: c.name })))
        } catch (error) {
            console.error('Failed to fetch categories:', error)
            toast.error('Lỗi tải danh sách danh mục')
        }
    }

    const handleAddCategories = async () => {
        if (!managingDiscount || selectedCategoryIds.length === 0) return

        try {
            await DiscountsApi.addCategoriesToDiscount(managingDiscount.id, { categoryIds: selectedCategoryIds })
            // Fetch lại discount đang quản lý và cập nhật trong danh sách
            const updated = await DiscountsApi.getDiscount(managingDiscount.id)
            setDiscounts(prev => prev.map(d => d.id === managingDiscount.id ? updated : d))
            setShowCategoryModal(false)
            setManagingDiscount(null)
            setSelectedCategoryIds([])
            toast.success('Thêm danh mục thành công!')
        } catch (error) {
            console.error('Failed to add categories:', error)
            toast.error('Lỗi thêm danh mục')
        }
    }

    const handleRemoveCategories = async () => {
        if (!managingDiscount || selectedCategoryIds.length === 0) return

        try {
            await DiscountsApi.removeCategoriesFromDiscount(managingDiscount.id, { categoryIds: selectedCategoryIds })
            // Fetch lại discount đang quản lý và cập nhật trong danh sách
            const updated = await DiscountsApi.getDiscount(managingDiscount.id)
            setDiscounts(prev => prev.map(d => d.id === managingDiscount.id ? updated : d))
            setShowCategoryModal(false)
            setManagingDiscount(null)
            setSelectedCategoryIds([])
            toast.success('Xóa danh mục thành công!')
        } catch (error) {
            console.error('Failed to remove categories:', error)
            toast.error('Lỗi xóa danh mục')
        }
    }

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center">
                <h1 className="text-2xl font-bold text-gray-900">Quản lý mã giảm giá</h1>
                <button
                    onClick={() => {
                        resetForm()
                        setShowCreateModal(true)
                    }}
                    className="bg-red-600 text-white px-4 py-2 rounded hover:bg-red-700 font-medium"
                >
                    + Thêm mã giảm giá
                </button>
            </div>

            {/* Filters */}
            <div className="bg-white shadow-sm rounded-lg p-4">
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Tìm kiếm theo mã</label>
                        <input
                            type="text"
                            value={searchCode}
                            onChange={(e) => {
                                setSearchCode(e.target.value)
                                setPage(0)
                            }}
                            placeholder="Nhập mã giảm giá..."
                            className="w-full border border-gray-300 rounded px-3 py-2"
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Trạng thái</label>
                        <select
                            value={filterActive === undefined ? 'all' : filterActive.toString()}
                            onChange={(e) => {
                                const val = e.target.value
                                setFilterActive(val === 'all' ? undefined : val === 'true')
                                setPage(0)
                            }}
                            className="w-full border border-gray-300 rounded px-3 py-2"
                        >
                            <option value="all">Tất cả</option>
                            <option value="true">Hoạt động</option>
                            <option value="false">Đã tắt</option>
                        </select>
                    </div>
                    <div className="flex items-end">
                        <button
                            onClick={() => {
                                setSearchCode('')
                                setFilterActive(undefined)
                                setPage(0)
                            }}
                            className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50"
                        >
                            Xóa bộ lọc
                        </button>
                    </div>
                </div>
            </div>

            {/* Table */}
            {loading ? (
                <div className="bg-white shadow-sm rounded-lg p-8 text-center text-gray-600">Đang tải...</div>
            ) : discounts.length === 0 ? (
                <div className="bg-white shadow-sm rounded-lg p-8 text-center text-gray-600">
                    Không tìm thấy mã giảm giá nào.
                </div>
            ) : (
                <>
                    <div className="bg-white shadow-sm rounded-lg overflow-hidden">
                        <div className="overflow-x-auto">
                            <table className="w-full">
                                <thead className="bg-gray-50 border-b border-gray-200">
                                    <tr>
                                        <th className="px-4 py-3 text-left text-sm font-semibold text-gray-900">Mã</th>
                                        <th className="px-4 py-3 text-left text-sm font-semibold text-gray-900">Tên</th>
                                        <th className="px-4 py-3 text-left text-sm font-semibold text-gray-900">Loại</th>
                                        <th className="px-4 py-3 text-left text-sm font-semibold text-gray-900">Giá trị</th>
                                        <th className="px-4 py-3 text-left text-sm font-semibold text-gray-900">
                                            Thời gian
                                        </th>
                                        <th className="px-4 py-3 text-left text-sm font-semibold text-gray-900">
                                            Giới hạn
                                        </th>
                                        <th className="px-4 py-3 text-left text-sm font-semibold text-gray-900">
                                            Trạng thái
                                        </th>
                                        <th className="px-4 py-3 text-left text-sm font-semibold text-gray-900">
                                            Hành động
                                        </th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-gray-200">
                                    {discounts.map((discount) => (
                                        <tr key={discount.id} className="hover:bg-gray-50">
                                            <td className="px-4 py-3">
                                                <span className="font-mono font-semibold text-red-600">
                                                    {discount.code}
                                                </span>
                                            </td>
                                            <td className="px-4 py-3">
                                                <div>
                                                    <div className="font-medium text-gray-900">{discount.name}</div>
                                                    {discount.description && (
                                                        <div className="text-sm text-gray-500">
                                                            {discount.description}
                                                        </div>
                                                    )}
                                                </div>
                                            </td>
                                            <td className="px-4 py-3 text-sm">
                                                {discount.type === 'PERCENTAGE' ? (
                                                    <span className="px-2 py-1 bg-blue-100 text-blue-700 rounded text-xs font-medium">
                                                        Phần trăm
                                                    </span>
                                                ) : (
                                                    <span className="px-2 py-1 bg-green-100 text-green-700 rounded text-xs font-medium">
                                                        Cố định
                                                    </span>
                                                )}
                                            </td>
                                            <td className="px-4 py-3 font-semibold text-gray-900">
                                                {formatValue(discount)}
                                            </td>
                                            <td className="px-4 py-3 text-sm text-gray-600">
                                                <div>Bắt đầu: {formatDate(discount.startsAt)}</div>
                                                <div>Kết thúc: {formatDate(discount.endsAt)}</div>
                                            </td>
                                            <td className="px-4 py-3 text-sm text-gray-600">
                                                <div>
                                                    Đơn tối thiểu:{' '}
                                                    {discount.minOrderAmount
                                                        ? `${discount.minOrderAmount.toLocaleString('vi-VN')}đ`
                                                        : '-'}
                                                </div>
                                                <div>Tối đa: {discount.maxRedemptions || '∞'}</div>
                                                <div>Mỗi người: {discount.perUserLimit || '∞'}</div>
                                            </td>
                                            <td className="px-4 py-3">
                                                {discount.active ? (
                                                    <span className="px-2 py-1 bg-green-100 text-green-700 rounded text-xs font-medium">
                                                        Hoạt động
                                                    </span>
                                                ) : (
                                                    <span className="px-2 py-1 bg-gray-100 text-gray-700 rounded text-xs font-medium">
                                                        Đã tắt
                                                    </span>
                                                )}
                                            </td>
                                            <td className="px-4 py-3">
                                                <div className="flex flex-col gap-1">
                                                    <div className="flex gap-2">
                                                        <button
                                                            onClick={() => openEditModal(discount)}
                                                            className="text-blue-600 hover:text-blue-800 font-medium text-sm"
                                                        >
                                                            Sửa
                                                        </button>
                                                        <button
                                                            onClick={() => handleDelete(discount.id)}
                                                            className="text-red-600 hover:text-red-800 font-medium text-sm"
                                                        >
                                                            Xóa
                                                        </button>
                                                    </div>
                                                    <div className="flex gap-2 text-xs">
                                                        <button
                                                            onClick={() => openProductModal(discount)}
                                                            className="text-purple-600 hover:text-purple-800 font-medium"
                                                        >
                                                            📦 SP ({discount.productIds?.length || 0})
                                                        </button>
                                                        <button
                                                            onClick={() => openCategoryModal(discount)}
                                                            className="text-orange-600 hover:text-orange-800 font-medium"
                                                        >
                                                            📁 DM ({discount.categoryIds?.length || 0})
                                                        </button>
                                                    </div>
                                                </div>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    </div>

                    {/* Pagination */}
                    <div className="flex items-center justify-center gap-2">
                        <button
                            disabled={page === 0}
                            onClick={() => setPage(page - 1)}
                            className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                            ← Trước
                        </button>
                        <span className="px-4 py-2 text-gray-700">
                            Trang <span className="font-semibold">{page + 1}</span> / {totalPages}
                        </span>
                        <button
                            disabled={page >= totalPages - 1}
                            onClick={() => setPage(page + 1)}
                            className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                            Sau →
                        </button>
                    </div>
                </>
            )}

            {/* Create Modal */}
            {showCreateModal && (
                <div className="fixed inset-0 bg-transparent flex items-center justify-center z-50 p-4">
                    <div className="bg-white rounded-lg p-6 w-full max-w-2xl max-h-[90vh] overflow-y-auto">
                        <h2 className="text-xl font-bold text-gray-900 mb-4">Thêm mã giảm giá mới</h2>

                        <div className="space-y-4">
                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Mã giảm giá <span className="text-red-600">*</span>
                                    </label>
                                    <input
                                        type="text"
                                        value={formData.code}
                                        onChange={(e) =>
                                            setFormData({ ...formData, code: e.target.value.toUpperCase() })
                                        }
                                        placeholder="VD: SUMMER2024"
                                        className="w-full border border-gray-300 rounded px-3 py-2 font-mono"
                                    />
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Tên chiến dịch <span className="text-red-600">*</span>
                                    </label>
                                    <input
                                        type="text"
                                        value={formData.name}
                                        onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                                        placeholder="VD: Giảm giá mùa hè 2024"
                                        className="w-full border border-gray-300 rounded px-3 py-2"
                                    />
                                </div>
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Mô tả</label>
                                <textarea
                                    value={formData.description}
                                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                                    placeholder="Mô tả chi tiết về chương trình..."
                                    rows={3}
                                    className="w-full border border-gray-300 rounded px-3 py-2"
                                />
                            </div>

                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Loại giảm giá <span className="text-red-600">*</span>
                                    </label>
                                    <select
                                        value={formData.type}
                                        onChange={(e) =>
                                            setFormData({
                                                ...formData,
                                                type: e.target.value as 'PERCENTAGE' | 'FIXED_AMOUNT',
                                            })
                                        }
                                        className="w-full border border-gray-300 rounded px-3 py-2"
                                    >
                                        <option value="PERCENTAGE">Phần trăm (%)</option>
                                        <option value="FIXED_AMOUNT">Số tiền cố định (đ)</option>
                                    </select>
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Giá trị <span className="text-red-600">*</span>
                                    </label>
                                    <input
                                        type="number"
                                        value={formData.value}
                                        onChange={(e) => setFormData({ ...formData, value: +e.target.value })}
                                        min="0"
                                        className="w-full border border-gray-300 rounded px-3 py-2"
                                    />
                                </div>
                            </div>

                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Bắt đầu từ
                                    </label>
                                    <input
                                        type="datetime-local" step="1"
                                        value={formData.startsAt}
                                        onChange={(e) => setFormData({ ...formData, startsAt: e.target.value })}
                                        className="w-full border border-gray-300 rounded px-3 py-2"
                                    />
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Kết thúc vào
                                    </label>
                                    <input
                                        type="datetime-local" step="1"
                                        value={formData.endsAt}
                                        onChange={(e) => setFormData({ ...formData, endsAt: e.target.value })}
                                        className="w-full border border-gray-300 rounded px-3 py-2"
                                    />
                                </div>
                            </div>

                            <div className="grid grid-cols-3 gap-4">
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Đơn hàng tối thiểu (đ)
                                    </label>
                                    <input
                                        type="number"
                                        value={formData.minOrderAmount || ''}
                                        onChange={(e) =>
                                            setFormData({
                                                ...formData,
                                                minOrderAmount: e.target.value ? +e.target.value : undefined,
                                            })
                                        }
                                        min="0"
                                        placeholder="0"
                                        className="w-full border border-gray-300 rounded px-3 py-2"
                                    />
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Số lần sử dụng tối đa
                                    </label>
                                    <input
                                        type="number"
                                        value={formData.maxRedemptions || ''}
                                        onChange={(e) =>
                                            setFormData({
                                                ...formData,
                                                maxRedemptions: e.target.value ? +e.target.value : undefined,
                                            })
                                        }
                                        min="1"
                                        placeholder="∞"
                                        className="w-full border border-gray-300 rounded px-3 py-2"
                                    />
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Giới hạn mỗi người
                                    </label>
                                    <input
                                        type="number"
                                        value={formData.perUserLimit || ''}
                                        onChange={(e) =>
                                            setFormData({
                                                ...formData,
                                                perUserLimit: e.target.value ? +e.target.value : undefined,
                                            })
                                        }
                                        min="1"
                                        placeholder="∞"
                                        className="w-full border border-gray-300 rounded px-3 py-2"
                                    />
                                </div>
                            </div>

                            <div className="flex items-center gap-2">
                                <input
                                    type="checkbox"
                                    id="active-create"
                                    checked={formData.active}
                                    onChange={(e) => setFormData({ ...formData, active: e.target.checked })}
                                    className="w-4 h-4 text-red-600"
                                />
                                <label htmlFor="active-create" className="text-sm font-medium text-gray-700">
                                    Kích hoạt ngay
                                </label>
                            </div>
                        </div>

                        <div className="flex gap-2 justify-end mt-6">
                            <button
                                onClick={() => {
                                    setShowCreateModal(false)
                                    resetForm()
                                }}
                                className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50"
                            >
                                Hủy
                            </button>
                            <button
                                onClick={handleCreate}
                                className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700"
                            >
                                Tạo mã giảm giá
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Edit Modal */}
            {showEditModal && editingDiscount && (
                <div className="fixed inset-0 bg-transparent flex items-center justify-center z-50 p-4">
                    <div className="bg-white rounded-lg p-6 w-full max-w-2xl max-h-[90vh] overflow-y-auto">
                        <h2 className="text-xl font-bold text-gray-900 mb-4">Sửa mã giảm giá</h2>

                        <div className="space-y-4">
                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Mã giảm giá <span className="text-red-600">*</span>
                                    </label>
                                    <input
                                        type="text"
                                        value={formData.code}
                                        onChange={(e) =>
                                            setFormData({ ...formData, code: e.target.value.toUpperCase() })
                                        }
                                        className="w-full border border-gray-300 rounded px-3 py-2 font-mono"
                                    />
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Tên chiến dịch <span className="text-red-600">*</span>
                                    </label>
                                    <input
                                        type="text"
                                        value={formData.name}
                                        onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                                        className="w-full border border-gray-300 rounded px-3 py-2"
                                    />
                                </div>
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Mô tả</label>
                                <textarea
                                    value={formData.description}
                                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                                    rows={3}
                                    className="w-full border border-gray-300 rounded px-3 py-2"
                                />
                            </div>

                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Loại giảm giá <span className="text-red-600">*</span>
                                    </label>
                                    <select
                                        value={formData.type}
                                        onChange={(e) =>
                                            setFormData({
                                                ...formData,
                                                type: e.target.value as 'PERCENTAGE' | 'FIXED_AMOUNT',
                                            })
                                        }
                                        className="w-full border border-gray-300 rounded px-3 py-2"
                                    >
                                        <option value="PERCENTAGE">Phần trăm (%)</option>
                                        <option value="FIXED_AMOUNT">Số tiền cố định (đ)</option>
                                    </select>
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Giá trị <span className="text-red-600">*</span>
                                    </label>
                                    <input
                                        type="number"
                                        value={formData.value}
                                        onChange={(e) => setFormData({ ...formData, value: +e.target.value })}
                                        min="0"
                                        className="w-full border border-gray-300 rounded px-3 py-2"
                                    />
                                </div>
                            </div>

                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Bắt đầu từ
                                    </label>
                                    <input
                                        type="datetime-local" step="1"
                                        value={formData.startsAt}
                                        onChange={(e) => setFormData({ ...formData, startsAt: e.target.value })}
                                        className="w-full border border-gray-300 rounded px-3 py-2"
                                    />
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Kết thúc vào
                                    </label>
                                    <input
                                        type="datetime-local" step="1"
                                        value={formData.endsAt}
                                        onChange={(e) => setFormData({ ...formData, endsAt: e.target.value })}
                                        className="w-full border border-gray-300 rounded px-3 py-2"
                                    />
                                </div>
                            </div>

                            <div className="grid grid-cols-3 gap-4">
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Đơn hàng tối thiểu (đ)
                                    </label>
                                    <input
                                        type="number"
                                        value={formData.minOrderAmount || ''}
                                        onChange={(e) =>
                                            setFormData({
                                                ...formData,
                                                minOrderAmount: e.target.value ? +e.target.value : undefined,
                                            })
                                        }
                                        min="0"
                                        className="w-full border border-gray-300 rounded px-3 py-2"
                                    />
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Số lần sử dụng tối đa
                                    </label>
                                    <input
                                        type="number"
                                        value={formData.maxRedemptions || ''}
                                        onChange={(e) =>
                                            setFormData({
                                                ...formData,
                                                maxRedemptions: e.target.value ? +e.target.value : undefined,
                                            })
                                        }
                                        min="1"
                                        className="w-full border border-gray-300 rounded px-3 py-2"
                                    />
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Giới hạn mỗi người
                                    </label>
                                    <input
                                        type="number"
                                        value={formData.perUserLimit || ''}
                                        onChange={(e) =>
                                            setFormData({
                                                ...formData,
                                                perUserLimit: e.target.value ? +e.target.value : undefined,
                                            })
                                        }
                                        min="1"
                                        className="w-full border border-gray-300 rounded px-3 py-2"
                                    />
                                </div>
                            </div>

                            <div className="flex items-center gap-2">
                                <input
                                    type="checkbox"
                                    id="active-edit"
                                    checked={formData.active}
                                    onChange={(e) => setFormData({ ...formData, active: e.target.checked })}
                                    className="w-4 h-4 text-red-600"
                                />
                                <label htmlFor="active-edit" className="text-sm font-medium text-gray-700">
                                    Kích hoạt
                                </label>
                            </div>
                        </div>

                        <div className="flex gap-2 justify-end mt-6">
                            <button
                                onClick={() => {
                                    setShowEditModal(false)
                                    setEditingDiscount(null)
                                    resetForm()
                                }}
                                className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50"
                            >
                                Hủy
                            </button>
                            <button
                                onClick={handleEdit}
                                className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700"
                            >
                                Cập nhật
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Product Management Modal */}
            {showProductModal && managingDiscount && (
                <div className="fixed inset-0 bg-transparent flex items-center justify-center z-50 p-4">
                    <div className="bg-white rounded-lg p-6 w-full max-w-2xl max-h-[90vh] overflow-y-auto">
                        <h2 className="text-xl font-bold text-gray-900 mb-4">
                            Quản lý sản phẩm - {managingDiscount.code}
                        </h2>

                        <div className="mb-4">
                            <input
                                type="text"
                                value={searchProduct}
                                onChange={(e) => setSearchProduct(e.target.value)}
                                placeholder="Tìm kiếm sản phẩm..."
                                className="w-full border border-gray-300 rounded px-3 py-2"
                            />
                        </div>

                        <div className="mb-4 max-h-96 overflow-y-auto border border-gray-200 rounded p-2">
                            {availableProducts
                                .filter(p =>
                                    p.name.toLowerCase().includes(searchProduct.toLowerCase())
                                )
                                .map((product) => (
                                    <label key={product.id} className="flex items-center gap-2 p-2 hover:bg-gray-50 cursor-pointer">
                                        <input
                                            type="checkbox"
                                            checked={selectedProductIds.includes(product.id)}
                                            onChange={(e) => {
                                                if (e.target.checked) {
                                                    setSelectedProductIds([...selectedProductIds, product.id])
                                                } else {
                                                    setSelectedProductIds(selectedProductIds.filter(id => id !== product.id))
                                                }
                                            }}
                                            className="w-4 h-4"
                                        />
                                        <span className="text-sm">{product.name}</span>
                                    </label>
                                ))}
                        </div>

                        <div className="mb-4 text-sm text-gray-600">
                            Đã chọn: <span className="font-semibold">{selectedProductIds.length}</span> sản phẩm
                        </div>

                        <div className="flex gap-2 justify-end">
                            <button
                                onClick={() => {
                                    setShowProductModal(false)
                                    setManagingDiscount(null)
                                    setSelectedProductIds([])
                                    setSearchProduct('')
                                }}
                                className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50"
                            >
                                Đóng
                            </button>
                            <button
                                onClick={handleRemoveProducts}
                                disabled={selectedProductIds.length === 0}
                                className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                Xóa đã chọn
                            </button>
                            <button
                                onClick={handleAddProducts}
                                disabled={selectedProductIds.length === 0}
                                className="px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                Thêm đã chọn
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Category Management Modal */}
            {showCategoryModal && managingDiscount && (
                <div className="fixed inset-0 bg-transparent flex items-center justify-center z-50 p-4">
                    <div className="bg-white rounded-lg p-6 w-full max-w-2xl max-h-[90vh] overflow-y-auto">
                        <h2 className="text-xl font-bold text-gray-900 mb-4">
                            Quản lý danh mục - {managingDiscount.code}
                        </h2>

                        <div className="mb-4">
                            <input
                                type="text"
                                value={searchCategory}
                                onChange={(e) => setSearchCategory(e.target.value)}
                                placeholder="Tìm kiếm danh mục..."
                                className="w-full border border-gray-300 rounded px-3 py-2"
                            />
                        </div>

                        <div className="mb-4 max-h-96 overflow-y-auto border border-gray-200 rounded p-2">
                            {availableCategories
                                .filter(c =>
                                    c.name.toLowerCase().includes(searchCategory.toLowerCase())
                                )
                                .map((category) => (
                                    <label key={category.id} className="flex items-center gap-2 p-2 hover:bg-gray-50 cursor-pointer">
                                        <input
                                            type="checkbox"
                                            checked={selectedCategoryIds.includes(category.id)}
                                            onChange={(e) => {
                                                if (e.target.checked) {
                                                    setSelectedCategoryIds([...selectedCategoryIds, category.id])
                                                } else {
                                                    setSelectedCategoryIds(selectedCategoryIds.filter(id => id !== category.id))
                                                }
                                            }}
                                            className="w-4 h-4"
                                        />
                                        <span className="text-sm">{category.name}</span>
                                    </label>
                                ))}
                        </div>

                        <div className="mb-4 text-sm text-gray-600">
                            Đã chọn: <span className="font-semibold">{selectedCategoryIds.length}</span> danh mục
                        </div>

                        <div className="flex gap-2 justify-end">
                            <button
                                onClick={() => {
                                    setShowCategoryModal(false)
                                    setManagingDiscount(null)
                                    setSelectedCategoryIds([])
                                    setSearchCategory('')
                                }}
                                className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50"
                            >
                                Đóng
                            </button>
                            <button
                                onClick={handleRemoveCategories}
                                disabled={selectedCategoryIds.length === 0}
                                className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                Xóa đã chọn
                            </button>
                            <button
                                onClick={handleAddCategories}
                                disabled={selectedCategoryIds.length === 0}
                                className="px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                Thêm đã chọn
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    )
}
