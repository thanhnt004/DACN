import api from '../http'

export interface UserProfileDto {
    id: string
    email: string
    fullName: string
    phone: string
    dateOfBirth?: string
    role: 'USER' | 'ADMIN' | 'STAFF'
    isActive: boolean
    createdAt: string
    updatedAt: string
}

export interface PageResponse<T> {
    content: T[]
    totalElements: number
    totalPages: number
    size: number
    number: number
}

export interface GetUsersParams {
    role?: 'USER' | 'ADMIN' | 'STAFF'
    isActive?: boolean
    page?: number
    size?: number
}

export const getUsers = async (params: GetUsersParams = {}) => {
    const res = await api.get<PageResponse<UserProfileDto>>('/api/v1/users', { params })
    return res.data
}

export const banUser = async (userId: string) => {
    const res = await api.post<{ status: string }>(`/api/v1/users/ban/${userId}`)
    return res.data
}

export const restoreUser = async (userId: string) => {
    const res = await api.post<{ status: string }>(`/api/v1/users/restore_user/${userId}`)
    return res.data
}

export default { getUsers, banUser, restoreUser }
