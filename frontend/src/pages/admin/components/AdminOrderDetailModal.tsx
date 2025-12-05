import { useEffect, useState } from 'react';
import * as OrderApi from '../../../api/order';
import * as AdminOrderApi from '../../../api/admin/orders';
import { X, Package, ThumbsUp, Truck, Loader2, Printer } from 'lucide-react';
import { toast } from 'react-toastify';
import { OrderResponse } from '../../../api/order';
import AdminOrderActions from './AdminOrderActions';

interface AdminOrderDetailModalProps {
    orderId: string;
    onClose: () => void;
    onOrderUpdate: () => void;
}

const STATUS_INFO_MAP: Record<string, { label: string; description: string; icon: React.FC<any> }> = {
    PENDING: { label: 'Chờ xử lý', description: 'Đơn hàng đang chờ được xử lý.', icon: Package },
    CONFIRMED: { label: 'Đã xác nhận', description: 'Đơn hàng đã được xác nhận và sẽ sớm được đóng gói.', icon: ThumbsUp },
    PROCESSING: { label: 'Đang xử lý', description: 'Cửa hàng đang chuẩn bị và đóng gói đơn hàng của bạn.', icon: Package },
    SHIPPED: { label: 'Đang giao hàng', description: 'Đơn hàng đang trên đường đến với bạn.', icon: Truck },
    DELIVERED: { label: 'Đã giao', description: 'Đơn hàng đã được giao thành công.', icon: ThumbsUp },
    CANCELING: { label: 'Yêu cầu hủy', description: 'Yêu cầu hủy đơn của bạn đang được xử lý.', icon: X },
    CANCELLED: { label: 'Đã hủy', description: 'Đơn hàng đã được hủy thành công.', icon: X },
    RETURNING: { label: 'Yêu cầu trả hàng', description: 'Yêu cầu trả hàng của bạn đang được xử lý.', icon: Truck },
    RETURNED: { label: 'Đã trả hàng', description: 'Đơn hàng đã được trả lại thành công.', icon: ThumbsUp },
    REFUNDED: { label: 'Đã hoàn tiền', description: 'Đơn hàng đã được hoàn tiền thành công.', icon: ThumbsUp },
};

export default function AdminOrderDetailModal({ orderId, onClose, onOrderUpdate }: AdminOrderDetailModalProps) {
    const [order, setOrder] = useState<OrderResponse | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchOrder = async () => {
            try {
                const orderDetails = await OrderApi.getOrderDetail(orderId);
                setOrder(orderDetails);
            } catch (error) {
                console.error('Failed to fetch order details:', error);
                toast.error('Không thể tải chi tiết đơn hàng.');
                onClose();
            } finally {
                setLoading(false);
            }
        };
        fetchOrder();
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
        const printWindow = window.open('', '_blank');
        if (printWindow) {
            printWindow.document.write('<html><head><title>Đang xử lý...</title></head><body><p>Đang tạo đơn giao hàng và lấy link in, vui lòng chờ...</p></body></html>');
        } else {
            toast.error('Vui lòng cho phép pop-up để có thể in đơn hàng.');
            return;
        }

        try {
            await AdminOrderApi.shipOrders([id]);
            const printUrl = await AdminOrderApi.getPrintUrlForOrders([id]);
            if (printWindow) {
                printWindow.location.href = printUrl;
            }
            toast.success('Đã tạo yêu cầu giao hàng.');
            onOrderUpdate();
        } catch (err) {
            toast.error('Lỗi khi giao hàng.');
            console.error(err);
            if (printWindow) {
                printWindow.close();
            }
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


    if (loading || !order) {
        return (
            <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
                <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full p-6">
                    <Loader2 className="mx-auto h-12 w-12 animate-spin text-gray-500" />
                    <p className="text-center mt-4">Đang tải chi tiết đơn hàng...</p>
                </div>
            </div>
        );
    }

    const statusInfo = STATUS_INFO_MAP[order.status] ?? { label: order.status, description: 'Trạng thái không xác định', icon: Package };

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
            <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] flex flex-col">
                <div className="flex justify-between items-center p-4 border-b">
                    <h3 className="text-lg font-semibold">Chi tiết đơn hàng #{order.orderNumber}</h3>
                    <button onClick={onClose} className="text-gray-400 hover:text-gray-600"><X /></button>
                </div>

                <div className="p-6 overflow-y-auto">
                    <div className="mb-6">
                        <div className="flex items-center gap-4">
                            <div className="flex-shrink-0 w-12 h-12 rounded-full bg-gray-100 flex items-center justify-center">
                                <statusInfo.icon className="w-6 h-6 text-gray-600" />
                            </div>
                            <div>
                                <h4 className="font-semibold text-lg">{statusInfo.label}</h4>
                                <p className="text-sm text-gray-500">{statusInfo.description}</p>
                            </div>
                        </div>
                    </div>

                    <div className="space-y-4 mb-6">
                        {order.items.map(item => (
                            <div key={item.id} className="flex gap-4">
                                <img src={item.imageUrl || 'https://placehold.co/100x100'} alt={item.productName} className="w-20 h-20 object-cover rounded-lg" />
                                <div className="flex-1">
                                    <p className="font-semibold">{item.productName}</p>
                                    <p className="text-sm text-gray-500">{item.variantName}</p>
                                    <div className="flex justify-between items-center mt-2">
                                        <p className="text-sm">Số lượng: {item.quantity}</p>
                                        <p className="font-semibold text-red-600">{(item.unitPriceAmount * item.quantity).toLocaleString('vi-VN')} ₫</p>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>

                    <div className="border-t pt-4 space-y-2">
                        <div className="flex justify-between"><span>Tạm tính</span><span>{order.subtotalAmount.toLocaleString('vi-VN')} ₫</span></div>
                        <div className="flex justify-between"><span>Phí vận chuyển</span><span>{order.shippingAmount.toLocaleString('vi-VN')} ₫</span></div>
                        <div className="flex justify-between font-bold text-lg"><span>Tổng cộng</span><span className="text-red-600">{order.totalAmount.toLocaleString('vi-VN')} ₫</span></div>
                    </div>
                </div>

                <div className="p-4 border-t flex justify-end items-center gap-4">
                    <AdminOrderActions order={order} onConfirm={handleConfirmOrder} onCancel={handleCancelOrder} onShip={handleShipOrder} onReview={() => {}} />
                    {order.status === 'SHIPPED' && (
                        <button onClick={handlePrintShippingLabel} className="flex items-center gap-2 px-4 py-2 bg-gray-600 text-white rounded-lg hover:bg-gray-700">
                            <Printer size={16} />
                            In phiếu giao
                        </button>
                    )}
                </div>
            </div>
        </div>
    );
}
