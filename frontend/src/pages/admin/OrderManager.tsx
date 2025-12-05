import { Fragment, useCallback, useEffect, useMemo, useState } from 'react';
import { toast } from 'react-toastify';
import * as AdminOrderApi from '../../api/admin/orders';
import type { OrderResponse } from '../../api/order';
import { Search, SlidersHorizontal, Printer } from 'lucide-react';
import AdminOrderActions from './components/AdminOrderActions';
import RejectReasonModal from './components/RejectReasonModal';
import AdminOrderDetailModal from './components/AdminOrderDetailModal';

type AdminOrderStatus = AdminOrderApi.AdminOrderStatus;

type StatusFilterOption = {
    value: string;
    label: string;
    statusQuery: AdminOrderStatus | string;
    paymentType?: string;
};

const STATUS_FILTERS: ReadonlyArray<StatusFilterOption> = [
    { value: 'ALL', label: 'Tất cả', statusQuery: 'ALL' },
    { value: 'AWAITING_CONFIRMATION', label: 'Chờ xác nhận', statusQuery: 'AWAITING_CONFIRMATION' },
    { value: 'UNPAID', label: 'Chưa thanh toán', statusQuery: 'UNPAID' },
    { value: 'PROCESSING', label: 'Chuẩn bị hàng', statusQuery: 'PROCESSING' },
    { value: 'SHIPPED', label: 'Đang giao', statusQuery: 'SHIPPED' },
    { value: 'DELIVERED', label: 'Đã giao', statusQuery: 'DELIVERED' },
    { value: 'CANCELING', label: 'Yêu cầu hủy', statusQuery: 'CANCELING' },
    { value: 'CANCELLED', label: 'Đã hủy', statusQuery: 'CANCELLED' },
    { value: 'RETURNING', label: 'Yêu cầu trả hàng', statusQuery: 'RETURNING' },
    { value: 'RETURNED', label: 'Đã trả hàng/Hoàn tiền', statusQuery: 'RETURNED,REFUNDED' },
];

const STATUS_STYLES: Record<string, { label: string; className: string }> = {
    PENDING: { label: 'Chờ thanh toán', className: 'bg-amber-100 text-amber-800' },
    CONFIRMED: { label: 'Đã xác nhận', className: 'bg-sky-100 text-sky-800' },
    PROCESSING: { label: 'Chuẩn bị hàng', className: 'bg-indigo-100 text-indigo-800' },
    SHIPPED: { label: 'Đang giao', className: 'bg-blue-100 text-blue-800' },
    DELIVERED: { label: 'Đã giao', className: 'bg-green-100 text-green-800' },
    CANCELING: { label: 'Yêu cầu hủy', className: 'bg-red-100 text-red-700' },
    CANCELLED: { label: 'Đã hủy', className: 'bg-red-200 text-red-800' },
    RETURNING: { label: 'Yêu cầu trả hàng', className: 'bg-purple-100 text-purple-700' },
    RETURNED: { label: 'Đã trả hàng', className: 'bg-purple-200 text-purple-800' },
    REFUNDED: { label: 'Đã hoàn tiền', className: 'bg-purple-200 text-purple-800' },
};


const PAGE_SIZE_OPTIONS = [10, 20, 50] as const;

