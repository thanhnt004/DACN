import React from 'react';
import { Link, NavLink } from 'react-router-dom';
import {
    LuLayoutDashboard,
    LuBoxes,
    LuShoppingCart,
    LuTicket
} from 'react-icons/lu'; // Dùng react-icons cho đẹp (cài bằng: npm install react-icons)

/**
 * Component menu điều hướng bên trái.
 */
const Sidebar: React.FC = () => {

    // Hàm trợ giúp để tạo className cho NavLink
    // 'isActive' được NavLink tự động cung cấp
    const getNavLinkClass = ({ isActive }: { isActive: boolean }): string => {
        const baseClasses = 'flex items-center gap-3 px-4 py-3 rounded-lg transition-colors';
        if (isActive) {
            // Style khi link đang active (màu đỏ giống theme login của bạn)
            return `${baseClasses} bg-red-100 text-red-600 font-semibold`;
        }
        // Style mặc định
        return `${baseClasses} text-gray-700 hover:bg-gray-50`;
    };

    const getSubLinkClass = ({ isActive }: { isActive: boolean }): string => {
        const base = 'flex items-center gap-2 px-4 py-2 rounded-lg transition-colors text-sm';
        return isActive
            ? `${base} bg-red-50 text-red-600 font-semibold`
            : `${base} text-gray-600 hover:bg-gray-50`;
    };

    return (
        // Cố định chiều rộng sidebar
        // 'h-screen' và 'sticky top-0' giữ cho nó cố định khi cuộn nội dung
        <nav className="w-64 h-screen bg-white border-r border-gray-200 p-4 sticky top-0">
            <div className="flex items-center gap-2 p-4 mb-4">
                <Link to="/" className="text-2xl font-bold text-red-600 hover:text-red-700">
                <img
                        src="/src/assets/img/logo.svg"
                        alt="WearWave Logo"
                        className="h-12 w-12"
                />
                </Link>
            </div>

            <ul className="flex flex-col gap-2">
                <li>
                    <NavLink to="/admin" end className={getNavLinkClass}>
                        <LuLayoutDashboard size={20} />
                        <span>Bảng điều khiển</span>
                    </NavLink>
                </li>
                <li>
                    <div className="flex flex-col gap-1">
                        <NavLink to="/admin/products" className={getNavLinkClass}>
                            <LuBoxes size={20} />
                            <span>Sản phẩm</span>
                        </NavLink>
                        <ul className="ml-6 flex flex-col gap-1">
                            <li>
                                <NavLink to="/admin/products/create" className={getSubLinkClass}>
                                    <span>Thêm sản phẩm</span>
                                </NavLink>
                            </li>
                            <li>
                                <NavLink to="/admin/products" end className={getSubLinkClass}>
                                    <span>Danh sách sản phẩm</span>
                                </NavLink>
                            </li>
                            <li>
                                <NavLink to="/admin/products/sizes" className={getSubLinkClass}>
                                    <span>Kích thước</span>
                                </NavLink>
                            </li>
                            <li>
                                <NavLink to="/admin/products/colors" className={getSubLinkClass}>
                                    <span>Màu sắc</span>
                                </NavLink>
                            </li>
                        </ul>
                    </div>
                </li>
                <li>
                    <NavLink to="/admin/orders" className={getNavLinkClass}>
                        <LuShoppingCart size={20} />
                        <span>Đơn hàng</span>
                    </NavLink>
                </li>
                <li>
                    <NavLink to="/admin/users" className={getNavLinkClass}>
                        <span>Khách hàng</span>
                    </NavLink>
                </li>
                <li>
                    <NavLink to="/admin/discounts" className={getNavLinkClass}>
                        <LuTicket size={20} />
                        <span>Khuyến mãi</span>
                    </NavLink>
                </li>
                <li>
                    <NavLink to="/admin/brands" className={getNavLinkClass}>
                        <span>Thương hiệu</span>
                    </NavLink>
                </li>
                <li>
                    <NavLink to="/admin/categories" className={getNavLinkClass}>
                        <span>Danh mục</span>
                    </NavLink>
                </li>
            </ul>
        </nav>
    );
};

export default Sidebar;