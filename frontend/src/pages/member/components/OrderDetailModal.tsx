import { useMemo, useState, useEffect } from 'react';
import * as OrderApi from '../../../api/order';
import { uploadImagesToCloudinary } from '../../../api/media';
import { X, Package, ThumbsUp, Truck, Undo2, Loader2, UploadCloud, Clock, AlertCircle, CheckCircle } from 'lucide-react';
import { toast } from 'react-toastify';
import { OrderResponse } from '../../../api/order';
import { formatInstant } from '../../../lib/dateUtils';

interface OrderDetailModalProps {
    order: OrderResponse;
    onClose: () => void;
    onOrderUpdate: () => void;
}

const CANCELLATION_REASONS = [
    'Tôi không muốn mua nữa',
    'Tôi tìm thấy chỗ khác giá tốt hơn',
    'Tôi đã đặt nhầm sản phẩm',
    'Khác'
];

const STATUS_INFO_MAP: Record<string, { label: string; description: string; icon: React.FC<{ className?: string }> }> = {
    PENDING: { label: 'Chưa thanh toán', description: 'Đơn hàng của bạn đang chờ thanh toán.', icon: Package },
    CONFIRMED: { label: 'Đã xác nhận', description: 'Đơn hàng đã được xác nhận và sẽ sớm được đóng gói.', icon: ThumbsUp },
    PROCESSING: { label: 'Đang xử lý', description: 'Cửa hàng đang chuẩn bị và đóng gói đơn hàng của bạn.', icon: Package },
    SHIPPED: { label: 'Đang giao hàng', description: 'Đơn hàng đang trên đường đến với bạn.', icon: Truck },
    DELIVERED: { label: 'Đã giao', description: 'Đơn hàng đã được giao thành công.', icon: ThumbsUp },
    CANCELING: { label: 'Yêu cầu hủy', description: 'Yêu cầu hủy đơn của bạn đang được xử lý.', icon: Undo2 },
    CANCELLED: { label: 'Đã hủy', description: 'Đơn hàng đã được hủy thành công.', icon: X },
    RETURNING: { label: 'Yêu cầu trả hàng', description: 'Yêu cầu trả hàng của bạn đang được xử lý.', icon: Undo2 },
    RETURNED: { label: 'Đã trả hàng', description: 'Đơn hàng đã được trả lại thành công.', icon: Undo2 },
    REFUNDED: { label: 'Đã hoàn tiền', description: 'Đơn hàng đã được hoàn tiền thành công.', icon: Undo2 },
};

