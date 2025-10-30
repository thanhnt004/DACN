import { useEffect, useState } from 'react'
import * as CatalogApi from '../../api/admin/catalog'
import type { SizeDto } from '../../api/admin/catalog'
import z from 'zod'
import { zodResolver } from '@hookform/resolvers/zod/src/index.js'
import { useForm } from 'react-hook-form'
const sizeSchema = z.object({
    code: z.string().min(1, "Mã không được để trống"),
    name: z.string().min(1, "Tên không được để trống"),
})
type SizeFormValues = z.infer<typeof sizeSchema>

export default function SizeManager() {
    const [sizes, setSizes] = useState<SizeDto[]>([])
    const [loading, setLoading] = useState(false)
    const [showModal, setShowModal] = useState(false)
    const [isEditing, setIsEditing] = useState(false)
    const [form, setForm] = useState<SizeDto>({ name: '' })
    const {
        register,
        handleSubmit,
        formState: { errors },
        reset,
    } = useForm<SizeFormValues>({
        resolver: zodResolver(sizeSchema),
    })


    const loadSizes = async () => {
        setLoading(true)
        try {
            const res = await CatalogApi.getSizes()
            setSizes(res)
        } catch {
            alert('Không thể tải danh sách kích thước')
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        loadSizes()
    }, [])

    const openEdit = (item: SizeDto) => {
        setIsEditing(true)
        setForm(item)
        setShowModal(true)
    }

    const submit = async () => {
        try {
            if (isEditing && form.id) {
                await CatalogApi.updateSize(form.id, form)
            } else {
                await CatalogApi.createSize({ name: form.name, code: form.code })
            }
            setShowModal(false)
            await loadSizes()
        } catch {
            alert('Lưu kích thước thất bại')
        }
    }

    const handleDelete = async (id?: string) => {
        if (!id) return
        if (!confirm('Bạn có chắc muốn xóa kích thước này?')) return
        try {
            await CatalogApi.deleteSize(id)
            await loadSizes()
        } catch {
            alert('Xóa kích thước thất bại')
        }
    }
    const onSubmit = async (data: SizeFormValues) => {
        console.log("Dữ liệu size:", data)
        try {
            await CatalogApi.createSize(data)
        } catch {
            alert('Lưu kích thước thất bại')
        }
        await loadSizes()
        reset()
    }

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <h1 className="text-2xl font-bold">Quản lý kích thước</h1>
            </div>
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
                {/* Mã */}
                <div>
                    <label className="block font-medium mb-1">Mã Size</label>
                    <input
                        type="text"
                        {...register("code")}
                        placeholder="VD: S, M, L, XL..."
                        className="w-full p-2 border rounded-lg focus:outline-none focus:ring focus:ring-blue-300"
                    />
                    {errors.code && (
                        <p className="text-red-500 text-sm mt-1">{errors.code.message}</p>
                    )}
                </div>

                {/* Tên */}
                <div>
                    <label className="block font-medium mb-1">Tên Size</label>
                    <input
                        type="text"
                        {...register("name")}
                        placeholder="VD: Small, Medium, Large..."
                        className="w-full p-2 border rounded-lg focus:outline-none focus:ring focus:ring-blue-300"
                    />
                    {errors.name && (
                        <p className="text-red-500 text-sm mt-1">{errors.name.message}</p>
                    )}
                </div>
                <button
                    type="submit"
                    className="w-full bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700 transition"
                >
                    Tạo mới
                </button>
            </form>
            {loading ? (
                <div>Đang tải...</div>
            ) : sizes.length === 0 ? (
                <div className="bg-white shadow rounded p-6 text-center text-gray-600">
                    Danh sách hiện đang rỗng.
                </div>
            ) : (
                <table className="w-full border-collapse bg-white shadow rounded">
                    <thead className="bg-gray-100">
                        <tr>
                            <th className="border p-2 text-left">Tên</th>
                            <th className="border p-2 text-left">Code</th>
                            <th className="border p-2 text-left">Hành động</th>
                        </tr>
                    </thead>
                    <tbody>
                        {sizes.map((item) => (
                            <tr key={item.id}>
                                <td className="border p-2">{item.name}</td>
                                <td className="border p-2">{item.code || '-'}</td>
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
                            {isEditing ? 'Sửa kích thước' : 'Thêm kích thước'}
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
                            <label className="block text-sm font-medium mb-1">Viết tắt</label>
                            <input
                                type="text"
                                value={form.code || ''}
                                onChange={(e) => setForm({ ...form, code: e.target.value })}
                                className="w-full border rounded px-3 py-2"
                            />
                        </div>
                        <div className="flex justify-end gap-2">
                            <button onClick={() => setShowModal(false)} className="px-4 py-2 border rounded">
                                Hủy
                            </button>
                            <button
                                onClick={submit}
                                className="px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700"
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
