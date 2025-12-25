import api from '../http'
import type { PageResponse } from './users'
import type { CategoryResponse } from './brandCategory'

export type ProductStatus = 'ACTIVE' | 'DRAFT' | 'ARCHIVED'
export type VariantStatus = 'ACTIVE' | 'DISCONTINUED'
export type Gender = 'men' | 'women' | 'unisex'

export interface InventoryPayload {
    quantityOnHand?: number
    quantityReserved?: number
    reorderLevel?: number
}

export interface InventoryResponse extends InventoryPayload {
    variantId: string
    available?: number
    updatedAt?: string
}

export interface ProductImagePayload {
    id?: string
    imageUrl: string
    alt?: string
    position?: number
    publicId?: string
    isDefault?: boolean
    colorId?: string
    variantId?: string
}

export interface ProductImageResponse extends ProductImagePayload {
    id: string
}

export interface ProductOptions {
    size?: Array<{ id: string; code: string; name: string }>
    color?: Array<{ id: string; name: string; hexCode: string }>
}

export interface ProductDetailResponse {
    id: string
    name: string
    slug: string
    description?: string
    material?: string
    priceAmount: number
    gender?: Gender
    status?: ProductStatus
    seoTitle?: string
    seoDescription?: string
    version?: number
    primaryImageUrl?: string
    isInStock?: boolean
    brandId?: string
    brand?: { id: string; name: string; slug: string }
    categories?: CategoryResponse[]
    images?: ProductImageResponse[]
    variants?: VariantResponse[]
    options?: ProductOptions
    createdAt?: string
    updatedAt?: string
}

export interface ProductSummaryResponse {
    id: string
    slug: string
    colors?: string[] // List of color names
    sizes?: string[] // List of size names
    imageUrl?: string
    name: string
    gender?: string
    status?: ProductStatus
    priceAmount: number
    isInStock?: boolean
    totalStock?: number
    brandId?: string
    brandName?: string
}

export interface VariantResponse {
    id: string
    productId: string
    sku: string
    barcode?: string
    sizeId?: string
    colorId?: string
    size?: { id: string; name: string; code: string }
    color?: { id: string; name: string; hexCode: string }
    priceAmount: number
    compareAtAmount?: number
    historyCost?: number
    weightGrams?: number
    status?: VariantStatus
    createdAt?: string
    updatedAt?: string
    deletedAt?: string
    version?: number
    inventory?: InventoryResponse
    image?: ProductImageResponse
}

export interface VariantCreateRequest {
    sku: string
    barcode?: string
    sizeId?: string
    colorId?: string
    priceAmount: number
    compareAtAmount?: number
    historyCost?: number
    weightGrams?: number
    status?: VariantStatus
    inventory?: InventoryPayload
    image?: ProductImagePayload
}

export interface VariantUpdateRequest {
    sku?: string
    barcode?: string
    sizeId?: string
    colorId?: string
    priceAmount?: number
    compareAtAmount?: number
    historyCost?: number
    weightGrams?: number
    status?: VariantStatus
    version: number
    inventory?: InventoryPayload
    image?: ProductImagePayload
}

export interface ProductCreateRequest {
    name: string
    slug: string
    brandId?: string
    categoryId?: string[]
    priceAmount: number
    description?: string
    material?: string
    gender?: Gender
    seoTitle?: string
    seoDescription?: string
    images?: ProductImagePayload[]
}

export interface ProductUpdateRequest {
    id: string
    name?: string
    slug?: string
    brandId?: string
    categoryId?: string[]
    description?: string
    material?: string
    priceAmount?: number
    gender?: Gender
    seoTitle?: string
    seoDescription?: string
    primaryImageUrl?: string
    status?: ProductStatus
    images?: ProductImagePayload[]
    version: number
}

export interface GetProductsParams {
    page?: number
    size?: number
    search?: string
    categoryId?: string
    status?: string
    gender?: string
    brandId?: string
    minPriceAmount?: number
    maxPriceAmount?: number
    sizeIds?: string[]
    colorIds?: string[]
    sortBy?: string
    direction?: string
}

export const getProducts = async (params: GetProductsParams = {}) => {
    const res = await api.get<PageResponse<ProductSummaryResponse>>('/api/v1/products', { params })
    return res.data
}

export const getProductDetail = async (slugOrId: string, includes?: string[]) => {
    const res = await api.get<ProductDetailResponse>(`/api/v1/products/${slugOrId}`, {
        params: includes && includes.length ? { includes } : undefined,
    })
    return res.data
}

export const createProduct = async (data: ProductCreateRequest) => {
    const res = await api.post<ProductDetailResponse>('/api/v1/admin/products', data)
    return res.data
}

export const updateProduct = async (id: string, data: ProductUpdateRequest) => {
    const res = await api.put<ProductDetailResponse>(`/api/v1/admin/products/${id}`, data)
    return res.data
}

export const deleteProduct = async (id: string) => {
    await api.delete(`/api/v1/admin/products/${id}`)
}

export const changeProductStatus = async (id: string, status: ProductStatus) => {
    await api.patch(`/api/v1/admin/products/${id}`, null, { params: { status } })
}

export const getProductVariants = async (productId: string) => {
    const res = await api.get<VariantResponse[]>(`/api/v1/products/${productId}/variants`)
    return res.data
}

export const createVariant = async (productId: string, data: VariantCreateRequest) => {
    const res = await api.post<VariantResponse>(`/api/v1/products/${productId}/variants`, data)
    return res.data
}

export const createVariantBulk = async (productId: string, data: VariantCreateRequest[]) => {
    const res = await api.post<VariantResponse[]>(`/api/v1/products/${productId}/variants/bulk`, data)
    return res.data
}

export const updateVariant = async (productId: string, variantId: string, data: VariantUpdateRequest) => {
    const res = await api.put<VariantResponse>(`/api/v1/products/${productId}/variants/${variantId}`, data)
    return res.data
}

export const deleteVariant = async (productId: string, variantId: string) => {
    await api.delete(`/api/v1/products/${productId}/variants/${variantId}`)
}

export const getProductImages = async (productId: string) => {
    const res = await api.get<ProductImageResponse[]>(`/api/v1/admin/products/${productId}/images`)
    return res.data
}

export const createProductImage = async (productId: string, data: ProductImagePayload) => {
    await api.post(`/api/v1/admin/products/${productId}/images`, data)
}

export const updateProductImage = async (productId: string, imageId: string, data: ProductImagePayload) => {
    await api.put(`/api/v1/admin/products/${productId}/images/${imageId}`, data)
}

export const deleteProductImage = async (productId: string, imageId: string) => {
    await api.delete(`/api/v1/admin/products/${productId}/images/${imageId}`)
}

export default {
    getProducts,
    getProductDetail,
    createProduct,
    updateProduct,
    deleteProduct,
    changeProductStatus,
    getProductVariants,
    createVariant,
    createVariantBulk,
    updateVariant,
    deleteVariant,
    getProductImages,
    createProductImage,
    updateProductImage,
    deleteProductImage,
}