export default function OrderDetailModal({ order, onClose, onOrderUpdate }: OrderDetailModalProps) {
    const [isCancelModalOpen, setIsCancelModalOpen] = useState(false);
    const [cancelReason, setCancelReason] = useState('');
    const [otherReason, setOtherReason] = useState('');
    const [isReturnModalOpen, setIsReturnModalOpen] = useState(false);
    const [returnReason, setReturnReason] = useState('');
    const [returnImages, setReturnImages] = useState<FileList | null>(null);
    const [isUploading, setIsUploading] = useState(false);
    const [timeRemaining, setTimeRemaining] = useState<number | null>(null);

    const [paymentRefundMethod, setPaymentRefundMethod] = useState('BANK_TRANSFER');
    const [bankName, setBankName] = useState('');
    const [accountNumber, setAccountNumber] = useState('');
    const [accountName, setAccountName] = useState('');
    const [returnOptionType, setReturnOptionType] = useState('PICKUP');

    const statusInfo = useMemo(() => {
        return STATUS_INFO_MAP[order.status] || { label: 'Không xác định', description: 'Trạng thái đơn hàng không xác định.', icon: Package };
    }, [order.status]);

    const isPendingUnpaidNonCod = useMemo(() => {
        const payment = order.payments?.[0];
        return order.status === 'PENDING' && payment && payment.provider !== 'COD' && payment.status !== 'CAPTURED';
    }, [order.status, order.payments]);

    const isWithinPaymentWindow = useMemo(() => {
        if (!order.createdAt) return false;
        const creationTime = new Date(order.createdAt).getTime();
        const currentTime = new Date().getTime();
        const paymentWindow = 15 * 60 * 1000; // 15 minutes
        return currentTime - creationTime < paymentWindow;
    }, [order.createdAt]);

    useEffect(() => {
        let timer: NodeJS.Timeout | undefined;
        if (isPendingUnpaidNonCod && order.createdAt) {
            const calculateTimeRemaining = () => {
                const creationTime = new Date(order.createdAt).getTime();
                const currentTime = new Date().getTime();
                const paymentWindow = 15 * 60 * 1000;
                const elapsed = currentTime - creationTime;
                const remaining = Math.max(0, paymentWindow - elapsed);
                setTimeRemaining(Math.round(remaining / 1000));

                if (remaining <= 0) {
                    clearInterval(timer);
                }
            };
            calculateTimeRemaining();
            timer = setInterval(calculateTimeRemaining, 1000);
        }
        return () => clearInterval(timer);
    }, [isPendingUnpaidNonCod, order.createdAt]);

    const handleOpenCancelModal = () => setIsCancelModalOpen(true);
    const handleCloseCancelModal = () => setIsCancelModalOpen(false);

    const handleConfirmCancel = async () => {
        const finalReason = cancelReason === 'Khác' ? otherReason : cancelReason;
        if (!finalReason || (cancelReason === 'Khác' && otherReason.length < 5)) {
            toast.error('Vui lòng cung cấp lý do hủy đơn hợp lệ.');
            return;
        }

        const requiresRefundInfo = order.payments?.some(p => p.status === 'CAPTURED' && p.provider !== 'COD');
        if (requiresRefundInfo && paymentRefundMethod === 'BANK_TRANSFER' && (!bankName || !accountNumber || !accountName)) {
            toast.error('Vui lòng cung cấp đầy đủ thông tin tài khoản ngân hàng để hoàn tiền.');
            return;
        }

        try {
            const payload: OrderApi.CancelOrderRequest = {
                reason: finalReason,
                paymentRefundOption: requiresRefundInfo
                    ? {
                        method: paymentRefundMethod,
                        data: paymentRefundMethod === 'BANK_TRANSFER' ? { bankName, accountNumber, accountName } : {}
                    }
                    : undefined
            };
            await OrderApi.requestCancel(order.id, payload);
            toast.success('Yêu cầu hủy đơn hàng đã được gửi.');
            onOrderUpdate();
            handleCloseCancelModal();
            onClose();
        } catch (error) {
            console.error('Failed to cancel order:', error);
            toast.error('Không thể gửi yêu cầu hủy đơn hàng.');
        }
    };

    const handleRePay = async () => {
        try {
            const paymentUrl = await OrderApi.retryPayment(order.id);
            if (paymentUrl) {
                window.location.href = paymentUrl;
            } else {
                toast.error('Không thể lấy được link thanh toán. Vui lòng thử lại.');
            }
        } catch (error) {
            console.error('Failed to retry payment:', error);
            toast.error('Thanh toán lại thất bại. Vui lòng thử lại.');
        }
    };
    
        // State cho modal chỉnh sửa thông tin hoàn tiền
        const [isEditRefundModalOpen, setIsEditRefundModalOpen] = useState(false);
        const [editRefundImage, setEditRefundImage] = useState(order.changeRequest?.metadata?.refundImage || '');
        const [editRefundNote, setEditRefundNote] = useState(order.changeRequest?.metadata?.refundNote || order.changeRequest?.adminNote || '');

        // Hàm xử lý lưu thông tin hoàn tiền đã chỉnh sửa
        const handleSaveRefundInfo = async () => {
            // TODO: Gọi API cập nhật thông tin hoàn tiền cho đơn hàng bị hủy
            // Ví dụ: await OrderApi.updateRefundInfo(order.changeRequest?.id, { refundImage: editRefundImage, refundNote: editRefundNote })
            toast.success('Đã cập nhật thông tin hoàn tiền!');
            setIsEditRefundModalOpen(false);
            onOrderUpdate();
        };

    // --- Return Logic ---
    const handleOpenReturnModal = () => setIsReturnModalOpen(true);
    const handleCloseReturnModal = () => setIsReturnModalOpen(false);

    const handleConfirmReturn = async () => {
        if (!returnReason || returnReason.trim().length < 10) {
            toast.error('Vui lòng mô tả lý do trả hàng chi tiết (tối thiểu 10 ký tự).');
            return;
        }
        if (!order.shippingAddress) {
            toast.error('Không tìm thấy địa chỉ giao hàng để tạo yêu cầu trả hàng.');
            return;
        }

        // Validate payment refund info if BANK_TRANSFER
        if (paymentRefundMethod === 'BANK_TRANSFER') {
            if (!bankName || !accountNumber || !accountName) {
                toast.error('Vui lòng cung cấp đầy đủ thông tin tài khoản ngân hàng để hoàn tiền.');
                return;
            }
        }

        setIsUploading(true);
        try {
            let uploadedImageUrls: string[] = [];
            if (returnImages && returnImages.length > 0) {
                uploadedImageUrls = await uploadImagesToCloudinary(Array.from(returnImages), 'return', order.id);
            }

            const payload: OrderApi.ReturnOrderRequest = {
                reason: returnReason,
                images: uploadedImageUrls,
                returnAddress: order.shippingAddress,
                returnOption: {
                    type: returnOptionType,
                    description: returnOptionType === 'PICKUP' ? 'Nhân viên đến lấy hàng' : 'Tự gửi hàng về shop'
                },
                paymentRefundOption: {
                    method: paymentRefundMethod,
                    data: paymentRefundMethod === 'BANK_TRANSFER' 
                        ? { bankName, accountNumber, accountName }
                        : {}
                }
            };

            await OrderApi.requestReturn(order.id, payload);
            toast.success('Yêu cầu trả hàng / hoàn tiền đã được gửi đi.');
            onOrderUpdate();
            handleCloseReturnModal();
            onClose();
        } catch (error) {
            console.error('Failed to request return:', error);
            toast.error('Không thể gửi yêu cầu trả hàng.');
        } finally {
            setIsUploading(false);
        }
    };


    const handleCancelReturnRequest = async () => {
        if (!confirm('Bạn có chắc chắn muốn hủy yêu cầu trả hàng?')) return;
        try {
            await OrderApi.cancelReturn(order.id);
            toast.success('Yêu cầu trả hàng đã được hủy.');
            onOrderUpdate();
            onClose();
        } catch (error) {
            console.error('Failed to cancel return request:', error);
            toast.error('Không thể hủy yêu cầu trả hàng.');
        }
    };

    const renderActionButtons = () => {
        switch (order.status) {
            case 'PENDING':
            case 'CONFIRMED':
            case 'PROCESSING':
                return (
                    <div className="flex flex-col gap-2">
                        {isPendingUnpaidNonCod && isWithinPaymentWindow && (
                            <>
                                {timeRemaining !== null && timeRemaining > 0 && (
                                    <div className="flex items-center justify-center gap-2 text-sm text-gray-600 mb-1">
                                        <Clock className="w-4 h-4" />
                                        <span>Thời gian còn lại: </span>
                                        <span className={`font-semibold ${timeRemaining < 300 ? 'text-red-600' : 'text-blue-600'}`}>
                                            {Math.floor(timeRemaining / 60).toString().padStart(2, '0')}:{(timeRemaining % 60).toString().padStart(2, '0')}
                                        </span>
                                    </div>
                                )}
                                <button 
                                    onClick={handleRePay} 
                                    disabled={timeRemaining !== null && timeRemaining <= 0}
                                    className="w-full bg-blue-600 text-white px-4 py-2 rounded-lg disabled:bg-gray-400 disabled:cursor-not-allowed"
                                >
                                    Thanh toán lại
                                </button>
                            </>
                        )}
                        <button onClick={handleOpenCancelModal} className="w-full bg-red-600 text-white px-4 py-2 rounded-lg">Hủy đơn hàng</button>
                    </div>
                );
            case 'DELIVERED':
                return <button onClick={handleOpenReturnModal} className="w-full bg-blue-600 text-white px-4 py-2 rounded-lg">Trả hàng / Hoàn tiền</button>;
            case 'RETURNING':
                return (
                    <div className="text-center">
                        <p className="text-yellow-600 mb-2">Đang xử lý yêu cầu...</p>
                        <button onClick={handleCancelReturnRequest} className="w-full bg-gray-600 text-white px-4 py-2 rounded-lg">Hủy yêu cầu trả hàng</button>
                    </div>
                );
            case 'CANCELING':
                return (
                    <div className="text-center">
                        <p className="text-yellow-600 mb-2">Đang xử lý yêu cầu hủy...</p>
                    </div>
                );
            default:
                return null;
        }
    };


    return (
        <>
            <div 
                className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4"
                onClick={onClose}
            >
                <div 
                    className="bg-white rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] flex flex-col"
                    onClick={(e) => e.stopPropagation()}
                >
                    {/* Header */}
                    <div className="flex justify-between items-center p-4 border-b">
                        <h3 className="text-lg font-semibold">Chi tiết đơn hàng #{order.orderNumber}</h3>
                        <button onClick={onClose} className="text-gray-400 hover:text-gray-600"><X /></button>
                    </div>

                    {/* Body */}
                    <div className="p-6 overflow-y-auto">
                        <div className="mb-6">
                            <div className="flex items-center gap-4">
                                <div className="flex-shrink-0 w-12 h-12 rounded-full bg-gray-100 flex items-center justify-center">
                                    <statusInfo.icon className="w-6 h-6 text-gray-600" />
                                </div>
                                <div>
                                    <h4 className="font-semibold text-lg">{statusInfo.label}</h4>
                                    <p className="text-sm text-gray-500">{statusInfo.description}</p>
                                    {order.status === 'SHIPPED' && order.estimatedDeliveryTime && (
                                        <p className="text-sm text-green-600">Dự kiến giao ngày: {formatInstant(order.estimatedDeliveryTime, 'vi-VN', { year: 'numeric', month: '2-digit', day: '2-digit' })}</p>
                                    )}
                                </div>
                            </div>
                        </div>

                        {/* Shipping Address */}
                        {order.shippingAddress && (
                            <div className="mb-6 p-4 border rounded-lg">
                                <h4 className="font-semibold text-base mb-2">Địa chỉ giao hàng</h4>
                                <p className="text-sm text-gray-700">{order.shippingAddress.fullName}</p>
                                <p className="text-sm text-gray-600 mt-1">
                                    {order.shippingAddress.ward}, {order.shippingAddress.district}, {order.shippingAddress.province}
                                </p>
                            </div>
                        )}

                        {/* Shipment Tracking Info */}
                        {order.shipment && (order.status === 'SHIPPED' || order.status === 'RETURNING' || order.status === 'RETURNED' || order.shipment.isReturn) && (
                            <div className="mb-6 p-4 border rounded-lg bg-blue-50">
                                <h4 className="font-semibold text-base mb-2 flex items-center gap-2">
                                    <Truck className="w-5 h-5 text-blue-600" />
                                    {order.shipment.isReturn ? 'Thông tin vận chuyển (Trả hàng)' : 'Thông tin vận chuyển'}
                                </h4>
                                <div className="space-y-2 text-sm">
                                    {order.shipment.carrier && (
                                        <div className="flex items-center gap-2">
                                            <span className="text-gray-600">Đơn vị vận chuyển:</span>
                                            <span className="font-medium">{order.shipment.carrier}</span>
                                        </div>
                                    )}
                                    {order.shipment.trackingNumber && (
                                        <div className="flex items-center gap-2">
                                            <span className="text-gray-600">Mã vận đơn:</span>
                                            <span className="font-medium font-mono text-blue-600">{order.shipment.trackingNumber}</span>
                                        </div>
                                    )}
                                    {order.shipment.warehouse && (
                                        <div className="flex items-center gap-2">
                                            <span className="text-gray-600">Kho hàng:</span>
                                            <span className="font-medium">{order.shipment.warehouse}</span>
                                        </div>
                                    )}
                                    {order.shipment.status && (
                                        <div className="flex items-center gap-2">
                                            <span className="text-gray-600">Trạng thái:</span>
                                            <span className="font-medium">{order.shipment.status}</span>
                                        </div>
                                    )}
                                </div>
                            </div>
                        )}
                        
                        {order.changeRequest && (
                            <div className="mb-6 p-4 border rounded-lg bg-gray-50">
                                <div className="flex items-center justify-between mb-3">
                                    <h4 className="font-semibold text-base">
                                        {order.changeRequest.type === 'CANCEL' ? 'Yêu cầu hủy đơn' : 'Yêu cầu trả hàng'}
                                    </h4>
                                    <span className={`px-3 py-1 rounded-full text-xs font-medium flex items-center gap-1 ${
                                        (order.changeRequest.status === 'PENDING' || order.changeRequest.status === 'WAITING_FOR_FINANCE')
                                            ? 'bg-yellow-100 text-yellow-800' 
                                            : order.changeRequest.status === 'APPROVED' 
                                            ? 'bg-green-100 text-green-800' 
                                            : 'bg-red-100 text-red-800'
                                    }`}>
                                        {(order.changeRequest.status === 'PENDING' || order.changeRequest.status === 'WAITING_FOR_FINANCE') && <AlertCircle className="w-3 h-3" />}
                                        {order.changeRequest.status === 'APPROVED' && <CheckCircle className="w-3 h-3" />}
                                        {order.changeRequest.status === 'REJECTED' && <AlertCircle className="w-3 h-3" />}
                                        {(order.changeRequest.status === 'PENDING' || order.changeRequest.status === 'WAITING_FOR_FINANCE') ? 'Chờ xử lý' : order.changeRequest.status === 'APPROVED' ? 'Đã duyệt' : order.changeRequest.status === 'REJECTED' ? 'Từ chối' : order.changeRequest.status}
                                    </span>
                                </div>

                                {order.status === 'CANCELLED' && (
                                    <div className="flex justify-end mt-2">
                                        <button
                                            className="px-3 py-1 bg-blue-600 text-white rounded hover:bg-blue-700 text-sm"
                                            onClick={() => setIsEditRefundModalOpen(true)}
                                        >
                                            Chỉnh sửa thông tin hoàn tiền
                                        </button>
                                    </div>
                                )}
                                <div className="space-y-3">
                                    <div>
                                        <p className="text-sm font-medium text-gray-700 mb-1">Lý do:</p>
                                        <div className="p-2 bg-white border rounded text-sm text-gray-600">
                                            {order.changeRequest.reason}
                                        </div>
                                    </div>

                                    {/* Hiển thị thông tin hoàn tiền nếu đơn đã hủy và có metadata hoặc adminNote */}
                                    {order.status === 'CANCELLED' && (
                                        <>
                                            {order.changeRequest.adminNote && (
                                                <div className="p-3 bg-blue-50 border border-blue-200 rounded">
                                                    <p className="text-sm font-medium text-blue-700 mb-1">Ghi chú Admin:</p>
                                                    <p className="text-sm text-blue-600">{order.changeRequest.adminNote}</p>
                                                </div>
                                            )}
                                            {order.changeRequest.metadata && (
                                                <div className="mt-2">
                                                    <p className="text-sm font-medium text-gray-700 mb-2">Thông tin hoàn tiền:</p>
                                                    <div className="grid grid-cols-2 gap-2 text-sm">
                                                        {order.changeRequest.metadata.bankName && (
                                                            <div>
                                                                <span className="text-gray-500">Ngân hàng:</span>
                                                                <span className="ml-2 font-medium">{order.changeRequest.metadata.bankName}</span>
                                                            </div>
                                                        )}
                                                        {order.changeRequest.metadata.accountNumber && (
                                                            <div>
                                                                <span className="text-gray-500">Số tài khoản:</span>
                                                                <span className="ml-2 font-medium">{order.changeRequest.metadata.accountNumber}</span>
                                                            </div>
                                                        )}
                                                        {order.changeRequest.metadata.accountName && (
                                                            <div className="col-span-2">
                                                                <span className="text-gray-500">Tên chủ tài khoản:</span>
                                                                <span className="ml-2 font-medium">{order.changeRequest.metadata.accountName}</span>
                                                            </div>
                                                        )}
                                                        {order.changeRequest.metadata.refundImage && (
                                                            <div className="col-span-2 mt-2">
                                                                <span className="text-gray-500">Ảnh chứng từ hoàn tiền:</span>
                                                                <img src={order.changeRequest.metadata.refundImage} alt="Refund proof" className="mt-2 w-40 h-auto rounded border" />
                                                            </div>
                                                        )}
                                                        {order.changeRequest.metadata.refundNote && (
                                                            <div className="col-span-2 mt-2">
                                                                <span className="text-gray-500">Ghi chú hoàn tiền:</span>
                                                                <span className="ml-2 font-medium">{order.changeRequest.metadata.refundNote}</span>
                                                            </div>
                                                        )}
                                                    </div>
                                                </div>
                                            )}
                                        </>
                                    )}

                                    {order.changeRequest.status === 'REJECTED' && order.changeRequest.adminNote && (
                                        <div className="p-3 bg-red-50 border border-red-200 rounded">
                                            <p className="text-sm font-medium text-red-700 mb-1">Lý do từ chối:</p>
                                            <p className="text-sm text-red-600">{order.changeRequest.adminNote}</p>
                                        </div>
                                    )}

                                    {order.changeRequest.type === 'RETURN' && order.changeRequest.images && order.changeRequest.images.length > 0 && (
                                        <div>
                                            <p className="text-sm font-medium text-gray-700 mb-2">Hình ảnh sản phẩm:</p>
                                            <div className="grid grid-cols-3 gap-2">
                                                {order.changeRequest.images.map((img, idx) => (
                                                    <img 
                                                        key={idx} 
                                                        src={img} 
                                                        alt={`Return image ${idx + 1}`}
                                                        className="w-full h-24 object-cover rounded border"
                                                    />
                                                ))}
                                            </div>
                                        </div>
                                    )}
                                </div>
                            </div>
                        )}

                        {/* Modal chỉnh sửa thông tin hoàn tiền */}
                        {isEditRefundModalOpen && (
                            <div className="fixed inset-0 flex items-center justify-center z-[9999] p-4 bg-black bg-opacity-30" onClick={() => setIsEditRefundModalOpen(false)}>
                                <div className="bg-white rounded-lg shadow-2xl max-w-md w-full p-6" onClick={e => e.stopPropagation()}>
                                    <h3 className="text-xl font-bold mb-4">Chỉnh sửa thông tin hoàn tiền</h3>
                                    <div className="space-y-4 mb-6">
                                        <div>
                                            <label className="block text-sm font-medium mb-2">Link ảnh chứng từ hoàn tiền</label>
                                            <input
                                                type="text"
                                                value={editRefundImage}
                                                onChange={e => setEditRefundImage(e.target.value)}
                                                className="w-full px-4 py-2 border rounded-lg"
                                                placeholder="Nhập URL ảnh chứng từ..."
                                            />
                                        </div>
                                        <div>
                                            <label className="block text-sm font-medium mb-2">Ghi chú hoàn tiền</label>
                                            <textarea
                                                value={editRefundNote}
                                                onChange={e => setEditRefundNote(e.target.value)}
                                                rows={4}
                                                className="w-full px-4 py-2 border rounded-lg"
                                                placeholder="Nhập ghi chú..."
                                            />
                                        </div>
                                    </div>
                                    <div className="flex gap-3 justify-end">
                                        <button
                                            onClick={() => setIsEditRefundModalOpen(false)}
                                            className="px-6 py-2 border rounded-lg hover:bg-gray-50"
                                        >
                                            Hủy
                                        </button>
                                        <button
                                            onClick={handleSaveRefundInfo}
                                            className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
                                        >
                                            Lưu
                                        </button>
                                    </div>
                                </div>
                            </div>
                        )}

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

                        {/* Payment Information */}
                        {order.payments && order.payments.length > 0 && (
                            <div className="border-t pt-4 pb-4">
                                <h4 className="font-semibold mb-2">Thông tin thanh toán</h4>
                                {order.payments.map((payment, idx) => (
                                    <div key={idx} className="flex justify-between items-center text-sm">
                                        <span className="text-gray-600">Phương thức:</span>
                                        <span className="font-medium">
                                            {payment.provider === 'COD' ? 'Thanh toán khi nhận hàng (COD)' : 
                                             payment.provider === 'VNPAY' ? 'VNPay' :
                                             payment.provider === 'MOMO' ? 'MoMo' : payment.provider}
                                            {' - '}
                                            <span className={`${
                                                payment.status === 'CAPTURED' ? 'text-green-600' :
                                                payment.status === 'PENDING' ? 'text-yellow-600' :
                                                payment.status === 'FAILED' ? 'text-red-600' :
                                                'text-gray-600'
                                            }`}>
                                                {payment.status === 'CAPTURED' ? 'Đã thanh toán' :
                                                 payment.status === 'PENDING' ? 'Chưa thanh toán' :
                                                 payment.status === 'FAILED' ? 'Thanh toán thất bại' :
                                                 payment.status === 'REFUNDED' ? 'Đã hoàn tiền' : payment.status}
                                            </span>
                                        </span>
                                    </div>
                                ))}
                            </div>
                        )}

                        <div className="border-t pt-4 space-y-2">
                            <div className="flex justify-between"><span>Tạm tính</span><span>{order.subtotalAmount.toLocaleString('vi-VN')} ₫</span></div>
                            <div className="flex justify-between"><span>Phí vận chuyển</span><span>{order.shippingAmount.toLocaleString('vi-VN')} ₫</span></div>
                            <div className="flex justify-between font-bold text-lg"><span>Tổng cộng</span><span className="text-red-600">{order.totalAmount.toLocaleString('vi-VN')} ₫</span></div>
                        </div>
                    </div>

                    {/* Footer */}
                    <div className="p-4 border-t">{renderActionButtons()}</div>
                </div>
            </div>

            {/* Cancellation Modal */}
            {isCancelModalOpen && (
                <div 
                    className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-[100] p-4"
                    onClick={() => setIsCancelModalOpen(false)}
                >
                    <div 
                        className="bg-white rounded-lg shadow-xl w-full max-w-lg max-h-[90vh] overflow-y-auto"
                        onClick={(e) => e.stopPropagation()}
                    >
                        <div className="p-6">
                            <h3 className="text-xl font-semibold mb-4">Hủy đơn hàng</h3>
                            
                            <div className="space-y-4">
                                <div>
                                    <label className="block font-medium text-gray-700 mb-2">Lý do hủy đơn</label>
                                    <div className="space-y-2">
                                        {CANCELLATION_REASONS.map(reason => (
                                            <label key={reason} className="flex items-center gap-3 p-3 rounded-lg border has-[:checked]:bg-gray-100 has-[:checked]:border-gray-400 cursor-pointer">
                                                <input type="radio" name="cancel_reason" value={reason} checked={cancelReason === reason} onChange={(e) => setCancelReason(e.target.value)} className="w-4 h-4" />
                                                <span>{reason}</span>
                                            </label>
                                        ))}
                                    </div>
                                    {cancelReason === 'Khác' && (
                                        <textarea value={otherReason} onChange={(e) => setOtherReason(e.target.value)} placeholder="Vui lòng nhập lý do khác (tối thiểu 5 ký tự)..." className="w-full p-3 border rounded-lg mt-2" rows={3} />
                                    )}
                                </div>

                                {/* Chỉ hiển thị phương thức hoàn tiền nếu đơn đã thanh toán (CAPTURED) và không phải COD */}
                                {order.payments && order.payments.some(p => p.status === 'CAPTURED' && p.provider !== 'COD') && (
                                    <>
                                        <div>
                                            <label className="block font-medium text-gray-700 mb-2">Phương thức hoàn tiền</label>
                                            <div className="space-y-2">
                                                <label className="flex items-center gap-3 p-3 rounded-lg border has-[:checked]:bg-gray-100 has-[:checked]:border-gray-400 cursor-pointer">
                                                    <input type="radio" name="refund_method" value="BANK_TRANSFER" checked={paymentRefundMethod === 'BANK_TRANSFER'} onChange={() => setPaymentRefundMethod('BANK_TRANSFER')} className="w-4 h-4" />
                                                    <span>Chuyển khoản ngân hàng</span>
                                                </label>
                                                <label className="flex items-center gap-3 p-3 rounded-lg border has-[:checked]:bg-gray-100 has-[:checked]:border-gray-400 cursor-pointer">
                                                    <input type="radio" name="refund_method" value="OTHER" checked={paymentRefundMethod === 'OTHER'} onChange={() => setPaymentRefundMethod('OTHER')} className="w-4 h-4" />
                                                    <span>Phương thức khác</span>
                                                </label>
                                            </div>
                                        </div>

                                        {paymentRefundMethod === 'BANK_TRANSFER' && (
                                            <div className="space-y-3 p-4 bg-blue-50 rounded-lg">
                                                <h4 className="font-medium text-sm text-blue-900">Thông tin tài khoản ngân hàng</h4>
                                                <input type="text" value={bankName} onChange={(e) => setBankName(e.target.value)} placeholder="Tên ngân hàng (VD: Vietcombank)" className="w-full p-2 border rounded-lg" />
                                                <input type="text" value={accountNumber} onChange={(e) => setAccountNumber(e.target.value)} placeholder="Số tài khoản" className="w-full p-2 border rounded-lg" />
                                                <input type="text" value={accountName} onChange={(e) => setAccountName(e.target.value)} placeholder="Tên chủ tài khoản" className="w-full p-2 border rounded-lg" />
                                            </div>
                                        )}
                                    </>
                                )}
                            </div>
                        </div>
                        <div className="flex justify-end gap-3 p-4 bg-gray-50 rounded-b-lg">
                            <button onClick={handleCloseCancelModal} className="px-5 py-2 rounded-lg text-gray-700 bg-gray-100 hover:bg-gray-200">Hủy bỏ</button>
                            <button onClick={handleConfirmCancel} className="px-5 py-2 rounded-lg text-white bg-red-600 hover:bg-red-700">Xác nhận hủy đơn</button>
                        </div>
                    </div>
                </div>
            )}

            {/* Return Modal */}
            {isReturnModalOpen && (
                <div 
                    className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-[100] p-4"
                    onClick={handleCloseReturnModal}
                >
                    <div 
                        className="bg-white rounded-lg shadow-xl w-full max-w-2xl max-h-[90vh] flex flex-col"
                        onClick={(e) => e.stopPropagation()}
                    >
                        <div className="flex justify-between items-center p-5 border-b">
                            <h3 className="text-xl font-semibold">Yêu cầu Trả hàng / Hoàn tiền</h3>
                            <button onClick={handleCloseReturnModal} disabled={isUploading} className="text-gray-400 hover:text-gray-600"><X /></button>
                        </div>
                        <div className="p-6 space-y-6 overflow-y-auto">
                            <div>
                                <label className="font-semibold text-gray-700">Lý do trả hàng</label>
                                <textarea value={returnReason} onChange={(e) => setReturnReason(e.target.value)} placeholder="Mô tả chi tiết lý do bạn muốn trả hàng (ví dụ: sản phẩm lỗi, sai kích thước,...)" className="w-full p-2 border rounded-lg mt-2" rows={4} />
                            </div>
                            <div>
                                <label className="font-semibold text-gray-700">Hình ảnh / Video bằng chứng</label>
                                <div className="mt-2 p-6 border-2 border-dashed rounded-lg text-center">
                                    <UploadCloud className="mx-auto h-12 w-12 text-gray-400" />
                                    <input type="file" multiple accept="image/*" onChange={(e) => setReturnImages(e.target.files)} className="text-sm text-gray-600 file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:font-semibold file:bg-violet-50 file:text-violet-700 hover:file:bg-violet-100" />
                                </div>
                            </div>
                            <div>
                                <label className="font-semibold text-gray-700">Phương thức hoàn trả</label>
                                <div className="mt-2 grid grid-cols-1 md:grid-cols-2 gap-3">
                                    <label className="flex items-center gap-3 p-3 rounded-lg border has-[:checked]:bg-gray-100 has-[:checked]:border-gray-400 cursor-pointer">
                                        <input type="radio" name="return_option" value="PICKUP" checked={returnOptionType === 'PICKUP'} onChange={() => setReturnOptionType('PICKUP')} className="w-4 h-4" />
                                        <span>Shop đến lấy hàng</span>
                                    </label>
                                    <label className="flex items-center gap-3 p-3 rounded-lg border has-[:checked]:bg-gray-100 has-[:checked]:border-gray-400 cursor-pointer">
                                        <input type="radio" name="return_option" value="DROP_OFF" checked={returnOptionType === 'DROP_OFF'} onChange={() => setReturnOptionType('DROP_OFF')} className="w-4 h-4" />
                                        <span>Tôi tự gửi hàng</span>
                                    </label>
                                </div>
                            </div>
                            <div>
                                <label className="font-semibold text-gray-700">Phương thức hoàn tiền</label>
                                <div className="mt-2 grid grid-cols-1 md:grid-cols-2 gap-3">
                                    <label className="flex items-center gap-3 p-3 rounded-lg border has-[:checked]:bg-gray-100 has-[:checked]:border-gray-400 cursor-pointer">
                                        <input type="radio" name="refund_method_return" value="BANK_TRANSFER" checked={paymentRefundMethod === 'BANK_TRANSFER'} onChange={() => setPaymentRefundMethod('BANK_TRANSFER')} className="w-4 h-4" />
                                        <span>Chuyển khoản ngân hàng</span>
                                    </label>
                                    <label className="flex items-center gap-3 p-3 rounded-lg border has-[:checked]:bg-gray-100 has-[:checked]:border-gray-400 cursor-pointer">
                                        <input type="radio" name="refund_method_return" value="OTHER" checked={paymentRefundMethod === 'OTHER'} onChange={() => setPaymentRefundMethod('OTHER')} className="w-4 h-4" />
                                        <span>Phương thức khác</span>
                                    </label>
                                </div>
                            </div>
                            {paymentRefundMethod === 'BANK_TRANSFER' && (
                                <div className="space-y-3 p-4 bg-blue-50 rounded-lg">
                                    <h4 className="font-medium text-sm text-blue-900">Thông tin tài khoản ngân hàng</h4>
                                    <input type="text" value={bankName} onChange={(e) => setBankName(e.target.value)} placeholder="Tên ngân hàng (VD: Vietcombank)" className="w-full p-2 border rounded-lg" />
                                    <input type="text" value={accountNumber} onChange={(e) => setAccountNumber(e.target.value)} placeholder="Số tài khoản" className="w-full p-2 border rounded-lg" />
                                    <input type="text" value={accountName} onChange={(e) => setAccountName(e.target.value)} placeholder="Tên chủ tài khoản" className="w-full p-2 border rounded-lg" />
                                </div>
                            )}
                        </div>
                        <div className="flex justify-end gap-3 p-4 bg-gray-50 rounded-b-lg">
                            <button onClick={handleCloseReturnModal} disabled={isUploading} className="px-5 py-2 rounded-lg text-gray-700 bg-gray-100 hover:bg-gray-200 disabled:opacity-50">Hủy</button>
                            <button onClick={handleConfirmReturn} disabled={isUploading} className="px-5 py-2 rounded-lg text-white bg-blue-600 hover:bg-blue-700 flex items-center gap-2 disabled:opacity-50">
                                {isUploading && <Loader2 className="w-4 h-4 animate-spin" />}
                                {isUploading ? 'Đang xử lý...' : 'Xác nhận'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
            
        </>
    );
}

