import { useMemo, useState } from 'react';
import * as OrderApi from '../../../api/order';
import { ReturnOption, PaymentRefundOption } from '../../../api/order';
import { uploadImagesToCloudinary } from '../../../api/media';
import { X, Package, ThumbsUp, Truck, Undo2, Loader2, UploadCloud } from 'lucide-react';
import { toast } from 'react-toastify';
import { OrderResponse } from '../../../api/order';

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

const STATUS_INFO_MAP: Record<string, { label: string; description: string; icon: React.FC<any> }> = {
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
    const [returnOption, setReturnOption] = useState<ReturnOption>(ReturnOption.PICKUP);
    const [paymentRefundOption, setPaymentRefundOption] = useState<PaymentRefundOption>(PaymentRefundOption.BANK_TRANSFER);
    const [isUploading, setIsUploading] = useState(false);


    const statusInfo = useMemo(() => STATUS_INFO_MAP[order.status] ?? {
        label: order.status,
        description: 'Trạng thái không xác định',
        icon: Package
    }, [order.status]);

    const isPendingUnpaidNonCod = order.status === 'PENDING' &&
        order.payments &&
        order.payments.length > 0 &&
        order.payments.every(p => p.provider !== 'COD') &&
        !order.paidAt;

    // Check if within 30 minutes of creation for re-payment
    const isWithinPaymentWindow = useMemo(() => {
        if (order.placedAt) {
            const placedTime = new Date(order.placedAt).getTime();
            const thirtyMinutes = 30 * 60 * 1000;
            return (Date.now() - placedTime) < thirtyMinutes;
        }
        return false;
    }, [order.placedAt]);

    const handleRePay = async () => {
        try {
            const response = await OrderApi.rePay(order.id);
            if (response.paymentUrl) {
                window.location.href = response.paymentUrl;
            } else {
                toast.error('Không thể tạo lại liên kết thanh toán.');
            }
        } catch (error) {
            console.error('Failed to re-pay:', error);
            toast.error('Lỗi khi tạo lại liên kết thanh toán.');
        }
    };

    // --- Cancellation Logic ---
    const handleOpenCancelModal = () => setIsCancelModalOpen(true);
    const handleCloseCancelModal = () => setIsCancelModalOpen(false);

    const handleConfirmCancel = async () => {
        const reason = cancelReason === 'Khác' ? otherReason : cancelReason;
        if (!reason || reason.trim().length < 5) {
            toast.error('Vui lòng cung cấp lý do hủy đơn hợp lệ (tối thiểu 5 ký tự).');
            return;
        }

        try {
            await OrderApi.cancelOrder(order.id, { reason });
            toast.success('Yêu cầu hủy đơn hàng đã được gửi đi.');
            onOrderUpdate();
            handleCloseCancelModal();
            onClose();
        } catch (error) {
            console.error('Failed to cancel order:', error);
            toast.error('Không thể gửi yêu cầu hủy đơn hàng.');
        }
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
                returnOption,
                paymentRefundOption,
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
                            <button onClick={handleRePay} className="w-full bg-blue-600 text-white px-4 py-2 rounded-lg">
                                Thanh toán lại
                            </button>
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
            <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
                <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] flex flex-col">
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
                                        <p className="text-sm text-green-600">Dự kiến giao ngày: {new Date(order.estimatedDeliveryTime).toLocaleDateString('vi-VN')}</p>
                                    )}
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

                    {/* Footer */}
                    <div className="p-4 border-t">{renderActionButtons()}</div>
                </div>
            </div>

            {/* Cancellation Modal */}
            {isCancelModalOpen && (
                <div className="fixed inset-0 bg-black bg-opacity-60 flex items-center justify-center z-[100]">
                    <div className="bg-white rounded-lg shadow-xl w-full max-w-md">
                        <div className="p-6">
                            <h3 className="text-xl font-semibold mb-4">Lý do hủy đơn hàng</h3>
                            <div className="space-y-3">
                                {CANCELLATION_REASONS.map(reason => (
                                    <label key={reason} className="flex items-center gap-3 p-3 rounded-lg border has-[:checked]:bg-gray-100 has-[:checked]:border-gray-400 cursor-pointer">
                                        <input type="radio" name="cancel_reason" value={reason} checked={cancelReason === reason} onChange={(e) => setCancelReason(e.target.value)} className="w-4 h-4" />
                                        <span>{reason}</span>
                                    </label>
                                ))}
                                {cancelReason === 'Khác' && (
                                    <textarea value={otherReason} onChange={(e) => setOtherReason(e.target.value)} placeholder="Vui lòng nhập lý do khác..." className="w-full p-2 border rounded-lg mt-2" rows={3} />
                                )}
                            </div>
                        </div>
                        <div className="flex justify-end gap-3 p-4 bg-gray-50 rounded-b-lg">
                            <button onClick={handleCloseCancelModal} className="px-5 py-2 rounded-lg text-gray-700 bg-gray-100 hover:bg-gray-200">Hủy bỏ</button>
                            <button onClick={handleConfirmCancel} className="px-5 py-2 rounded-lg text-white bg-red-600 hover:bg-red-700">Xác nhận</button>
                        </div>
                    </div>
                </div>
            )}

            {/* Return Modal */}
            {isReturnModalOpen && (
                <div className="fixed inset-0 bg-black bg-opacity-60 flex items-center justify-center z-[100] p-4">
                    <div className="bg-white rounded-lg shadow-xl w-full max-w-2xl max-h-[90vh] flex flex-col">
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
                                        <input type="radio" name="return_option" value={ReturnOption.PICKUP} checked={returnOption === ReturnOption.PICKUP} onChange={() => setReturnOption(ReturnOption.PICKUP)} className="w-4 h-4" />
                                        <span>Shop đến lấy hàng</span>
                                    </label>
                                    <label className="flex items-center gap-3 p-3 rounded-lg border has-[:checked]:bg-gray-100 has-[:checked]:border-gray-400 cursor-pointer">
                                        <input type="radio" name="return_option" value={ReturnOption.SELF_ARRANGED} checked={returnOption === ReturnOption.SELF_ARRANGED} onChange={() => setReturnOption(ReturnOption.SELF_ARRANGED)} className="w-4 h-4" />
                                        <span>Tôi tự gửi hàng</span>
                                    </label>
                                </div>
                            </div>
                            <div>
                                <label className="font-semibold text-gray-700">Phương thức hoàn tiền</label>
                                <div className="mt-2 grid grid-cols-1 md:grid-cols-2 gap-3">
                                    <label className="flex items-center gap-3 p-3 rounded-lg border has-[:checked]:bg-gray-100 has-[:checked]:border-gray-400 cursor-pointer">
                                        <input type="radio" name="refund_option" value={PaymentRefundOption.BANK_TRANSFER} checked={paymentRefundOption === PaymentRefundOption.BANK_TRANSFER} onChange={() => setPaymentRefundOption(PaymentRefundOption.BANK_TRANSFER)} className="w-4 h-4" />
                                        <span>Chuyển khoản ngân hàng</span>
                                    </label>
                                    <label className="flex items-center gap-3 p-3 rounded-lg border has-[:checked]:bg-gray-100 has-[:checked]:border-gray-400 cursor-pointer">
                                        <input type="radio" name="refund_option" value={PaymentRefundOption.STORE_CREDIT} checked={paymentRefundOption === PaymentRefundOption.STORE_CREDIT} onChange={() => setPaymentRefundOption(PaymentRefundOption.STORE_CREDIT)} className="w-4 h-4" />
                                        <span>Tín dụng cửa hàng</span>
                                    </label>
                                </div>
                            </div>
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
