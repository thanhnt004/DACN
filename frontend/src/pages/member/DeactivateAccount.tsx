import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '../../store/auth'
import * as ProfileApi from '../../api/profile'
import { useState } from 'react'

export default function DeactivateAccount() {
    const navigate = useNavigate()
    const { logout } = useAuthStore()
    const [password, setPassword] = useState('')
    const [loading, setLoading] = useState(false)

    const handleDeactivate = async () => {
        if (!password) {
            alert('Vui lòng nhập mật khẩu để xác nhận')
            return
        }

        if (!confirm('Bạn có chắc chắn muốn hủy tài khoản? Hành động này không thể hoàn tác!')) {
            return
        }

        setLoading(true)
        try {
            await ProfileApi.deactivateAccount(password)
            alert('Tài khoản đã được hủy thành công')
            await logout()
            navigate('/')
        } catch (error) {
            console.error('Failed to deactivate account:', error)
            alert('Không thể hủy tài khoản. Vui lòng kiểm tra mật khẩu.')
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="bg-white rounded-lg shadow-sm p-8">
            <h1 className="text-2xl font-bold text-red-600 mb-4">Hủy tài khoản</h1>

            <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded">
                <p className="text-red-800 font-medium mb-2">Cảnh báo:</p>
                <ul className="text-red-700 text-sm space-y-1 list-disc list-inside">
                    <li>Tài khoản của bạn sẽ bị vô hiệu hóa vĩnh viễn</li>
                    <li>Tất cả dữ liệu cá nhân sẽ bị xóa</li>
                    <li>Bạn không thể khôi phục tài khoản sau khi hủy</li>
                    <li>Các đơn hàng đang xử lý sẽ bị hủy</li>
                </ul>
            </div>

            <div className="max-w-md space-y-4">
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                        Nhập mật khẩu để xác nhận:
                    </label>
                    <input
                        type="password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        className="w-full border border-gray-300 rounded px-3 py-2"
                        placeholder="Mật khẩu của bạn"
                    />
                </div>

                <div className="flex gap-2">
                    <button
                        onClick={handleDeactivate}
                        disabled={loading}
                        className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700 disabled:opacity-50"
                    >
                        {loading ? 'Đang xử lý...' : 'Xác nhận hủy tài khoản'}
                    </button>
                    <button
                        onClick={() => navigate('/member')}
                        className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50"
                    >
                        Hủy bỏ
                    </button>
                </div>
            </div>
        </div>
    )
}
