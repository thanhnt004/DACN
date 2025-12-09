import { X } from 'lucide-react';
import { ReactNode } from 'react';

interface ModalProps {
    isOpen: boolean;
    onClose: () => void;
    title?: string;
    children: ReactNode;
    maxWidth?: 'sm' | 'md' | 'lg' | 'xl' | '2xl' | '3xl' | '4xl' | '5xl' | '6xl' | '7xl';
    showCloseButton?: boolean;
    closeOnBackdropClick?: boolean;
}

export default function Modal({
    isOpen,
    onClose,
    title,
    children,
    maxWidth = '2xl',
    showCloseButton = true,
    closeOnBackdropClick = true
}: ModalProps) {
    if (!isOpen) return null;

    const maxWidthClass = {
        'sm': 'max-w-sm',
        'md': 'max-w-md',
        'lg': 'max-w-lg',
        'xl': 'max-w-xl',
        '2xl': 'max-w-2xl',
        '3xl': 'max-w-3xl',
        '4xl': 'max-w-4xl',
        '5xl': 'max-w-5xl',
        '6xl': 'max-w-6xl',
        '7xl': 'max-w-7xl'
    }[maxWidth];

    const handleBackdropClick = () => {
        if (closeOnBackdropClick) {
            onClose();
        }
    };

    return (
        <div 
            className="fixed inset-0 bg-transparent flex items-center justify-center z-50 p-4"
            onClick={handleBackdropClick}
        >
            <div 
                className={`bg-white rounded-lg shadow-xl ${maxWidthClass} w-full max-h-[90vh] overflow-y-auto`}
                onClick={(e) => e.stopPropagation()}
            >
                {/* Header */}
                {(title || showCloseButton) && (
                    <div className="sticky top-0 bg-white border-b px-6 py-4 flex justify-between items-center z-10">
                        {title && <h3 className="text-lg font-bold text-gray-900">{title}</h3>}
                        {!title && <div />}
                        {showCloseButton && (
                            <button
                                onClick={onClose}
                                className="text-gray-400 hover:text-gray-600 transition-colors"
                                aria-label="Đóng"
                            >
                                <X className="w-6 h-6" />
                            </button>
                        )}
                    </div>
                )}

                {/* Content */}
                <div className={title || showCloseButton ? 'p-6' : ''}>
                    {children}
                </div>
            </div>
        </div>
    );
}
