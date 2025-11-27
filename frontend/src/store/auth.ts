import { create } from 'zustand'
import { bindTokenAccess } from '../api/http'
import * as AuthApi from '../api/auth'
import { extractProblemMessage } from '../lib/problemDetails'

// Helper: parse JWT và lấy thời gian hết hạn (exp)
function parseJwt(token: string): { exp?: number } | null {
    try {
        const base64Url = token.split('.')[1]
        if (!base64Url) return null
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
        const jsonPayload = decodeURIComponent(
            atob(base64)
                .split('')
                .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
                .join('')
        )
        return JSON.parse(jsonPayload)
    } catch {
        return null
    }
}

let refreshTimerId: ReturnType<typeof setTimeout> | null = null

// Schedule auto refresh trước khi token expire (refresh 1 phút trước khi hết hạn)
function scheduleTokenRefresh(token: string, refreshFn: () => Promise<void>) {
    if (refreshTimerId) clearTimeout(refreshTimerId)

    const payload = parseJwt(token)
    if (!payload?.exp) return

    const now = Math.floor(Date.now() / 1000)
    const expiresIn = payload.exp - now
    // Refresh 60s trước khi hết hạn, hoặc ngay lập tức nếu còn < 60s
    const refreshIn = Math.max(0, expiresIn - 60)

    refreshTimerId = setTimeout(async () => {
        try {
            await refreshFn()
        } catch {
            // Silent fail: interceptor sẽ xử lý logout nếu refresh thất bại
        }
    }, refreshIn * 1000)
}

type AuthState = {
    accessToken: string | null
    isAuthenticated: boolean
    loading: boolean
    error: string | null
    isAdmin: boolean
    user: { email: string } | null
    requireEmailVerification: boolean
    login: (identifier: string, password: string) => Promise<AuthApi.LoginResponse>
    logout: () => Promise<void>
    setAccessToken: (token: string | null) => void
    setIsAdmin: (isAdmin: boolean) => void
    setUser: (user: { email: string } | null) => void
    setRequireEmailVerification: (required: boolean) => void
    refreshToken: () => Promise<void>
}

export const useAuthStore = create<AuthState>((set, get) => ({
    accessToken: localStorage.getItem('access_token'),
    isAuthenticated: !!localStorage.getItem('access_token'),
    loading: false,
    error: null,
    isAdmin: localStorage.getItem('is_admin') === 'true',
    user: null,
    requireEmailVerification: false,
    setAccessToken: (token) => {
        if (token) {
            localStorage.setItem('access_token', token)
            // Schedule auto refresh khi set token mới
            scheduleTokenRefresh(token, () => get().refreshToken())
        } else {
            localStorage.removeItem('access_token')
            // Clear timer khi logout
            if (refreshTimerId) clearTimeout(refreshTimerId)
        }
        set({ accessToken: token, isAuthenticated: !!token })
    },
    refreshToken: async () => {
        try {
            const res = await AuthApi.refresh()
            get().setAccessToken(res.accessToken)
            // Backend có thể trả isAdmin trong refresh response
            if ('admin' in res || 'isAdmin' in res) {
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                const isAdminValue = (res as any).admin ?? (res as any).isAdmin ?? false
                get().setIsAdmin(!!isAdminValue)
            }
        } catch (err) {
            // Refresh fail -> logout
            get().setAccessToken(null)
            get().setIsAdmin(false)
            throw err
        }
    },
    login: async (identifier, password) => {
        set({ loading: true, error: null })
        try {
            const res = await AuthApi.login({ identifier, password }) as AuthApi.LoginResponse

            // Check if email verification is required
            if (res.requireEmailVerification) {
                set({
                    requireEmailVerification: true,
                    user: { email: identifier }
                })
                return res
            }

            get().setAccessToken(res.accessToken)
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            const isAdminValue = (res as any).admin ?? res.isAdmin ?? false
            get().setIsAdmin(!!isAdminValue)
            set({ requireEmailVerification: false })
            return res
        } catch (e: unknown) {
            const responseData = typeof e === 'object' && e && 'response' in e
                ? (e as { response?: { data?: unknown } }).response?.data
                : undefined
            const fallback = typeof e === 'object' && e && 'message' in e && typeof (e as { message?: unknown }).message === 'string'
                ? (e as { message: string }).message
                : 'Login failed'
            const message = extractProblemMessage(responseData, fallback)
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
            get().setIsAdmin(false)
            set({ user: null, requireEmailVerification: false })
        }
    },
    setIsAdmin: (isAdmin) => {
        if (isAdmin) localStorage.setItem('is_admin', 'true')
        else localStorage.removeItem('is_admin')
        set({ isAdmin })
    },
    setUser: (user) => {
        set({ user })
    },
    setRequireEmailVerification: (required) => {
        set({ requireEmailVerification: required })
    },
}))

// Bind token access for http layer
bindTokenAccess(
    () => useAuthStore.getState().accessToken,
    (t) => useAuthStore.getState().setAccessToken(t)
)

// Auto schedule refresh nếu có token trong localStorage khi app load
const initialToken = localStorage.getItem('access_token')
if (initialToken) {
    scheduleTokenRefresh(initialToken, () => useAuthStore.getState().refreshToken())
}
