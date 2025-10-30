export default function AdminDashboard() {
    return (
        <div className="space-y-6">
            <h1 className="text-3xl font-bold">Bảng điều khiển</h1>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                <div className="bg-white shadow rounded p-6">
                    <h3 className="text-gray-600 text-sm font-medium">Tổng đơn hàng</h3>
                    <p className="text-3xl font-bold mt-2">0</p>
                </div>
                <div className="bg-white shadow rounded p-6">
                    <h3 className="text-gray-600 text-sm font-medium">Doanh thu</h3>
                    <p className="text-3xl font-bold mt-2">0 VNĐ</p>
                </div>
                <div className="bg-white shadow rounded p-6">
                    <h3 className="text-gray-600 text-sm font-medium">Sản phẩm</h3>
                    <p className="text-3xl font-bold mt-2">0</p>
                </div>
                <div className="bg-white shadow rounded p-6">
                    <h3 className="text-gray-600 text-sm font-medium">Khách hàng</h3>
                    <p className="text-3xl font-bold mt-2">0</p>
                </div>
            </div>
            <div className="bg-white shadow rounded p-6">
                <h2 className="text-xl font-semibold mb-4">Hoạt động gần đây</h2>
                <p className="text-gray-600">Chưa có dữ liệu.</p>
            </div>
        </div>
    )
}
