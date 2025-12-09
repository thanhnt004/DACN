import { Check } from 'lucide-react'

interface ProductStepperProps {
    currentStep: number
    completedSteps: number[]
}

const steps = [
    { id: 1, name: 'Thông tin chung', description: 'Đang tạo...' },
    { id: 2, name: 'Phân loại', description: 'Danh mục' },
    { id: 3, name: 'Biến thể & Kho', description: 'Variants' },
    { id: 4, name: 'Hình ảnh', description: 'Gallery' },
    { id: 5, name: 'SEO & Hoàn tất', description: 'Publish' },
]

export default function ProductStepper({ currentStep, completedSteps }: ProductStepperProps) {
    return (
        <div className="bg-white shadow rounded-lg p-6 mb-6">
            <nav aria-label="Progress">
                <ol className="flex items-center justify-between">
                    {steps.map((step, stepIdx) => {
                        const isCompleted = completedSteps.includes(step.id)
                        const isCurrent = currentStep === step.id
                        const isPast = step.id < currentStep

                        return (
                            <li key={step.id} className={`relative ${stepIdx !== steps.length - 1 ? 'flex-1' : ''}`}>
                                <div className="flex items-center">
                                    {/* Step Circle */}
                                    <div className="relative flex items-center justify-center">
                                        <span
                                            className={`
                                                w-12 h-12 flex items-center justify-center rounded-full border-2 
                                                transition-all duration-200
                                                ${isCurrent
                                                    ? 'border-red-600 bg-red-600 text-white shadow-lg'
                                                    : isCompleted || isPast
                                                    ? 'border-green-600 bg-green-600 text-white'
                                                    : 'border-gray-300 bg-white text-gray-500'
                                                }
                                            `}
                                        >
                                            {isCompleted || isPast ? (
                                                <Check className="w-6 h-6" />
                                            ) : (
                                                <span className="text-lg font-semibold">{step.id}</span>
                                            )}
                                        </span>
                                    </div>

                                    {/* Step Label */}
                                    <div className="ml-4 min-w-0">
                                        <p
                                            className={`
                                                text-sm font-medium
                                                ${isCurrent ? 'text-red-600' : isCompleted || isPast ? 'text-green-600' : 'text-gray-500'}
                                            `}
                                        >
                                            {step.name}
                                        </p>
                                        <p className="text-xs text-gray-500">{step.description}</p>
                                    </div>

                                    {/* Connector Line */}
                                    {stepIdx !== steps.length - 1 && (
                                        <div
                                            className={`
                                                hidden lg:block absolute top-6 left-20 w-full h-0.5
                                                ${isPast || isCompleted ? 'bg-green-600' : 'bg-gray-300'}
                                            `}
                                            style={{ width: 'calc(100% - 5rem)' }}
                                        />
                                    )}
                                </div>
                            </li>
                        )
                    })}
                </ol>
            </nav>
        </div>
    )
}
