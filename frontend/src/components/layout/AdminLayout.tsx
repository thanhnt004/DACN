import React from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from './SideBar';

/**
 * Component layout chính cho trang quản trị.
 * Chia màn hình thành 2 cột: Sidebar (trái) và Content (phải).
 */
const AdminLayout: React.FC = () => {
    return (
        // Sử dụng 'flex' để tạo layout 2 cột
        // 'min-h-screen' đảm bảo layout luôn cao tối thiểu bằng màn hình
        <div className="flex min-h-screen bg-gray-100">

            {/* Cột 1: Sidebar */}
            {/* 'flex-shrink-0' ngăn sidebar bị co lại */}
            <div className="flex-shrink-0">
                <Sidebar />
            </div>

            {/* Cột 2: Nội dung chính */}
            {/* 'flex-1' làm cho cột này chiếm toàn bộ không gian còn lại
          'overflow-y-auto' cho phép nội dung cuộn độc lập với sidebar
      */}
            <main className="flex-1 overflow-y-auto">

                {/* <Outlet /> là nơi React Router sẽ render các
            component con (ví dụ: ProductManager, OrderManager)
        */}
                <Outlet />

            </main>
        </div>
    );
};

export default AdminLayout;