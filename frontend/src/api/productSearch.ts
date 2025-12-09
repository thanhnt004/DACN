import api from './http'
import type { ProductSummaryResponse } from './products'

export interface ProductSearchResult {
    productId: string
    embeddingId: number
    content: string
    similarity: number
    product: ProductSummaryResponse
}

export interface SearchStats {
    totalProducts: number
    totalEmbeddings: number
    lastIndexed?: string
}

export const vectorSearch = async (
    query: string,
    limit: number = 10,
    threshold?: number
): Promise<ProductSearchResult[]> => {
    const params: Record<string, any> = { query, limit }
    if (threshold !== undefined) {
        params.threshold = threshold
    }
    const response = await api.get<ProductSearchResult[]>('/api/v1/products/search/vector', { params })
    return response.data
}

export const getSearchSuggestions = async (query: string, limit: number = 5): Promise<string[]> => {
    const response = await api.get<string[]>('/api/v1/products/search/suggest', {
        params: { query, limit }
    })
    return response.data
}

export const getSearchStats = async (): Promise<SearchStats> => {
    const response = await api.get<SearchStats>('/api/v1/products/search/stats')
    return response.data
}
