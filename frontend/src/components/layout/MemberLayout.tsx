import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { Home, ShoppingBag, Shield, LogOut, UserX, FileText } from 'lucide-react'
import { Header } from './Header'
import Footer from './Footer'
import { useAuthStore } from '../../store/auth'

const menuItems = [
    { icon: ShoppingBag, label: 'Lịch sử mua hàng', path: '/member/orders' },
    { icon: FileText, label: 'Thông tin tài khoản', path: '/member/profile' },
    { icon: FileText, label: 'Góp ý - Phản hồi - Hỗ trợ', path: '/member/support' },
    { icon: Shield, label: 'Điều khoản sử dụng', path: '/member/terms' },
]

export default function MemberLayout() {
    const location = useLocation()
    const navigate = useNavigate()
    const { logout } = useAuthStore()

    const handleLogout = async () => {
        if (confirm('Bạn có chắc chắn muốn đăng xuất?')) {
            await logout()
            navigate('/')
        }
    }

    const handleDeactivate = () => {
        if (confirm('Bạn có chắc chắn muốn hủy tài khoản? Hành động này không thể hoàn tác!')) {
            navigate('/member/deactivate')
        }
    }

    return (
        <div className="min-h-screen bg-gray-50">
            <Header />

            <div className="container mx-auto px-4 py-8">
                <div className="flex gap-6">
                    {/* Sidebar */}
                    <aside className="w-80 flex-shrink-0">
                        <div className="bg-white rounded-lg shadow-sm overflow-hidden sticky top-4">
                            <nav className="p-4">
                                {menuItems.map((item) => {
                                    const Icon = item.icon
                                    const isActive = location.pathname === item.path
                                    return (
                                        <Link
                                            key={item.path}
                                            to={item.path}
                                            className={`flex items-center gap-3 px-4 py-3 rounded-lg mb-1 transition-colors ${isActive
                                                ? 'bg-red-50 text-red-600 font-medium'
                                                : 'text-gray-700 hover:bg-gray-50'
                                                }`}
                                        >
                                            <Icon className="w-5 h-5" />
                                            <span className="text-sm">{item.label}</span>
                                        </Link>
                                    )
                                })}

                                {/* Divider */}
                                <div className="border-t border-gray-200 my-2" />

                                {/* Logout */}
                                <button
                                    onClick={handleLogout}
                                    className="w-full flex items-center gap-3 px-4 py-3 rounded-lg text-gray-700 hover:bg-gray-50 transition-colors"
                                >
                                    <LogOut className="w-5 h-5" />
                                    <span className="text-sm">Đăng xuất</span>
                                </button>

                                {/* Deactivate Account */}
                                <button
                                    onClick={handleDeactivate}
                                    className="w-full flex items-center gap-3 px-4 py-3 rounded-lg text-red-600 hover:bg-red-50 transition-colors"
                                >
                                    <UserX className="w-5 h-5" />
                                    <span className="text-sm">Hủy tài khoản</span>
                                </button>
                            </nav>
                        </div>
                    </aside>

                    {/* Main Content */}
                    <main className="flex-1">
                        <Outlet />
                    </main>
                </div>
            </div>

            <Footer />
        </div>
    )
}
