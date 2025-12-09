import { useState } from 'react'

interface ErrorModalState {
    isOpen: boolean
    message: string
    title?: string
}

interface ConfirmModalState {
    isOpen: boolean
    message: string
    title?: string
    onConfirm: () => void
}

export function useModals() {
    const [errorModal, setErrorModal] = useState<ErrorModalState>({
        isOpen: false,
        message: '',
        title: 'Thất bại'
    })

    const [confirmModal, setConfirmModal] = useState<ConfirmModalState>({
        isOpen: false,
        message: '',
        title: 'Xác nhận',
        onConfirm: () => {}
    })

    const showError = (message: string, title?: string) => {
        setErrorModal({
            isOpen: true,
            message,
            title: title || 'Thất bại'
        })
    }

    const closeError = () => {
        setErrorModal(prev => ({ ...prev, isOpen: false }))
    }

    const showConfirm = (message: string, onConfirm: () => void, title?: string) => {
        setConfirmModal({
            isOpen: true,
            message,
            title: title || 'Xác nhận',
            onConfirm
        })
    }

    const closeConfirm = () => {
        setConfirmModal(prev => ({ ...prev, isOpen: false }))
    }

    return {
        errorModal,
        showError,
        closeError,
        confirmModal,
        showConfirm,
        closeConfirm
    }
}
