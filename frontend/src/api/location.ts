import api from './http'

export interface ProvinceOption {
    id: number
    name: string
}

export interface DistrictOption {
    id: number
    name: string
    provinceId: number
}

export interface WardOption {
    code: string
    name: string
    districtId: number
}

let provinceCache: ProvinceOption[] | null = null
const districtCache = new Map<number, DistrictOption[]>()
const wardCache = new Map<number, WardOption[]>()

export const getProvinces = async (force = false): Promise<ProvinceOption[]> => {
    if (!force && provinceCache) {
        return provinceCache
    }
    const res = await api.get<ProvinceOption[]>('/api/v1/shipping/ghn/provinces')
    provinceCache = res.data
    return res.data
}

export const getDistricts = async (provinceId: number, force = false): Promise<DistrictOption[]> => {
    if (!force && districtCache.has(provinceId)) {
        return districtCache.get(provinceId) as DistrictOption[]
    }
    const res = await api.get<DistrictOption[]>(`/api/v1/shipping/ghn/provinces/${provinceId}/districts`)
    districtCache.set(provinceId, res.data)
    return res.data
}

export const getWards = async (districtId: number, force = false): Promise<WardOption[]> => {
    if (!force && wardCache.has(districtId)) {
        return wardCache.get(districtId) as WardOption[]
    }
    const res = await api.get<WardOption[]>(`/api/v1/shipping/ghn/districts/${districtId}/wards`)
    wardCache.set(districtId, res.data)
    return res.data
}

export const clearLocationCache = () => {
    provinceCache = null
    districtCache.clear()
    wardCache.clear()
}

export default {
    getProvinces,
    getDistricts,
    getWards,
    clearLocationCache
}
