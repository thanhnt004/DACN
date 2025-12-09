import { OrderResponse } from '../../../api/order';
import { PackageCheck, Truck, XCircle, CheckCircle2 } from 'lucide-react';

type AdminOrderActionsProps = {
    order: OrderResponse;
    onConfirm: (orderId: string) => void;
    onCancel: (orderId: string) => void;
    onShip: (orderId: string) => void;
    onReview: (requestId: string, decision: 'APPROVED' | 'REJECTED', note?: string) => void;
};

export default function AdminOrderActions({ order, onConfirm, onCancel, onShip, onReview }: AdminOrderActionsProps) {
    const handleReject = () => {
        onReview(order.changeRequest!.id, 'REJECTED');
    };

    const handleApprove = () => {
        onReview(order.changeRequest!.id, 'APPROVED');
    };

    const baseButtonClass = "p-1.5 rounded text-white shadow-sm transition-transform transform hover:scale-110";

    // Kiểm tra trạng thái thanh toán
    const payment = order.payments?.[0];
    const isPaid = payment?.status === 'CAPTURED';
    const isCOD = payment?.provider?.toUpperCase().includes('COD');

    switch (order.status) {
        case 'PENDING':
            // Tab "Chưa thanh toán" (PENDING + chưa thanh toán): chỉ nút Hủy
            if (!isPaid && !isCOD) {
                return (
                    <div className="flex flex-row gap-2 items-center">
                        <button title="Hủy đơn hàng" onClick={() => onCancel(order.id)} className={`${baseButtonClass} bg-red-600 hover:bg-red-700`}><XCircle size={18} /></button>
                    </div>
                );
            }
            // Tab "Chờ xác nhận" (PENDING + đã thanh toán hoặc COD): nút Xác nhận và Hủy
            return (
                <div className="flex flex-row gap-2 items-center">
                    <button title="Xác nhận đơn hàng" onClick={() => onConfirm(order.id)} className={`${baseButtonClass} bg-green-600 hover:bg-green-700`}><PackageCheck size={18} /></button>
                    <button title="Hủy đơn hàng" onClick={() => onCancel(order.id)} className={`${baseButtonClass} bg-red-600 hover:bg-red-700`}><XCircle size={18} /></button>
                </div>
            );

        case 'CONFIRMED':
            return (
                <div className="flex flex-row gap-2 items-center">
                    <button title="Chuẩn bị hàng" onClick={() => onConfirm(order.id)} className={`${baseButtonClass} bg-blue-600 hover:bg-blue-700`}><PackageCheck size={18} /></button>
                    <button title="Hủy đơn hàng" onClick={() => onCancel(order.id)} className={`${baseButtonClass} bg-red-600 hover:bg-red-700`}><XCircle size={18} /></button>
                </div>
            );

        case 'PROCESSING':
            return (
                <div className="flex flex-row gap-2 items-center">
                    <button title="Giao hàng & In đơn" onClick={() => onShip(order.id)} className={`${baseButtonClass} bg-sky-600 hover:bg-sky-700`}><Truck size={18} /></button>
                    <button title="Hủy đơn hàng" onClick={() => onCancel(order.id)} className={`${baseButtonClass} bg-red-600 hover:bg-red-700`}><XCircle size={18} /></button>
                </div>
            );

        case 'SHIPPED':
             return (
                <div className="flex flex-row gap-2 items-center">
                    <button title="Hủy đơn giao hàng" onClick={() => onCancel(order.id)} className={`${baseButtonClass} bg-red-600 hover:bg-red-700`}><XCircle size={18} /></button>
                </div>
            );

        case 'CANCELING':
        case 'RETURNING':
            return (
                <div className="flex flex-row gap-2 items-center">
                    <button title="Đồng ý" onClick={handleApprove} className={`${baseButtonClass} bg-green-600 hover:bg-green-700`}><CheckCircle2 size={18} /></button>
                    <button title="Từ chối" onClick={handleReject} className={`${baseButtonClass} bg-red-600 hover:bg-red-700`}><XCircle size={18} /></button>
                </div>
            );

        case 'DELIVERED':
             return <span className="text-sm text-gray-500">-</span>; 

        default:
            return <span className="text-sm text-gray-500">-</span>;
    }
}
