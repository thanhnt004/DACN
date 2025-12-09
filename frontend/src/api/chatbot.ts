import api from './http'

export interface ChatRequest {
    message: string
}

export interface ChatResponse {
    response: string
    status: 'success' | 'error'
}

export const sendMessage = async (message: string): Promise<ChatResponse> => {
    const response = await api.post<ChatResponse>('/api/v1/chatbot/chat', { message })
    return response.data
}

export const checkHealth = async (): Promise<{ status: string }> => {
    const response = await api.get<{ status: string }>('/api/v1/chatbot/health')
    return response.data
}
