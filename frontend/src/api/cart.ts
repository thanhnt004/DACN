import api from './http'

export interface CartItemRequest {
    variantId: string
    quantity: number
}

export interface UpdateCartItemVariantRequest {
    variantId: string
    quantity: number
}

export interface CartItemResponse {
    id: string
    productId: string
    productName: string
    variantId: string
    variantName?: string
    imageUrl?: string
    quantity: number
    stockQuantity: number
    unitPriceAmount: number
    isInStock: boolean
}

export interface CartResponse {
    id: string
    items: CartItemResponse[]
    cartStatus?: 'ACTIVE' | 'MERGED' | 'CONVERTED_TO_ORDER'
    createdAt?: string
    updatedAt?: string
    warnings?: string[]
}

// Helper to get/set guest cart ID from localStorage
export const getGuestCartId = (): string | null => {
    return localStorage.getItem('guestCartId')
}

export const setGuestCartId = (cartId: string) => {
    localStorage.setItem('guestCartId', cartId)
}

export const clearGuestCartId = () => {
    localStorage.removeItem('guestCartId')
}

// Get cart headers with guest cart ID
const getCartHeaders = () => {
    const guestCartId = getGuestCartId()
    return guestCartId ? { 'X-Cart-ID': guestCartId } : {}
}

/**
 * Get or create cart
 * This endpoint will get existing cart or create a new one if none exists
 */
export const getOrCreateCart = async (): Promise<CartResponse> => {
    const res = await api.get<CartResponse>('/api/v1/carts', {
        headers: getCartHeaders()
    })

    // If this is a new cart and we don't have a guest cart ID yet, save it
    if (!getGuestCartId() && res.data.id) {
        setGuestCartId(res.data.id)
    }

    return res.data
}

/**
 * Merge guest cart with user cart after login
 */
export const mergeCart = async () => {
    const res = await api.post<CartResponse>('/api/v1/carts/merge', null, {
        headers: getCartHeaders()
    })
    clearGuestCartId() // Clear guest cart after merge
    return res.data
}

/**
 * Add item to cart
 */
export const addItemToCart = async (item: CartItemRequest): Promise<CartResponse> => {
    const res = await api.post<CartResponse>('/api/v1/carts/items', item, {
        headers: getCartHeaders()
    })

    // If no guest cart exists yet, save the cart ID
    if (!getGuestCartId() && res.data.id) {
        setGuestCartId(res.data.id)
    }

    return res.data
}

/**
 * Remove item from cart
 */
export const removeItemFromCart = async (itemId: string): Promise<CartResponse> => {
    const res = await api.delete<CartResponse>(`/api/v1/carts/items/${itemId}`, {
        headers: getCartHeaders()
    })
    return res.data
}

/**
 * Remove all items from cart
 */
export const removeAllItems = async (): Promise<CartResponse> => {
    const res = await api.delete<CartResponse>('/api/v1/carts/items', {
        headers: getCartHeaders()
    })
    return res.data
}

/**
 * Update cart item (quantity or variant)
 */
export const updateCartItem = async (
    itemId: string,
    update: UpdateCartItemVariantRequest
): Promise<CartResponse> => {
    const res = await api.put<CartResponse>(`/api/v1/carts/items/${itemId}`, update, {
        headers: getCartHeaders()
    })
    return res.data
}

/**
 * Calculate cart totals
 */
export const calculateCartTotal = (cart: CartResponse | null) => {
    if (!cart || !cart.items || cart.items.length === 0) {
        return {
            subtotal: 0,
            total: 0,
            itemCount: 0
        }
    }

    const subtotal = cart.items.reduce((sum, item) => {
        return sum + (item.unitPriceAmount * item.quantity)
    }, 0)

    const itemCount = cart.items.reduce((sum, item) => sum + item.quantity, 0)

    return {
        subtotal,
        total: subtotal, // Can add shipping, tax, discounts here
        itemCount
    }
}

export default {
    getOrCreateCart,
    mergeCart,
    addItemToCart,
    removeItemFromCart,
    removeAllItems,
    updateCartItem,
    calculateCartTotal,
    getGuestCartId,
    setGuestCartId,
    clearGuestCartId
}
