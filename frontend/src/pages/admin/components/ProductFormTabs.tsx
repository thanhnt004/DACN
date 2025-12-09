import { Package, Box } from 'lucide-react'

interface ProductFormTabsProps {
    activeTab: 'basic' | 'variants'
    onTabChange: (tab: 'basic' | 'variants') => void
    variantCount: number
    disabled?: boolean
}

export default function ProductFormTabs({ 
    activeTab, 
    onTabChange, 
    variantCount,
    disabled = false 
}: ProductFormTabsProps) {
    const tabs = [
        { 
            id: 'basic' as const, 
            label: 'Thông tin sản phẩm', 
            icon: Package,
            count: 0
        },
        { 
            id: 'variants' as const, 
            label: 'Biến thể', 
            icon: Box,
            count: variantCount
        },
    ]

    return (
        <div className="border-b border-gray-200 bg-white">
            <nav className="-mb-px flex space-x-8 px-6" aria-label="Tabs">
                {tabs.map((tab) => {
                    const Icon = tab.icon
                    const isActive = activeTab === tab.id
                    const isDisabled = disabled && tab.id !== 'basic'
                    
                    return (
                        <button
                            key={tab.id}
                            onClick={() => !isDisabled && onTabChange(tab.id)}
                            disabled={isDisabled}
                            className={`
                                group inline-flex items-center gap-2 py-4 px-1 border-b-2 font-medium text-sm
                                transition-colors duration-150
                                ${isActive
                                    ? 'border-red-600 text-red-600'
                                    : isDisabled
                                    ? 'border-transparent text-gray-400 cursor-not-allowed'
                                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                                }
                            `}
                        >
                            <Icon className="h-5 w-5" />
                            <span>{tab.label}</span>
                            {tab.count > 0 && (
                                <span className={`
                                    inline-flex items-center justify-center px-2 py-0.5 rounded-full text-xs font-medium
                                    ${isActive
                                        ? 'bg-red-100 text-red-600'
                                        : 'bg-gray-100 text-gray-600'
                                    }
                                `}>
                                    {tab.count}
                                </span>
                            )}
                        </button>
                    )
                })}
            </nav>
        </div>
    )
}
