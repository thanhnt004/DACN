import { useState, useEffect, useRef } from 'react'
import * as ReportsApi from '../../api/admin/reports'
import { TrendingUp, TrendingDown, Package, ShoppingCart, DollarSign, Users, Download, MoreVertical } from 'lucide-react'
import { toast } from 'react-toastify'
import * as XLSX from 'xlsx'

export default function AdminDashboard() {
    const [overview, setOverview] = useState<ReportsApi.DashboardOverview | null>(null)
    const [revenueChart, setRevenueChart] = useState<ReportsApi.RevenueChart | null>(null)
    const [topProducts, setTopProducts] = useState<ReportsApi.TopProductsReport | null>(null)
    const [topCategories, setTopCategories] = useState<ReportsApi.CategoryRevenueReport | null>(null)
    const [profitReport, setProfitReport] = useState<ReportsApi.ProfitReport | null>(null)
    const [monthlyProfitReport, setMonthlyProfitReport] = useState<ReportsApi.MonthlyProfitReport | null>(null)
    const [categoryProfitReport, setCategoryProfitReport] = useState<ReportsApi.CategoryProfitReport | null>(null)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)
    const [exporting, setExporting] = useState(false)
    const [topProductLimit, setTopProductLimit] = useState(5)
    const [dateRange, setDateRange] = useState({
        startDate: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0], // 30 days ago
        endDate: new Date().toISOString().split('T')[0]
    })
    const [revenueMenuOpen, setRevenueMenuOpen] = useState(false)
    const [categoryMenuOpen, setCategoryMenuOpen] = useState(false)
    const revenueMenuRef = useRef<HTMLDivElement>(null)
    const categoryMenuRef = useRef<HTMLDivElement>(null)

    useEffect(() => {
        loadDashboardData()
    }, [dateRange.startDate, dateRange.endDate, topProductLimit])

    const loadDashboardData = async () => {
        setLoading(true)
        setError(null)
        try {
            const params: ReportsApi.ReportParams = {
                startDate: dateRange.startDate,
                endDate: dateRange.endDate,
                timezone: Intl.DateTimeFormat().resolvedOptions().timeZone
            }

            console.log('Loading dashboard data with params:', params)
            console.log('Top product limit:', topProductLimit)

            const [overviewData, chartData, productsData, categoriesData, profitData, monthlyProfitData, categoryProfitData] = await Promise.all([
                ReportsApi.getDashboardOverview(params),
                ReportsApi.getRevenueChart(params),
                ReportsApi.getTopSellingProducts({ ...params, limit: topProductLimit }),
                ReportsApi.getTopCategoryRevenue(params),
                ReportsApi.getProfitReport(params),
                ReportsApi.getMonthlyProfitReport(params),
                ReportsApi.getCategoryProfitReport(params)
            ])

            console.log('Dashboard data loaded:', { overviewData, chartData, productsData, categoriesData, profitData, monthlyProfitData, categoryProfitData })
            console.log('Products data received:', productsData)

            setOverview(overviewData)
            setRevenueChart(chartData)
            setTopProducts(productsData)
            setTopCategories(categoriesData)
            setProfitReport(profitData)
            setMonthlyProfitReport(monthlyProfitData)
            setCategoryProfitReport(categoryProfitData)
        } catch (err: any) {
            console.error('Failed to load dashboard data:', err)
            setError(err?.response?.data?.message || err?.message || 'Không thể tải dữ liệu bảng điều khiển')
        } finally {
            setLoading(false)
        }
    }

    const handleExportExcel = async () => {
        setExporting(true)
        try {
            const params: ReportsApi.ReportParams = {
                startDate: dateRange.startDate,
                endDate: dateRange.endDate,
                timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
                limit: topProductLimit
            }

            const blob = await ReportsApi.exportTopSellingProductsToExcel(params)
            const url = window.URL.createObjectURL(blob)
            const link = document.createElement('a')
            link.href = url
            link.download = `top-selling-products_${dateRange.startDate}_${dateRange.endDate}.xlsx`
            document.body.appendChild(link)
            link.click()
            document.body.removeChild(link)
            window.URL.revokeObjectURL(url)

            toast.success('Xuất file Excel thành công!')
        } catch (err: any) {
            console.error('Failed to export Excel:', err)
            toast.error(err?.response?.data?.message || 'Không thể xuất file Excel')
        } finally {
            setExporting(false)
        }
    }

    const formatCurrency = (amount: number) => {
        return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount)
    }

    // Export Revenue Chart to Excel
    const exportRevenueToExcel = async () => {
        if (!revenueChart || revenueChart.dataPoints.length === 0) {
            toast.warning('Không có dữ liệu để xuất')
            return
        }
        setRevenueMenuOpen(false)
        try {
            const ws_data = [
                ['Ngày', 'Doanh thu', 'Số đơn hàng'],
                ...revenueChart.dataPoints.map(point => [
                    point.reportDate,
                    point.revenue,
                    point.orderCount
                ])
            ]
            
            const ws = XLSX.utils.aoa_to_sheet(ws_data)
            const wb = XLSX.utils.book_new()
            XLSX.utils.book_append_sheet(wb, ws, 'Doanh thu')
            XLSX.writeFile(wb, `doanh-thu_${dateRange.startDate}_${dateRange.endDate}.xlsx`)
            toast.success('Xuất Excel thành công!')
        } catch (err) {
            console.error('Export error:', err)
            toast.error('Lỗi khi xuất file Excel')
        }
    }

    // Export Revenue Chart to CSV
    const exportRevenueToCSV = () => {
        if (!revenueChart || revenueChart.dataPoints.length === 0) {
            toast.warning('Không có dữ liệu để xuất')
            return
        }
        setRevenueMenuOpen(false)
        try {
            const csvContent = [
                ['Ngày', 'Doanh thu', 'Số đơn hàng'].join(','),
                ...revenueChart.dataPoints.map(point => 
                    [point.reportDate, point.revenue, point.orderCount].join(',')
                )
            ].join('\n')
            
            const blob = new Blob([`\uFEFF${csvContent}`], { type: 'text/csv;charset=utf-8;' })
            const link = document.createElement('a')
            link.href = URL.createObjectURL(blob)
            link.download = `doanh-thu_${dateRange.startDate}_${dateRange.endDate}.csv`
            link.click()
            toast.success('Xuất CSV thành công!')
        } catch (err) {
            console.error('Export error:', err)
            toast.error('Lỗi khi xuất file CSV')
        }
    }

    // Export Category Chart to Excel
    const exportCategoryToExcel = async () => {
        if (!topCategories || topCategories.categories.length === 0) {
            toast.warning('Không có dữ liệu để xuất')
            return
        }
        setCategoryMenuOpen(false)
        try {
            const ws_data = [
                ['Danh mục', 'Doanh thu', 'Số lượng bán'],
                ...topCategories.categories.map(cat => [
                    cat.categoryName,
                    cat.totalRevenue,
                    cat.totalSold
                ])
            ]
            
            const ws = XLSX.utils.aoa_to_sheet(ws_data)
            const wb = XLSX.utils.book_new()
            XLSX.utils.book_append_sheet(wb, ws, 'Danh mục bán chạy')
            XLSX.writeFile(wb, `danh-muc-ban-chay_${dateRange.startDate}_${dateRange.endDate}.xlsx`)
            toast.success('Xuất Excel thành công!')
        } catch (err) {
            console.error('Export error:', err)
            toast.error('Lỗi khi xuất file Excel')
        }
    }

    // Export Category Chart to CSV
    const exportCategoryToCSV = () => {
        if (!topCategories || topCategories.categories.length === 0) {
            toast.warning('Không có dữ liệu để xuất')
            return
        }
        setCategoryMenuOpen(false)
        try {
            const csvContent = [
                ['Danh mục', 'Doanh thu', 'Số lượng bán'].join(','),
                ...topCategories.categories.map(cat => 
                    [cat.categoryName, cat.totalRevenue, cat.totalSold].join(',')
                )
            ].join('\n')
            
            const blob = new Blob([`\uFEFF${csvContent}`], { type: 'text/csv;charset=utf-8;' })
            const link = document.createElement('a')
            link.href = URL.createObjectURL(blob)
            link.download = `danh-muc-ban-chay_${dateRange.startDate}_${dateRange.endDate}.csv`
            link.click()
            toast.success('Xuất CSV thành công!')
        } catch (err) {
            console.error('Export error:', err)
            toast.error('Lỗi khi xuất file CSV')
        }
    }

    // Close dropdown when clicking outside
    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (revenueMenuRef.current && !revenueMenuRef.current.contains(event.target as Node)) {
                setRevenueMenuOpen(false)
            }
            if (categoryMenuRef.current && !categoryMenuRef.current.contains(event.target as Node)) {
                setCategoryMenuOpen(false)
            }
        }
        document.addEventListener('mousedown', handleClickOutside)
        return () => document.removeEventListener('mousedown', handleClickOutside)
    }, [])

    if (loading) {
        return (
            <div className="flex flex-col items-center justify-center min-h-[400px]">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mb-4"></div>
                <p className="text-gray-600">Đang tải dữ liệu bảng điều khiển...</p>
            </div>
        )
    }

    if (error) {
        return (
            <div className="space-y-6">
                <h1 className="text-3xl font-bold">Bảng điều khiển</h1>
                <div className="bg-red-50 border border-red-200 rounded-lg p-6 text-center">
                    <p className="text-red-700 mb-4">{error}</p>
                    <button
                        onClick={loadDashboardData}
                        className="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700"
                    >
                        Thử lại
                    </button>
                </div>
            </div>
        )
    }

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center">
                <h1 className="text-3xl font-bold">Bảng điều khiển</h1>
                <div className="flex gap-2">
                    <input
                        type="date"
                        value={dateRange.startDate}
                        onChange={(e) => setDateRange({ ...dateRange, startDate: e.target.value })}
                        className="border rounded px-3 py-2"
                    />
                    <span className="self-center">đến</span>
                    <input
                        type="date"
                        value={dateRange.endDate}
                        onChange={(e) => setDateRange({ ...dateRange, endDate: e.target.value })}
                        className="border rounded px-3 py-2"
                    />
                </div>
            </div>

            {/* No Data Warning */}
            {!overview && !revenueChart && !topProducts && !topCategories && (
                <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-6">
                    <p className="text-yellow-800 text-center">
                        Chưa có dữ liệu để hiển thị. Vui lòng kiểm tra lại khoảng thời gian hoặc đảm bảo đã có đơn hàng trong hệ thống.
                    </p>
                </div>
            )}

            {/* Stats Cards */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                <div className="bg-white shadow rounded-lg p-6">
                    <div className="flex items-center justify-between">
                        <div>
                            <h3 className="text-gray-600 text-sm font-medium">Tổng đơn hàng</h3>
                            <p className="text-3xl font-bold mt-2">{overview?.totalOrders ?? 0}</p>
                            <p className="text-sm text-gray-500 mt-1">
                                {overview?.pendingOrders ?? 0} đang chờ xử lý
                            </p>
                        </div>
                        <div className="bg-blue-100 p-3 rounded-full">
                            <ShoppingCart className="w-6 h-6 text-blue-600" />
                        </div>
                    </div>
                </div>

                <div className="bg-white shadow rounded-lg p-6">
                    <div className="flex items-center justify-between">
                        <div>
                            <h3 className="text-gray-600 text-sm font-medium">Doanh thu</h3>
                            <p className="text-3xl font-bold mt-2">{formatCurrency(overview?.totalRevenue ?? 0)}</p>
                            {overview && overview.totalRevenue > 0 && (
                                <p className="text-sm text-gray-500 mt-1">
                                    Từ {dateRange.startDate} đến {dateRange.endDate}
                                </p>
                            )}
                        </div>
                        <div className="bg-green-100 p-3 rounded-full">
                            <DollarSign className="w-6 h-6 text-green-600" />
                        </div>
                    </div>
                </div>

                <div className="bg-white shadow rounded-lg p-6">
                    <div className="flex items-center justify-between">
                        <div>
                            <h3 className="text-gray-600 text-sm font-medium">Giảm giá</h3>
                            <p className="text-3xl font-bold mt-2">{formatCurrency(overview?.totalDiscount ?? 0)}</p>
                            <p className="text-sm text-gray-500 mt-1">Tổng giảm giá đã áp dụng</p>
                        </div>
                        <div className="bg-orange-100 p-3 rounded-full">
                            <Package className="w-6 h-6 text-orange-600" />
                        </div>
                    </div>
                </div>

                <div className="bg-white shadow rounded-lg p-6">
                    <div className="flex items-center justify-between">
                        <div>
                            <h3 className="text-gray-600 text-sm font-medium">Sản phẩm bán chạy</h3>
                            <p className="text-3xl font-bold mt-2">{topProducts?.products?.length ?? 0}</p>
                            <p className="text-sm text-gray-500 mt-1">Top sản phẩm</p>
                        </div>
                        <div className="bg-purple-100 p-3 rounded-full">
                            <Users className="w-6 h-6 text-purple-600" />
                        </div>
                    </div>
                </div>
            </div>

            {/* Charts Row */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Revenue Chart */}
                <div className="bg-white shadow rounded-lg p-6">
                    <div className="flex justify-between items-center mb-4">
                        <h2 className="text-xl font-semibold">Biểu đồ doanh thu</h2>
                        <div className="relative" ref={revenueMenuRef}>
                            <button
                                onClick={() => setRevenueMenuOpen(!revenueMenuOpen)}
                                className="p-2 hover:bg-gray-100 rounded-full transition-colors"
                                title="Tùy chọn xuất file"
                            >
                                <MoreVertical className="w-5 h-5 text-gray-600" />
                            </button>
                            {revenueMenuOpen && (
                                <div className="absolute right-0 mt-2 w-48 bg-white rounded-lg shadow-lg border border-gray-200 z-10">
                                    <button
                                        onClick={exportRevenueToExcel}
                                        className="w-full text-left px-4 py-2 hover:bg-gray-50 flex items-center gap-2 text-sm"
                                    >
                                        <Download className="w-4 h-4" />
                                        Xuất Excel
                                    </button>
                                    <button
                                        onClick={exportRevenueToCSV}
                                        className="w-full text-left px-4 py-2 hover:bg-gray-50 flex items-center gap-2 text-sm border-t"
                                    >
                                        <Download className="w-4 h-4" />
                                        Xuất CSV
                                    </button>
                                </div>
                            )}
                        </div>
                    </div>
                    {revenueChart && revenueChart.dataPoints.length > 0 ? (
                        <div className="flex gap-4">
                            {/* Y-Axis */}
                            <div className="flex flex-col justify-between h-64 py-2 text-xs text-gray-600 w-20">
                                {(() => {
                                    const maxRevenue = Math.max(...revenueChart.dataPoints.map(p => p.revenue));
                                    const step = maxRevenue / 4;
                                    return [4, 3, 2, 1, 0].map((i) => (
                                        <div key={i} className="text-right">
                                            {formatCurrency(step * i).replace(/\u20AB/g, '').trim()}
                                        </div>
                                    ));
                                })()}
                            </div>
                            {/* Chart */}
                            <div className="flex-1 flex flex-col">
                                <div className="flex items-end justify-around gap-2" style={{ height: '300px', paddingTop: '40px' }}>
                                    {revenueChart.dataPoints.slice(-7).map((point, idx) => {
                                        const maxRevenue = Math.max(...revenueChart.dataPoints.map(p => p.revenue));
                                        const heightPercent = (point.revenue / maxRevenue) * 100;
                                        return (
                                            <div key={idx} className="flex flex-col items-center flex-1">
                                                <div className="relative w-full flex flex-col items-center justify-end" style={{ height: '220px' }}>
                                                    {/* Value on top */}
                                                    <div className="absolute -top-9 text-[10px] text-gray-700 font-semibold text-center w-full overflow-hidden">
                                                        <div className="truncate px-0.5">
                                                            {formatCurrency(point.revenue).replace(/\u20AB/g, '').trim()}
                                                        </div>
                                                    </div>
                                                    <div
                                                        className="w-full bg-blue-600 rounded-t hover:bg-blue-700 transition-colors relative"
                                                        style={{
                                                            height: `${heightPercent}%`,
                                                            minHeight: '4px'
                                                        }}
                                                    >
                                                    </div>
                                                </div>
                                                <div className="text-xs text-gray-600 mt-2 text-center">{point.reportDate}</div>
                                                <div className="text-xs text-gray-500 text-center">{point.orderCount} đơn</div>
                                            </div>
                                        );
                                    })}
                                </div>
                            </div>
                        </div>
                    ) : (
                        <p className="text-gray-600">Chưa có dữ liệu doanh thu.</p>
                    )}
                </div>

                {/* Top Categories */}
                <div className="bg-white shadow rounded-lg p-6">
                    <div className="flex justify-between items-center mb-4">
                        <h2 className="text-xl font-semibold">Danh mục bán chạy</h2>
                        <div className="relative" ref={categoryMenuRef}>
                            <button
                                onClick={() => setCategoryMenuOpen(!categoryMenuOpen)}
                                className="p-2 hover:bg-gray-100 rounded-full transition-colors"
                                title="Tùy chọn xuất file"
                            >
                                <MoreVertical className="w-5 h-5 text-gray-600" />
                            </button>
                            {categoryMenuOpen && (
                                <div className="absolute right-0 mt-2 w-48 bg-white rounded-lg shadow-lg border border-gray-200 z-10">
                                    <button
                                        onClick={exportCategoryToExcel}
                                        className="w-full text-left px-4 py-2 hover:bg-gray-50 flex items-center gap-2 text-sm"
                                    >
                                        <Download className="w-4 h-4" />
                                        Xuất Excel
                                    </button>
                                    <button
                                        onClick={exportCategoryToCSV}
                                        className="w-full text-left px-4 py-2 hover:bg-gray-50 flex items-center gap-2 text-sm border-t"
                                    >
                                        <Download className="w-4 h-4" />
                                        Xuất CSV
                                    </button>
                                </div>
                            )}
                        </div>
                    </div>
                    {topCategories && topCategories.categories.length > 0 ? (
                        <div className="flex gap-4">
                            {/* Y-Axis */}
                            <div className="flex flex-col justify-between h-64 py-2 text-xs text-gray-600 w-20">
                                {(() => {
                                    const maxRevenue = Math.max(...topCategories.categories.map(c => c.totalRevenue));
                                    const step = maxRevenue / 4;
                                    return [4, 3, 2, 1, 0].map((i) => (
                                        <div key={i} className="text-right">
                                            {formatCurrency(step * i).replace(/\u20AB/g, '').trim()}
                                        </div>
                                    ));
                                })()}
                            </div>
                            {/* Chart */}
                            <div className="flex-1 flex flex-col">
                                <div className="flex items-end justify-around gap-2" style={{ height: '300px', paddingTop: '40px' }}>
                                    {topCategories.categories.slice(0, 5).map((category, idx) => {
                                        const maxRevenue = Math.max(...topCategories.categories.map(c => c.totalRevenue));
                                        const heightPercent = (category.totalRevenue / maxRevenue) * 100;
                                        return (
                                            <div key={idx} className="flex flex-col items-center flex-1">
                                                <div className="relative w-full flex flex-col items-center justify-end" style={{ height: '220px' }}>
                                                    {/* Value on top */}
                                                    <div className="absolute -top-9 text-[10px] text-gray-700 font-semibold text-center w-full overflow-hidden">
                                                        <div className="truncate px-0.5">
                                                            {formatCurrency(category.totalRevenue).replace(/\u20AB/g, '').trim()}
                                                        </div>
                                                    </div>
                                                    <div
                                                        className="w-full bg-green-600 rounded-t hover:bg-green-700 transition-colors relative"
                                                        style={{
                                                            height: `${heightPercent}%`,
                                                            minHeight: '4px'
                                                        }}
                                                    >
                                                    </div>
                                                </div>
                                                <div className="text-xs text-gray-600 mt-2 text-center truncate w-full" title={category.categoryName}>
                                                    {category.categoryName}
                                                </div>
                                                <div className="text-xs text-gray-500 text-center">{category.totalSold} sp</div>
                                            </div>
                                        );
                                    })}
                                </div>
                            </div>
                        </div>
                    ) : (
                        <p className="text-gray-600">Chưa có dữ liệu danh mục.</p>
                    )}
                </div>
            </div>

            {/* Profit Charts Row */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Monthly Profit Chart */}
                <div className="bg-white shadow rounded-lg p-6">
                    <div className="flex justify-between items-center mb-4">
                        <h2 className="text-xl font-semibold">Lợi nhuận theo tháng</h2>
                    </div>
                    {monthlyProfitReport && monthlyProfitReport.dataPoints.length > 0 ? (
                        <div className="flex gap-4">
                            {/* Y-Axis */}
                            <div className="flex flex-col justify-between h-64 py-2 text-xs text-gray-600 w-20">
                                {(() => {
                                    const maxProfit = Math.max(...monthlyProfitReport.dataPoints.map(p => p.grossProfit));
                                    const step = maxProfit / 4;
                                    return [4, 3, 2, 1, 0].map((i) => (
                                        <div key={i} className="text-right">
                                            {formatCurrency(step * i).replace(/\u20AB/g, '').trim()}
                                        </div>
                                    ));
                                })()}
                            </div>
                            {/* Chart */}
                            <div className="flex-1 flex flex-col">
                                <div className="flex items-end justify-around gap-2" style={{ height: '300px', paddingTop: '40px' }}>
                                    {monthlyProfitReport.dataPoints.map((point, idx) => {
                                        const maxProfit = Math.max(...monthlyProfitReport.dataPoints.map(p => p.grossProfit));
                                        const heightPercent = maxProfit > 0 ? (point.grossProfit / maxProfit) * 100 : 0;
                                        return (
                                            <div key={idx} className="flex flex-col items-center flex-1">
                                                <div className="relative w-full flex flex-col items-center justify-end" style={{ height: '220px' }}>
                                                    {/* Value on top */}
                                                    <div className="absolute -top-9 text-[10px] text-gray-700 font-semibold text-center w-full overflow-hidden">
                                                        <div className="truncate px-0.5">
                                                            {formatCurrency(point.grossProfit).replace(/\u20AB/g, '').trim()}
                                                        </div>
                                                    </div>
                                                    <div
                                                        className="w-full bg-green-600 rounded-t hover:bg-green-700 transition-colors relative"
                                                        style={{
                                                            height: `${heightPercent}%`,
                                                            minHeight: '4px'
                                                        }}
                                                        title={`Doanh thu: ${formatCurrency(point.totalRevenue)}\nChi phí: ${formatCurrency(point.totalCost)}\nLợi nhuận: ${formatCurrency(point.grossProfit)}`}
                                                    >
                                                    </div>
                                                </div>
                                                <div className="text-xs text-gray-600 mt-2 text-center">{point.month}</div>
                                                <div className="text-xs text-gray-500 text-center">{point.orderCount} đơn</div>
                                            </div>
                                        );
                                    })}
                                </div>
                            </div>
                        </div>
                    ) : (
                        <p className="text-gray-600">Chưa có dữ liệu lợi nhuận theo tháng.</p>
                    )}
                </div>

                {/* Category Profit Chart */}
                <div className="bg-white shadow rounded-lg p-6">
                    <div className="flex justify-between items-center mb-4">
                        <h2 className="text-xl font-semibold">Lợi nhuận theo danh mục</h2>
                    </div>
                    {categoryProfitReport && categoryProfitReport.categories.length > 0 ? (
                        <div className="flex gap-4">
                            {/* Y-Axis */}
                            <div className="flex flex-col justify-between h-64 py-2 text-xs text-gray-600 w-20">
                                {(() => {
                                    const maxProfit = Math.max(...categoryProfitReport.categories.map(c => c.grossProfit));
                                    const step = maxProfit / 4;
                                    return [4, 3, 2, 1, 0].map((i) => (
                                        <div key={i} className="text-right">
                                            {formatCurrency(step * i).replace(/\u20AB/g, '').trim()}
                                        </div>
                                    ));
                                })()}
                            </div>
                            {/* Chart */}
                            <div className="flex-1 flex flex-col">
                                <div className="flex items-end justify-around gap-2" style={{ height: '300px', paddingTop: '40px' }}>
                                    {categoryProfitReport.categories.slice(0, 5).map((category, idx) => {
                                        const maxProfit = Math.max(...categoryProfitReport.categories.map(c => c.grossProfit));
                                        const heightPercent = maxProfit > 0 ? (category.grossProfit / maxProfit) * 100 : 0;
                                        return (
                                            <div key={idx} className="flex flex-col items-center flex-1">
                                                <div className="relative w-full flex flex-col items-center justify-end" style={{ height: '220px' }}>
                                                    {/* Value on top */}
                                                    <div className="absolute -top-9 text-[10px] text-gray-700 font-semibold text-center w-full overflow-hidden">
                                                        <div className="truncate px-0.5">
                                                            {formatCurrency(category.grossProfit).replace(/\u20AB/g, '').trim()}
                                                        </div>
                                                    </div>
                                                    <div
                                                        className="w-full bg-purple-600 rounded-t hover:bg-purple-700 transition-colors relative"
                                                        style={{
                                                            height: `${heightPercent}%`,
                                                            minHeight: '4px'
                                                        }}
                                                        title={`Doanh thu: ${formatCurrency(category.totalRevenue)}\nChi phí: ${formatCurrency(category.totalCost)}\nLợi nhuận: ${formatCurrency(category.grossProfit)}\nTỷ suất: ${category.totalRevenue > 0 ? ((category.grossProfit / category.totalRevenue) * 100).toFixed(1) : '0.0'}%`}
                                                    >
                                                    </div>
                                                </div>
                                                <div className="text-xs text-gray-600 mt-2 text-center truncate w-full" title={category.categoryName}>
                                                    {category.categoryName}
                                                </div>
                                                <div className="text-xs text-gray-500 text-center">{category.totalRevenue > 0 ? ((category.grossProfit / category.totalRevenue) * 100).toFixed(1) : '0.0'}%</div>
                                            </div>
                                        );
                                    })}
                                </div>
                            </div>
                        </div>
                    ) : (
                        <p className="text-gray-600">Chưa có dữ liệu lợi nhuận theo danh mục.</p>
                    )}
                </div>
            </div>

            {/* Old Profit Overview Section - Keep for summary */}
            <div className="grid grid-cols-1 gap-6">
                {/* Profit Overview Chart */}
                <div className="bg-white shadow rounded-lg p-6">
                    <div className="flex justify-between items-center mb-4">
                        <h2 className="text-xl font-semibold">Tổng quan lợi nhuận</h2>
                    </div>
                    {profitReport ? (
                        <div className="space-y-6">
                            {/* Summary Cards */}
                            <div className="grid grid-cols-3 gap-4">
                                <div className="bg-blue-50 p-4 rounded-lg">
                                    <div className="text-sm text-blue-600 font-medium mb-1">Doanh thu</div>
                                    <div className="text-lg font-bold text-blue-700">
                                        {formatCurrency(profitReport.totalRevenue)}
                                    </div>
                                </div>
                                <div className="bg-red-50 p-4 rounded-lg">
                                    <div className="text-sm text-red-600 font-medium mb-1">Chi phí</div>
                                    <div className="text-lg font-bold text-red-700">
                                        {formatCurrency(profitReport.totalCost)}
                                    </div>
                                </div>
                                <div className="bg-green-50 p-4 rounded-lg">
                                    <div className="text-sm text-green-600 font-medium mb-1">Lợi nhuận</div>
                                    <div className="text-lg font-bold text-green-700">
                                        {formatCurrency(profitReport.grossProfit)}
                                    </div>
                                </div>
                            </div>

                            {/* Additional Metrics */}
                            <div className="grid grid-cols-2 gap-4 pt-4 border-t">
                                <div>
                                    <div className="text-xs text-gray-500 mb-1">Chi phí trên doanh thu</div>
                                    <div className="text-lg font-bold text-gray-700">
                                        {profitReport.totalRevenue > 0
                                            ? ((profitReport.totalCost / profitReport.totalRevenue) * 100).toFixed(1)
                                            : '0.0'}%
                                    </div>
                                </div>
                                <div>
                                    <div className="text-xs text-gray-500 mb-1">Lợi nhuận trên doanh thu</div>
                                    <div className="text-lg font-bold text-green-600">
                                        {profitReport.totalRevenue > 0
                                            ? ((profitReport.grossProfit / profitReport.totalRevenue) * 100).toFixed(1)
                                            : '0.0'}%
                                    </div>
                                </div>
                            </div>
                        </div>
                    ) : (
                        <p className="text-gray-600">Chưa có dữ liệu lợi nhuận.</p>
                    )}
                </div>
            </div>


            {/* Top Products */}
            <div className="bg-white shadow rounded-lg p-6">
                <div className="flex justify-between items-center mb-4">
                    <h2 className="text-xl font-semibold">Sản phẩm bán chạy nhất</h2>
                    <div className="flex items-center gap-3">
                        <label className="text-sm text-gray-600">Hiển thị top:</label>
                        <select
                            value={topProductLimit}
                            onChange={(e) => setTopProductLimit(Number(e.target.value))}
                            className="border rounded px-3 py-1.5 text-sm"
                        >
                            <option value="5">5 sản phẩm</option>
                            <option value="10">10 sản phẩm</option>
                            <option value="20">20 sản phẩm</option>
                            <option value="50">50 sản phẩm</option>
                        </select>
                        <button
                            onClick={handleExportExcel}
                            disabled={exporting || !topProducts || topProducts.products.length === 0}
                            className="flex items-center gap-2 px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                            <Download className="w-4 h-4" />
                            {exporting ? 'Đang xuất...' : 'Xuất Excel'}
                        </button>
                    </div>
                </div>
                {topProducts && topProducts.products.length > 0 ? (
                    <div className="overflow-x-auto">
                        <table className="w-full">
                            <thead className="bg-gray-50 border-b">
                                <tr>
                                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Sản phẩm</th>
                                    <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">Đã bán</th>
                                    <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">Doanh thu</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-200">
                                {topProducts.products.map((product, idx) => (
                                    <tr key={idx} className="hover:bg-gray-50">
                                        <td className="px-4 py-3">
                                            <div className="flex items-center gap-3">
                                                {product.primaryImageUrl ? (
                                                    <img
                                                        src={product.primaryImageUrl}
                                                        alt={product.productName}
                                                        className="w-12 h-12 object-cover rounded"
                                                    />
                                                ) : (
                                                    <div className="w-12 h-12 bg-gray-200 rounded flex items-center justify-center">
                                                        <Package className="w-6 h-6 text-gray-400" />
                                                    </div>
                                                )}
                                                <span className="font-medium">{product.productName}</span>
                                            </div>
                                        </td>
                                        <td className="px-4 py-3 text-right">{product.quantitySold}</td>
                                        <td className="px-4 py-3 text-right font-semibold text-green-600">
                                            {formatCurrency(product.revenueGenerated)}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                ) : (
                    <p className="text-gray-600">Chưa có dữ liệu sản phẩm.</p>
                )}
            </div>
        </div>
    )
}
