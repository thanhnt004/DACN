import api from './http'
import { UserAddress } from './checkout'

export interface OrderItemDTO {
    id: string
    productId: string
    productName: string
    variantId: string
    variantName: string
    sku: string
    quantity: number
    unitPriceAmount: number
    totalAmount: number
    imageUrl?: string
}

export interface OrderResponse {
    id: string
    orderNumber: string
    status: string
    subtotalAmount: number
    discountAmount: number
    shippingAmount: number
    totalAmount: number
    shippingAddress: UserAddress
    notes?: string
    placedAt: string
    items: OrderItemDTO[]
}

export interface PageResponse<T> {
    content: T[]
    pageNo: number
    pageSize: number
    totalElements: number
    totalPages: number
    last: boolean
}

export const getOrders = async (status: string = 'ALL', page: number = 0, size: number = 20): Promise<PageResponse<OrderResponse>> => {
    const response = await api.get('/api/v1/orders/get-order-list', {
        params: {
            status,
            page,
            size
        }
    })
    return response.data
}

export const getOrderDetail = async (id: string): Promise<OrderResponse> => {
    const response = await api.get(`/api/v1/orders/${id}`)
    return response.data
}

export const cancelOrder = async (id: string): Promise<void> => {
    await api.post(`/api/v1/orders/${id}/cancel`)
}
