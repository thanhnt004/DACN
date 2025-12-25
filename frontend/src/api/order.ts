import api from './http';
import { UserAddress } from './checkout';

export type ReturnOption = 'PICKUP' | 'SELF_SHIP';

export type PaymentRefundOption = 'BANK_TRANSFER' | 'E_WALLET';

export interface OrderItemDTO {
    id?: string;
    productId: string;
    productName: string;
    variantId: string;
    variantName: string;
    sku: string;
    quantity: number;
    unitPriceAmount: number;
    totalAmount: number;
    imageUrl?: string;
}

export interface ShipmentResponse {
    id: string;
    carrier?: string;
    trackingNumber?: string;
    status?: string;
    deliveredAt?: string;
    warehouse?: string;
    isReturn?: boolean;
}

export interface OrderChangeRequestResponse {
    id: string;
    type: 'CANCEL' | 'RETURN';
    status: 'PENDING' | 'APPROVED' | 'REJECTED';
    reason: string;
    adminNote?: string;
    metadata?: Record<string, string>;
    images?: string[];
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
    shipment?: ShipmentResponse;
    estimatedDeliveryTime?: string;
    changeRequest?: OrderChangeRequestResponse;
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

export const retryPayment = async (orderId: string): Promise<string> => {
    const res = await rePay(orderId);
    return res.paymentUrl;
};

export interface CancelOrderRequest {
    reason: string;
    paymentRefundOption?: {
        method: 'BANK_TRANSFER' | 'OTHER';
        data?: Record<string, string | number | boolean>;
    };
}

export const requestCancel = async (id: string, data: CancelOrderRequest): Promise<void> => {
    await api.post(`/api/v1/orders/${id}/cancel`, data);
};

export interface ReturnOrderRequest {
    returnAddress: UserAddress;
    returnOption: {
        type: 'PICKUP' | 'DROP_OFF';
        description?: string;
    };
    reason: string;
    images?: string[];
    paymentRefundOption?: {
        method: 'BANK_TRANSFER' | 'OTHER';
        data?: Record<string, string | number | boolean>;
    };
}

export const getOrderDetail = async (orderId: string): Promise<OrderResponse> => {
    const response = await api.get(`/api/v1/orders/${orderId}`);
    return response.data;
};

export const cancelOrder = async (orderId: string, request: CancelOrderRequest): Promise<void> => {
    await api.post(`/api/v1/orders/${orderId}/cancel`, request);
};

export const requestReturn = async (orderId: string, request: ReturnOrderRequest): Promise<void> => {
    await api.post(`/api/v1/orders/${orderId}/return`, request);
};

export const mergeOrders = async (): Promise<void> => {
    await api.post('/api/v1/orders/merge-orders');
};
