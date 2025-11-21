import { Fragment, useCallback, useEffect, useMemo, useState } from 'react'
import * as AdminOrderApi from '../../api/admin/orders'
import type { OrderResponse } from '../../api/order'

type AdminOrderStatus = AdminOrderApi.AdminOrderStatus

type StatusFilterOption = {
    value: string
    label: string
    statusQuery: AdminOrderStatus | string
    paymentType?: string
}

const STATUS_FILTERS: ReadonlyArray<StatusFilterOption> = [
    { value: 'ALL', label: 'Tất cả', statusQuery: 'ALL' },
    { value: 'PENDING_ALL', label: 'Chờ thanh toán', statusQuery: 'PENDING' },
    { value: 'PENDING_COD', label: 'Chưa xác nhận COD', statusQuery: 'PENDING', paymentType: 'COD' },
    { value: 'PENDING_ONLINE', label: 'Chờ thanh toán online', statusQuery: 'PENDING', paymentType: 'ONLINE' },
    { value: 'CONFIRMED', label: 'Đã xác nhận', statusQuery: 'CONFIRMED' },
    { value: 'PROCESSING', label: 'Chuẩn bị hàng', statusQuery: 'PROCESSING' },
    { value: 'SHIPPED', label: 'Đang giao', statusQuery: 'SHIPPED' },
    { value: 'DELIVERED', label: 'Đã giao', statusQuery: 'DELIVERED' },
    { value: 'CANCELLED', label: 'Đã hủy', statusQuery: 'CANCELLED' },
    { value: 'REFUNDED', label: 'Đã trả hàng', statusQuery: 'REFUNDED' }
]

const ORDER_STATUS_OPTIONS: ReadonlyArray<{ value: Exclude<AdminOrderStatus, 'ALL'>; label: string }> = [
    { value: 'PENDING', label: 'Chờ thanh toán' },
    { value: 'CONFIRMED', label: 'Đã xác nhận' },
    { value: 'PROCESSING', label: 'Chuẩn bị hàng' },
    { value: 'SHIPPED', label: 'Đang giao' },
    { value: 'DELIVERED', label: 'Đã giao' },
    { value: 'CANCELLED', label: 'Đã hủy' },
    { value: 'REFUNDED', label: 'Đã trả hàng' }
]

const STATUS_STYLES: Record<string, { label: string; className: string }> = {
    PENDING: { label: 'Chờ thanh toán', className: 'bg-amber-100 text-amber-800' },
    CONFIRMED: { label: 'Đã xác nhận', className: 'bg-sky-100 text-sky-800' },
    PROCESSING: { label: 'Chuẩn bị hàng', className: 'bg-indigo-100 text-indigo-800' },
    SHIPPED: { label: 'Đang giao', className: 'bg-blue-100 text-blue-800' },
    DELIVERED: { label: 'Đã giao', className: 'bg-green-100 text-green-800' },
    CANCELLED: { label: 'Đã hủy', className: 'bg-red-100 text-red-700' },
    REFUNDED: { label: 'Đã trả hàng', className: 'bg-purple-100 text-purple-800' }
}

const PAGE_SIZE_OPTIONS = [10, 20, 50] as const

