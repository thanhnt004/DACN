import api from '../http'
import type { PageResponse } from './users'

export interface BrandDto {
    id?: string
    name: string
    slug: string
    description?: string
    productsCount?: number
    totalSales?: number
    createdAt?: string
    updatedAt?: string
}

export interface BrandFilter {
    name?: string
    minProduct?: number
    maxProduct?: number
    sortBy?: string
    direction?: string
    page?: number
    size?: number
}

export interface CategoryResponse {
    id: string
    name: string
    slug: string
    description?: string
    parentId?: string
    level: number
    productsCount?: number
    children?: CategoryResponse[]
    createdAt: string
    updatedAt: string
}

export interface CategoryCreateRequest {
    name: string
    slug: string
    description?: string
    parentId?: string
}

export interface CategoryUpdateRequest {
    id: string
    name?: string
    slug?: string
    description?: string
    parentId?: string
}

export interface GetBrandsParams {
    page?: number
    size?: number
    search?: string
}

export interface GetCategoriesParams {
    page?: number
    size?: number
    search?: string
    parentId?: string
    sort?: string
    all?: boolean
}

// Brand APIs
export const getBrands = async (filter: BrandFilter = {}) => {
    const res = await api.get<PageResponse<BrandDto>>('/api/v1/brands', { params: filter })
    return res.data
}

export const getBrand = async (slugOrId: string) => {
    const res = await api.get<BrandDto>(`/api/v1/brands/${slugOrId}`)
    return res.data
}

export const createBrand = async (data: BrandDto) => {
    const res = await api.post<BrandDto>('/api/v1/brands', data)
    return res.data
}

export const updateBrand = async (id: string, data: BrandDto) => {
    const res = await api.put<BrandDto>(`/api/v1/brands/${id}`, data)
    return res.data
}

export const deleteBrand = async (id: string) => {
    await api.delete(`/api/v1/brands/${id}`)
}

// Category APIs
export const getCategories = async (params: GetCategoriesParams = {}) => {
    const res = await api.get<PageResponse<CategoryResponse>>('/api/v1/categories/flat', { params })
    return res.data
}

export const getCategoryTree = async (rootId?: string, depth: number = 3) => {
    const res = await api.get<CategoryResponse>('/api/v1/categories', {
        params: { rootId, depth },
    })
    return res.data
}

export const getCategory = async (slugOrId: string) => {
    const res = await api.get<CategoryResponse>(`/api/v1/categories/${slugOrId}`)
    return res.data
}

export const createCategory = async (data: CategoryCreateRequest) => {
    const res = await api.post<CategoryResponse>('/api/v1/categories', data)
    return res.data
}

export const updateCategory = async (id: string, data: CategoryUpdateRequest) => {
    // Ensure id is in the request body as required by backend
    const payload = { ...data, id }
    const res = await api.put<CategoryResponse>(`/api/v1/categories/${id}`, payload)
    return res.data
}

export const deleteCategory = async (id: string, force?: boolean, reassignTo?: string) => {
    await api.delete(`/api/v1/categories/${id}`, {
        params: { force, reassignTo },
    })
}

export const moveCategory = async (id: string, newParentId?: string) => {
    await api.put(`/api/v1/categories/${id}/move`, { newParentId })
}

export default {
    getBrands,
    getBrand,
    createBrand,
    updateBrand,
    deleteBrand,
    getCategories,
    getCategoryTree,
    getCategory,
    createCategory,
    updateCategory,
    deleteCategory,
    moveCategory,
}
