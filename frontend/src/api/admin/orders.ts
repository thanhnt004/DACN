import api from '../http';
import type { PageResponse, OrderResponse } from '../order';

export type AdminOrderStatus =
    | 'ALL'
    | 'PENDING'
    | 'CONFIRMED'
    | 'PROCESSING'
    | 'SHIPPED'
    | 'DELIVERED'
    | 'CANCELING'
    | 'CANCELLED'
    | 'RETURNING'
    | 'RETURNED'
    | 'REFUNDED';


export interface AdminOrderListParams {
    status?: AdminOrderStatus | string;
    paymentType?: string;
    page?: number;
    size?: number;
    orderNumber?: string;
    startDate?: string | Date;
    endDate?: string | Date;
}

export interface ReviewRequestPayload {
    status: 'APPROVED' | 'REJECTED';
    adminNote?: string;
}

export interface BatchResult<T> {
    successes: T[];
    failures: Record<string, string>;
    hasFailures: boolean;
}


export const getOrders = async (
    params: AdminOrderListParams = {}
): Promise<PageResponse<OrderResponse>> => {
    const { status = 'ALL', paymentType, page = 0, size = 20, orderNumber, startDate, endDate } = params;
    const queryParams: Record<string, unknown> = {
        status,
        page,
        size
    };

    if (paymentType) {
        queryParams.paymentType = paymentType;
    }
    if (orderNumber) {
        queryParams.orderNumber = orderNumber;
    }
    if (startDate) {
        queryParams.startDate = new Date(startDate).toISOString();
    }
    if (endDate) {
        queryParams.endDate = new Date(endDate).toISOString();
    }


    const response = await api.get('/api/v1/admin/orders/get-order-list', {
        params: queryParams
    });
    return response.data;
};

export const confirmOrders = async (orderIds: string[]): Promise<BatchResult<string>> => {
    const response = await api.put<BatchResult<string>>('/api/v1/admin/orders/orders/confirm', orderIds);
    return response.data;
};

export const shipOrders = async (orderIds: string[]): Promise<BatchResult<string>> => {
    const response = await api.put<BatchResult<string>>('/api/v1/admin/orders/orders/ship', orderIds);
    return response.data;
};

export const getPrintUrlForOrders = async (orderIds: string[]): Promise<string> => {
    const response = await api.get<string>('/api/v1/admin/orders/orders/shipment-print-url', {
        params: { orderIds: orderIds.join(',') }
    });
    return response.data;
};

export const cancelOrderByAdmin = async (orderId: string): Promise<string> => {
    const response = await api.put<string>(`/api/v1/admin/orders/${orderId}/cancel`);
    return response.data;
};

export const reviewChangeRequest = async (requestId: string, payload: ReviewRequestPayload): Promise<void> => {
    await api.post(`/api/v1/admin/orders/requests/${requestId}/review`, payload);
};

// This is a legacy method and might be removed. The new functions above are more specific.
export const updateOrderStatus = async (
    orderId: string,
    status: Exclude<AdminOrderStatus, 'ALL'>
): Promise<OrderResponse> => {
    const response = await api.put(`/api/v1/admin/orders/${orderId}/status`, null, {
        params: { status }
    });
    return response.data;
};
