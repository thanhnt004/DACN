import axios, { AxiosError } from 'axios'
import type { AxiosInstance, AxiosRequestConfig, InternalAxiosRequestConfig } from 'axios'

// Simple token accessor to avoid circular imports
let accessTokenGetter: () => string | null = () => localStorage.getItem('access_token')
let accessTokenSetter: (token: string | null) => void = (t) => {
    if (t) localStorage.setItem('access_token', t)
    else localStorage.removeItem('access_token')
}
export const bindTokenAccess = (getter: typeof accessTokenGetter, setter: typeof accessTokenSetter) => {
    accessTokenGetter = getter
    accessTokenSetter = setter
}

const BASE_URL: string = (import.meta as unknown as { env?: Record<string, string> }).env?.VITE_API_BASE_URL || 'http://localhost:8089'

export const basicApi: AxiosInstance = axios.create({
    baseURL: BASE_URL,
    withCredentials: true, // send/receive refresh token cookie
    headers: { 'Content-Type': 'application/json' },
})

export const api: AxiosInstance = axios.create({
    baseURL: BASE_URL,
    withCredentials: true,
    headers: { 'Content-Type': 'application/json' },
})

// Request: attach access token
api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
    const token = accessTokenGetter()
    if (token) {
        config.headers = config.headers || {}
        const h = config.headers as Record<string, string>
        h['Authorization'] = `Bearer ${token}`
    }
    return config
})

let isRefreshing = false
let refreshPromise: Promise<string> | null = null
let queue: Array<(token: string | null) => void> = []

const notifyQueue = (token: string | null) => {
    queue.forEach((cb) => cb(token))
    queue = []
}

async function refreshAccessToken(): Promise<string> {
    if (refreshPromise) return refreshPromise
    isRefreshing = true
    refreshPromise = basicApi
        .post('/api/v1/auth/refresh')
        .then((res) => {
            const token: string = res.data?.accessToken
            if (!token) throw new Error('No access token in refresh response')
            accessTokenSetter(token)
            return token
        })
        .finally(() => {
            isRefreshing = false
            refreshPromise = null
        })
    return refreshPromise
}

// Response: on 401, try refresh once and retry original request
api.interceptors.response.use(
    (res) => res,
    async (error: AxiosError) => {
        const original = error.config as AxiosRequestConfig & { _retry?: boolean }
        const status = error.response?.status
        const isAuthEndpoint = original?.url?.startsWith('/api/v1/auth/')

        if (status === 401 && !original._retry && !isAuthEndpoint) {
            original._retry = true
            try {
                if (!isRefreshing) {
                    const newToken = await refreshAccessToken()
                    notifyQueue(newToken)
                    original.headers = original.headers || {}
                        ; (original.headers as Record<string, string>)['Authorization'] = `Bearer ${newToken}`
                    return api(original)
                } else {
                    // wait for queued refresh
                    const newToken = await new Promise<string | null>((resolve) => queue.push(resolve))
                    if (newToken) {
                        original.headers = original.headers || {}
                            ; (original.headers as Record<string, string>)['Authorization'] = `Bearer ${newToken}`
                        return api(original)
                    }
                }
            } catch {
                accessTokenSetter(null)
            }
        }
        return Promise.reject(error)
    }
)

export default api
