import api from './http'

export interface ShipmentInfo {
    id: string
    carrier: string
    trackingNumber: string
    status: string
    deliveredAt: string | null
    warehouse: string | null
}

export const getShipmentInfo = async (orderId: string): Promise<ShipmentInfo> => {
    const response = await api.get<ShipmentInfo>(`/api/v1/shipping/${orderId}`)
    return response.data
}

export const retryCreateReturnShipment = async (orderId: string): Promise<void> => {
    await api.post(`/api/v1/shipping/admin/orders/${orderId}/retry-return-shipment`)
}
