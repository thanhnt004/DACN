import api from './http'

export interface ImageUploadSignRequest {
    type: string
    targetId?: string
}

export interface ImageUploadSignResponse {
    cloudName: string
    apiKey: string
    uploadUrl: string
    timestamp: number
    signature: string
    folder: string
    maxSize: number
    allowedFormat?: string[]
    transformations?: string
}

export const signImageUpload = async (payload: ImageUploadSignRequest) => {
    const res = await api.post<ImageUploadSignResponse>('/api/v1/cloudinary/sign', payload)
    return res.data
}

/**
 * Upload nhiều file ảnh lên Cloudinary
 * @param files - Danh sách file cần upload
 * @param type - Loại upload (product, user, brand, category, etc.)
 * @param targetId - ID của đối tượng liên quan (optional)
 * @returns Danh sách URL của ảnh đã upload
 */
export const uploadImagesToCloudinary = async (
    files: File[],
    type: string,
    targetId?: string
): Promise<string[]> => {
    const imageFiles = files.filter((file) => file.type.startsWith('image/'))
    if (!imageFiles.length) return []

    const payload = targetId ? { type, targetId } : { type }

    // Lấy signature từ backend — bao bọc try để bắt 401/403 dễ nhìn
    let signResp
    try {
        signResp = await signImageUpload(payload)
    } catch (err: unknown) {
        // cụ thể xử lý lỗi 401
        if (err instanceof Error && /401|Unauthorized/i.test(err.message)) {
            throw new Error('Không lấy được signature từ server (401). Kiểm tra Authorization header / token.')
        }
        throw err instanceof Error ? err : new Error('Lỗi khi gọi sign endpoint')
    }

    const allowed = signResp.allowedFormat?.map((fmt) => fmt.toLowerCase())
    const maxSize = signResp.maxSize ?? Number.POSITIVE_INFINITY

    // Debug: in thông tin sign để so sánh với "String to sign" nếu có lỗi signature
    console.log('Cloudinary sign response:', {
        apiKey: signResp.apiKey,
        timestamp: signResp.timestamp,
        signature: signResp.signature,
        folder: signResp.folder,
        transformations: signResp.transformations,
        uploadUrl: signResp.uploadUrl,
    })

    const uploadedUrls: string[] = []

    for (const file of imageFiles) {
        if (!file) continue
        if (file.size > maxSize) {
            throw new Error(`File "${file.name}" vượt quá kích thước cho phép (${file.size} > ${maxSize})`)
        }

        const ext = file.name.split('.').pop()?.toLowerCase()
        if (allowed && ext && !allowed.includes(ext)) {
            throw new Error(`Định dạng ${ext} không được phép. Chỉ chấp nhận: ${allowed.join(', ')}`)
        }

        const form = new FormData()
        form.append('file', file)
        form.append('api_key', signResp.apiKey)
        form.append('timestamp', String(signResp.timestamp))
        form.append('signature', signResp.signature)
        // folder có trong string-to-sign theo log lỗi -> phải gửi chính xác
        if (signResp.folder) form.append('folder', signResp.folder)

        // lưu ý: Cloudinary string-to-sign báo 'transformation' (singular)
        if (signResp.transformations) {
            form.append('transformation', signResp.transformations)
        }

        // DEBUG: log tất cả entries của FormData trước khi gửi để so sánh
        // (trong browser bạn có thể xem network > Request payload cũng sẽ hiển thị)
        try {
            // eslint-disable-next-line @typescript-eslint/no-unused-vars
            for (const entry of (form as any).entries()) {
                // entry[0] là key, entry[1] có thể là File hoặc string
                // chỉ log các key/values không nhạy cảm
                // (File sẽ log như File {name:..., size:...})
                // Không in toàn bộ file content
                // => Dùng console.log để xem
                // eslint-disable-next-line no-console
                console.log('form entry', entry[0], entry[1] instanceof File ? `${(entry[1] as File).name}` : entry[1])
            }
        } catch (consoleErr) {
            // nếu browser không cho in entries, bỏ qua
            // eslint-disable-next-line no-console
            console.log('Could not enumerate FormData entries for debug', consoleErr)
        }

        const res = await fetch(signResp.uploadUrl, { method: 'POST', body: form })

        // đọc text để debug lỗi (Cloudinary có thể trả html/text hoặc json)
        const text = await res.text().catch(() => '')
        let body: unknown = null
        try {
            body = text ? JSON.parse(text) : null
        } catch {
            body = text
        }

        if (!res.ok) {
            // nếu backend hoặc cloudinary trả lỗi invalid signature sẽ hiển thị trong text/json
            const cloudMsg =
                typeof body === 'string'
                    ? body
                    : body && typeof body === 'object' && 'error' in (body as any)
                        ? (body as any).error.message
                        : JSON.stringify(body).slice(0, 1000)
            throw new Error(`Upload failed (status ${res.status}). Cloudinary: ${cloudMsg}`)
        }

        // success
        if (body && typeof body === 'object' && 'secure_url' in (body as any)) {
            uploadedUrls.push((body as any).secure_url as string)
        } else if (body && typeof body === 'object' && 'url' in (body as any)) {
            uploadedUrls.push((body as any).url as string)
        } else {
            // unexpected shape
            // eslint-disable-next-line no-console
            console.warn('Upload success but response did not contain secure_url. Response:', body)
        }
    }

    return uploadedUrls
}

export default {
    signImageUpload,
    uploadImagesToCloudinary,
}
