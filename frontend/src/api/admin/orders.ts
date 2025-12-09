import api from '../http';
import type { PageResponse, OrderResponse } from '../order';
import { toInstantString } from '../../lib/dateUtils';

/**
 * Order Tab Filter - corresponds to backend OrderTabFilter enum
 */
export type OrderTabFilter =
    | 'ALL'              // Tất cả
    | 'UNPAID'           // Chưa thanh toán
    | 'TO_CONFIRM'       // Chờ xác nhận
    | 'PROCESSING'       // Chuẩn bị hàng
    | 'SHIPPING'         // Đang giao
    | 'COMPLETED'        // Đã giao
    | 'CANCEL_REQ'       // Yêu cầu hủy
    | 'CANCELLED'        // Đã hủy
    | 'RETURN_REQ'       // Yêu cầu trả hàng
    | 'REFUNDED'         // Đã trả hàng
    | 'WAITING_REFUND';  // Chờ hoàn tiền

export type AdminOrderStatus = OrderTabFilter;

export interface AdminOrderListParams {
    tab?: OrderTabFilter;
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

export interface FailureItem<T> {
    item: T;
    reason: string;
}

export interface BatchResult<T> {
    successItems: T[];
    failedItems: FailureItem<T>[];
    hasFailures: boolean;
}


export const getOrders = async (
    params: AdminOrderListParams = {}
): Promise<PageResponse<OrderResponse>> => {
    const { tab = 'ALL', page = 0, size = 20, orderNumber, startDate, endDate } = params;
    const queryParams: Record<string, unknown> = {
        tab,
        page,
        size
    };

    if (orderNumber) {
        queryParams.orderNumber = orderNumber;
    }
    if (startDate) {
        queryParams.startDate = toInstantString(startDate);
    }
    if (endDate) {
        queryParams.endDate = toInstantString(endDate);
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
    console.log('[API] shipOrders - Gọi request với orderIds:', orderIds);
    try {
        const response = await api.put<BatchResult<string>>('/api/v1/admin/orders/orders/ship', orderIds);
        console.log('[API] shipOrders - Response:', response.data);
        return response.data;
    } catch (error: any) {
        console.error('[API] shipOrders - Lỗi:', error);
        console.error('[API] shipOrders - Error details:', {
            message: error?.message,
            response: error?.response?.data,
            status: error?.response?.status
        });
        throw error;
    }
};

export const getPrintUrlForOrders = async (orderIds: string[]): Promise<string> => {
    console.log('[API] getPrintUrlForOrders - Gọi request với orderIds:', orderIds);
    try {
        const response = await api.get<string>('/api/v1/admin/orders/orders/shipment-print-url', {
            params: { orderIds: orderIds.join(',') }
        });
        console.log('[API] getPrintUrlForOrders - Response:', response.data);
        return response.data;
    } catch (error: any) {
        console.error('[API] getPrintUrlForOrders - Lỗi:', error);
        console.error('[API] getPrintUrlForOrders - Error details:', {
            message: error?.message,
            response: error?.response?.data,
            status: error?.response?.status
        });
        throw error;
    }
};

export const cancelOrderByAdmin = async (orderId: string): Promise<string> => {
    const response = await api.put<string>(`/api/v1/admin/orders/${orderId}/cancel`);
    return response.data;
};

export const reviewChangeRequest = async (requestId: string, payload: ReviewRequestPayload): Promise<void> => {
    await api.post(`/api/v1/admin/orders/requests/${requestId}/review`, payload);
};

export const returnOrderByAdmin = async (orderId: string, adminNote?: string): Promise<void> => {
    await api.put(`/api/v1/admin/orders/${orderId}/return`, { adminNote });
};

export interface RefundConfirmRequest {
    imageProof?: string;
    note?: string;
}

export const confirmRefund = async (requestId: string, payload: RefundConfirmRequest): Promise<void> => {
    await api.post(`/api/v1/admin/orders/admin/confirm-refund/${requestId}`, payload);
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
