import { useCallback, useEffect, useState } from 'react'
import * as OrderApi from '../../api/order'
import { Link } from 'react-router-dom'
import OrderDetailModal from './components/OrderDetailModal'
import { formatInstant } from '../../lib/dateUtils'

type StatusFilterOption = {
    value: string
    label: string
    query: string
    paymentType?: string
}

const STATUS_FILTER_OPTIONS: readonly StatusFilterOption[] = [
    { value: 'ALL', label: 'Tất cả', query: 'ALL' },
    { value: 'UNPAID', label: 'Chờ thanh toán', query: 'PENDING' },
    { value: 'TO_CONFIRM', label: 'Chờ xác nhận', query: 'CONFIRMED' },
    { value: 'PROCESSING', label: 'Đã xác nhận', query: 'PROCESSING' },
    { value: 'SHIPPING', label: 'Đang giao hàng', query: 'SHIPPED' },
    { value: 'COMPLETED', label: 'Đã giao', query: 'DELIVERED' },
    { value: 'CANCEL_REQ', label: 'Chờ hủy', query: 'CANCELING' },
    { value: 'CANCELLED', label: 'Đã hủy', query: 'CANCELLED' },
    { value: 'RETURN_REQ', label: 'Đang trả hàng', query: 'RETURNING' },
    { value: 'REFUNDED', label: 'Đã trả hàng', query: 'REFUNDED,RETURNED' },
]

type OrderStatusFilter = (typeof STATUS_FILTER_OPTIONS)[number]['value']

const STATUS_BADGE_MAP: Record<string, { label: string; className: string }> = {
    PENDING: { label: 'Chưa thanh toán', className: 'bg-yellow-100 text-yellow-800' },
    CONFIRMED: { label: 'Đã thanh toán - Chờ xác nhận', className: 'bg-blue-100 text-blue-800' },
    PROCESSING: { label: 'Đã xác nhận', className: 'bg-indigo-100 text-indigo-800' },
    SHIPPED: { label: 'Đang giao hàng', className: 'bg-sky-100 text-sky-800' },
    DELIVERED: { label: 'Đã giao', className: 'bg-green-100 text-green-800' },
    CANCELING: { label: 'Đang hủy', className: 'bg-red-100 text-red-800' },
    CANCELLED: { label: 'Đã hủy', className: 'bg-red-100 text-red-800' },
    RETURNING: { label: 'Đang trả hàng', className: 'bg-purple-100 text-purple-800' },
    RETURNED: { label: 'Đã trả hàng', className: 'bg-purple-100 text-purple-800' },
    REFUNDED: { label: 'Đã hoàn tiền', className: 'bg-purple-100 text-purple-800' },
}

