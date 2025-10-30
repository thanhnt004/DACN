import { basicApi } from './http'

export interface LoginRequest {
    identifier: string
    password: string
}
export interface LoginResponse {
    accessToken: string
    tokenType: 'Bearer'
    expiresIn: number
    isAdmin: boolean
    loginAt: string
    requireEmailVerification: boolean
}

export interface RegisterRequest {
    email: string
    fullName: string
    password: string
    phone: string
    dateOfBirth?: string // ISO date yyyy-MM-dd
}
export interface RegisterResponse {
    message: string
    emailVerificationRequired: boolean
    email: string
    createdAt: string
    userId: string
}

export const login = async (payload: LoginRequest) => {
    const res = await basicApi.post<LoginResponse>('/api/v1/auth/login', payload)
    return res.data
}

export const logout = async () => {
    const res = await basicApi.post<{ message: string }>('/api/v1/auth/logout')
    return res.data
}

export const refresh = async () => {
    const res = await basicApi.post<LoginResponse>('/api/v1/auth/refresh')
    return res.data
}

export const register = async (payload: RegisterRequest) => {
    const res = await basicApi.post<RegisterResponse>('/api/v1/auth/register', payload)
    return res.data
}

export interface VerifyEmailRequest {
    token: string
}

export interface VerifyEmailResponse {
    status: 'verified' | 'already_verified'
    userId?: string
    email?: string
    verifiedAt?: string
}

export const verifyEmail = async (payload: VerifyEmailRequest) => {
    const res = await basicApi.post<VerifyEmailResponse>('/api/v1/auth/verify-email', payload)
    return res.data
}

export interface ResendVerificationRequest {
    email: string
}

export const resendVerification = async (payload: ResendVerificationRequest) => {
    const res = await basicApi.post('/api/v1/auth/verify-email/resend', payload)
    return res.data
}

export default { login, logout, refresh, register, verifyEmail, resendVerification }
