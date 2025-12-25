import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { CheckCircle, XCircle, Loader } from 'lucide-react';

interface PaymentStatusEvent {
    orderId: string;
    status: 'PENDING' | 'PAID' | 'FAILED' | 'CANCELLED' | 'EXPIRED';
    message: string;
    errorCode?: string;
    timestamp: string;
}

const PaymentReturnPage = () => {
    const [status, setStatus] = useState<string>('PENDING');
    const [message, setMessage] = useState<string>('Đang chờ xác nhận thanh toán...');
    const navigate = useNavigate();
    const location = useLocation();

    // Get API Base URL from environment or default
    const BASE_URL = (import.meta as unknown as { env?: Record<string, string> }).env?.VITE_API_BASE_URL || 'http://localhost:8089';

    useEffect(() => {
        const orderId = localStorage.getItem('pendingOrderId');
        const params = new URLSearchParams(location.search);
        const gateway = params.get('gateway');

        if (!orderId || gateway !== 'vnpay') {
            navigate('/');
            return;
        }

        const eventSource = new EventSource(`${BASE_URL}/api/v1/payments/status/stream/${orderId}`);

        eventSource.onopen = () => {
            console.log('SSE connection opened.');
        };

        eventSource.addEventListener('payment-status', (event) => {
            try {
                const eventData: PaymentStatusEvent = JSON.parse(event.data);
                setStatus(eventData.status);
                setMessage(eventData.message);

                if (eventData.status === 'PAID' || eventData.status === 'FAILED' || eventData.status === 'CANCELLED' || eventData.status === 'EXPIRED') {
                    eventSource.close();
                    localStorage.removeItem('pendingOrderId');
                }
            } catch (error) {
                console.error('Failed to parse SSE event data:', error);
            }
        });

        eventSource.onerror = (error) => {
            console.error('SSE error:', error);
            setStatus('FAILED');
            setMessage('Không thể kết nối để kiểm tra trạng thái thanh toán. Vui lòng liên hệ hỗ trợ.');
            eventSource.close();
            localStorage.removeItem('pendingOrderId');
        };

        return () => {
            eventSource.close();
        };

    }, [navigate, location.search]);

    const renderStatus = () => {
        switch (status) {
            case 'PAID':
                return (
                    <div className="text-center text-green-600">
                        <CheckCircle className="mx-auto h-16 w-16" />
                        <h2 className="mt-4 text-2xl font-bold">Thanh toán thành công</h2>
                        <p className="mt-2">{message}</p>
                    </div>
                );
            case 'FAILED':
            case 'CANCELLED':
            case 'EXPIRED':
                return (
                    <div className="text-center text-red-600">
                        <XCircle className="mx-auto h-16 w-16" />
                        <h2 className="mt-4 text-2xl font-bold">Thanh toán thất bại</h2>
                        <p className="mt-2">{message}</p>
                    </div>
                );
            default: // PENDING
                return (
                    <div className="text-center text-gray-600">
                        <Loader className="mx-auto h-16 w-16 animate-spin" />
                        <h2 className="mt-4 text-2xl font-bold">Đang xử lý...</h2>
                        <p className="mt-2">{message}</p>
                    </div>
                );
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-50">
            <div className="max-w-md w-full bg-white shadow-lg rounded-lg p-8">
                {renderStatus()}
                <div className="mt-8 flex justify-center gap-4">
                    <button onClick={() => navigate('/products')} className="px-4 py-2 bg-gray-800 text-white rounded hover:bg-gray-700">
                        Tiếp tục mua sắm
                    </button>
                    <button onClick={() => navigate('/member/orders')} className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-100">
                        Xem đơn hàng
                    </button>
                </div>
            </div>
        </div>
    );
};

export default PaymentReturnPage;
