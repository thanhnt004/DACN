import api from '../http'

export interface SizeDto {
    id?: string
    name: string
    code?: string
    abbreviation?: string
}

export interface ColorDto {
    id?: string
    name: string
    hexCode?: string
}

// Size APIs
export const getSizes = async () => {
    const res = await api.get<SizeDto[]>('/api/v1/sizes')
    return res.data
}

export const getSize = async (id: string) => {
    const res = await api.get<SizeDto>(`/api/v1/sizes/${id}`)
    return res.data
}

export const createSize = async (data: SizeDto) => {
    const res = await api.post<SizeDto>('/api/v1/sizes', data)
    return res.data
}

export const updateSize = async (id: string, data: SizeDto) => {
    const res = await api.put<SizeDto>(`/api/v1/sizes/${id}`, data)
    return res.data
}

export const deleteSize = async (id: string) => {
    await api.delete(`/api/v1/sizes/${id}`)
}

// Color APIs
export const getColors = async () => {
    const res = await api.get<ColorDto[]>('/api/v1/colors')
    return res.data
}

export const getColor = async (id: string) => {
    const res = await api.get<ColorDto>(`/api/v1/colors/${id}`)
    return res.data
}

export const createColor = async (data: ColorDto) => {
    const res = await api.post<ColorDto>('/api/v1/colors', data)
    return res.data
}

export const updateColor = async (id: string, data: ColorDto) => {
    const res = await api.put<ColorDto>(`/api/v1/colors/${id}`, data)
    return res.data
}

export const deleteColor = async (id: string) => {
    await api.delete(`/api/v1/colors/${id}`)
}

export default {
    getSizes,
    getSize,
    createSize,
    updateSize,
    deleteSize,
    getColors,
    getColor,
    createColor,
    updateColor,
    deleteColor,
}
