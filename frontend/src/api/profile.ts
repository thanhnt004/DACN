import api from './http'

export interface Address {
    id?: string
    fullName: string
    phone: string
    line1: string
    line2?: string
    ward?: string
    district?: string
    province?: string
    isDefaultShipping: boolean
}

export interface UserProfile {
    id: string
    email: string
    fullName: string
    phone: string
    dateOfBirth?: string
    gender?: 'M' | 'F' | 'O'
    avatarUrl?: string
    role: string
    isActive: boolean
    passwordChangedAt?: string
}

export interface UpdateProfileRequest {
    fullName?: string
    phone?: string
    dateOfBirth?: string
    gender?: 'M' | 'F' | 'O'
    avatarUrl?: string
}

export interface ChangePasswordRequest {
    currentPassword: string
    newPassword: string
    newPasswordConfirm: string
}

export interface OAuthAccount {
    provider: string
}

// Get current user profile
export const getProfile = async () => {
    const res = await api.get<UserProfile>('/api/v1/me')
    return res.data
}

// Update profile
export const updateProfile = async (data: UpdateProfileRequest) => {
    await api.put('/api/v1/me', data)
    // Backend returns 204 No Content on success
}

// Change password
export const changePassword = async (data: ChangePasswordRequest) => {
    await api.post('/api/v1/auth/change-password', data)
    // Backend returns 204 No Content on success
}

// Get addresses
export const getAddresses = async () => {
    const res = await api.get<Address[]>('/api/v1/me/address')
    return res.data
}

// Add address
export const addAddress = async (data: Omit<Address, 'id'>) => {
    const res = await api.post<Address>('/api/v1/me/address', data)
    return res.data
}

// Update address
export const updateAddress = async (id: string, data: Omit<Address, 'id'>) => {
    await api.put<Address>(`/api/v1/me/addresses/${id}`, data)
    // Backend returns 204 No Content on success
}

// Delete address
export const deleteAddress = async (id: string) => {
    await api.delete(`/api/v1/me/addresses/${id}`)
    // Backend returns 204 No Content on success
}

// Get OAuth accounts
export const getOAuthAccounts = async () => {
    const res = await api.get<OAuthAccount[]>('/api/v1/me/linked-account')
    return res.data
}

// Link OAuth account - Get OAuth URL from backend
export const linkOAuthAccount = (provider: 'google' | 'facebook') => {
    const redirectUrl = `${window.location.origin}${window.location.pathname}`;
    const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8089';
    window.location.href = `${baseUrl}/api/v1/auth/oauth2/link/${provider}?redirect=${encodeURIComponent(redirectUrl)}`;
};

// Unlink OAuth account
export const unlinkOAuthAccount = async (provider: 'google' | 'facebook') => {
    await api.delete(`/api/v1/auth/oauth2/link/${provider}`)
    // Backend returns 204 No Content on success
};


// Deactivate account - Backend không có API này
export const deactivateAccount = async (_password: string) => {
    // Backend chưa implement API này
    throw new Error('API chưa được triển khai')
}

export default {
    getProfile,
    updateProfile,
    changePassword,
    getAddresses,
    addAddress,
    updateAddress,
    deleteAddress,
    getOAuthAccounts,
    linkOAuthAccount,
    unlinkOAuthAccount,
    deactivateAccount
}
