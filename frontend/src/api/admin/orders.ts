import api from '../http'
import type { PageResponse } from '../order'
import type { OrderResponse } from '../order'

export type AdminOrderStatus =
    | 'ALL'
    | 'PENDING'
    | 'CONFIRMED'
    | 'PROCESSING'
    | 'SHIPPED'
    | 'DELIVERED'
    | 'CANCELLED'
    | 'REFUNDED'

export interface AdminOrderListParams {
    status?: AdminOrderStatus | string
    paymentType?: string
    page?: number
    size?: number
}

export const getOrders = async (
    params: AdminOrderListParams = {}
): Promise<PageResponse<OrderResponse>> => {
    const { status = 'ALL', paymentType, page = 0, size = 20 } = params
    const queryParams: Record<string, unknown> = {
        status,
        page,
        size
    }

    if (paymentType) {
        queryParams.paymentType = paymentType
    }

    const response = await api.get('/api/v1/admin/orders/get-order-list', {
        params: queryParams
    })
    return response.data
}

export const updateOrderStatus = async (
    orderId: string,
    status: Exclude<AdminOrderStatus, 'ALL'>
): Promise<OrderResponse> => {
    const response = await api.put(`/api/v1/admin/orders/${orderId}/status`, null, {
        params: { status }
    })
    return response.data
}