export default function MemberOrders() {
    const [orders, setOrders] = useState<OrderApi.OrderResponse[]>([])
    const [loading, setLoading] = useState(true)
    const [statusFilter, setStatusFilter] = useState<OrderStatusFilter>('ALL')
    const [error, setError] = useState<string | null>(null)
    const [selectedOrder, setSelectedOrder] = useState<OrderApi.OrderResponse | null>(null)

    const fetchOrders = useCallback(async () => {
        setLoading(true)
        setError(null)
        try {
            const selectedOption = STATUS_FILTER_OPTIONS.find(option => option.value === statusFilter) ?? STATUS_FILTER_OPTIONS[0]
            console.log('🔍 Fetching orders with:', { status: selectedOption.query, paymentType: selectedOption.paymentType })
            const response = await OrderApi.getOrders(selectedOption.query, 0, 100, selectedOption.paymentType)
            console.log('✅ Orders response:', response)
            console.log('📦 Orders count:', response.content?.length || 0)
            setOrders(response.content)
        } catch (err) {
            console.error('❌ Failed to load orders:', err)
            setOrders([])
            setError('Không thể tải danh sách đơn hàng. Vui lòng thử lại sau.')
        } finally {
            setLoading(false)
        }
    }, [statusFilter])

    useEffect(() => {
        void fetchOrders()
    }, [fetchOrders])

    const handleFilterChange = (value: OrderStatusFilter) => {
        if (value !== statusFilter) {
            setStatusFilter(value)
        }
    }

    const handleRetry = () => {
        void fetchOrders()
    }

    const handleOrderClick = (order: OrderApi.OrderResponse) => {
        setSelectedOrder(order)
    }

    const handleCloseModal = () => {
        setSelectedOrder(null)
    }

    const handleOrderUpdate = () => {
        void fetchOrders()
    }

    return (
        <>
            <div className="bg-white rounded-lg shadow-sm p-6">
                <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 mb-6">
                    <div>
                        <h2 className="text-xl font-bold">Lịch sử mua hàng</h2>
                        <p className="text-sm text-gray-500 mt-1">Theo dõi trạng thái đơn hàng của bạn</p>
                    </div>
                    <div className="flex flex-wrap gap-2">
                        {STATUS_FILTER_OPTIONS.map(option => {
                            const isActive = statusFilter === option.value
                            return (
                                <button
                                    key={option.value}
                                    type="button"
                                    onClick={() => handleFilterChange(option.value)}
                                    className={`px-4 py-2 rounded-full text-sm font-medium border transition whitespace-nowrap ${isActive ? 'bg-red-600 text-white border-red-600 shadow-sm' : 'bg-white text-gray-600 border-gray-200 hover:border-red-300'}`}
                                >
                                    {option.label}
                                </button>
                            )
                        })}
                    </div>
                </div>

                {loading && (
                    <div className="py-10 text-center text-gray-500">Đang tải đơn hàng...</div>
                )}

                {!loading && error && (
                    <div className="py-10 text-center">
                        <p className="text-gray-600 mb-4">{error}</p>
                        <button
                            type="button"
                            onClick={handleRetry}
                            className="inline-flex items-center justify-center px-4 py-2 rounded-md bg-red-600 text-white hover:bg-red-700"
                        >
                            Thử lại
                        </button>
                    </div>
                )}

                {!loading && !error && orders.length === 0 && (
                    <div className="py-10 text-center">
                        <h3 className="text-lg font-semibold mb-3">Không có đơn hàng phù hợp</h3>
                        <p className="text-sm text-gray-500 mb-4">Bạn chưa có đơn ở trạng thái này.</p>
                        <Link to="/products" className="inline-flex items-center justify-center px-4 py-2 rounded-md border border-red-200 text-red-600 hover:bg-red-50">
                            Mua sắm ngay
                        </Link>
                    </div>
                )}

                {!loading && !error && orders.length > 0 && (
                    <div className="space-y-6">
                        {orders.map(order => {
                            const badge = STATUS_BADGE_MAP[order.status] ?? { label: order.status, className: 'bg-gray-100 text-gray-600' }
                            return (
                                <div key={order.id} className="border rounded-lg p-4 cursor-pointer hover:shadow-md transition-shadow" onClick={() => handleOrderClick(order)}>
                                    <div className="flex flex-col md:flex-row md:items-start md:justify-between gap-4 border-b pb-4 mb-4">
                                        <div>
                                            <p className="font-medium text-base">Đơn hàng #{order.orderNumber}</p>
                                            <p className="text-sm text-gray-500">
                                                {formatInstant(order.placedAt, 'vi-VN', {
                                                    day: '2-digit',
                                                    month: '2-digit',
                                                    year: 'numeric',
                                                    hour: '2-digit',
                                                    minute: '2-digit'
                                                })}
                                            </p>
                                        </div>
                                        <div className="text-left md:text-right">
                                            <span className={`inline-flex items-center px-3 py-1 rounded-full text-xs font-semibold ${badge.className}`}>
                                                {badge.label}
                                            </span>
                                            <p className="font-bold text-red-600 mt-2">
                                                {order.totalAmount.toLocaleString('vi-VN')} ₫
                                            </p>
                                        </div>
                                    </div>
                                    <div className="space-y-3">
                                        {order.items.map(item => (
                                            <div key={item.id} className="flex gap-4">
                                                <img
                                                    src={item.imageUrl || 'https://placehold.co/60x60'}
                                                    alt={item.productName}
                                                    className="w-16 h-16 object-cover rounded"
                                                />
                                                <div className="flex-1">
                                                    <div className="flex justify-between">
                                                        <p className="font-medium text-sm text-gray-900">{item.productName}</p>
                                                        <p className="text-sm font-medium text-gray-700">
                                                            {item.totalAmount.toLocaleString('vi-VN')} ₫
                                                        </p>
                                                    </div>
                                                    <p className="text-xs text-gray-500 mt-1">
                                                        {item.variantName ? `${item.variantName} · ` : ''}SKU: {item.sku} · Số lượng: {item.quantity}
                                                    </p>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            )
                        })}
                    </div>
                )}
            </div>
            {selectedOrder && (
                <OrderDetailModal
                    order={selectedOrder}
                    onClose={handleCloseModal}
                    onOrderUpdate={handleOrderUpdate}
                />
            )}
        </>
    )
}
