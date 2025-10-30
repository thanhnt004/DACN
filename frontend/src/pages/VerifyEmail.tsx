import { useEffect, useState } from 'react'
import { useSearchParams, useNavigate } from 'react-router-dom'
import * as AuthApi from '../api/auth'
import { CheckCircle, XCircle, Loader2 } from 'lucide-react'

export default function VerifyEmail() {
    const [searchParams] = useSearchParams()
    const navigate = useNavigate()
    const [status, setStatus] = useState<'loading' | 'success' | 'error' | 'already_verified'>('loading')
    const [message, setMessage] = useState('')

    useEffect(() => {
        const token = searchParams.get('token')
        if (!token) {
            setStatus('error')
            setMessage('Token xác thực không hợp lệ')
            return
        }

        const verify = async () => {
            try {
                const result = await AuthApi.verifyEmail({ token })
                if (result.status === 'verified') {
                    setStatus('success')
                    setMessage('Xác thực email thành công! Đang chuyển hướng đến trang đăng nhập...')
                    // Tự động chuyển hướng sau 2 giây
                    setTimeout(() => {
                        navigate('/login')
                    }, 2000)
                } else if (result.status === 'already_verified') {
                    setStatus('already_verified')
                    setMessage('Email đã được xác thực trước đó.')
                }
            } catch (error) {
                setStatus('error')
                const err = error as { response?: { data?: { message?: string } } }
                setMessage(err.response?.data?.message || 'Không thể xác thực email. Vui lòng thử lại.')
            }
        }

        verify()
    }, [searchParams, navigate])

    const handleGoToLogin = () => {
        navigate('/login')
    }

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
            <div className="max-w-md w-full bg-white rounded-lg shadow-lg p-8 text-center">
                {status === 'loading' && (
                    <>
                        <Loader2 className="w-16 h-16 mx-auto text-blue-600 animate-spin mb-4" />
                        <h2 className="text-2xl font-bold text-gray-900 mb-2">Đang xác thực email</h2>
                        <p className="text-gray-600">Vui lòng đợi trong giây lát...</p>
                    </>
                )}

                {status === 'success' && (
                    <>
                        <CheckCircle className="w-16 h-16 mx-auto text-green-600 mb-4" />
                        <h2 className="text-2xl font-bold text-gray-900 mb-2">Xác thực thành công!</h2>
                        <p className="text-gray-600 mb-6">{message}</p>
                        <button
                            onClick={handleGoToLogin}
                            className="w-full bg-red-600 text-white py-3 rounded-lg hover:bg-red-700 font-semibold"
                        >
                            Đăng nhập ngay
                        </button>
                    </>
                )}

                {status === 'already_verified' && (
                    <>
                        <CheckCircle className="w-16 h-16 mx-auto text-blue-600 mb-4" />
                        <h2 className="text-2xl font-bold text-gray-900 mb-2">Email đã được xác thực</h2>
                        <p className="text-gray-600 mb-6">{message}</p>
                        <button
                            onClick={handleGoToLogin}
                            className="w-full bg-red-600 text-white py-3 rounded-lg hover:bg-red-700 font-semibold"
                        >
                            Đăng nhập
                        </button>
                    </>
                )}

                {status === 'error' && (
                    <>
                        <XCircle className="w-16 h-16 mx-auto text-red-600 mb-4" />
                        <h2 className="text-2xl font-bold text-gray-900 mb-2">Xác thực thất bại</h2>
                        <p className="text-gray-600 mb-6">{message}</p>
                        <div className="space-y-3">
                            <button
                                onClick={() => navigate('/resend-verification')}
                                className="w-full bg-blue-600 text-white py-3 rounded-lg hover:bg-blue-700 font-semibold"
                            >
                                Gửi lại email xác thực
                            </button>
                            <button
                                onClick={handleGoToLogin}
                                className="w-full border border-gray-300 text-gray-700 py-3 rounded-lg hover:bg-gray-50 font-semibold"
                            >
                                Quay lại đăng nhập
                            </button>
                        </div>
                    </>
                )}
            </div>
        </div>
    )
}
