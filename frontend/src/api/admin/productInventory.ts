import api from '../http'

export interface InventoryRequest {
    quantityOnHand: number
    quantityReserved?: number
    reorderLevel: number
}

export interface InventoryResponse {
    variantId: string
    quantityOnHand: number
    quantityReserved: number
    available: number
    reorderLevel: number
    updatedAt: string
}

export const createInventory = async (
    variantId: string,
    data: InventoryRequest
): Promise<InventoryResponse> => {
    const response = await api.post<InventoryResponse>(
        `/api/v1/admin/products/variants/${variantId}/inventory`,
        data
    )
    return response.data
}

export const updateInventory = async (
    variantId: string,
    data: InventoryRequest
): Promise<InventoryResponse> => {
    const response = await api.put<InventoryResponse>(
        `/api/v1/admin/products/variants/${variantId}/inventory`,
        data
    )
    return response.data
}

export const getInventory = async (variantId: string): Promise<InventoryResponse> => {
    const response = await api.get<InventoryResponse>(
        `/api/v1/admin/products/variants/${variantId}/inventory`
    )
    return response.data
}
