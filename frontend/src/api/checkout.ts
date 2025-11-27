import api from './http'

export interface CheckoutItem {
    cartItemId?: string
    variantId: string
    quantity: number
}

export interface CheckoutSessionCreateRequest {
    cartId?: string
    items: CheckoutItem[]
    discountCode?: string
}

export interface UserAddress {
    id?: string
    fullName: string
    phone: string
    line1: string
    line2?: string
    ward?: string
    district?: string
    province?: string
    isDefaultShipping?: boolean
}

export type UpdateAddressRequest = UserAddress

export interface UpdatePaymentMethodRequest {
    paymentMethodId: string
}

export interface PaymentMethodResponse {
    id: string
    name: string
    description?: string
    type: string
    iconUrl?: string
    isAvailable?: boolean
    unavailableReason?: string
    feeAmount?: number
    isRecommended?: boolean
}

export interface CheckoutSession {
    id: string
    cartId?: string
    items: CheckoutItem[]
    notes?: string
    subtotalAmount: number
    discountAmount: number
    shippingAmount: number
    totalAmount: number
    shippingAddress?: UserAddress
    selectedPaymentMethod?: PaymentMethodResponse
    availablePaymentMethods: PaymentMethodResponse[]
    canConfirm: boolean
    validationErrors?: string[]
    warnings?: string[]
    sessionToken: string
}

export interface OrderCreatedResponse {
    id: string
    orderNumber: string
    totalAmount: number
    status: string
    paymentUrl?: string
}

export interface UpdateDiscountRequest {
    discountCode: string
}

export const createSession = async (data: CheckoutSessionCreateRequest): Promise<CheckoutSession> => {
    const response = await api.post('/api/v1/checkout/sessions', data)
    return response.data
}

export const getSession = async (sessionId: string, token: string): Promise<CheckoutSession> => {
    const response = await api.get(`/api/v1/checkout/sessions/${sessionId}`, {
        headers: { 'X-Session-Token': token }
    })
    return response.data
}

export const updateAddress = async (sessionId: string, token: string, data: UpdateAddressRequest): Promise<CheckoutSession> => {
    const response = await api.put(`/api/v1/checkout/sessions/${sessionId}/address`, data, {
        headers: { 'X-Session-Token': token }
    })
    return response.data
}

export const updatePaymentMethod = async (sessionId: string, token: string, data: UpdatePaymentMethodRequest): Promise<CheckoutSession> => {
    const response = await api.put(`/api/v1/checkout/sessions/${sessionId}/payment-method`, data, {
        headers: { 'X-Session-Token': token }
    })
    return response.data
}

export const updateNotes = async (sessionId: string, token: string, notes: string): Promise<CheckoutSession> => {
    const response = await api.put(`/api/v1/checkout/sessions/${sessionId}/notes`, notes, {
        headers: { 'X-Session-Token': token }
    })
    return response.data
}

export const updateDiscount = async (sessionId: string, token: string, discountCode: string): Promise<CheckoutSession> => {
    const response = await api.put(`/api/v1/checkout/sessions/${sessionId}/discount`, { discountCode }, {
        headers: { 'X-Session-Token': token }
    })
    return response.data
}

const generateIdempotencyKey = (): string => {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
        return crypto.randomUUID()
    }
    const timestamp = Date.now()
    const random = Math.random().toString(36).slice(2, 10)
    return `idem-${timestamp}-${random}`
}

export const createIdempotencyKey = (): string => generateIdempotencyKey()

export const confirmCheckout = async (
    sessionId: string,
    token: string,
    idempotencyKey?: string,
    notes?: string
): Promise<OrderCreatedResponse> => {
    const key = idempotencyKey ?? generateIdempotencyKey()
    const payload = notes ?? ''
    const response = await api.post(`/api/v1/checkout/sessions/${sessionId}/confirm`, payload, {
        headers: {
            'X-Session-Token': token,
            'Idempotency-Key': key,
            'Content-Type': 'text/plain; charset=utf-8'
        }
    })
    return response.data
}
