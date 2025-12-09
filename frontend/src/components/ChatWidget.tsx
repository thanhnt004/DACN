import { useState, useRef, useEffect } from 'react'
import { MessageCircle, X, Send, Bot, Loader2 } from 'lucide-react'
import * as ChatbotApi from '../api/chatbot'

interface Message {
    id: string
    text: string
    sender: 'user' | 'bot'
    timestamp: Date
}

export default function ChatWidget() {
    const [isOpen, setIsOpen] = useState(false)
    const [showOptions, setShowOptions] = useState(true)
    const [messages, setMessages] = useState<Message[]>([
        {
            id: '1',
            text: 'Xin chào! Tôi là trợ lý AI của WearWave. Tôi có thể giúp bạn tìm kiếm sản phẩm, tư vấn thời trang và trả lời các câu hỏi về cửa hàng. Bạn cần hỗ trợ gì không?',
            sender: 'bot',
            timestamp: new Date()
        }
    ])
    const [inputMessage, setInputMessage] = useState('')
    const [isSending, setIsSending] = useState(false)
    const messagesEndRef = useRef<HTMLDivElement>(null)

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
    }

    useEffect(() => {
        scrollToBottom()
    }, [messages])

    const handleOpenChat = () => {
        setShowOptions(false)
        setIsOpen(true)
    }

    const handleClose = () => {
        setIsOpen(false)
        setShowOptions(true)
    }

    const handleZaloClick = () => {
        window.open('https://zalo.me/0326725877', '_blank')
    }

    const handleSendMessage = async () => {
        if (!inputMessage.trim() || isSending) return

        const userMessage: Message = {
            id: Date.now().toString(),
            text: inputMessage.trim(),
            sender: 'user',
            timestamp: new Date()
        }

        setMessages(prev => [...prev, userMessage])
        setInputMessage('')
        setIsSending(true)

        try {
            const response = await ChatbotApi.sendMessage(userMessage.text)
            
            const botMessage: Message = {
                id: (Date.now() + 1).toString(),
                text: response.response,
                sender: 'bot',
                timestamp: new Date()
            }

            setMessages(prev => [...prev, botMessage])
        } catch (error) {
            console.error('Chat error:', error)
            const errorMessage: Message = {
                id: (Date.now() + 1).toString(),
                text: 'Xin lỗi, đã có lỗi xảy ra. Vui lòng thử lại sau.',
                sender: 'bot',
                timestamp: new Date()
            }
            setMessages(prev => [...prev, errorMessage])
        } finally {
            setIsSending(false)
        }
    }

    const handleKeyPress = (e: React.KeyboardEvent<HTMLInputElement>) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault()
            handleSendMessage()
        }
    }

    return (
        <>
            {/* Floating Button */}
            {!isOpen && (
                <div className="fixed bottom-6 right-6 z-50">
                    {showOptions ? (
                        <div className="flex flex-col gap-3 items-end">
                            {/* AI Chat Button */}
                            <button
                                onClick={handleOpenChat}
                                className="group flex items-center gap-3 bg-gradient-to-r from-blue-600 to-purple-600 text-white px-5 py-3 rounded-full shadow-lg hover:shadow-xl transform hover:scale-105 transition-all duration-300"
                            >
                                <span className="font-medium whitespace-nowrap">AI Chat Bot</span>
                                <Bot className="w-5 h-5" />
                            </button>

                            {/* Zalo Button */}
                            <button
                                onClick={handleZaloClick}
                                className="group flex items-center gap-3 bg-gradient-to-r from-blue-500 to-blue-600 text-white px-5 py-3 rounded-full shadow-lg hover:shadow-xl transform hover:scale-105 transition-all duration-300"
                            >
                                <span className="font-medium whitespace-nowrap">Liên hệ Zalo</span>
                                <MessageCircle className="w-5 h-5" />
                            </button>
                        </div>
                    ) : (
                        <button
                            onClick={() => setShowOptions(true)}
                            className="bg-gradient-to-r from-blue-600 to-purple-600 text-white p-4 rounded-full shadow-lg hover:shadow-xl transform hover:scale-110 transition-all duration-300"
                        >
                            <MessageCircle className="w-6 h-6" />
                        </button>
                    )}
                </div>
            )}

            {/* Chat Window */}
            {isOpen && (
                <div className="fixed bottom-6 right-6 w-96 h-[600px] bg-white rounded-2xl shadow-2xl z-50 flex flex-col overflow-hidden border border-gray-200">
                    {/* Header */}
                    <div className="bg-gradient-to-r from-blue-600 to-purple-600 text-white p-4 flex items-center justify-between">
                        <div className="flex items-center gap-3">
                            <div className="bg-white/20 p-2 rounded-full">
                                <Bot className="w-5 h-5" />
                            </div>
                            <div>
                                <h3 className="font-semibold">AI Chat Bot</h3>
                                <p className="text-xs text-white/80">Trợ lý ảo của WearWave</p>
                            </div>
                        </div>
                        <button
                            onClick={handleClose}
                            className="text-white hover:bg-white/20 p-2 rounded-full transition-colors"
                        >
                            <X className="w-5 h-5" />
                        </button>
                    </div>

                    {/* Messages */}
                    <div className="flex-1 overflow-y-auto p-4 space-y-4 bg-gray-50">
                        {messages.map((message) => (
                            <div
                                key={message.id}
                                className={`flex ${message.sender === 'user' ? 'justify-end' : 'justify-start'}`}
                            >
                                <div
                                    className={`max-w-[80%] rounded-2xl px-4 py-2 ${
                                        message.sender === 'user'
                                            ? 'bg-gradient-to-r from-blue-600 to-purple-600 text-white'
                                            : 'bg-white text-gray-800 shadow-sm border border-gray-200'
                                    }`}
                                >
                                    <p className="text-sm whitespace-pre-wrap break-words">{message.text}</p>
                                    <p
                                        className={`text-xs mt-1 ${
                                            message.sender === 'user' ? 'text-white/70' : 'text-gray-400'
                                        }`}
                                    >
                                        {message.timestamp.toLocaleTimeString('vi-VN', {
                                            hour: '2-digit',
                                            minute: '2-digit'
                                        })}
                                    </p>
                                </div>
                            </div>
                        ))}
                        {isSending && (
                            <div className="flex justify-start">
                                <div className="bg-white text-gray-800 shadow-sm border border-gray-200 rounded-2xl px-4 py-3 flex items-center gap-2">
                                    <Loader2 className="w-4 h-4 animate-spin text-blue-600" />
                                    <span className="text-sm text-gray-500">Đang trả lời...</span>
                                </div>
                            </div>
                        )}
                        <div ref={messagesEndRef} />
                    </div>

                    {/* Input */}
                    <div className="p-4 bg-white border-t border-gray-200">
                        <div className="flex gap-2">
                            <input
                                type="text"
                                value={inputMessage}
                                onChange={(e) => setInputMessage(e.target.value)}
                                onKeyPress={handleKeyPress}
                                placeholder="Nhập tin nhắn..."
                                disabled={isSending}
                                className="flex-1 px-4 py-2 border border-gray-300 rounded-full focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:bg-gray-100 disabled:cursor-not-allowed"
                            />
                            <button
                                onClick={handleSendMessage}
                                disabled={!inputMessage.trim() || isSending}
                                className="bg-gradient-to-r from-blue-600 to-purple-600 text-white p-2 rounded-full hover:shadow-lg disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-300 disabled:hover:shadow-none"
                            >
                                <Send className="w-5 h-5" />
                            </button>
                        </div>
                    </div>

                    {/* Quick Actions */}
                    <div className="px-4 pb-4 bg-white">
                        <button
                            onClick={handleZaloClick}
                            className="w-full flex items-center justify-center gap-2 text-blue-600 hover:bg-blue-50 py-2 rounded-lg text-sm font-medium transition-colors"
                        >
                            <MessageCircle className="w-4 h-4" />
                            <span>Liên hệ qua Zalo</span>
                        </button>
                    </div>
                </div>
            )}
        </>
    )
}
