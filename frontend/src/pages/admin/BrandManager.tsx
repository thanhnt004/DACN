import { useEffect, useState } from 'react'
import * as BrandCategoryApi from '../../api/admin/brandCategory'
import type { BrandDto, BrandFilter } from '../../api/admin/brandCategory'

export default function BrandManager() {
    const [brands, setBrands] = useState<BrandDto[]>([])
    const [loading, setLoading] = useState(false)
    const [page, setPage] = useState(0)
    const [totalPages, setTotalPages] = useState(0)
    const [showModal, setShowModal] = useState(false)
    const [editingBrand, setEditingBrand] = useState<BrandDto | null>(null)
    const [formData, setFormData] = useState<BrandDto>({ name: '', slug: '' })

    // Filter state
    const [filter, setFilter] = useState<BrandFilter>({
        name: '',
        minProduct: undefined,
        maxProduct: undefined,
        sortBy: 'createdAt',
        direction: 'desc',
        page: 0,
        size: 20
    })

    // Auto-generate slug from name
    const generateSlug = (name: string) => {
        return name
            .toLowerCase()
            .normalize('NFD')
            .replace(/[\u0300-\u036f]/g, '') // Remove diacritics
            .replace(/đ/g, 'd')
            .replace(/[^a-z0-9]+/g, '-')
            .replace(/^-+|-+$/g, '')
    }

    const fetchBrands = async () => {
        setLoading(true)
        try {
            const res = await BrandCategoryApi.getBrands({ ...filter, page })
            setBrands(res.content)
            setTotalPages(res.totalPages)
        } catch {
            alert('Lỗi tải brands')
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        fetchBrands()
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [page, filter])

    const handleSubmit = async () => {
        if (!formData.name.trim()) {
            alert('Vui lòng nhập tên thương hiệu')
            return
        }
        if (!formData.slug?.trim()) {
            alert('Vui lòng nhập slug')
            return
        }

        try {
            if (editingBrand?.id) {
                await BrandCategoryApi.updateBrand(editingBrand.id, formData)
            } else {
                await BrandCategoryApi.createBrand(formData)
            }
            setShowModal(false)
            setEditingBrand(null)
            setFormData({ name: '', slug: '' })
            fetchBrands()
        } catch {
            alert('Lỗi lưu brand')
        }
    }

    const handleDelete = async (id: string) => {
        if (!confirm('Bạn có chắc muốn xóa brand này?')) return
        try {
            await BrandCategoryApi.deleteBrand(id)
            fetchBrands()
        } catch {
            alert('Lỗi xóa brand')
        }
    }

    const openCreateModal = () => {
        setEditingBrand(null)
        setFormData({ name: '', slug: '' })
        setShowModal(true)
    }

    const openEditModal = (brand: BrandDto) => {
        setEditingBrand(brand)
        setFormData(brand)
        setShowModal(true)
    }

    return (
        <div className="space-y-4">
            <div className="flex justify-between items-center">
                <h1 className="text-2xl font-bold">Quản lý thương hiệu</h1>
                <button
                    onClick={openCreateModal}
                    className="bg-red-600 text-white px-4 py-2 rounded hover:bg-red-700"
                >
                    + Thêm thương hiệu
                </button>
            </div>

            {/* Filter Section */}
            <div className="bg-white shadow rounded p-4 space-y-4">
                <h3 className="font-semibold text-lg">Bộ lọc</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                    <div>
                        <label className="block text-sm font-medium mb-1">Tên thương hiệu</label>
                        <input
                            type="text"
                            placeholder="Tìm kiếm..."
                            value={filter.name || ''}
                            onChange={(e) => setFilter({ ...filter, name: e.target.value })}
                            className="w-full border rounded px-3 py-2"
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium mb-1">Số sản phẩm tối thiểu</label>
                        <input
                            type="number"
                            placeholder="0"
                            value={filter.minProduct || ''}
                            onChange={(e) => setFilter({ ...filter, minProduct: e.target.value ? parseInt(e.target.value) : undefined })}
                            className="w-full border rounded px-3 py-2"
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium mb-1">Số sản phẩm tối đa</label>
                        <input
                            type="number"
                            placeholder="999"
                            value={filter.maxProduct || ''}
                            onChange={(e) => setFilter({ ...filter, maxProduct: e.target.value ? parseInt(e.target.value) : undefined })}
                            className="w-full border rounded px-3 py-2"
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium mb-1">Sắp xếp theo</label>
                        <select
                            value={filter.sortBy || 'createdAt'}
                            onChange={(e) => setFilter({ ...filter, sortBy: e.target.value })}
                            className="w-full border rounded px-3 py-2"
                        >
                            <option value="name">Tên</option>
                            <option value="createdAt">Ngày tạo</option>
                            <option value="updatedAt">Ngày cập nhật</option>
                            <option value="productsCount">Số sản phẩm</option>
                        </select>
                    </div>
                    <div>
                        <label className="block text-sm font-medium mb-1">Thứ tự</label>
                        <select
                            value={filter.direction || 'desc'}
                            onChange={(e) => setFilter({ ...filter, direction: e.target.value })}
                            className="w-full border rounded px-3 py-2"
                        >
                            <option value="asc">Tăng dần</option>
                            <option value="desc">Giảm dần</option>
                        </select>
                    </div>
                    <div className="flex items-end">
                        <button
                            onClick={() => {
                                setFilter({
                                    name: '',
                                    minProduct: undefined,
                                    maxProduct: undefined,
                                    sortBy: 'createdAt',
                                    direction: 'desc',
                                    page: 0,
                                    size: 20
                                })
                                setPage(0)
                            }}
                            className="w-full border border-gray-300 rounded px-4 py-2 hover:bg-gray-50"
                        >
                            Xóa bộ lọc
                        </button>
                    </div>
                </div>
            </div>

            {loading ? (
                <div>Đang tải...</div>
            ) : brands.length === 0 ? (
                <div className="bg-white shadow rounded p-6 text-center text-gray-600">
                    Danh sách hiện đang rỗng.
                </div>
            ) : (
                <>
                    <table className="w-full border-collapse bg-white shadow rounded">
                        <thead className="bg-gray-100">
                            <tr>
                                <th className="border p-2 text-left">Tên thương hiệu</th>
                                <th className="border p-2 text-left">Slug</th>
                                <th className="border p-2 text-left">Số sản phẩm</th>
                                <th className="border p-2 text-left">Tổng bán</th>
                                <th className="border p-2 text-left">Ngày tạo</th>
                                <th className="border p-2 text-left">Hành động</th>
                            </tr>
                        </thead>
                        <tbody>
                            {brands.map((b) => (
                                <tr key={b.id}>
                                    <td className="border p-2 font-medium">{b.name}</td>
                                    <td className="border p-2 text-gray-600">{b.slug || '-'}</td>
                                    <td className="border p-2 text-center">{b.productsCount ?? 0}</td>
                                    <td className="border p-2 text-right">{b.totalSales?.toLocaleString() ?? 0}</td>
                                    <td className="border p-2">
                                        {b.createdAt ? new Date(b.createdAt).toLocaleDateString() : '-'}
                                    </td>
                                    <td className="border p-2 space-x-2">
                                        <button
                                            onClick={() => openEditModal(b)}
                                            className="text-blue-600 hover:underline"
                                        >
                                            Sửa
                                        </button>
                                        <button
                                            onClick={() => b.id && handleDelete(b.id)}
                                            className="text-red-600 hover:underline"
                                        >
                                            Xóa
                                        </button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>

                    {/* Pagination */}
                    <div className="flex gap-2 justify-center">
                        <button
                            disabled={page === 0}
                            onClick={() => setPage(page - 1)}
                            className="px-4 py-2 border rounded disabled:opacity-50"
                        >
                            Trước
                        </button>
                        <span className="px-4 py-2">
                            Trang {page + 1} / {totalPages}
                        </span>
                        <button
                            disabled={page >= totalPages - 1}
                            onClick={() => setPage(page + 1)}
                            className="px-4 py-2 border rounded disabled:opacity-50"
                        >
                            Sau
                        </button>
                    </div>
                </>
            )}

            {/* Create/Edit Modal */}
            {showModal && (
                <div className="fixed inset-0 flex items-center justify-center z-50">
                    <div className="bg-white rounded p-6 w-full max-w-md space-y-4">
                        <h2 className="text-xl font-bold">
                            {editingBrand ? 'Sửa thương hiệu' : 'Thêm thương hiệu'}
                        </h2>
                        <div>
                            <label className="block text-sm font-medium mb-1">Tên *</label>
                            <input
                                type="text"
                                value={formData.name}
                                onChange={(e) => {
                                    const newName = e.target.value
                                    setFormData({
                                        ...formData,
                                        name: newName,
                                        // Auto-generate slug only when creating new brand
                                        ...(editingBrand ? {} : { slug: generateSlug(newName) })
                                    })
                                }}
                                className="w-full border rounded px-3 py-2"
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium mb-1">Slug *</label>
                            <input
                                type="text"
                                value={formData.slug || ''}
                                onChange={(e) => setFormData({ ...formData, slug: e.target.value })}
                                className="w-full border rounded px-3 py-2"
                                placeholder="vi-du-slug"
                            />
                            <p className="text-xs text-gray-500 mt-1">URL-friendly identifier (tự động tạo từ tên)</p>
                        </div>
                        <div>
                            <label className="block text-sm font-medium mb-1">Mô tả</label>
                            <textarea
                                value={formData.description || ''}
                                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                                className="w-full border rounded px-3 py-2"
                                rows={3}
                            />
                        </div>
                        <div className="flex gap-2 justify-end">
                            <button
                                onClick={() => setShowModal(false)}
                                className="px-4 py-2 border rounded"
                            >
                                Hủy
                            </button>
                            <button
                                onClick={handleSubmit}
                                className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700"
                            >
                                {editingBrand ? 'Cập nhật' : 'Tạo'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    )
}
