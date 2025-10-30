import { useEffect, useState } from 'react'
import * as UsersApi from '../../api/admin/users'
import type { UserProfileDto } from '../../api/admin/users'

export default function UserManager() {
    const [users, setUsers] = useState<UserProfileDto[]>([])
    const [loading, setLoading] = useState(false)
    const [page, setPage] = useState(0)
    const [totalPages, setTotalPages] = useState(0)
    const [role, setRole] = useState<'USER' | 'ADMIN' | 'STAFF' | undefined>()
    const [isActive, setIsActive] = useState<boolean | undefined>()

    const fetchUsers = async () => {
        setLoading(true)
        try {
            const res = await UsersApi.getUsers({ role, isActive, page, size: 20 })
            setUsers(res.content)
            setTotalPages(res.totalPages)
        } catch (e) {
            console.error(e)
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        fetchUsers()
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [page, role, isActive])

    const handleBan = async (userId: string) => {
        if (!confirm('Bạn có chắc muốn khóa người dùng này?')) return
        try {
            await UsersApi.banUser(userId)
            fetchUsers()
        } catch {
            alert('Lỗi khóa người dùng')
        }
    }

    const handleRestore = async (userId: string) => {
        try {
            await UsersApi.restoreUser(userId)
            fetchUsers()
        } catch {
            alert('Lỗi mở khóa người dùng')
        }
    }

    return (
        <div className="space-y-4">
            <h1 className="text-2xl font-bold">Quản lý người dùng</h1>

            {/* Filters */}
            <div className="flex gap-4">
                <select
                    value={role || ''}
                    onChange={(e) => {
                        const val = e.target.value
                        setRole(val ? (val as 'USER' | 'ADMIN' | 'STAFF') : undefined)
                    }}
                    className="border rounded px-3 py-2"
                >
                    <option value="">Tất cả vai trò</option>
                    <option value="USER">USER</option>
                    <option value="ADMIN">ADMIN</option>
                    <option value="STAFF">STAFF</option>
                </select>
                <select
                    value={isActive === undefined ? '' : String(isActive)}
                    onChange={(e) =>
                        setIsActive(e.target.value === '' ? undefined : e.target.value === 'true')
                    }
                    className="border rounded px-3 py-2"
                >
                    <option value="">Tất cả trạng thái</option>
                    <option value="true">Đang hoạt động</option>
                    <option value="false">Bị khóa</option>
                </select>
            </div>

            {loading ? (
                <div>Đang tải...</div>
            ) : users.length === 0 ? (
                <div className="bg-white shadow rounded p-6 text-center text-gray-600">
                    Danh sách hiện đang rỗng.
                </div>
            ) : (
                <>
                    <table className="w-full border-collapse bg-white shadow rounded">
                        <thead className="bg-gray-100">
                            <tr>
                                <th className="border p-2 text-left">Họ tên</th>
                                <th className="border p-2 text-left">Email</th>
                                <th className="border p-2 text-left">SĐT</th>
                                <th className="border p-2 text-left">Vai trò</th>
                                <th className="border p-2 text-left">Trạng thái</th>
                                <th className="border p-2 text-left">Hành động</th>
                            </tr>
                        </thead>
                        <tbody>
                            {users.map((u) => (
                                <tr key={u.id}>
                                    <td className="border p-2">{u.fullName}</td>
                                    <td className="border p-2">{u.email}</td>
                                    <td className="border p-2">{u.phone}</td>
                                    <td className="border p-2">{u.role}</td>
                                    <td className="border p-2">
                                        {u.isActive ? (
                                            <span className="text-green-600">Hoạt động</span>
                                        ) : (
                                            <span className="text-red-600">Bị khóa</span>
                                        )}
                                    </td>
                                    <td className="border p-2 space-x-2">
                                        {u.isActive ? (
                                            <button
                                                onClick={() => handleBan(u.id)}
                                                className="text-red-600 hover:underline"
                                            >
                                                Khóa
                                            </button>
                                        ) : (
                                            <button
                                                onClick={() => handleRestore(u.id)}
                                                className="text-blue-600 hover:underline"
                                            >
                                                Mở khóa
                                            </button>
                                        )}
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
        </div>
    )
}
