import { create } from 'zustand'
import { bindTokenAccess } from '../api/http'
import * as AuthApi from '../api/auth'

type AuthState = {
    accessToken: string | null
    isAuthenticated: boolean
    loading: boolean
    error: string | null
    login: (identifier: string, password: string) => Promise<void>
    logout: () => Promise<void>
    setAccessToken: (token: string | null) => void
}

export const useAuthStore = create<AuthState>((set, get) => ({
    accessToken: localStorage.getItem('access_token'),
    isAuthenticated: !!localStorage.getItem('access_token'),
    loading: false,
    error: null,
    setAccessToken: (token) => {
        if (token) localStorage.setItem('access_token', token)
        else localStorage.removeItem('access_token')
        set({ accessToken: token, isAuthenticated: !!token })
    },
    login: async (identifier, password) => {
        set({ loading: true, error: null })
        try {
            const res = await AuthApi.login({ identifier, password })
            get().setAccessToken(res.accessToken)
        } catch (e: unknown) {
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            const err = e as any
            const message = err?.response?.data?.message || err?.message || 'Login failed'
            set({ error: message })
            throw e
        } finally {
            set({ loading: false })
        }
    },
    logout: async () => {
        try {
            await AuthApi.logout()
        } finally {
            get().setAccessToken(null)
        }
    },
}))

// Bind token access for http layer
bindTokenAccess(
    () => useAuthStore.getState().accessToken,
    (t) => useAuthStore.getState().setAccessToken(t)
)
