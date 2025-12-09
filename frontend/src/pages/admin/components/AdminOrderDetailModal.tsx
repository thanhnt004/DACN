import { useEffect, useState } from 'react';
import * as OrderApi from '../../../api/order';
import * as AdminOrderApi from '../../../api/admin/orders';
import * as AuditLogApi from '../../../api/admin/auditLogs';
import { X, Package, ThumbsUp, Truck, Loader2, Printer, Copy, Phone, MapPin, CreditCard, Clock, User, AlertCircle } from 'lucide-react';
import { toast } from 'react-toastify';
import { OrderResponse } from '../../../api/order';
import { AuditLogResponse } from '../../../api/admin/auditLogs';
import AdminOrderActions from './AdminOrderActions';
import { formatInstant } from '../../../lib/dateUtils';

interface AdminOrderDetailModalProps {
    orderId: string;
    onClose: () => void;
    onOrderUpdate: () => void;
}

const STATUS_INFO_MAP: Record<string, { label: string; description: string; icon: React.FC<any>; color: string }> = {
    PENDING: { label: 'Chờ xử lý', description: 'Đơn hàng đang chờ được xử lý.', icon: Package, color: 'bg-amber-100 text-amber-800 border-amber-200' },
    CONFIRMED: { label: 'Đã xác nhận', description: 'Đơn hàng đã được xác nhận và sẽ sớm được đóng gói.', icon: ThumbsUp, color: 'bg-sky-100 text-sky-800 border-sky-200' },
    PROCESSING: { label: 'Đang xử lý', description: 'Cửa hàng đang chuẩn bị và đóng gói đơn hàng.', icon: Package, color: 'bg-indigo-100 text-indigo-800 border-indigo-200' },
    SHIPPED: { label: 'Đang giao hàng', description: 'Đơn hàng đang trên đường đến.', icon: Truck, color: 'bg-blue-100 text-blue-800 border-blue-200' },
    DELIVERED: { label: 'Đã giao', description: 'Đơn hàng đã được giao thành công.', icon: ThumbsUp, color: 'bg-green-100 text-green-800 border-green-200' },
    CANCELING: { label: 'Yêu cầu hủy', description: 'Yêu cầu hủy đơn đang được xử lý.', icon: X, color: 'bg-red-100 text-red-700 border-red-200' },
    CANCELLED: { label: 'Đã hủy', description: 'Đơn hàng đã được hủy.', icon: X, color: 'bg-red-200 text-red-800 border-red-300' },
    RETURNING: { label: 'Yêu cầu trả hàng', description: 'Yêu cầu trả hàng đang được xử lý.', icon: Truck, color: 'bg-purple-100 text-purple-700 border-purple-200' },
    RETURNED: { label: 'Đã trả hàng', description: 'Đơn hàng đã được trả lại.', icon: ThumbsUp, color: 'bg-purple-200 text-purple-800 border-purple-300' },
    REFUNDED: { label: 'Đã hoàn tiền', description: 'Đơn hàng đã được hoàn tiền.', icon: ThumbsUp, color: 'bg-purple-200 text-purple-800 border-purple-300' },
};

