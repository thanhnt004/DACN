import { create } from 'zustand'
import * as CartApi from '../api/cart'

interface CartState {
    cart: CartApi.CartResponse | null
    loading: boolean
    error: string | null

    // Actions
    fetchCart: () => Promise<void>
    addToCart: (variantId: string, quantity: number) => Promise<void>
    removeFromCart: (itemId: string) => Promise<void>
    updateCartItem: (itemId: string, variantId: string, quantity: number) => Promise<void>
    clearCart: () => Promise<void>
    mergeCart: () => Promise<void>

    // Computed
    getItemCount: () => number
    getTotal: () => number
}

export const useCartStore = create<CartState>((set, get) => ({
    cart: null,
    loading: false,
    error: null,

    fetchCart: async () => {
        set({ loading: true, error: null })
        try {
            const cart = await CartApi.getOrCreateCart()
            set({ cart, loading: false })
        } catch (error: any) {
            console.error('Error fetching cart:', error)
            set({ error: error.message || 'Failed to fetch cart', loading: false })
        }
    },

    addToCart: async (variantId: string, quantity: number) => {
        set({ loading: true, error: null })
        try {
            const cart = await CartApi.addItemToCart({ variantId, quantity })
            set({ cart, loading: false })
        } catch (error: any) {
            console.error('Error adding to cart:', error)

            // Retry logic: if it failed, maybe the cart ID is bad or expired.
            // Try clearing it and retrying.
            if (CartApi.getGuestCartId()) {
                console.log('Retrying add to cart without guest ID...')
                CartApi.clearGuestCartId()
                try {
                    const cart = await CartApi.addItemToCart({ variantId, quantity })
                    set({ cart, loading: false })
                    return // Success on retry
                } catch (retryError) {
                    console.error('Retry failed:', retryError)
                }
            }

            set({ error: error.message || 'Failed to add item to cart', loading: false })
            throw error
        }
    },

    removeFromCart: async (itemId: string) => {
        set({ loading: true, error: null })
        try {
            const cart = await CartApi.removeItemFromCart(itemId)
            set({ cart, loading: false })
        } catch (error: any) {
            console.error('Error removing from cart:', error)
            set({ error: error.message || 'Failed to remove item', loading: false })
            throw error
        }
    },

    updateCartItem: async (itemId: string, variantId: string, quantity: number) => {
        set({ loading: true, error: null })
        try {
            const cart = await CartApi.updateCartItem(itemId, { newVariantId: variantId, newQuantity: quantity })
            set({ cart, loading: false })
        } catch (error: any) {
            console.error('Error updating cart item:', error)
            set({ error: error.message || 'Failed to update item', loading: false })
            throw error
        }
    },

    clearCart: async () => {
        set({ loading: true, error: null })
        try {
            const cart = await CartApi.removeAllItems()
            set({ cart, loading: false })
        } catch (error: any) {
            console.error('Error clearing cart:', error)
            set({ error: error.message || 'Failed to clear cart', loading: false })
            throw error
        }
    },

    mergeCart: async () => {
        set({ loading: true, error: null })
        try {
            const cart = await CartApi.mergeCart()
            set({ cart, loading: false })
        } catch (error: any) {
            console.error('Error merging cart:', error)
            set({ error: error.message || 'Failed to merge cart', loading: false })
        }
    },

    getItemCount: () => {
        const { cart } = get()
        if (!cart || !cart.items) return 0
        return cart.items.reduce((sum, item) => sum + item.quantity, 0)
    },

    getTotal: () => {
        const { cart } = get()
        const totals = CartApi.calculateCartTotal(cart)
        return totals.total
    }
}))
