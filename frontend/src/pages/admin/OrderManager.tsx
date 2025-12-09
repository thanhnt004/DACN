import { Fragment, useCallback, useEffect, useMemo, useState } from 'react';
import { toast } from 'react-toastify';
import * as AdminOrderApi from '../../api/admin/orders';
import type { OrderResponse } from '../../api/order';
import { Search, SlidersHorizontal, Printer } from 'lucide-react';
import AdminOrderActions from './components/AdminOrderActions';
import RejectReasonModal from './components/RejectReasonModal';
import AdminOrderDetailModal from './components/AdminOrderDetailModal';
import { formatInstant } from '../../lib/dateUtils';
import { resolveErrorMessage } from '../../lib/problemDetails';

type OrderTabFilter = AdminOrderApi.OrderTabFilter;

type TabFilterOption = {
    value: OrderTabFilter;
    label: string;
    description?: string;
};

const TAB_FILTERS: ReadonlyArray<TabFilterOption> = [
    { value: 'ALL', label: 'Tất cả', description: 'Tất cả đơn hàng' },
    { value: 'UNPAID', label: 'Chưa thanh toán', description: 'Đơn online chưa thanh toán' },
    { value: 'TO_CONFIRM', label: 'Chờ xác nhận', description: 'Đơn COD mới và đơn đã thanh toán' },
    { value: 'PROCESSING', label: 'Chuẩn bị hàng', description: 'Đang đóng gói' },
    { value: 'SHIPPING', label: 'Đang giao', description: 'Đang vận chuyển' },
    { value: 'COMPLETED', label: 'Đã giao', description: 'Giao hàng thành công' },
    { value: 'CANCEL_REQ', label: 'Yêu cầu hủy', description: 'Chờ duyệt hủy' },
    { value: 'CANCELLED', label: 'Đã hủy', description: 'Đơn đã bị hủy' },
    { value: 'RETURN_REQ', label: 'Yêu cầu trả hàng', description: 'Đang xử lý trả hàng' },
    { value: 'REFUNDED', label: 'Đã trả hàng', description: 'Đã hoàn tiền' },
];

