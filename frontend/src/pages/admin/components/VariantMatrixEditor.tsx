import { useState } from 'react'
import { toast } from 'react-toastify'
import { Check, Edit2, Trash2 } from 'lucide-react'
import * as CatalogApi from '../../../api/admin/catalog'

interface VariantMatrixRow {
    id: string
    sizeId?: string
    sizeName?: string
    colorId?: string
    colorName?: string
    colorHex?: string
    sku: string
    priceAmount: string
    historyCost: string
    compareAtAmount: string
    quantityOnHand: string
    weightGrams: string
    reorderLevel: string
}

interface VariantMatrixEditorProps {
    sizes: CatalogApi.SizeDto[]
    colors: CatalogApi.ColorDto[]
    variants: VariantMatrixRow[]
    onVariantsChange: (variants: VariantMatrixRow[]) => void
    productName: string
}

export default function VariantMatrixEditor({
    sizes,
    colors,
    variants,
    onVariantsChange,
    productName,
}: VariantMatrixEditorProps) {
    const [selectedSizes, setSelectedSizes] = useState<string[]>([])
    const [selectedColors, setSelectedColors] = useState<string[]>([])
    const [bulkPrice, setBulkPrice] = useState('')
    const [bulkHistoryCost, setBulkHistoryCost] = useState('')
    const [bulkQuantity, setBulkQuantity] = useState('')
    const [bulkWeight, setBulkWeight] = useState('200')

    const generateVariants = () => {
        if (selectedSizes.length === 0 && selectedColors.length === 0) {
            toast.warning('Vui lòng chọn ít nhất một size hoặc màu sắc')
            return
        }

        const newVariants: VariantMatrixRow[] = []
        
        if (selectedSizes.length > 0 && selectedColors.length > 0) {
            // Both size and color selected
            selectedSizes.forEach(sizeId => {
                selectedColors.forEach(colorId => {
                    const size = sizes.find(s => s.id === sizeId)
                    const color = colors.find(c => c.id === colorId)
                    newVariants.push({
                        id: `temp-${Date.now()}-${sizeId}-${colorId}`,
                        sizeId,
                        sizeName: size?.name,
                        colorId,
                        colorName: color?.name,
                        colorHex: color?.hexCode,
                        sku: `SKU-${size?.code || ''}-${color?.name || ''}`.toUpperCase(),
                        priceAmount: bulkPrice || '',
                        historyCost: bulkHistoryCost || '',
                        compareAtAmount: '',
                        quantityOnHand: bulkQuantity || '0',
                        weightGrams: bulkWeight || '200',
                        reorderLevel: '5',
                    })
                })
            })
        } else if (selectedSizes.length > 0) {
            // Only size selected
            selectedSizes.forEach(sizeId => {
                const size = sizes.find(s => s.id === sizeId)
                newVariants.push({
                    id: `temp-${Date.now()}-${sizeId}`,
                    sizeId,
                    sizeName: size?.name,
                    sku: `SKU-${size?.code || ''}`.toUpperCase(),
                    priceAmount: bulkPrice || '',
                    historyCost: bulkHistoryCost || '',
                    compareAtAmount: '',
                    quantityOnHand: bulkQuantity || '0',
                    weightGrams: bulkWeight || '200',
                    reorderLevel: '5',
                })
            })
        } else {
            // Only color selected
            selectedColors.forEach(colorId => {
                const color = colors.find(c => c.id === colorId)
                newVariants.push({
                    id: `temp-${Date.now()}-${colorId}`,
                    colorId,
                    colorName: color?.name,
                    colorHex: color?.hexCode,
                    sku: `SKU-${color?.name || ''}`.toUpperCase(),
                    priceAmount: bulkPrice || '',
                    historyCost: bulkHistoryCost || '',
                    compareAtAmount: '',
                    quantityOnHand: bulkQuantity || '0',
                    weightGrams: bulkWeight || '200',
                    reorderLevel: '5',
                })
            })
        }

        onVariantsChange(newVariants)
    }

    const applyBulkPrice = () => {
        if (!bulkPrice) return
        const updated = variants.map(v => ({ ...v, priceAmount: bulkPrice }))
        onVariantsChange(updated)
    }

    const applyBulkCost = () => {
        if (!bulkHistoryCost) return
        const updated = variants.map(v => ({ ...v, historyCost: bulkHistoryCost }))
        onVariantsChange(updated)
    }

    const applyBulkQuantity = () => {
        if (!bulkQuantity) return
        const updated = variants.map(v => ({ ...v, quantityOnHand: bulkQuantity }))
        onVariantsChange(updated)
    }

    const applyBulkWeight = () => {
        if (!bulkWeight) return
        const updated = variants.map(v => ({ ...v, weightGrams: bulkWeight }))
        onVariantsChange(updated)
    }

    const updateVariant = (id: string, field: keyof VariantMatrixRow, value: string) => {
        const updated = variants.map(v => (v.id === id ? { ...v, [field]: value } : v))
        onVariantsChange(updated)
    }

    const deleteVariant = (id: string) => {
        onVariantsChange(variants.filter(v => v.id !== id))
    }

    return (
        <div className="space-y-6">
            {/* Part A: Attribute Selection */}
            {variants.length === 0 && (
                <div className="bg-blue-50 border border-blue-200 rounded-lg p-6 space-y-6">
                    <h3 className="font-semibold text-lg">Bước 1: Chọn thuộc tính để tạo tổ hợp</h3>

                    {/* Size Selection */}
                    <div>
                        <label className="block text-sm font-medium mb-3">Chọn kích thước (Size)</label>
                        <div className="flex flex-wrap gap-2">
                            {sizes.map(size => (
                                <label
                                    key={size.id}
                                    className={`
                                        flex items-center gap-2 px-4 py-2 rounded-lg border-2 cursor-pointer transition-all
                                        ${selectedSizes.includes(size.id!)
                                            ? 'border-red-600 bg-red-50 text-red-700'
                                            : 'border-gray-300 bg-white hover:border-gray-400'
                                        }
                                    `}
                                >
                                    <input
                                        type="checkbox"
                                        checked={selectedSizes.includes(size.id!)}
                                        onChange={(e) => {
                                            if (e.target.checked) {
                                                setSelectedSizes([...selectedSizes, size.id!])
                                            } else {
                                                setSelectedSizes(selectedSizes.filter(id => id !== size.id))
                                            }
                                        }}
                                        className="w-4 h-4"
                                    />
                                    <span className="font-medium">{size.name}</span>
                                    <span className="text-xs text-gray-500">({size.code})</span>
                                </label>
                            ))}
                        </div>
                    </div>

                    {/* Color Selection */}
                    <div>
                        <label className="block text-sm font-medium mb-3">Chọn màu sắc (Color)</label>
                        <div className="flex flex-wrap gap-2">
                            {colors.map(color => (
                                <label
                                    key={color.id}
                                    className={`
                                        flex items-center gap-3 px-4 py-2 rounded-lg border-2 cursor-pointer transition-all
                                        ${selectedColors.includes(color.id!)
                                            ? 'border-red-600 bg-red-50'
                                            : 'border-gray-300 bg-white hover:border-gray-400'
                                        }
                                    `}
                                >
                                    <input
                                        type="checkbox"
                                        checked={selectedColors.includes(color.id!)}
                                        onChange={(e) => {
                                            if (e.target.checked) {
                                                setSelectedColors([...selectedColors, color.id!])
                                            } else {
                                                setSelectedColors(selectedColors.filter(id => id !== color.id))
                                            }
                                        }}
                                        className="w-4 h-4"
                                    />
                                    <div
                                        className="w-6 h-6 rounded border-2 border-gray-300"
                                        style={{ backgroundColor: color.hexCode }}
                                    />
                                    <span className="font-medium">{color.name}</span>
                                </label>
                            ))}
                        </div>
                    </div>

                    {/* Default Values */}
                    <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 pt-4 border-t">
                        <div>
                            <label className="block text-xs font-medium text-gray-700 mb-1">Giá bán mặc định (VNĐ)</label>
                            <input
                                type="number"
                                value={bulkPrice}
                                onChange={(e) => setBulkPrice(e.target.value)}
                                className="w-full border rounded px-3 py-2 text-sm"
                                placeholder="300000"
                            />
                        </div>
                        <div>
                            <label className="block text-xs font-medium text-gray-700 mb-1">Giá nhập mặc định (VNĐ)</label>
                            <input
                                type="number"
                                value={bulkHistoryCost}
                                onChange={(e) => setBulkHistoryCost(e.target.value)}
                                className="w-full border rounded px-3 py-2 text-sm"
                                placeholder="200000"
                            />
                        </div>
                        <div>
                            <label className="block text-xs font-medium text-gray-700 mb-1">Số lượng tồn kho</label>
                            <input
                                type="number"
                                value={bulkQuantity}
                                onChange={(e) => setBulkQuantity(e.target.value)}
                                className="w-full border rounded px-3 py-2 text-sm"
                                placeholder="100"
                            />
                        </div>
                        <div>
                            <label className="block text-xs font-medium text-gray-700 mb-1">Khối lượng (gram)</label>
                            <input
                                type="number"
                                value={bulkWeight}
                                onChange={(e) => setBulkWeight(e.target.value)}
                                className="w-full border rounded px-3 py-2 text-sm"
                                placeholder="200"
                            />
                        </div>
                    </div>

                    {/* Generate Button */}
                    <button
                        type="button"
                        onClick={generateVariants}
                        disabled={selectedSizes.length === 0 && selectedColors.length === 0}
                        className="w-full bg-red-600 text-white px-6 py-3 rounded-lg font-medium hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        Tạo danh sách biến thể
                    </button>
                </div>
            )}

            {/* Part B: Variants Table */}
            {variants.length > 0 && (
                <div className="space-y-4">
                    {/* Bulk Actions Toolbar */}
                    <div className="bg-gray-50 border border-gray-200 rounded-lg p-4">
                        <h4 className="font-medium mb-3">Công cụ áp dụng hàng loạt</h4>
                        <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
                            <div className="flex gap-2">
                                <input
                                    type="number"
                                    value={bulkPrice}
                                    onChange={(e) => setBulkPrice(e.target.value)}
                                    className="flex-1 border rounded px-2 py-1 text-sm"
                                    placeholder="Giá bán"
                                />
                                <button
                                    onClick={applyBulkPrice}
                                    className="px-3 py-1 bg-blue-600 text-white rounded text-sm hover:bg-blue-700"
                                >
                                    Áp dụng
                                </button>
                            </div>
                            <div className="flex gap-2">
                                <input
                                    type="number"
                                    value={bulkHistoryCost}
                                    onChange={(e) => setBulkHistoryCost(e.target.value)}
                                    className="flex-1 border rounded px-2 py-1 text-sm"
                                    placeholder="Giá nhập"
                                />
                                <button
                                    onClick={applyBulkCost}
                                    className="px-3 py-1 bg-blue-600 text-white rounded text-sm hover:bg-blue-700"
                                >
                                    Áp dụng
                                </button>
                            </div>
                            <div className="flex gap-2">
                                <input
                                    type="number"
                                    value={bulkQuantity}
                                    onChange={(e) => setBulkQuantity(e.target.value)}
                                    className="flex-1 border rounded px-2 py-1 text-sm"
                                    placeholder="Tồn kho"
                                />
                                <button
                                    onClick={applyBulkQuantity}
                                    className="px-3 py-1 bg-blue-600 text-white rounded text-sm hover:bg-blue-700"
                                >
                                    Áp dụng
                                </button>
                            </div>
                            <div className="flex gap-2">
                                <input
                                    type="number"
                                    value={bulkWeight}
                                    onChange={(e) => setBulkWeight(e.target.value)}
                                    className="flex-1 border rounded px-2 py-1 text-sm"
                                    placeholder="Khối lượng"
                                />
                                <button
                                    onClick={applyBulkWeight}
                                    className="px-3 py-1 bg-blue-600 text-white rounded text-sm hover:bg-blue-700"
                                >
                                    Áp dụng
                                </button>
                            </div>
                        </div>
                    </div>

                    {/* Variants Table */}
                    <div className="overflow-x-auto border rounded-lg">
                        <table className="min-w-full divide-y divide-gray-200">
                            <thead className="bg-gray-50">
                                <tr>
                                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Tên biến thể</th>
                                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">SKU</th>
                                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Giá bán</th>
                                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Giá nhập</th>
                                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Tồn kho</th>
                                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Trọng lượng (g)</th>
                                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Hành động</th>
                                </tr>
                            </thead>
                            <tbody className="bg-white divide-y divide-gray-200">
                                {variants.map((variant) => (
                                    <tr key={variant.id} className="hover:bg-gray-50">
                                        <td className="px-4 py-3">
                                            <div className="flex items-center gap-2">
                                                {variant.colorHex && (
                                                    <div
                                                        className="w-5 h-5 rounded border"
                                                        style={{ backgroundColor: variant.colorHex }}
                                                    />
                                                )}
                                                <span className="text-sm font-medium">
                                                    {productName} {variant.colorName ? `- ${variant.colorName}` : ''} {variant.sizeName ? `- ${variant.sizeName}` : ''}
                                                </span>
                                            </div>
                                        </td>
                                        <td className="px-4 py-3">
                                            <input
                                                type="text"
                                                value={variant.sku}
                                                onChange={(e) => updateVariant(variant.id, 'sku', e.target.value)}
                                                className="w-full border rounded px-2 py-1 text-sm"
                                            />
                                        </td>
                                        <td className="px-4 py-3">
                                            <input
                                                type="number"
                                                value={variant.priceAmount}
                                                onChange={(e) => updateVariant(variant.id, 'priceAmount', e.target.value)}
                                                className="w-24 border rounded px-2 py-1 text-sm"
                                                placeholder="0"
                                            />
                                        </td>
                                        <td className="px-4 py-3">
                                            <input
                                                type="number"
                                                value={variant.historyCost}
                                                onChange={(e) => updateVariant(variant.id, 'historyCost', e.target.value)}
                                                className="w-24 border rounded px-2 py-1 text-sm"
                                                placeholder="0"
                                            />
                                        </td>
                                        <td className="px-4 py-3">
                                            <input
                                                type="number"
                                                value={variant.quantityOnHand}
                                                onChange={(e) => updateVariant(variant.id, 'quantityOnHand', e.target.value)}
                                                className="w-20 border rounded px-2 py-1 text-sm"
                                                placeholder="0"
                                            />
                                        </td>
                                        <td className="px-4 py-3">
                                            <input
                                                type="number"
                                                value={variant.weightGrams}
                                                onChange={(e) => updateVariant(variant.id, 'weightGrams', e.target.value)}
                                                className="w-20 border rounded px-2 py-1 text-sm"
                                                placeholder="200"
                                            />
                                        </td>
                                        <td className="px-4 py-3">
                                            <button
                                                onClick={() => deleteVariant(variant.id)}
                                                className="text-red-600 hover:text-red-800"
                                                title="Xóa biến thể"
                                            >
                                                <Trash2 className="w-4 h-4" />
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>

                    {/* Reset Button */}
                    <button
                        type="button"
                        onClick={() => {
                            if (confirm('Bạn có chắc muốn xóa tất cả biến thể và tạo lại?')) {
                                onVariantsChange([])
                                setSelectedSizes([])
                                setSelectedColors([])
                            }
                        }}
                        className="text-sm text-red-600 hover:text-red-800 underline"
                    >
                        Xóa tất cả và tạo lại
                    </button>
                </div>
            )}
        </div>
    )
}
