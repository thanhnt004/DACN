import api from '../http'

export interface OrderChangeRequest {
    id: string
    type: 'CANCEL' | 'RETURN'
    status: 'PENDING' | 'APPROVED' | 'REJECTED'
    reason: string
    adminNote?: string
    metadata?: Record<string, string>
    images?: string[]
}

export interface PaymentRefundOption {
    method: 'BANK_TRANSFER' | 'E_WALLET'
    bankName?: string
    accountNumber?: string
    accountName?: string
}

export const getChangeRequest = async (orderId: string): Promise<OrderChangeRequest> => {
    const response = await api.get<OrderChangeRequest>(`/api/v1/admin/orders/change-request/${orderId}`)
    return response.data
}

export const updateRefundInfo = async (
    changeRequestId: string,
    refundOption: PaymentRefundOption
): Promise<void> => {
    await api.put(`/api/v1/admin/orders/change-request/${changeRequestId}`, refundOption)
}