export default function OrderManager() {
    const [orders, setOrders] = useState<OrderResponse[]>([])
    const [statusFilter, setStatusFilter] = useState<StatusFilterOption['value']>('ALL')
    const [page, setPage] = useState(0)
    const [pageSize, setPageSize] = useState<typeof PAGE_SIZE_OPTIONS[number]>(10)
    const [totalPages, setTotalPages] = useState(0)
    const [totalElements, setTotalElements] = useState(0)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)
    const [expandedOrderId, setExpandedOrderId] = useState<string | null>(null)
    const [updatingOrderId, setUpdatingOrderId] = useState<string | null>(null)

    const fetchOrders = useCallback(async () => {
        setLoading(true)
        setError(null)
        try {
            const selectedFilter = STATUS_FILTERS.find(option => option.value === statusFilter) ?? STATUS_FILTERS[0]
            const response = await AdminOrderApi.getOrders({
                status: selectedFilter.statusQuery,
                paymentType: selectedFilter.paymentType,
                page,
                size: pageSize
            })
            setOrders(response.content)
            setTotalPages(response.totalPages)
            setTotalElements(response.totalElements)
        } catch (err) {
            console.error('Failed to load admin orders', err)
            setError('Không thể tải danh sách đơn hàng. Vui lòng thử lại sau.')
            setOrders([])
        } finally {
            setLoading(false)
        }
    }, [page, pageSize, statusFilter])

    useEffect(() => {
        void fetchOrders()
    }, [fetchOrders])

    const handleStatusFilterChange = (value: StatusFilterOption['value']) => {
        if (value !== statusFilter) {
            setStatusFilter(value)
            setPage(0)
        }
    }

    const handlePageChange = (nextPage: number) => {
        if (nextPage >= 0 && nextPage < totalPages) {
            setPage(nextPage)
        }
    }

    const handlePageSizeChange = (event: React.ChangeEvent<HTMLSelectElement>) => {
        const nextSize = Number(event.target.value) as typeof PAGE_SIZE_OPTIONS[number]
        if (nextSize !== pageSize) {
            setPageSize(nextSize)
            setPage(0)
        }
    }

    const handleRefresh = () => {
        void fetchOrders()
    }

    const handleStatusUpdate = async (order: OrderResponse, nextStatus: Exclude<AdminOrderStatus, 'ALL'>) => {
        if (order.status === nextStatus) {
            return
        }
        setUpdatingOrderId(order.id)
        try {
            const updated = await AdminOrderApi.updateOrderStatus(order.id, nextStatus)
            setOrders(prev => prev.map(item => (item.id === updated.id ? updated : item)))
        } catch (err) {
            console.error('Failed to update order status', err)
            alert('Không thể cập nhật trạng thái đơn hàng. Vui lòng thử lại.')
        } finally {
            setUpdatingOrderId(null)
        }
    }

    const statusHeading = useMemo(() => {
        const current = STATUS_FILTERS.find(option => option.value === statusFilter)
        return current?.label ?? 'Đơn hàng'
    }, [statusFilter])

    return (
        <div className="px-6 py-6 space-y-6">
            <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                <div>
                    <h1 className="text-2xl font-bold">Quản lý đơn hàng</h1>
                    <p className="text-sm text-gray-500">{`Tổng quan: ${statusHeading.toLowerCase()}`}</p>
                </div>
                <div className="flex items-center gap-3">
                    <label className="text-sm text-gray-600" htmlFor="pageSize">Số lượng / trang</label>
                    <select
                        id="pageSize"
                        value={pageSize}
                        onChange={handlePageSizeChange}
                        className="border rounded px-3 py-1.5 text-sm"
                    >
                        {PAGE_SIZE_OPTIONS.map(option => (
                            <option key={option} value={option}>{option}</option>
                        ))}
                    </select>
                    <button
                        type="button"
                        onClick={handleRefresh}
                        className="px-4 py-2 text-sm font-medium text-white bg-red-600 rounded hover:bg-red-700"
                    >
                        Làm mới
                    </button>
                </div>
            </div>

            <div className="flex flex-wrap gap-2">
                {STATUS_FILTERS.map(option => {
                    const isActive = option.value === statusFilter
                    return (
                        <button
                            key={option.value}
                            type="button"
                            onClick={() => handleStatusFilterChange(option.value)}
                            className={`px-4 py-2 rounded-full text-sm font-medium border transition ${isActive ? 'bg-red-600 text-white border-red-600 shadow-sm' : 'bg-white text-gray-600 border-gray-200 hover:border-red-300'}`}
                        >
                            {option.label}
                        </button>
                    )
                })}
            </div>

            {loading && (
                <div className="bg-white rounded shadow p-6 text-center text-gray-500">Đang tải dữ liệu...</div>
            )}

            {!loading && error && (
                <div className="bg-white rounded shadow p-6 text-center">
                    <p className="text-gray-600 mb-4">{error}</p>
                    <button
                        type="button"
                        onClick={handleRefresh}
                        className="px-4 py-2 text-sm font-medium text-white bg-red-600 rounded hover:bg-red-700"
                    >
                        Thử lại
                    </button>
                </div>
            )}

            {!loading && !error && orders.length === 0 && (
                <div className="bg-white rounded shadow p-6 text-center text-gray-600">
                    Không có đơn hàng nào trong trạng thái này.
                </div>
            )}

            {!loading && !error && orders.length > 0 && (
                <div className="bg-white rounded shadow overflow-hidden">
                    <div className="overflow-x-auto">
                        <table className="min-w-full table-auto">
                            <thead className="bg-gray-50">
                                <tr>
                                    <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Đơn hàng</th>
                                    <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Khách hàng</th>
                                    <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Giá trị</th>
                                    <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Trạng thái</th>
                                    <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Ngày tạo</th>
                                    <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Cập nhật</th>
                                    <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Thao tác</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-100">
                                {orders.map(order => {
                                    const badge = STATUS_STYLES[order.status] ?? { label: order.status, className: 'bg-gray-100 text-gray-700' }
                                    const shippingName = order.shippingAddress?.fullName || 'Khách lẻ'
                                    const shippingPhone = order.shippingAddress?.phone || ''
                                    const placedAt = order.placedAt ? new Date(order.placedAt).toLocaleString('vi-VN') : 'N/A'
                                    const updatedAt = order.updatedAt ? new Date(order.updatedAt).toLocaleString('vi-VN') : 'N/A'
                                    const isExpanded = expandedOrderId === order.id
                                    const hasSelectableStatus = ORDER_STATUS_OPTIONS.some(option => option.value === order.status)
                                    const selectValue = hasSelectableStatus ? order.status : 'PENDING'
                                    return (
                                        <Fragment key={order.id}>
                                            <tr className={isExpanded ? 'bg-red-50/30' : undefined}>
                                                <td className="px-4 py-3 align-top">
                                                    <div className="font-semibold text-gray-900">#{order.orderNumber}</div>
                                                    <div className="text-xs text-gray-500">ID: {order.id}</div>
                                                </td>
                                                <td className="px-4 py-3 align-top">
                                                    <div className="font-medium text-gray-900">{shippingName}</div>
                                                    {shippingPhone && <div className="text-xs text-gray-500">{shippingPhone}</div>}
                                                </td>
                                                <td className="px-4 py-3 align-top">
                                                    <div className="font-semibold text-red-600">{order.totalAmount.toLocaleString('vi-VN')} ₫</div>
                                                    <div className="text-xs text-gray-500">Ship: {order.shippingAmount.toLocaleString('vi-VN')} ₫</div>
                                                </td>
                                                <td className="px-4 py-3 align-top">
                                                    <span className={`inline-flex px-2.5 py-1 rounded-full text-xs font-semibold ${badge.className}`}>
                                                        {badge.label}
                                                    </span>
                                                </td>
                                                <td className="px-4 py-3 align-top text-sm text-gray-700">{placedAt}</td>
                                                <td className="px-4 py-3 align-top text-sm text-gray-700">{updatedAt}</td>
                                                <td className="px-4 py-3 align-top">
                                                    <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
                                                        <select
                                                            value={selectValue}
                                                            onChange={(event) => handleStatusUpdate(order, event.target.value as Exclude<AdminOrderStatus, 'ALL'>)}
                                                            className="border rounded px-2 py-1 text-sm"
                                                            disabled={updatingOrderId === order.id}
                                                        >
                                                            {ORDER_STATUS_OPTIONS.map(option => (
                                                                <option key={option.value} value={option.value}>
                                                                    {option.label}
                                                                </option>
                                                            ))}
                                                        </select>
                                                        <button
                                                            type="button"
                                                            onClick={() => setExpandedOrderId(prev => (prev === order.id ? null : order.id))}
                                                            className="px-3 py-1.5 text-sm font-medium text-red-600 border border-red-200 rounded hover:bg-red-50"
                                                        >
                                                            {isExpanded ? 'Thu gọn' : 'Xem chi tiết'}
                                                        </button>
                                                    </div>
                                                </td>
                                            </tr>
                                            {isExpanded && (
                                                <tr className="bg-gray-50">
                                                    <td colSpan={7} className="px-6 py-4 text-sm text-gray-700">
                                                        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                                            <div className="space-y-3">
                                                                <h3 className="font-semibold text-gray-900">Thông tin giao hàng</h3>
                                                                {order.shippingAddress ? (
                                                                    <div className="space-y-1 text-gray-700">
                                                                        <p>{order.shippingAddress.fullName}</p>
                                                                        {order.shippingAddress.phone && <p>{order.shippingAddress.phone}</p>}
                                                                        <p>{order.shippingAddress.line1}</p>
                                                                        <p>{[order.shippingAddress.ward, order.shippingAddress.district, order.shippingAddress.province].filter(Boolean).join(', ')}</p>
                                                                    </div>
                                                                ) : (
                                                                    <p className="text-gray-500">Không có thông tin giao hàng</p>
                                                                )}

                                                                {order.notes && (
                                                                    <div>
                                                                        <h4 className="font-medium text-gray-900">Ghi chú</h4>
                                                                        <p className="text-gray-600">{order.notes}</p>
                                                                    </div>
                                                                )}
                                                            </div>
                                                            <div className="space-y-3">
                                                                <h3 className="font-semibold text-gray-900">Sản phẩm ({order.items.length})</h3>
                                                                <div className="space-y-3">
                                                                    {order.items.map(item => (
                                                                        <div key={item.id} className="flex gap-3 border rounded p-3">
                                                                            <img
                                                                                src={item.imageUrl || 'https://placehold.co/80x80'}
                                                                                alt={item.productName}
                                                                                className="w-16 h-16 object-cover rounded"
                                                                            />
                                                                            <div className="flex-1 text-sm">
                                                                                <p className="font-medium text-gray-900">{item.productName}</p>
                                                                                <p className="text-gray-500">{item.variantName ? `${item.variantName} · ` : ''}SKU: {item.sku}</p>
                                                                                <p className="text-gray-500">Số lượng: {item.quantity}</p>
                                                                                <p className="font-semibold text-red-600">{item.totalAmount.toLocaleString('vi-VN')} ₫</p>
                                                                            </div>
                                                                        </div>
                                                                    ))}
                                                                </div>
                                                            </div>
                                                        </div>
                                                    </td>
                                                </tr>
                                            )}
                                        </Fragment>
                                    )
                                })}
                            </tbody>
                        </table>
                    </div>

                    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 px-4 py-4 border-t bg-gray-50 text-sm text-gray-600">
                        <div>
                            {totalElements > 0 ? (
                                <span>
                                    Hiển thị {(page * pageSize) + 1} - {Math.min((page + 1) * pageSize, totalElements)} trên {totalElements} đơn hàng
                                </span>
                            ) : (
                                <span>Không có dữ liệu</span>
                            )}
                        </div>
                        <div className="flex items-center gap-2">
                            <button
                                type="button"
                                onClick={() => handlePageChange(page - 1)}
                                disabled={page === 0}
                                className="px-3 py-1.5 border rounded disabled:opacity-40"
                            >
                                Trước
                            </button>
                            <span>Trang {totalPages === 0 ? 0 : page + 1} / {totalPages}</span>
                            <button
                                type="button"
                                onClick={() => handlePageChange(page + 1)}
                                disabled={page + 1 >= totalPages}
                                className="px-3 py-1.5 border rounded disabled:opacity-40"
                            >
                                Sau
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    )
}