export default function AdminOrderDetailModal({ orderId, onClose, onOrderUpdate }: AdminOrderDetailModalProps) {
    const [order, setOrder] = useState<OrderResponse | null>(null);
    const [auditLogs, setAuditLogs] = useState<AuditLogResponse[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchOrderData = async () => {
            try {
                const [orderDetails, logs] = await Promise.all([
                    OrderApi.getOrderDetail(orderId),
                    AuditLogApi.getEntityHistory(orderId).catch(() => [])
                ]);
                setOrder(orderDetails);
                setAuditLogs(logs);
            } catch (error) {
                console.error('Failed to fetch order details:', error);
                toast.error('Không thể tải chi tiết đơn hàng.');
                onClose();
            } finally {
                setLoading(false);
            }
        };
        fetchOrderData();
    }, [orderId, onClose]);

    const handleConfirmOrder = async (id: string) => {
        await AdminOrderApi.confirmOrders([id]);
        toast.success('Đã xác nhận đơn hàng.');
        onOrderUpdate();
    };

    const handleCancelOrder = async (id: string) => {
        await AdminOrderApi.cancelOrderByAdmin(id);
        toast.success('Đã hủy đơn hàng.');
        onOrderUpdate();
    };

    const handleShipOrder = async (id: string) => {
        console.log('[SHIP_ORDER] Bắt đầu giao đơn hàng:', id);
        
        try {
            console.log('[SHIP_ORDER] Gọi API shipOrders...');
            await AdminOrderApi.shipOrders([id]);
            console.log('[SHIP_ORDER] shipOrders thành công');
            
            // Mở popup loading
            console.log('[SHIP_ORDER] Mở popup loading...');
            const printWindow = window.open('', '_blank');
            if (!printWindow) {
                toast.error('Vui lòng cho phép pop-up để có thể in đơn hàng.');
                toast.success('Đã tạo yêu cầu giao hàng.');
                onOrderUpdate();
                return;
            }
            
            // Hiển thị trang loading
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
                            background: #f5f5f5;
                        }
                        .loader {
                            text-align: center;
                        }
                        .spinner {
                            border: 4px solid #f3f3f3;
                            border-top: 4px solid #3498db;
                            border-radius: 50%;
                            width: 40px;
                            height: 40px;
                            animation: spin 1s linear infinite;
                            margin: 0 auto 20px;
                        }
                        @keyframes spin {
                            0% { transform: rotate(0deg); }
                            100% { transform: rotate(360deg); }
                        }
                        h2 { color: #333; }
                        p { color: #666; }
                    </style>
                </head>
                <body>
                    <div class="loader">
                        <div class="spinner"></div>
                        <h2>Đang lấy link in đơn...</h2>
                        <p>Vui lòng chờ trong giây lát</p>
                    </div>
                </body>
                </html>
            `);
            
            console.log('[SHIP_ORDER] Gọi API getPrintUrlForOrders...');
            const printUrl = await AdminOrderApi.getPrintUrlForOrders([id]);
            console.log('[SHIP_ORDER] Nhận print URL:', printUrl);
            
            // Redirect đến URL in sau khi đã lấy được
            printWindow.location.href = printUrl;
            console.log('[SHIP_ORDER] Đã redirect đến print URL');
            
            toast.success('Đã tạo yêu cầu giao hàng.');
            onOrderUpdate();
        } catch (err: any) {
            console.error('[SHIP_ORDER] Lỗi:', err);
            console.error('[SHIP_ORDER] Error details:', {
                message: err?.message,
                response: err?.response?.data,
                status: err?.response?.status
            });
            
            const errorMsg = err?.response?.data?.message || err?.message || 'Lỗi khi giao hàng.';
            toast.error(errorMsg);
        }
    };
    const handlePrintShippingLabel = async () => {
        if (!order) return;
        const printWindow = window.open('', '_blank');
        if (printWindow) {
            printWindow.document.write('<html><head><title>Đang xử lý...</title></head><body><p>Đang lấy link in, vui lòng chờ...</p></body></html>');
        } else {
            toast.error('Vui lòng cho phép pop-up để có thể in đơn hàng.');
            return;
        }
        try {
            const printUrl = await AdminOrderApi.getPrintUrlForOrders([order.id]);
            if (printWindow) {
                printWindow.location.href = printUrl;
            }
        } catch (error) {
            console.error('Failed to get print URL:', error);
            toast.error('Không thể lấy link in. Vui lòng thử lại.');
            if (printWindow) {
                printWindow.close();
            }
        }
    };


    const copyToClipboard = (text: string, label: string) => {
        navigator.clipboard.writeText(text);
        toast.success(`Đã sao chép ${label}`);
    };

    const formatDateTime = (instantString: string | null | undefined) => {
        if (!instantString) return 'N/A';
        return formatInstant(instantString, 'vi-VN', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    };

    if (loading || !order) {
        return (
            <div 
                className="fixed inset-0 bg-transparent backdrop-blur-sm flex items-center justify-center z-50 p-4"
                onClick={onClose}
            >
                <div 
                    className="bg-white rounded-lg shadow-xl max-w-4xl w-full p-6"
                    onClick={(e) => e.stopPropagation()}
                >
                    <Loader2 className="mx-auto h-12 w-12 animate-spin text-gray-500" />
                    <p className="text-center mt-4">Đang tải chi tiết đơn hàng...</p>
                </div>
            </div>
        );
    }

    const statusInfo = STATUS_INFO_MAP[order.status] ?? { label: order.status, description: 'Trạng thái không xác định', icon: Package, color: 'bg-gray-100 text-gray-700 border-gray-200' };
    const payment = order.payments?.[0];
    const isPaid = payment?.status === 'CAPTURED';
    const shippingAddr = order.shippingAddress;
    const fullAddress = [shippingAddr?.line1, shippingAddr?.ward, shippingAddr?.district, shippingAddr?.province].filter(Boolean).join(', ') || shippingAddr?.address || 'N/A';

    return (
        <div 
            className="fixed inset-0 bg-transparent backdrop-blur-sm flex items-center justify-center z-50 p-4 overflow-y-auto"
            onClick={onClose}
        >
            <div 
                className="bg-white rounded-lg shadow-xl max-w-5xl w-full my-8"
                onClick={(e) => e.stopPropagation()}
            >
                {/* Header */}
                <div className="flex justify-between items-center p-6 border-b bg-gradient-to-r from-red-50 to-orange-50">
                    <div>
                        <h2 className="text-2xl font-bold text-gray-900">Đơn hàng #{order.orderNumber}</h2>
                        <p className="text-sm text-gray-600 mt-1">
                            <Clock className="inline w-4 h-4 mr-1" />
                            Ngày đặt: {formatDateTime(order.placedAt)}
                        </p>
                    </div>
                    <button onClick={onClose} className="text-gray-400 hover:text-gray-600 p-2">
                        <X size={24} />
                    </button>
                </div>

                <div className="p-6 overflow-y-auto max-h-[calc(90vh-200px)]">
                    {/* A. Khối Thông Tin Chung */}
                    <div className={`mb-6 p-4 rounded-lg border-2 ${statusInfo.color}`}>
                        <div className="flex items-start gap-4">
                            <div className="flex-shrink-0 w-12 h-12 rounded-full bg-white flex items-center justify-center shadow">
                                <statusInfo.icon className="w-6 h-6" />
                            </div>
                            <div className="flex-1">
                                <h3 className="font-bold text-lg">{statusInfo.label}</h3>
                                <p className="text-sm mt-1">{statusInfo.description}</p>
                                {order.notes && (
                                    <div className="mt-3 p-3 bg-white bg-opacity-80 rounded-lg border border-orange-300">
                                        <div className="flex items-start gap-2">
                                            <AlertCircle className="w-5 h-5 text-orange-600 flex-shrink-0 mt-0.5" />
                                            <div>
                                                <p className="font-semibold text-orange-800">Ghi chú từ khách hàng:</p>
                                                <p className="text-sm text-gray-700 mt-1">{order.notes}</p>
                                            </div>
                                        </div>
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>

                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                        {/* Left Column */}
                        <div className="space-y-6">
                            {/* B. Khối Sản Phẩm */}
                            <div className="border rounded-lg p-4 bg-gray-50">
                                <h3 className="font-bold text-lg mb-4 flex items-center gap-2">
                                    <Package className="w-5 h-5 text-red-600" />
                                    Sản phẩm ({order.items.length})
                                </h3>
                                <div className="space-y-3">
                                    {order.items.map(item => (
                                        <div key={item.id} className="flex gap-3 bg-white p-3 rounded-lg border">
                                            <img 
                                                src={item.imageUrl || 'https://placehold.co/80x80?text=No+Image'} 
                                                alt={item.productName} 
                                                className="w-20 h-20 object-cover rounded-md flex-shrink-0 border"
                                            />
                                            <div className="flex-1 min-w-0">
                                                <p className="font-semibold text-sm text-gray-900 line-clamp-2">{item.productName}</p>
                                                <p className="text-xs text-gray-600 mt-1">
                                                    <span className="font-medium">Phân loại:</span> {item.variantName}
                                                </p>
                                                <p className="text-xs text-gray-500">SKU: {item.sku}</p>
                                                <div className="flex justify-between items-center mt-2">
                                                    <span className="text-sm text-gray-700">
                                                        {item.unitPriceAmount.toLocaleString('vi-VN')}₫ × {item.quantity}
                                                    </span>
                                                    <span className="font-bold text-red-600">
                                                        {item.totalAmount.toLocaleString('vi-VN')}₫
                                                    </span>
                                                </div>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </div>

                            {/* C. Khối Tài Chính */}
                            <div className="border rounded-lg p-4 bg-gray-50">
                                <h3 className="font-bold text-lg mb-4 flex items-center gap-2">
                                    <CreditCard className="w-5 h-5 text-green-600" />
                                    Thông tin tài chính
                                </h3>
                                <div className="bg-white p-4 rounded-lg space-y-3">
                                    <div className="flex justify-between text-sm">
                                        <span className="text-gray-600">Tạm tính:</span>
                                        <span className="font-medium">{order.subtotalAmount.toLocaleString('vi-VN')}₫</span>
                                    </div>
                                    <div className="flex justify-between text-sm">
                                        <span className="text-gray-600">Phí vận chuyển:</span>
                                        <span className="font-medium">{order.shippingAmount.toLocaleString('vi-VN')}₫</span>
                                    </div>
                                    {order.discountAmount > 0 && (
                                        <div className="flex justify-between text-sm">
                                            <span className="text-gray-600">Giảm giá:</span>
                                            <span className="font-medium text-red-600">-{order.discountAmount.toLocaleString('vi-VN')}₫</span>
                                        </div>
                                    )}
                                    <div className="border-t pt-3 flex justify-between">
                                        <span className="font-bold text-lg">Tổng cộng:</span>
                                        <span className="font-bold text-xl text-red-600">{order.totalAmount.toLocaleString('vi-VN')}₫</span>
                                    </div>
                                    
                                    {/* Payment Info */}
                                    <div className="border-t pt-3 mt-3">
                                        <p className="text-sm font-semibold text-gray-700 mb-2">Thanh toán:</p>
                                        {payment && (
                                            <div className="bg-gray-50 p-3 rounded-lg">
                                                <div className="flex justify-between items-center mb-2">
                                                    <span className="text-sm text-gray-600">Phương thức:</span>
                                                    <span className="font-medium">{payment.provider}</span>
                                                </div>
                                                <div className="flex justify-between items-center">
                                                    <span className="text-sm text-gray-600">Trạng thái:</span>
                                                    <span className={`px-3 py-1 rounded-full text-xs font-semibold ${
                                                        isPaid ? 'bg-green-100 text-green-800' : 'bg-amber-100 text-amber-800'
                                                    }`}>
                                                        {isPaid ? 'Đã thanh toán' : 'Chưa thanh toán'}
                                                    </span>
                                                </div>
                                                {order.paidAt && (
                                                    <div className="mt-2 text-xs text-gray-500">
                                                        Thanh toán lúc: {formatDateTime(order.paidAt)}
                                                    </div>
                                                )}
                                            </div>
                                        )}
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* Right Column */}
                        <div className="space-y-6">
                            {/* D. Khối Thông Tin Giao Hàng */}
                            <div className="border rounded-lg p-4 bg-gray-50">
                                <h3 className="font-bold text-lg mb-4 flex items-center gap-2">
                                    <Truck className="w-5 h-5 text-blue-600" />
                                    Thông tin giao hàng
                                </h3>
                                <div className="bg-white p-4 rounded-lg space-y-3">
                                    <div>
                                        <p className="text-xs text-gray-500 mb-1">Người nhận</p>
                                        <p className="font-semibold text-gray-900 flex items-center gap-2">
                                            <User className="w-4 h-4" />
                                            {shippingAddr?.fullName || 'N/A'}
                                        </p>
                                    </div>
                                    
                                    <div>
                                        <p className="text-xs text-gray-500 mb-1">Số điện thoại</p>
                                        <div className="flex items-center gap-2">
                                            <Phone className="w-4 h-4 text-gray-600" />
                                            <span className="font-medium">{shippingAddr?.phone || 'N/A'}</span>
                                            {shippingAddr?.phone && (
                                                <button
                                                    onClick={() => copyToClipboard(shippingAddr.phone, 'số điện thoại')}
                                                    className="ml-auto p-1 hover:bg-gray-100 rounded"
                                                    title="Sao chép"
                                                >
                                                    <Copy className="w-4 h-4 text-gray-500" />
                                                </button>
                                            )}
                                        </div>
                                    </div>
                                    
                                    <div>
                                        <p className="text-xs text-gray-500 mb-1">Địa chỉ giao hàng</p>
                                        <div className="flex items-start gap-2">
                                            <MapPin className="w-4 h-4 text-gray-600 flex-shrink-0 mt-1" />
                                            <span className="text-sm text-gray-700">{fullAddress}</span>
                                        </div>
                                    </div>

                                    {order.shipment && (
                                        <>
                                            <div className="border-t pt-3 mt-3">
                                                <div className="flex justify-between items-center mb-2">
                                                    <span className="text-sm text-gray-600">Đơn vị vận chuyển:</span>
                                                    <span className="font-semibold">{order.shipment.carrier}</span>
                                                </div>
                                                {order.shipment.trackingNumber && (
                                                    <div className="flex justify-between items-center">
                                                        <span className="text-sm text-gray-600">Mã vận đơn:</span>
                                                        <div className="flex items-center gap-2">
                                                            <span className="font-mono text-sm font-medium">{order.shipment.trackingNumber}</span>
                                                            <button
                                                                onClick={() => copyToClipboard(order.shipment!.trackingNumber!, 'mã vận đơn')}
                                                                className="p-1 hover:bg-gray-100 rounded"
                                                                title="Sao chép"
                                                            >
                                                                <Copy className="w-4 h-4 text-gray-500" />
                                                            </button>
                                                        </div>
                                                    </div>
                                                )}
                                                <div className="mt-2">
                                                    <span className="text-sm text-gray-600">Trạng thái: </span>
                                                    <span className="px-2 py-1 bg-blue-100 text-blue-800 rounded text-xs font-semibold">
                                                        {order.shipment.status}
                                                    </span>
                                                </div>
                                                {order.shipment.deliveredAt && (
                                                    <p className="text-xs text-gray-500 mt-2">
                                                        Giao hàng lúc: {formatDateTime(order.shipment.deliveredAt)}
                                                    </p>
                                                )}
                                            </div>
                                        </>
                                    )}
                                </div>
                            </div>

                            {/* E. Khối Lịch Sử */}
                            <div className="border rounded-lg p-4 bg-gray-50">
                                <h3 className="font-bold text-lg mb-4 flex items-center gap-2">
                                    <Clock className="w-5 h-5 text-purple-600" />
                                    Lịch sử thay đổi
                                </h3>
                                <div className="bg-white p-4 rounded-lg">
                                    {auditLogs.length > 0 ? (
                                        <div className="space-y-3">
                                            {auditLogs.map((log, index) => (
                                                <div key={log.id} className={`flex gap-3 ${index !== auditLogs.length - 1 ? 'pb-3 border-b' : ''}`}>
                                                    <div className="flex-shrink-0 w-2 h-2 rounded-full bg-red-500 mt-2"></div>
                                                    <div className="flex-1">
                                                        <p className="text-sm font-semibold text-gray-900">{log.actionDescription}</p>
                                                        <p className="text-xs text-gray-600 mt-1">
                                                            Bởi: <span className="font-medium">{log.actor?.fullName || log.actor?.email || 'Hệ thống'}</span>
                                                            {log.actor?.role && ` (${log.actor.role})`}
                                                        </p>
                                                        <p className="text-xs text-gray-500 mt-1">
                                                            {formatDateTime(log.createdAt)}
                                                        </p>
                                                    </div>
                                                </div>
                                            ))}
                                        </div>
                                    ) : (
                                        <p className="text-sm text-gray-500 text-center py-4">Chưa có lịch sử thay đổi</p>
                                    )}
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Footer Actions */}
                <div className="p-4 border-t bg-gray-50 flex justify-between items-center gap-4">
                    <button 
                        onClick={onClose}
                        className="px-6 py-2 text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-100"
                    >
                        Đóng
                    </button>
                    <div className="flex gap-3">
                        <AdminOrderActions 
                            order={order} 
                            onConfirm={handleConfirmOrder} 
                            onCancel={handleCancelOrder} 
                            onShip={handleShipOrder} 
                            onReview={() => {}} 
                        />
                        {order.shipment?.trackingNumber && (
                            <button 
                                onClick={handlePrintShippingLabel} 
                                className="flex items-center gap-2 px-4 py-2 bg-gray-600 text-white rounded-lg hover:bg-gray-700"
                            >
                                <Printer size={16} />
                                In phiếu giao
                            </button>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}