const STATUS_STYLES: Record<string, { label: string; className: string }> = {
    PENDING: { label: 'Chờ xác nhận', className: 'bg-amber-100 text-amber-800' },
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
    const [activeTab, setActiveTab] = useState<OrderTabFilter>('ALL');
    const [page, setPage] = useState(0);
    const [pageSize, setPageSize] = useState<typeof PAGE_SIZE_OPTIONS[number]>(20);
    const [totalPages, setTotalPages] = useState(0);
    const [totalElements, setTotalElements] = useState(0);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [selectedOrderId, setSelectedOrderId] = useState<string | null>(null);
    const [fetchNonce, setFetchNonce] = useState(0);
    
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
            const response = await AdminOrderApi.getOrders({
                tab: activeTab,
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
            const errorMsg = resolveErrorMessage(err, 'Không thể tải danh sách đơn hàng');
            setError(errorMsg);
            setOrders([]);
        } finally {
            setLoading(false);
        }
    }, [page, pageSize, activeTab, orderNumberSearch, startDate, endDate]);

    useEffect(() => {
        void fetchOrders();
    }, [fetchOrders, fetchNonce]);

    const handleFilterSubmit = () => {
        if (page !== 0) {
            setPage(0);
        } else {
            setFetchNonce(n => n + 1);
        }
    };

    const handleClearFilters = () => {
        const filtersWereActive = orderNumberSearch || startDate || endDate;
        setOrderNumberSearch('');
        setStartDate('');
        setEndDate('');
        setSelectedOrders(new Set()); // Clear selection when filters change
        
        if (filtersWereActive) {
            if (page !== 0) {
                setPage(0);
            } else {
                setFetchNonce(n => n + 1);
            }
        }
    };

    const areAllOnPageSelected = useMemo(() => {
        if (orders.length === 0) return false;
        const currentPageIds = new Set(orders.map(o => o.id));
        return orders.every(order => selectedOrders.has(order.id));
    }, [orders, selectedOrders]);

    const handleToggleSelectAll = () => {
        setSelectedOrders(prev => {
            const next = new Set(prev);
            const currentPageIds = orders.map(o => o.id);

            if (areAllOnPageSelected) {
                // If all are selected, deselect them
                currentPageIds.forEach(id => next.delete(id));
            } else {
                // Otherwise, select them all
                currentPageIds.forEach(id => next.add(id));
            }
            return next;
        });
    };

    const handleTabChange = (tab: OrderTabFilter) => {
        if (tab !== activeTab) {
            setActiveTab(tab);
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
            setFetchNonce(n => n + 1);
        } catch (err) {
            const errorMsg = resolveErrorMessage(err, 'Lỗi khi xác nhận đơn hàng');
            toast.error(errorMsg);
            console.error(err);
        }
    };
    
    const handleCancelOrder = async (orderId: string) => {
        if (!confirm('Bạn có chắc muốn hủy đơn hàng này? Thao tác này không thể hoàn tác.')) return;
        try {
            await AdminOrderApi.cancelOrderByAdmin(orderId);
            toast.success('Đã hủy đơn hàng.');
            setFetchNonce(n => n + 1);
        } catch (err) {
            const errorMsg = resolveErrorMessage(err, 'Lỗi khi hủy đơn hàng');
            toast.error(errorMsg);
            console.error(err);
        }
    };

    const handleShipOrders = async (orderIds: string[]) => {
        console.log('[SHIP_ORDERS] Bắt đầu xử lý giao hàng cho:', orderIds);
        if (orderIds.length === 0) return;
        if (!confirm(`Bạn có chắc muốn giao ${orderIds.length} đơn hàng đã chọn?`)) return;

        // Tạo map orderNumber từ orders hiện tại
        const orderMap = new Map(orders.map(o => [o.id, o.orderNumber]));

        // Mở popup loading ngay từ đầu
        console.log('[SHIP_ORDERS] Mở popup loading...');
        const printWindow = window.open('', '_blank');
        if (!printWindow) {
            toast.error('Vui lòng cho phép pop-up để có thể in đơn hàng.');
            return;
        }
        
        // Hiển thị trang loading với nền trong suốt
        printWindow.document.write(`
            <html>
            <head>
                <title>Đang xử lý...</title>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        height: 100vh;
                        margin: 0;
                        background: rgba(0, 0, 0, 0.5);
                    }
                    .loader {
                        background: white;
                        padding: 40px;
                        border-radius: 12px;
                        box-shadow: 0 10px 40px rgba(0,0,0,0.3);
                        text-align: center;
                        color: #333;
                    }
                    .spinner {
                        border: 8px solid rgba(102, 126, 234, 0.2);
                        border-top: 8px solid #667eea;
                        border-radius: 50%;
                        width: 60px;
                        height: 60px;
                        animation: spin 1s linear infinite;
                        margin: 0 auto 20px;
                    }
                    @keyframes spin {
                        0% { transform: rotate(0deg); }
                        100% { transform: rotate(360deg); }
                    }
                    .result {
                        background: white;
                        color: #333;
                        padding: 30px;
                        border-radius: 12px;
                        box-shadow: 0 10px 40px rgba(0,0,0,0.3);
                        max-width: 600px;
                        margin: 20px;
                    }
                    .result h2 {
                        margin-top: 0;
                    }
                    .result h2.success {
                        color: #10b981;
                    }
                    .result h2.error {
                        color: #ef4444;
                    }
                    .result pre {
                        background: #f3f4f6;
                        color: #1f2937;
                        padding: 15px;
                        border-radius: 6px;
                        overflow: auto;
                        font-size: 12px;
                        max-height: 400px;
                        text-align: left;
                    }
                    .result .section {
                        margin: 15px 0;
                        text-align: left;
                    }
                    .result .section h3 {
                        color: #666;
                        margin-bottom: 10px;
                    }
                    .result .order-list {
                        background: #f9fafb;
                        padding: 10px;
                        border-radius: 6px;
                        max-height: 200px;
                        overflow-y: auto;
                    }
                </style>
            </head>
            <body>
                <div class="loader">
                    <div class="spinner"></div>
                    <h2>Đang tạo đơn giao hàng...</h2>
                    <p>Vui lòng chờ trong giây lát</p>
                </div>
            </body>
            </html>
        `);

        try {
            console.log('[SHIP_ORDERS] Gọi API shipOrders...');
            const shipResult = await AdminOrderApi.shipOrders(orderIds);
            console.log('[SHIP_ORDERS] Kết quả shipOrders:', shipResult);
            
            // Ensure arrays exist
            const successItems = shipResult?.successItems || [];
            const failedItems = shipResult?.failedItems || [];

            const successCount = successItems.length;
            const failureCount = failedItems.length;
            
            // Lấy orderNumber cho các đơn thành công và thất bại
            const successOrdersList = successItems
                .map(id => orderMap.get(id) || id)
                .join(', ');
            const failureOrdersList = failedItems
                .map(item => `• ${orderMap.get(item.item) || item.item}: ${item.reason}`)
                .join('\\n');
            
            // Nếu có đơn thành công, gọi API lấy print URL
            if (successCount > 0) {
                try {
                    console.log('[SHIP_ORDERS] Gọi API getPrintUrlForOrders cho các đơn thành công...');
                    const printUrl = await AdminOrderApi.getPrintUrlForOrders(successItems);
                    console.log('[SHIP_ORDERS] Nhận print URL:', printUrl);
                    
                    // Nếu có cả thành công và thất bại, hiển thị kết quả trước khi redirect
                    if (failureCount > 0) {
                        printWindow.document.body.innerHTML = `
                            <div class="result">
                                <h2 class="success">✅ Kết quả giao hàng</h2>
                                <div class="section">
                                    <h3 style="color: #10b981;">Thành công (${successCount} đơn):</h3>
                                    <div class="order-list">${successOrdersList}</div>
                                </div>
                                <div class="section">
                                    <h3 style="color: #ef4444;">Thất bại (${failureCount} đơn):</h3>
                                    <pre>${failureOrdersList}</pre>
                                </div>
                                <p style="margin-top: 20px;">
                                    <button onclick="window.location.href='${printUrl}'" style="padding: 12px 24px; background: #10b981; color: white; border: none; border-radius: 6px; cursor: pointer; font-weight: bold; margin-right: 10px;">In đơn hàng thành công</button>
                                    <button onclick="window.close()" style="padding: 12px 24px; background: #6b7280; color: white; border: none; border-radius: 6px; cursor: pointer; font-weight: bold;">Đóng</button>
                                </p>
                            </div>
                        `;
                        toast.warn(`Giao thành công ${successCount} đơn. Thất bại: ${failureCount} đơn`);
                    } else {
                        // Tất cả thành công, redirect trực tiếp
                        printWindow.location.href = printUrl;
                        toast.success(`Đã tạo và in ${successCount} đơn hàng thành công`);
                    }
                } catch (printErr: any) {
                    console.error('[SHIP_ORDERS] Lỗi khi lấy print URL:', printErr);
                    const errorMsg = resolveErrorMessage(printErr, 'Không thể lấy URL in đơn');
                    
                    // Hiển thị kết quả ship nhưng không có print URL
                    printWindow.document.body.innerHTML = `
                        <div class="result">
                            <h2 class="success">✅ Đã tạo đơn giao hàng</h2>
                            <div class="section">
                                <h3 style="color: #10b981;">Thành công (${successCount} đơn):</h3>
                                <div class="order-list">${successOrdersList}</div>
                            </div>
                            ${failureCount > 0 ? `
                            <div class="section">
                                <h3 style="color: #ef4444;">Thất bại (${failureCount} đơn):</h3>
                                <pre>${failureOrdersList}</pre>
                            </div>
                            ` : ''}
                            <div class="section">
                                <h3 style="color: #f59e0b;">⚠️ Lỗi khi tạo URL in:</h3>
                                <pre>${errorMsg}</pre>
                            </div>
                            <p style="margin-top: 20px;">
                                <button onclick="window.close()" style="padding: 12px 24px; background: #6b7280; color: white; border: none; border-radius: 6px; cursor: pointer; font-weight: bold;">Đóng</button>
                            </p>
                        </div>
                    `;
                    toast.warn(`Đã tạo ${successCount} đơn nhưng không thể lấy URL in`);
                }
            } else {
                // Tất cả đều thất bại HOẶC không có item nào (trường hợp lạ)
                if (failureCount > 0) {
                    printWindow.document.body.innerHTML = `
                        <div class="result">
                            <h2 class="error">❌ Giao hàng thất bại</h2>
                            <div class="section">
                                <h3>Chi tiết lỗi (${failureCount} đơn):</h3>
                                <pre>${failureOrdersList}</pre>
                            </div>
                            <p><button onclick="window.close()" style="padding: 10px 20px; background: #ef4444; color: white; border: none; border-radius: 6px; cursor: pointer; font-weight: bold; margin-top: 10px;">Đóng</button></p>
                        </div>
                    `;
                    toast.error(`Giao hàng thất bại: ${failureCount} đơn`);
                } else {
                     // Trường hợp 0 thành công, 0 thất bại (có thể do backend trả về rỗng hoặc lỗi logic)
                     // Nếu backend trả về 204 (đã fix thành 200), thì successItems phải có dữ liệu.
                     // Nếu vẫn vào đây, ta giả định là thành công nếu orderIds > 0, nhưng an toàn nhất là báo lỗi nhẹ.
                     printWindow.document.body.innerHTML = `
                        <div class="result">
                            <h2 class="error">⚠️ Không có đơn hàng nào được xử lý</h2>
                            <p>Vui lòng thử lại.</p>
                            <p><button onclick="window.close()" style="padding: 10px 20px; background: #6b7280; color: white; border: none; border-radius: 6px; cursor: pointer; font-weight: bold; margin-top: 10px;">Đóng</button></p>
                        </div>
                    `;
                    toast.info('Không có thay đổi nào.');
                }
            }
            
            // Reload data
            setFetchNonce(n => n + 1);
            setSelectedOrders(new Set()); // Clear selection
            
        } catch (err: any) {
            console.error('[SHIP_ORDERS] Lỗi:', err);
            console.error('[SHIP_ORDERS] Error details:', {
                message: err?.message,
                response: err?.response?.data,
                status: err?.response?.status
            });
            
            const errorMsg = resolveErrorMessage(err, 'Lỗi khi giao hàng');
            
            // Hiển thị lỗi trong popup với nền trong suốt
            printWindow.document.body.innerHTML = `
                <div class="result">
                    <h2 class="error">❌ Lỗi giao hàng</h2>
                    <pre>${errorMsg}</pre>
                    <p><button onclick="window.close()" style="padding: 10px 20px; background: #ef4444; color: white; border: none; border-radius: 6px; cursor: pointer; font-weight: bold; margin-top: 10px;">Đóng</button></p>
                </div>
            `;
            
            toast.error(errorMsg);
            setFetchNonce(n => n + 1);
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
            setFetchNonce(n => n + 1);
        } catch (err) {
            const errorMsg = resolveErrorMessage(err, 'Lỗi khi duyệt yêu cầu');
            toast.error(errorMsg);
            console.error(err);
        }
    };
    

    const statusHeading = useMemo(() => {
        const current = TAB_FILTERS.find(option => option.value === activeTab);
        return current?.label ?? 'Đơn hàng';
    }, [activeTab]);
    
    const isProcessingTab = useMemo(() => activeTab === 'PROCESSING', [activeTab]);

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
                        setFetchNonce(n => n + 1);
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
                        onClick={() => setFetchNonce(n => n + 1)}
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
                                type="datetime-local"
                                id="startDate"
                                value={startDate}
                                onChange={(e) => setStartDate(e.target.value)}
                                className="p-2 block w-full border-gray-300 rounded-md shadow-sm focus:ring-red-500 focus:border-red-500 sm:text-sm"
                            />
                        </div>
                        <div>
                           <label htmlFor="endDate" className="block text-sm font-medium text-gray-700 mb-1">Đến ngày</label>
                           <input
                                type="datetime-local"
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
                    {TAB_FILTERS.map(option => {
                        const isActive = option.value === activeTab;
                        return (
                            <button
                                key={option.value}
                                type="button"
                                onClick={() => handleTabChange(option.value)}
                                className={`px-4 py-2 rounded-full text-sm font-medium border transition ${isActive ? 'bg-red-600 text-white border-red-600 shadow-sm' : 'bg-white text-gray-600 border-gray-200 hover:border-red-300'}`}
                                title={option.description}
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
                                    {isProcessingTab && (
                                        <th className="p-3">
                                            <input
                                                type="checkbox"
                                                checked={areAllOnPageSelected}
                                                ref={el => el && (el.indeterminate = !areAllOnPageSelected && selectedOrders.size > 0)}
                                                onChange={handleToggleSelectAll}
                                                className="rounded border-gray-300 text-red-600 shadow-sm focus:border-red-300 focus:ring focus:ring-red-200 focus:ring-opacity-50"
                                            />
                                        </th>
                                    )}
                                    <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Mã đơn</th>
                                    <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Ngày đặt</th>
                                    <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Khách hàng</th>
                                    <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Tổng tiền</th>
                                    <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Thanh toán</th>
                                    <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Trạng thái</th>
                                    <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Vận chuyển</th>
                                    <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Thao tác</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-100">
                                {orders.map(order => {
                                    const badge = STATUS_STYLES[order.status] ?? { label: order.status, className: 'bg-gray-100 text-gray-700' };
                                    // Tính trạng thái thanh toán
                                    const payment = order.payments?.[0];
                                    const isCOD = payment?.provider?.toUpperCase().includes('COD');
                                    const isPaid = payment?.status === 'CAPTURED';
                                    const paymentStatus = isPaid ? 'Đã thanh toán' : 'Chưa thanh toán';
                                    const paymentBadge = isPaid 
                                        ? 'bg-green-100 text-green-800' 
                                        : 'bg-amber-100 text-amber-800';
                                    
                                    return (
                                        <Fragment key={order.id}>
                                            <tr className="hover:bg-gray-50">
                                                {isProcessingTab && (
                                                    <td className="p-3">
                                                        <input type="checkbox" checked={selectedOrders.has(order.id)} onChange={() => handleToggleSelectOrder(order.id)} />
                                                    </td>
                                                )}
                                                {/* Mã đơn */}
                                                <td className="px-4 py-3 align-top">
                                                    <div 
                                                        className="font-bold text-gray-900 hover:text-red-600 cursor-pointer" 
                                                        onClick={() => setSelectedOrderId(order.id)}
                                                    >
                                                        #{order.orderNumber}
                                                    </div>
                                                </td>
                                                {/* Ngày đặt */}
                                                <td className="px-4 py-3 align-top text-sm text-gray-700">
                                                    {formatInstant(order.placedAt, 'vi-VN', {
                                                        day: '2-digit',
                                                        month: '2-digit',
                                                        year: 'numeric'
                                                    })}
                                                    <div className="text-xs text-gray-500">
                                                        {formatInstant(order.placedAt, 'vi-VN', {
                                                            hour: '2-digit',
                                                            minute: '2-digit'
                                                        })}
                                                    </div>
                                                </td>
                                                {/* Khách hàng */}
                                                <td className="px-4 py-3 align-top">
                                                    <div className="font-medium text-gray-900">
                                                        {order.shippingAddress?.fullName || order.shippingAddress?.address || 'Khách lẻ'}
                                                    </div>
                                                    {order.shippingAddress?.phone && (
                                                        <div className="text-xs text-gray-500">{order.shippingAddress.phone}</div>
                                                    )}
                                                </td>
                                                {/* Tổng tiền */}
                                                <td className="px-4 py-3 align-top font-semibold text-red-600">
                                                    {order.totalAmount.toLocaleString('vi-VN')} ₫
                                                </td>
                                                {/* Thanh toán */}
                                                <td className="px-4 py-3 align-top">
                                                    <div className="text-sm font-medium text-gray-700">
                                                        {payment?.provider || 'N/A'}
                                                    </div>
                                                    <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-semibold ${paymentBadge}`}>
                                                        {paymentStatus}
                                                    </span>
                                                </td>
                                                {/* Trạng thái đơn */}
                                                <td className="px-4 py-3 align-top">
                                                    <span className={`inline-flex px-2.5 py-1 rounded-full text-xs font-semibold ${badge.className}`}>
                                                        {badge.label}
                                                    </span>
                                                </td>
                                                {/* Vận chuyển */}
                                                <td className="px-4 py-3 align-top text-sm">
                                                    {order.shipment?.trackingNumber ? (
                                                        <div>
                                                            <div className="font-medium text-gray-700">{order.shipment.trackingNumber}</div>
                                                            {order.shipment.carrier && (
                                                                <div className="text-xs text-gray-500">{order.shipment.carrier}</div>
                                                            )}
                                                        </div>
                                                    ) : (
                                                        <span className="text-gray-400 italic">
                                                            {order.shipment?.warehouse || 'Chưa giao'}
                                                        </span>
                                                    )}
                                                </td>
                                                {/* Thao tác */}
                                                <td className="px-4 py-3 align-top">
                                                    <AdminOrderActions 
                                                        order={order} 
                                                        onConfirm={handleConfirmOrder} 
                                                        onCancel={handleCancelOrder} 
                                                        onShip={() => handleShipOrders([order.id])} 
                                                        onReview={handleReviewRequest} 
                                                    />
                                                </td>
                                            </tr>
                                        </Fragment>
                                    );
                                })}
                            </tbody>
                        </table>
                    </div>
                    
                    {/* Pagination */}
                    <div className="px-4 py-3 border-t border-gray-200 flex items-center justify-between bg-gray-50">
                        <div className="flex items-center gap-2 text-sm text-gray-700">
                            <span>Hiển thị</span>
                            <select
                                value={pageSize}
                                onChange={(e) => {
                                    setPageSize(Number(e.target.value) as typeof PAGE_SIZE_OPTIONS[number]);
                                    setPage(0);
                                }}
                                className="border border-gray-300 rounded px-2 py-1 text-sm"
                            >
                                {PAGE_SIZE_OPTIONS.map(size => (
                                    <option key={size} value={size}>{size}</option>
                                ))}
                            </select>
                            <span>mục</span>
                            <span className="ml-2">|</span>
                            <span className="ml-2">
                                Tổng cộng: <strong>{totalElements}</strong> đơn hàng
                            </span>
                        </div>
                        
                        <div className="flex items-center gap-2">
                            <button
                                onClick={() => handlePageChange(0)}
                                disabled={page === 0}
                                className="px-3 py-1 text-sm border rounded hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                Đầu
                            </button>
                            <button
                                onClick={() => handlePageChange(page - 1)}
                                disabled={page === 0}
                                className="px-3 py-1 text-sm border rounded hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                Trước
                            </button>
                            <span className="px-3 py-1 text-sm text-gray-700">
                                Trang {page + 1} / {totalPages || 1}
                            </span>
                            <button
                                onClick={() => handlePageChange(page + 1)}
                                disabled={page >= totalPages - 1}
                                className="px-3 py-1 text-sm border rounded hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                Sau
                            </button>
                            <button
                                onClick={() => handlePageChange(totalPages - 1)}
                                disabled={page >= totalPages - 1}
                                className="px-3 py-1 text-sm border rounded hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                Cuối
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
