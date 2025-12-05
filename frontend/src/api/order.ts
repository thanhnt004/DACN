import api from './http';
import { UserAddress } from './checkout';

export enum ReturnOption {
    PICKUP = 'PICKUP',
    SELF_SHIP = 'SELF_SHIP'
}

export enum PaymentRefundOption {
    BANK_TRANSFER = 'BANK_TRANSFER',
    E_WALLET = 'E_WALLET'
}

export interface OrderItemDTO {
    id: string;
    productId: string;
    productName: string;
    variantId: string;
    variantName: string;
    sku: string;
    quantity: number;
    unitPriceAmount: number;
    totalAmount: number;
    imageUrl?: string;
    estimatedDeliveryTime?: string;
}

export interface OrderResponse {
    id: string;
    orderNumber: string;
    status: string;
    subtotalAmount: number;
    discountAmount: number;
    shippingAmount: number;
    totalAmount: number;
    shippingAddress: UserAddress;
    notes?: string;
    placedAt: string;
    items: OrderItemDTO[];
    payments: PaymentDTO[];
    estimatedDeliveryTime?: string;
}
export interface PaymentDTO {
    id: string;
    provider: string;
    status: string;
    amount: number;
    expireAt: string;
}

export interface PageResponse<T> {
    content: T[];
    pageNo: number;
    pageSize: number;
    totalElements: number;
    totalPages: number;
    last: boolean;
}
export const getOrders = async (status: string = 'ALL', page: number = 0, size: number = 20, paymentType?: string): Promise<PageResponse<OrderResponse>> => {
    const response = await api.get('/api/v1/orders/get-order-list', {
        params: {
            status,
            page,
            size,
            paymentType
        }
    });
    return response.data;
};

export const cancelReturn = async (id: string): Promise<void> => {
    await api.post(`/api/v1/orders/${id}/cancel-return`);
};

export const rePay = async (orderId: string): Promise<{ paymentUrl: string }> => {
    const response = await api.post(`/api/v1/orders/${orderId}/re-pay`);
    return response.data;
};