export default function OrderManager() {
    const [orders, setOrders] = useState<OrderResponse[]>([]);
    const [statusFilter, setStatusFilter] = useState<StatusFilterOption['value']>('ALL');
    const [page, setPage] = useState(0);
    const [pageSize, setPageSize] = useState<typeof PAGE_SIZE_OPTIONS[number]>(20);
    const [totalPages, setTotalPages] = useState(0);
    const [totalElements, setTotalElements] = useState(0);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [selectedOrderId, setSelectedOrderId] = useState<string | null>(null);
    
    // Filters
    const [orderNumberSearch, setOrderNumberSearch] = useState('');
    const [startDate, setStartDate] = useState('');
    const [endDate, setEndDate] = useState('');
    const [isFilterVisible, setIsFilterVisible] = useState(false);

    // Bulk actions
    const [selectedOrders, setSelectedOrders] = useState(new Set<string>());

    // Modals
    const [rejectModalState, setRejectModalState] = useState<{ isOpen: boolean; requestId: string | null }>({ isOpen: false, requestId: null });

    const fetchOrders = useCallback(async () => {
        setLoading(true);
        setError(null);
        setSelectedOrders(new Set()); // Clear selection on fetch
        try {
            const selectedFilter = STATUS_FILTERS.find(option => option.value === statusFilter) ?? STATUS_FILTERS[0];
            const response = await AdminOrderApi.getOrders({
                status: selectedFilter.statusQuery,
                paymentType: selectedFilter.paymentType,
                page,
                size: pageSize,
                orderNumber: orderNumberSearch,
                startDate: startDate || undefined,
                endDate: endDate || undefined,
            });
            setOrders(response.content);
            setTotalPages(response.totalPages);
            setTotalElements(response.totalElements);
        } catch (err) {
            console.error('Failed to load admin orders', err);
            setError('Không thể tải danh sách đơn hàng. Vui lòng thử lại sau.');
            setOrders([]);
        } finally {
            setLoading(false);
        }
    }, [page, pageSize, statusFilter, orderNumberSearch, startDate, endDate]);

    useEffect(() => {
        void fetchOrders();
    }, [fetchOrders]);

    const handleFilterSubmit = () => {
        setPage(0);
        void fetchOrders();
    };

    const handleClearFilters = () => {
        setOrderNumberSearch('');
        setStartDate('');
        setEndDate('');
        if (orderNumberSearch || startDate || endDate) {
            setPage(0);
        }
    };

    const handleStatusFilterChange = (value: StatusFilterOption['value']) => {
        if (value !== statusFilter) {
            setStatusFilter(value);
            setPage(0);
        }
    };

    const handlePageChange = (nextPage: number) => {
        if (nextPage >= 0 && nextPage < totalPages) {
            setPage(nextPage);
        }
    };
    
    const handleToggleSelectOrder = (orderId: string) => {
        setSelectedOrders(prev => {
            const next = new Set(prev);
            if (next.has(orderId)) {
                next.delete(orderId);
            } else {
                next.add(orderId);
            }
            return next;
        });
    };
    
    // --- Action Handlers ---
    const handleConfirmOrder = async (orderId: string) => {
        if (!confirm('Bạn có chắc muốn xác nhận đơn hàng này?')) return;
        try {
            await AdminOrderApi.confirmOrders([orderId]);
            toast.success('Đã xác nhận đơn hàng.');
            void fetchOrders();
        } catch (err) {
            toast.error('Lỗi khi xác nhận đơn hàng.');
            console.error(err);
        }
    };
    
    const handleCancelOrder = async (orderId: string) => {
        if (!confirm('Bạn có chắc muốn hủy đơn hàng này? Thao tác này không thể hoàn tác.')) return;
        try {
            await AdminOrderApi.cancelOrderByAdmin(orderId);
            toast.success('Đã hủy đơn hàng.');
            void fetchOrders();
        } catch (err) {
            toast.error('Lỗi khi hủy đơn hàng.');
            console.error(err);
        }
    };

    const handleShipOrders = async (orderIds: string[]) => {
        if (orderIds.length === 0) return;
        if (!confirm(`Bạn có chắc muốn giao ${orderIds.length} đơn hàng đã chọn?`)) return;

        const printWindow = window.open('', '_blank');
        if (printWindow) {
            printWindow.document.write('<html><head><title>Đang xử lý...</title></head><body><p>Đang tạo đơn giao hàng và lấy link in, vui lòng chờ...</p></body></html>');
        } else {
            toast.error('Vui lòng cho phép pop-up để có thể in đơn hàng.');
            return;
        }

        try {
            const shipResult = await AdminOrderApi.shipOrders(orderIds);
            if (shipResult.hasFailures) {
                toast.warn(`Giao thành công ${shipResult.successes.length} đơn. Thất bại: ${Object.keys(shipResult.failures).length}`);
            } else {
                toast.success(`Đã tạo yêu cầu giao ${shipResult.successes.length} đơn hàng.`);
            }
            
            const printUrl = await AdminOrderApi.getPrintUrlForOrders(orderIds);
            
            if (printWindow) {
                printWindow.location.href = printUrl;
            }

            void fetchOrders();
        } catch (err) {
            toast.error('Lỗi khi giao hàng.');
            console.error(err);
            if (printWindow) {
                printWindow.close();
            }
        }
    };

    const handleReviewRequest = async (requestId: string, decision: 'APPROVED' | 'REJECTED', adminNote?: string) => {
        if (decision === 'REJECTED' && !adminNote) {
            setRejectModalState({ isOpen: true, requestId });
            return;
        }
        
        try {
            await AdminOrderApi.reviewChangeRequest(requestId, { status: decision, adminNote });
            toast.success('Đã duyệt yêu cầu.');
            setRejectModalState({ isOpen: false, requestId: null });
            void fetchOrders();
        } catch (err) {
            toast.error('Lỗi khi duyệt yêu cầu.');
            console.error(err);
        }
    };
    

    const statusHeading = useMemo(() => {
        const current = STATUS_FILTERS.find(option => option.value === statusFilter);
        return current?.label ?? 'Đơn hàng';
    }, [statusFilter]);
    
    const isProcessingTab = useMemo(() => STATUS_FILTERS.find(f => f.value === statusFilter)?.statusQuery === 'PROCESSING', [statusFilter]);

    return (
        <div className="px-6 py-6 space-y-6">
            <RejectReasonModal
                isOpen={rejectModalState.isOpen}
                onClose={() => setRejectModalState({ isOpen: false, requestId: null })}
                onSubmit={(reason) => {
                    if (rejectModalState.requestId) {
                        handleReviewRequest(rejectModalState.requestId, 'REJECTED', reason);
                    }
                }}
                title="Từ chối yêu cầu"
                prompt="Vui lòng cung cấp lý do từ chối yêu cầu này."
            />
            {selectedOrderId && (
                <AdminOrderDetailModal
                    orderId={selectedOrderId}
                    onClose={() => setSelectedOrderId(null)}
                    onOrderUpdate={() => {
                        setSelectedOrderId(null);
                        fetchOrders();
                    }}
                />
            )}

            <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                <div>
                    <h1 className="text-2xl font-bold">Quản lý đơn hàng</h1>
                    <p className="text-sm text-gray-500">{`Tổng quan: ${statusHeading.toLowerCase()}`}</p>
                </div>
                 <div className="flex items-center gap-3">
                    <button
                        type="button"
                        onClick={() => setIsFilterVisible(!isFilterVisible)}
                        className="p-2 border rounded text-gray-600 hover:bg-gray-50"
                        title="Tìm kiếm và Lọc"
                    >
                        <SlidersHorizontal size={20} />
                    </button>
                    <button
                        type="button"
                        onClick={fetchOrders}
                        className="px-4 py-2 text-sm font-medium text-white bg-red-600 rounded hover:bg-red-700"
                    >
                        Làm mới
                    </button>
                </div>
            </div>
            
            {isFilterVisible && (
                <div className="bg-white p-4 rounded-lg shadow space-y-4">
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                        <div className="md:col-span-1">
                             <label htmlFor="orderNumber" className="block text-sm font-medium text-gray-700 mb-1">Mã đơn hàng</label>
                            <div className="relative">
                               <span className="absolute inset-y-0 left-0 flex items-center pl-3">
                                  <Search className="h-5 w-5 text-gray-400" />
                               </span>
                               <input
                                   type="text"
                                   id="orderNumber"
                                   value={orderNumberSearch}
                                   onChange={(e) => setOrderNumberSearch(e.target.value)}
                                   className="pl-10 p-2 block w-full border-gray-300 rounded-md shadow-sm focus:ring-red-500 focus:border-red-500 sm:text-sm"
                                   placeholder="Tìm theo mã đơn hàng..."
                               />
                            </div>
                        </div>
                        <div>
                             <label htmlFor="startDate" className="block text-sm font-medium text-gray-700 mb-1">Từ ngày</label>
                             <input
                                type="date"
                                id="startDate"
                                value={startDate}
                                onChange={(e) => setStartDate(e.target.value)}
                                className="p-2 block w-full border-gray-300 rounded-md shadow-sm focus:ring-red-500 focus:border-red-500 sm:text-sm"
                            />
                        </div>
                        <div>
                           <label htmlFor="endDate" className="block text-sm font-medium text-gray-700 mb-1">Đến ngày</label>
                           <input
                                type="date"
                                id="endDate"
                                value={endDate}
                                onChange={(e) => setEndDate(e.target.value)}
                                className="p-2 block w-full border-gray-300 rounded-md shadow-sm focus:ring-red-500 focus:border-red-500 sm:text-sm"
                            />
                        </div>
                    </div>
                     <div className="flex justify-end gap-3">
                        <button onClick={handleClearFilters} className="px-4 py-2 text-sm text-gray-700 bg-gray-100 rounded-md hover:bg-gray-200">Xóa bộ lọc</button>
                        <button onClick={handleFilterSubmit} className="px-4 py-2 text-sm text-white bg-red-600 rounded-md hover:bg-red-700">Áp dụng</button>
                    </div>
                </div>
            )}


            <div className="flex flex-wrap items-center justify-between gap-2">
                 <div className="flex flex-wrap gap-2">
                    {STATUS_FILTERS.map(option => {
                        const isActive = option.value === statusFilter;
                        return (
                            <button
                                key={option.value}
                                type="button"
                                onClick={() => handleStatusFilterChange(option.value)}
                                className={`px-4 py-2 rounded-full text-sm font-medium border transition ${isActive ? 'bg-red-600 text-white border-red-600 shadow-sm' : 'bg-white text-gray-600 border-gray-200 hover:border-red-300'}`}
                            >
                                {option.label}
                            </button>
                        );
                    })}
                </div>
                {isProcessingTab && selectedOrders.size > 0 && (
                     <button onClick={() => handleShipOrders(Array.from(selectedOrders))} className="px-4 py-2 text-sm font-medium text-white bg-sky-600 rounded-md hover:bg-sky-700 flex items-center gap-2">
                        <Printer size={16} />
                        Giao & In {selectedOrders.size} đơn đã chọn
                    </button>
                )}
            </div>

            {loading && (
                <div className="bg-white rounded shadow p-6 text-center text-gray-500">Đang tải dữ liệu...</div>
            )}

            {!loading && !error && orders.length === 0 && (
                <div className="bg-white rounded shadow p-6 text-center text-gray-600">
                    Không có đơn hàng nào phù hợp với tiêu chí.
                </div>
            )}

            {!loading && !error && orders.length > 0 && (
                <div className="bg-white rounded shadow overflow-hidden">
                    <div className="overflow-x-auto">
                        <table className="min-w-full table-auto">
                            <thead className="bg-gray-50">
                                <tr>
                                    {isProcessingTab && <th className="p-3"><input type="checkbox" disabled /></th>}
                                    <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Đơn hàng</th>
                                    <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Khách hàng</th>
                                    <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Giá trị</th>
                                    <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Trạng thái</th>
                                    <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Ngày tạo</th>
                                    <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Thao tác</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-100">
                                {orders.map(order => {
                                    const badge = STATUS_STYLES[order.status] ?? { label: order.status, className: 'bg-gray-100 text-gray-700' };
                                    return (
                                        <Fragment key={order.id}>
                                            <tr>
                                                {isProcessingTab && (
                                                    <td className="p-3">
                                                        <input type="checkbox" checked={selectedOrders.has(order.id)} onChange={() => handleToggleSelectOrder(order.id)} />
                                                    </td>
                                                )}
                                                <td className="px-4 py-3 align-top">
                                                    <div className="font-semibold text-gray-900 hover:text-red-600 cursor-pointer" onClick={() => setSelectedOrderId(order.id)}>#{order.orderNumber}</div>
                                                </td>
                                                <td className="px-4 py-3 align-top">
                                                    <div className="font-medium text-gray-900">{order.shippingAddress?.fullName || 'Khách lẻ'}</div>
                                                    {order.shippingAddress?.phone && <div className="text-xs text-gray-500">{order.shippingAddress.phone}</div>}
                                                </td>
                                                <td className="px-4 py-3 align-top font-semibold text-red-600">{order.totalAmount.toLocaleString('vi-VN')} ₫</td>
                                                <td className="px-4 py-3 align-top"><span className={`inline-flex px-2.5 py-1 rounded-full text-xs font-semibold ${badge.className}`}>{badge.label}</span></td>
                                                <td className="px-4 py-3 align-top text-sm text-gray-700">{new Date(order.placedAt).toLocaleString('vi-VN')}</td>
                                                <td className="px-4 py-3 align-top">
                                                    <AdminOrderActions order={order} onConfirm={handleConfirmOrder} onCancel={handleCancelOrder} onShip={() => handleShipOrders([order.id])} onReview={handleReviewRequest} />
                                                </td>
                                            </tr>
                                        </Fragment>
                                    );
                                })}
                            </tbody>
                        </table>
                    </div>
                    {/* Pagination etc. */}
                </div>
            )}
        </div>
    );
}
