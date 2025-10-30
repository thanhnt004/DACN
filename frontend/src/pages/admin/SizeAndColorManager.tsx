import { useEffect, useState } from 'react'
import * as CatalogApi from '../../api/admin/catalog'
import type { SizeDto, ColorDto } from '../../api/admin/catalog'

export default function SizeAndColorManager() {
    const [sizes, setSizes] = useState<SizeDto[]>([])
    const [colors, setColors] = useState<ColorDto[]>([])
    const [loadingSizes, setLoadingSizes] = useState(false)
    const [loadingColors, setLoadingColors] = useState(false)

    const [showSizeModal, setShowSizeModal] = useState(false)
    const [showColorModal, setShowColorModal] = useState(false)
    const [newSize, setNewSize] = useState<SizeDto>({ name: '' })
    const [newColor, setNewColor] = useState<ColorDto>({ name: '' })

    const fetchSizes = async () => {
        setLoadingSizes(true)
        try {
            const res = await CatalogApi.getSizes()
            setSizes(res)
        } catch {
            alert('Lỗi tải sizes')
        } finally {
            setLoadingSizes(false)
        }
    }

    const fetchColors = async () => {
        setLoadingColors(true)
        try {
            const res = await CatalogApi.getColors()
            setColors(res)
        } catch {
            alert('Lỗi tải colors')
        } finally {
            setLoadingColors(false)
        }
    }

    useEffect(() => {
        fetchSizes()
        fetchColors()
    }, [])

    const handleCreateSize = async () => {
        try {
            await CatalogApi.createSize(newSize)
            setShowSizeModal(false)
            setNewSize({ name: '' })
            fetchSizes()
        } catch {
            alert('Lỗi tạo size')
        }
    }

    const handleDeleteSize = async (id: string) => {
        if (!confirm('Bạn có chắc muốn xóa size này?')) return
        try {
            await CatalogApi.deleteSize(id)
            fetchSizes()
        } catch {
            alert('Lỗi xóa size')
        }
    }

    const handleCreateColor = async () => {
        try {
            await CatalogApi.createColor(newColor)
            setShowColorModal(false)
            setNewColor({ name: '' })
            fetchColors()
        } catch {
            alert('Lỗi tạo color')
        }
    }

    const handleDeleteColor = async (id: string) => {
        if (!confirm('Bạn có chắc muốn xóa màu này?')) return
        try {
            await CatalogApi.deleteColor(id)
            fetchColors()
        } catch {
            alert('Lỗi xóa color')
        }
    }

    return (
        <div className="space-y-8">
            <h1 className="text-2xl font-bold">Quản lý Size & Màu sắc</h1>

            {/* Sizes Section */}
            <div className="space-y-4">
                <div className="flex justify-between items-center">
                    <h2 className="text-xl font-semibold">Sizes</h2>
                    <button
                        onClick={() => setShowSizeModal(true)}
                        className="bg-red-600 text-white px-4 py-2 rounded hover:bg-red-700"
                    >
                        + Thêm Size
                    </button>
                </div>
                {loadingSizes ? (
                    <div>Đang tải...</div>
                ) : (
                    <table className="w-full border-collapse bg-white shadow rounded">
                        <thead className="bg-gray-100">
                            <tr>
                                <th className="border p-2 text-left">Tên</th>
                                <th className="border p-2 text-left">Viết tắt</th>
                                <th className="border p-2 text-left">Hành động</th>
                            </tr>
                        </thead>
                        <tbody>
                            {sizes.map((s) => (
                                <tr key={s.id}>
                                    <td className="border p-2">{s.name}</td>
                                    <td className="border p-2">{s.abbreviation || '-'}</td>
                                    <td className="border p-2">
                                        <button
                                            onClick={() => s.id && handleDeleteSize(s.id)}
                                            className="text-red-600 hover:underline"
                                        >
                                            Xóa
                                        </button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                )}
            </div>

            {/* Colors Section */}
            <div className="space-y-4">
                <div className="flex justify-between items-center">
                    <h2 className="text-xl font-semibold">Màu sắc</h2>
                    <button
                        onClick={() => setShowColorModal(true)}
                        className="bg-red-600 text-white px-4 py-2 rounded hover:bg-red-700"
                    >
                        + Thêm Màu
                    </button>
                </div>
                {loadingColors ? (
                    <div>Đang tải...</div>
                ) : (
                    <table className="w-full border-collapse bg-white shadow rounded">
                        <thead className="bg-gray-100">
                            <tr>
                                <th className="border p-2 text-left">Tên</th>
                                <th className="border p-2 text-left">Mã màu (hex)</th>
                                <th className="border p-2 text-left">Hành động</th>
                            </tr>
                        </thead>
                        <tbody>
                            {colors.map((c) => (
                                <tr key={c.id}>
                                    <td className="border p-2">{c.name}</td>
                                    <td className="border p-2">
                                        <div className="flex items-center gap-2">
                                            {c.hexCode && (
                                                <div
                                                    className="w-6 h-6 border rounded"
                                                    style={{ backgroundColor: c.hexCode }}
                                                />
                                            )}
                                            <span>{c.hexCode || '-'}</span>
                                        </div>
                                    </td>
                                    <td className="border p-2">
                                        <button
                                            onClick={() => c.id && handleDeleteColor(c.id)}
                                            className="text-red-600 hover:underline"
                                        >
                                            Xóa
                                        </button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                )}
            </div>

            {/* Size Modal */}
            {showSizeModal && (
                <div className="fixed inset-0 flex items-center justify-center z-50">
                    <div className="bg-white rounded p-6 w-full max-w-md space-y-4">
                        <h2 className="text-xl font-bold">Thêm Size</h2>
                        <div>
                            <label className="block text-sm font-medium mb-1">Tên *</label>
                            <input
                                type="text"
                                value={newSize.name}
                                onChange={(e) => setNewSize({ ...newSize, name: e.target.value })}
                                className="w-full border rounded px-3 py-2"
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium mb-1">Viết tắt</label>
                            <input
                                type="text"
                                value={newSize.abbreviation || ''}
                                onChange={(e) => setNewSize({ ...newSize, abbreviation: e.target.value })}
                                className="w-full border rounded px-3 py-2"
                            />
                        </div>
                        <div className="flex gap-2 justify-end">
                            <button onClick={() => setShowSizeModal(false)} className="px-4 py-2 border rounded">
                                Hủy
                            </button>
                            <button
                                onClick={handleCreateSize}
                                className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700"
                            >
                                Tạo
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Color Modal */}
            {showColorModal && (
                <div className="fixed inset-0 flex items-center justify-center z-50">
                    <div className="bg-white rounded p-6 w-full max-w-md space-y-4">
                        <h2 className="text-xl font-bold">Thêm Màu</h2>
                        <div>
                            <label className="block text-sm font-medium mb-1">Tên *</label>
                            <input
                                type="text"
                                value={newColor.name}
                                onChange={(e) => setNewColor({ ...newColor, name: e.target.value })}
                                className="w-full border rounded px-3 py-2"
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium mb-1">Mã màu (hex)</label>
                            <input
                                type="text"
                                placeholder="#ff0000"
                                value={newColor.hexCode || ''}
                                onChange={(e) => setNewColor({ ...newColor, hexCode: e.target.value })}
                                className="w-full border rounded px-3 py-2"
                            />
                        </div>
                        <div className="flex gap-2 justify-end">
                            <button
                                onClick={() => setShowColorModal(false)}
                                className="px-4 py-2 border rounded"
                            >
                                Hủy
                            </button>
                            <button
                                onClick={handleCreateColor}
                                className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700"
                            >
                                Tạo
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    )
}
