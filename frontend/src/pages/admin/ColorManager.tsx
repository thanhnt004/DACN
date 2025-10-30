import { useEffect, useState } from 'react'
import * as CatalogApi from '../../api/admin/catalog'
import type { ColorDto } from '../../api/admin/catalog'

export default function ColorManager() {
    const [colors, setColors] = useState<ColorDto[]>([])
    const [loading, setLoading] = useState(false)
    const [showModal, setShowModal] = useState(false)
    const [isEditing, setIsEditing] = useState(false)
    const [form, setForm] = useState<ColorDto>({ name: '' })

    const loadColors = async () => {
        setLoading(true)
        try {
            const res = await CatalogApi.getColors()
            setColors(res)
        } catch {
            alert('Không thể tải danh sách màu sắc')
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        loadColors()
    }, [])

    const openCreate = () => {
        setIsEditing(false)
        setForm({ name: '' })
        setShowModal(true)
    }

    const openEdit = (item: ColorDto) => {
        setIsEditing(true)
        setForm(item)
        setShowModal(true)
    }

    const handleSubmit = async () => {
        try {
            if (isEditing && form.id) {
                await CatalogApi.updateColor(form.id, form)
            } else {
                await CatalogApi.createColor({ name: form.name, hexCode: form.hexCode })
            }
            setShowModal(false)
            await loadColors()
        } catch {
            alert('Lưu màu sắc thất bại')
        }
    }

    const handleDelete = async (id?: string) => {
        if (!id) return
        if (!confirm('Bạn có chắc muốn xóa màu sắc này?')) return
        try {
            await CatalogApi.deleteColor(id)
            await loadColors()
        } catch {
            alert('Xóa màu sắc thất bại')
        }
    }

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <h1 className="text-2xl font-bold">Quản lý màu sắc</h1>
                <button
                    onClick={openCreate}
                    className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700"
                >
                    + Thêm màu sắc
                </button>
            </div>

            {loading ? (
                <div>Đang tải...</div>
            ) : colors.length === 0 ? (
                <div className="bg-white shadow rounded p-6 text-center text-gray-600">
                    Danh sách hiện đang rỗng.
                </div>
            ) : (
                <table className="w-full border-collapse bg-white shadow rounded">
                    <thead className="bg-gray-100">
                        <tr>
                            <th className="border p-2 text-left">Tên</th>
                            <th className="border p-2 text-left">Mã màu</th>
                            <th className="border p-2 text-left">Xem trước</th>
                            <th className="border p-2 text-left">Hành động</th>
                        </tr>
                    </thead>
                    <tbody>
                        {colors.map((item) => (
                            <tr key={item.id}>
                                <td className="border p-2">{item.name}</td>
                                <td className="border p-2 font-mono">{item.hexCode || '-'}</td>
                                <td className="border p-2">
                                    {item.hexCode ? (
                                        <span
                                            className="inline-block w-6 h-6 rounded border"
                                            style={{ backgroundColor: item.hexCode }}
                                        />
                                    ) : (
                                        '-'
                                    )}
                                </td>
                                <td className="border p-2 space-x-3">
                                    <button
                                        onClick={() => openEdit(item)}
                                        className="text-blue-600 hover:underline"
                                    >
                                        Sửa
                                    </button>
                                    <button
                                        onClick={() => handleDelete(item.id)}
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

            {showModal && (
                <div className="fixed inset-0 flex items-center justify-center z-50">
                    <div className="bg-white rounded p-6 w-full max-w-md space-y-4">
                        <h2 className="text-xl font-semibold">
                            {isEditing ? 'Sửa màu sắc' : 'Thêm màu sắc'}
                        </h2>
                        <div>
                            <label className="block text-sm font-medium mb-1">Tên *</label>
                            <input
                                type="text"
                                value={form.name}
                                onChange={(e) => setForm({ ...form, name: e.target.value })}
                                className="w-full border rounded px-3 py-2"
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium mb-1">Mã màu (hex)</label>
                            <input
                                type="text"
                                placeholder="#FF0000"
                                value={form.hexCode || ''}
                                onChange={(e) => setForm({ ...form, hexCode: e.target.value })}
                                className="w-full border rounded px-3 py-2"
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium mb-1">Bảng chọn màu</label>
                            <input
                                type="color"
                                value={/^#[0-9a-fA-F]{6}$/.test(form.hexCode || '') ? form.hexCode : '#000000'}
                                onChange={(e) => setForm({ ...form, hexCode: e.target.value.toUpperCase() })}
                                className="w-full h-10 border rounded"
                            />
                        </div>
                        <div className="flex justify-end gap-2">
                            <button onClick={() => setShowModal(false)} className="px-4 py-2 border rounded">
                                Hủy
                            </button>
                            <button
                                onClick={handleSubmit}
                                className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700"
                            >
                                {isEditing ? 'Cập nhật' : 'Tạo'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    )
}
