import { useState, useEffect } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import * as AuthApi from '../api/auth'
import { Mail, AlertCircle, CheckCircle } from 'lucide-react'
import { extractProblemMessage } from '../lib/problemDetails'

export default function ResendVerification() {
    const navigate = useNavigate()
    const location = useLocation()
    const email = location.state?.email || ''
    const [status, setStatus] = useState<'idle' | 'loading' | 'success' | 'error'>('idle')
    const [message, setMessage] = useState('')
    const [countdown, setCountdown] = useState(0)

    useEffect(() => {
        // If no email from registration, redirect to login
        if (!email) {
            navigate('/login', { replace: true })
            return
        }
    }, [email, navigate])

    useEffect(() => {
        if (countdown > 0) {
            const timer = setTimeout(() => setCountdown(countdown - 1), 1000)
            return () => clearTimeout(timer)
        }
    }, [countdown])

    const handleResend = async () => {
        if (!email) {
            setStatus('error')
            setMessage('Không tìm thấy thông tin email')
            return
        }

        setStatus('loading')
        try {
            await AuthApi.resendVerification({ email })
            setStatus('success')
            setMessage('Email xác thực đã được gửi! Vui lòng kiểm tra hộp thư của bạn.')
            setCountdown(60) // Bắt đầu đếm ngược 60 giây
        } catch (error) {
            setStatus('error')
            const responseData = error && typeof error === 'object' && 'response' in error
                ? (error as { response?: { data?: unknown } }).response?.data
                : undefined
            setMessage(extractProblemMessage(responseData, 'Không thể gửi email. Vui lòng thử lại.'))
        }
    }

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
            <div className="max-w-md w-full bg-white rounded-lg shadow-lg p-8">
                <div className="text-center mb-6">
                    <Mail className="w-16 h-16 mx-auto text-blue-600 mb-4" />
                    <h2 className="text-2xl font-bold text-gray-900 mb-2">Xác thực email của bạn</h2>
                    <p className="text-gray-600 mb-4">
                        Chúng tôi đã gửi email xác thực đến địa chỉ của bạn.
                    </p>
                    {email && (
                        <p className="text-sm text-gray-500">
                            Email: <span className="font-medium">{email}</span>
                        </p>
                    )}
                </div>

                {status === 'success' && (
                    <div className="flex items-start gap-2 p-3 bg-green-50 border border-green-200 rounded-lg mb-4">
                        <CheckCircle className="w-5 h-5 text-green-600 mt-0.5 flex-shrink-0" />
                        <p className="text-sm text-green-700">{message}</p>
                    </div>
                )}

                {status === 'error' && (
                    <div className="flex items-start gap-2 p-3 bg-red-50 border border-red-200 rounded-lg mb-4">
                        <AlertCircle className="w-5 h-5 text-red-600 mt-0.5 flex-shrink-0" />
                        <p className="text-sm text-red-700">{message}</p>
                    </div>
                )}

                <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6">
                    <p className="text-sm text-blue-800">
                        <strong>Lưu ý:</strong> Vui lòng kiểm tra hộp thư (kể cả thư mục spam) và nhấp vào liên kết để xác thực.
                    </p>
                </div>

                <div className="space-y-3">
                    <button
                        onClick={handleResend}
                        disabled={status === 'loading' || countdown > 0}
                        className="w-full bg-red-600 text-white py-3 rounded-lg hover:bg-red-700 font-semibold disabled:bg-gray-400 disabled:cursor-not-allowed"
                    >
                        {status === 'loading'
                            ? 'Đang gửi...'
                            : countdown > 0
                                ? `Gửi lại sau ${countdown}s`
                                : 'Gửi lại email xác thực'}
                    </button>

                    <button
                        onClick={() => navigate('/login')}
                        className="w-full border border-gray-300 text-gray-700 py-3 rounded-lg hover:bg-gray-50 font-semibold"
                    >
                        Quay lại đăng nhập
                    </button>
                </div>
            </div>
        </div>
    )
}
