export interface ProblemDetailsLike {
    title?: string
    status?: number
    detail?: string
    code?: string
    errors?: Record<string, unknown> | undefined
    message?: string
    error?: string
}

export const extractProblemMessage = (
    source: unknown,
    fallback: string
): string => {
    if (!source || typeof source !== 'object') {
        return fallback
    }

    const data = source as ProblemDetailsLike

    if (typeof data.detail === 'string' && data.detail.trim().length > 0) {
        return data.detail.trim()
    }

    if (data.errors && typeof data.errors === 'object') {
        const values = Object.values(data.errors)
            .flatMap(value => {
                if (Array.isArray(value)) {
                    return value
                }
                if (typeof value === 'string') {
                    return [value]
                }
                return []
            })
            .filter((value): value is string => typeof value === 'string' && value.trim().length > 0)

        if (values.length > 0) {
            return values.join(', ')
        }
    }

    if (typeof data.message === 'string' && data.message.trim().length > 0) {
        return data.message.trim()
    }

    if (typeof data.error === 'string' && data.error.trim().length > 0) {
        return data.error.trim()
    }

    if (typeof data.title === 'string' && data.title.trim().length > 0) {
        return data.title.trim()
    }

    return fallback
}

export const extractProblemCode = (source: unknown): string | undefined => {
    if (!source || typeof source !== 'object') {
        return undefined
    }
    const data = source as ProblemDetailsLike
    return typeof data.code === 'string' && data.code.trim().length > 0 ? data.code : undefined
}

/**
 * Xử lý lỗi từ API response và trả về message phù hợp
 * - Nếu lỗi 500: "Vui lòng thử lại sau"
 * - Nếu có detail/message: hiển thị chi tiết
 * - Ngược lại: fallback message
 */
export const resolveErrorMessage = (error: unknown, fallback: string): string => {
    // Kiểm tra error object có response không
    const response = error && typeof error === 'object' && 'response' in error
        ? (error as { response?: { status?: number; data?: unknown } }).response
        : undefined

    // Nếu lỗi 500, luôn trả về message chung
    if (response?.status === 500) {
        return 'Vui lòng thử lại sau'
    }

    // Lấy data từ response
    const responseData = response?.data

    // Nếu có message/detail trong response data, ưu tiên hiển thị
    const extractedMessage = extractProblemMessage(responseData, '')
    if (extractedMessage) {
        return extractedMessage
    }

    // Nếu error có message property (Error object)
    if (error instanceof Error && error.message) {
        return error.message
    }

    // Fallback
    return fallback
}
