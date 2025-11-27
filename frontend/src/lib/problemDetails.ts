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
