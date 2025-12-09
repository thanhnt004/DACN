import { X } from 'lucide-react'

interface ErrorModalProps {
    isOpen: boolean
    onClose: () => void
    title?: string
    message: string
}

export default function ErrorModal({ isOpen, onClose, title = 'Thất bại', message }: ErrorModalProps) {
    if (!isOpen) return null

    return (
        <div 
            className="fixed inset-0 z-[9999] flex items-center justify-center bg-transparent"
            onClick={onClose}
        >
            <div 
                className="bg-white rounded-lg shadow-2xl max-w-md w-full mx-4 overflow-hidden"
                onClick={(e) => e.stopPropagation()}
            >
                {/* Header */}
                <div className="flex items-center justify-between px-6 py-4 bg-red-600 text-white">
                    <h3 className="text-lg font-semibold">{title}</h3>
                    <button
                        onClick={onClose}
                        className="text-white hover:text-gray-200 transition"
                    >
                        <X className="w-5 h-5" />
                    </button>
                </div>

                {/* Content */}
                <div className="px-6 py-8">
                    <div className="flex flex-col items-center text-center">
                        <div className="w-16 h-16 rounded-full bg-red-100 flex items-center justify-center mb-4">
                            <svg
                                className="w-8 h-8 text-red-600"
                                fill="none"
                                stroke="currentColor"
                                viewBox="0 0 24 24"
                            >
                                <path
                                    strokeLinecap="round"
                                    strokeLinejoin="round"
                                    strokeWidth={2}
                                    d="M6 18L18 6M6 6l12 12"
                                />
                            </svg>
                        </div>
                        <p className="text-gray-800 text-base">{message}</p>
                    </div>
                </div>

                {/* Footer */}
                <div className="px-6 py-4 bg-gray-50 flex justify-center">
                    <button
                        onClick={onClose}
                        className="px-8 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition font-medium"
                    >
                        OK
                    </button>
                </div>
            </div>
        </div>
    )
}
