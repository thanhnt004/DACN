import api from '../http'
import type { PageResponse } from './users'

export interface DiscountResponse {
    id: string
    code: string
    name: string
    description?: string
    type: 'PERCENTAGE' | 'FIXED_AMOUNT'
    value: number
    minOrderAmount?: number
    maxRedemptions?: number
    perUserLimit?: number
    startsAt?: string
    endsAt?: string
    active: boolean
    createdAt: string
    updatedAt?: string
    productIds?: string[]
    categoryIds?: string[]
}

export interface DiscountCreateRequest {
    code: string
    name: string
    description?: string
    type: 'PERCENTAGE' | 'FIXED_AMOUNT'
    value: number
    minOrderAmount?: number
    maxRedemptions?: number
    perUserLimit?: number
    startsAt?: string
    endsAt?: string
    active?: boolean
}

export interface DiscountUpdateRequest {
    code?: string
    name?: string
    description?: string
    type?: 'PERCENTAGE' | 'FIXED_AMOUNT'
    value?: number
    minOrderAmount?: number
    maxRedemptions?: number
    perUserLimit?: number
    startsAt?: string
    endsAt?: string
    active?: boolean
}

export interface GetDiscountsParams {
    code?: string
    active?: boolean
    page?: number
    size?: number
}

export const getDiscounts = async (params: GetDiscountsParams = {}) => {
    const res = await api.get<PageResponse<DiscountResponse>>('/api/v1/discounts', { params })
    return res.data
}

export const getDiscount = async (id: string) => {
    const res = await api.get<DiscountResponse>(`/api/v1/discounts/${id}`)
    return res.data
}

export const createDiscount = async (data: DiscountCreateRequest) => {
    const res = await api.post<DiscountResponse>('/api/v1/discounts', data)
    return res.data
}

export const updateDiscount = async (id: string, data: DiscountUpdateRequest) => {
    const res = await api.put<DiscountResponse>(`/api/v1/discounts/${id}`, data)
    return res.data
}

export const deleteDiscount = async (id: string) => {
    await api.delete(`/api/v1/discounts/${id}`)
}

// Quản lý sản phẩm áp dụng discount
export interface ProductAssignmentRequest {
    productIds: string[]
}

export const addProductsToDiscount = async (id: string, data: ProductAssignmentRequest) => {
    await api.post(`/api/v1/discounts/${id}/products`, data)
}

export const removeProductsFromDiscount = async (id: string, data: ProductAssignmentRequest) => {
    await api.delete(`/api/v1/discounts/${id}/products`, { data })
}

// Quản lý danh mục áp dụng discount
export interface CategoryAssignmentRequest {
    categoryIds: string[]
}

export const addCategoriesToDiscount = async (id: string, data: CategoryAssignmentRequest) => {
    await api.post(`/api/v1/discounts/${id}/categories`, data)
}

export const removeCategoriesFromDiscount = async (id: string, data: CategoryAssignmentRequest) => {
    await api.delete(`/api/v1/discounts/${id}/categories`, { data })
}

export default {
    getDiscounts,
    getDiscount,
    createDiscount,
    updateDiscount,
    deleteDiscount,
    addProductsToDiscount,
    removeProductsFromDiscount,
    addCategoriesToDiscount,
    removeCategoriesFromDiscount,
}
