import api from '../http'

export interface ChangeRequestInfo {
    id: string
    type: string
    status: string
    reason: string
    adminNote?: string
    metadata?: Record<string, any>
    images?: string[]
}

export interface ChangeRequest {
    id: string
    orderNumber: string
    orderId: string
    status: string
    type: string
    reason: string
    note?: string
    adminNote?: string
    images?: string[]
    metadata?: Record<string, any>
    createdAt: string
}

export interface ReviewRequestDTO {
    status: 'APPROVED' | 'REJECTED'
    adminNote?: string
}

export interface RefundConfirmRequest {
    imageProof: string
    note: string
}

export const getChangeRequests = async (params: {
    page?: number
    size?: number
    type?: 'CANCEL' | 'RETURN'
    status?: string
}) => {
    const response = await api.get('/api/v1/admin/orders/get-order-list', {
        params: {
            tab: params.status ? params.status : (params.type === 'CANCEL' ? 'CANCEL_REQ' : 'RETURN_REQ'),
            page: params.page || 0,
            size: params.size || 20
        }
    })
    
    // Map OrderResponse to ChangeRequest
    const data = response.data
    if (data.content) {
        data.content = data.content.map((order: Record<string, unknown>) => ({
            id: order.changeRequest?.id || order.id,
            orderNumber: order.orderNumber,
            orderId: order.id,
            status: order.changeRequest?.status || 'UNKNOWN',
            type: order.changeRequest?.type || '',
            reason: order.changeRequest?.reason || '',
            adminNote: order.changeRequest?.adminNote,
            images: order.changeRequest?.images || [],
            metadata: order.changeRequest?.metadata || {},
            createdAt: order.createdAt
        }))
    }
    
    return data
}

export const reviewChangeRequest = async (requestId: string, data: ReviewRequestDTO) => {
    const response = await api.post(`/api/v1/order/admin/requests/${requestId}/review`, data)
    return response.data
}

export const confirmRefund = async (requestId: string, data: RefundConfirmRequest) => {
    const response = await api.post(`/api/v1/order/admin/admin/confirm-refund/${requestId}`, data)
    return response.data
}

export const cancelOrderByAdmin = async (orderId: string, adminNote?: string) => {
    const response = await api.put(`/api/v1/order/admin/${orderId}/cancel`, { adminNote })
    return response.data
}

export const returnOrderByAdmin = async (orderId: string, adminNote?: string) => {
    const response = await api.put(`/api/v1/order/admin/${orderId}/return`, { adminNote })
    return response.data
}
