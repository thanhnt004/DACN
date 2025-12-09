import api from './http'

export interface DiscountResponse {
    id: string
    code: string
    description: string
    discountType: 'PERCENTAGE' | 'FIXED_AMOUNT'
    discountValue: number
    maxDiscountAmount?: number
    minOrderAmount?: number
    startDate: string
    endDate: string
    usageLimit?: number
    usageCount: number
    isActive: boolean
}

export interface GetDiscountsParams {
    code?: string
    active?: boolean
    page?: number
    size?: number
}

export interface PageResponse<T> {
    content: T[]
    totalElements: number
    totalPages: number
    size: number
    number: number
}

export const getAvailableDiscounts = async (params: GetDiscountsParams = {}): Promise<PageResponse<DiscountResponse>> => {
    const response = await api.get<PageResponse<DiscountResponse>>('/api/v1/discounts', {
        params: {
            ...params,
            active: true, // Chỉ lấy mã đang active
        }
    })
    return response.data
}

export const searchDiscounts = async (searchTerm: string): Promise<PageResponse<DiscountResponse>> => {
    const response = await api.get<PageResponse<DiscountResponse>>('/api/v1/discounts', {
        params: {
            code: searchTerm,
            active: true,
            size: 50
        }
    })
    return response.data
}

export const getAvailableForProducts = async (productIds?: string[]): Promise<DiscountResponse[]> => {
    const response = await api.get<DiscountResponse[]>('/api/v1/discounts/get-available', {
        params: productIds && productIds.length > 0 ? {
            productIds: productIds
        } : {}
    })
    return response.data
}
