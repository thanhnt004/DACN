import { useCallback, useEffect, useState } from 'react'
import { getDistricts, getProvinces, getWards } from '../api/location'
import type { DistrictOption, ProvinceOption, WardOption } from '../api/location'

type LocationUpdates = Partial<Record<'province' | 'district' | 'ward', string>>

export interface GhnLocationSelector {
    provinceOptions: ProvinceOption[]
    districtOptions: DistrictOption[]
    wardOptions: WardOption[]
    selectedProvinceId: string
    selectedDistrictId: string
    selectedWardCode: string
    handleProvinceChange: (value: string) => Promise<void>
    handleDistrictChange: (value: string) => Promise<void>
    handleWardChange: (value: string) => void
    initializeFromNames: (province?: string, district?: string, ward?: string) => Promise<void>
    resetSelections: () => void
}

export function useGhnLocationSelector(onChange: (updates: LocationUpdates) => void): GhnLocationSelector {
    const [provinceOptions, setProvinceOptions] = useState<ProvinceOption[]>([])
    const [districtOptions, setDistrictOptions] = useState<DistrictOption[]>([])
    const [wardOptions, setWardOptions] = useState<WardOption[]>([])
    const [selectedProvinceId, setSelectedProvinceId] = useState<string>('')
    const [selectedDistrictId, setSelectedDistrictId] = useState<string>('')
    const [selectedWardCode, setSelectedWardCode] = useState<string>('')

    const loadProvinceOptions = useCallback(async () => {
        const data = await getProvinces()
        setProvinceOptions(data)
        return data
    }, [])

    const loadDistrictOptions = useCallback(async (provinceId: number) => {
        if (!provinceId) {
            setDistrictOptions([])
            return []
        }
        const data = await getDistricts(provinceId)
        setDistrictOptions(data)
        return data
    }, [])

    const loadWardOptions = useCallback(async (districtId: number) => {
        if (!districtId) {
            setWardOptions([])
            return []
        }
        const data = await getWards(districtId)
        setWardOptions(data)
        return data
    }, [])

    useEffect(() => {
        loadProvinceOptions().catch(error => {
            console.error('Không thể tải danh sách tỉnh/thành GHN', error)
        })
    }, [loadProvinceOptions])

    const resetSelections = useCallback(() => {
        setSelectedProvinceId('')
        setSelectedDistrictId('')
        setSelectedWardCode('')
        setDistrictOptions([])
        setWardOptions([])
    }, [])

    const handleProvinceChange = useCallback(async (value: string) => {
        setSelectedProvinceId(value)
        setSelectedDistrictId('')
        setSelectedWardCode('')
        setWardOptions([])
        if (!value) {
            setDistrictOptions([])
            onChange({ province: '', district: '', ward: '' })
            return
        }
        const provinceId = Number(value)
        const provinces = provinceOptions.length > 0 ? provinceOptions : await loadProvinceOptions()
        const selected = provinces.find(item => item.id === provinceId)
        onChange({ province: selected?.name ?? '', district: '', ward: '' })
        await loadDistrictOptions(provinceId)
    }, [provinceOptions, loadProvinceOptions, loadDistrictOptions, onChange])

    const handleDistrictChange = useCallback(async (value: string) => {
        setSelectedDistrictId(value)
        setSelectedWardCode('')
        if (!value) {
            setWardOptions([])
            onChange({ district: '', ward: '' })
            return
        }
        const districtId = Number(value)
        let districts = districtOptions
        if (districts.length === 0 && selectedProvinceId) {
            districts = await loadDistrictOptions(Number(selectedProvinceId))
        }
        const selected = districts.find(item => item.id === districtId)
        onChange({ district: selected?.name ?? '', ward: '' })
        await loadWardOptions(districtId)
    }, [districtOptions, selectedProvinceId, loadDistrictOptions, loadWardOptions, onChange])

    const handleWardChange = useCallback((value: string) => {
        setSelectedWardCode(value)
        if (!value) {
            onChange({ ward: '' })
            return
        }
        const selected = wardOptions.find(item => item.code === value)
        onChange({ ward: selected?.name ?? '' })
    }, [wardOptions, onChange])

    const initializeFromNames = useCallback(async (province?: string, district?: string, ward?: string) => {
        if (!province) {
            resetSelections()
            return
        }
        const provinces = provinceOptions.length > 0 ? provinceOptions : await loadProvinceOptions()
        const matchedProvince = provinces.find(item => item.name === province)
        if (!matchedProvince) {
            resetSelections()
            return
        }
        setSelectedProvinceId(String(matchedProvince.id))
        const districts = await loadDistrictOptions(matchedProvince.id)
        if (!district) {
            setSelectedDistrictId('')
            setWardOptions([])
            setSelectedWardCode('')
            return
        }
        const matchedDistrict = districts.find(item => item.name === district)
        if (!matchedDistrict) {
            setSelectedDistrictId('')
            setWardOptions([])
            setSelectedWardCode('')
            return
        }
        setSelectedDistrictId(String(matchedDistrict.id))
        const wards = await loadWardOptions(matchedDistrict.id)
        if (!ward) {
            setSelectedWardCode('')
            return
        }
        const matchedWard = wards.find(item => item.name === ward)
        setSelectedWardCode(matchedWard?.code ?? '')
    }, [provinceOptions, loadProvinceOptions, loadDistrictOptions, loadWardOptions, resetSelections])

    return {
        provinceOptions,
        districtOptions,
        wardOptions,
        selectedProvinceId,
        selectedDistrictId,
        selectedWardCode,
        handleProvinceChange,
        handleDistrictChange,
        handleWardChange,
        initializeFromNames,
        resetSelections
    }
}
