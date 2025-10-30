import { useState, type ChangeEvent, type DragEvent } from 'react'
import { uploadImagesToCloudinary } from '../../api/media'

type ImageUploadZoneProps = {
    id: string
    description: string
    onFilesSelected?: (files: File[]) => void | Promise<void>
    onUploadComplete?: (urls: string[]) => void | Promise<void>
    uploadType?: string
    targetId?: string
    className?: string
    buttonLabel?: string
    accept?: string
    multiple?: boolean
    uploadingText?: string
    autoUpload?: boolean
}

export default function ImageUploadZone({
    id,
    description,
    onFilesSelected,
    onUploadComplete,
    uploadType = 'product',
    targetId,
    className,
    buttonLabel = 'Chọn tệp từ máy',
    accept = 'image/*',
    multiple = true,
    uploadingText = 'Đang tải ảnh lên...',
    autoUpload = false
}: ImageUploadZoneProps) {
    const baseClasses = 'rounded border-dashed border-2 border-gray-200 p-4 text-sm text-gray-600'
    const mergedClasses = className ? `${baseClasses} ${className}` : baseClasses

    const [isUploading, setIsUploading] = useState(false)
    const [error, setError] = useState<string | null>(null)

    const handleFiles = async (files: FileList | null) => {
        if (!files || !files.length) return
        const fileArray = Array.from(files)

        setError(null)

        // Nếu autoUpload = true, tự động upload lên Cloudinary
        if (autoUpload) {
            setIsUploading(true)
            try {
                const urls = await uploadImagesToCloudinary(fileArray, uploadType, targetId)
                if (onUploadComplete) {
                    await onUploadComplete(urls)
                }
            } catch (err) {
                console.error(err)
                setError(err instanceof Error ? err.message : 'Tải ảnh lên thất bại')
            } finally {
                setIsUploading(false)
            }
        } else {
            // Chỉ gọi callback với files, để parent tự xử lý
            if (onFilesSelected) {
                await onFilesSelected(fileArray)
            }
        }
    }

    const handleInputChange = async (event: ChangeEvent<HTMLInputElement>) => {
        await handleFiles(event.target.files)
        event.target.value = ''
    }

    const handleDrop = async (event: DragEvent<HTMLDivElement>) => {
        event.preventDefault()
        await handleFiles(event.dataTransfer?.files ?? null)
    }

    const handleDragOver = (event: DragEvent<HTMLDivElement>) => {
        event.preventDefault()
    }

    return (
        <div onDrop={handleDrop} onDragOver={handleDragOver} className={mergedClasses}>
            <p>{description}</p>
            <input
                id={id}
                type="file"
                accept={accept}
                multiple={multiple}
                className="hidden"
                onChange={handleInputChange}
                disabled={isUploading}
            />
            <label
                htmlFor={id}
                className={`mt-3 inline-block cursor-pointer rounded border px-3 py-2 text-xs font-medium text-gray-700 hover:bg-gray-50 ${isUploading ? 'opacity-50 cursor-not-allowed' : ''}`}
            >
                {buttonLabel}
            </label>
            {isUploading && <p className="mt-2 text-xs text-gray-500">{uploadingText}</p>}
            {error && <p className="mt-2 text-xs text-red-600">{error}</p>}
        </div>
    )
}
