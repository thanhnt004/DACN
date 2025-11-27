import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import * as AuthApi from '../api/auth'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { SiFacebook, SiGoogle } from 'react-icons/si'
import { extractProblemMessage } from '../lib/problemDetails'

const phoneRegex = /^(\+84|0)[1-9]\d{8,9}$/
const schema = z.object({
    email: z.string().email('Email không hợp lệ').max(100),
    fullName: z.string().min(1, 'Vui lòng nhập họ tên').max(30),
    password: z
        .string()
        .min(8, 'Mật khẩu tối thiểu 8 ký tự')
        .regex(/[a-z]/, 'Phải có chữ thường')
        .regex(/[A-Z]/, 'Phải có chữ hoa')
        .regex(/\d/, 'Phải có số')
        .regex(/[^\da-zA-Z]/, 'Phải có ký tự đặc biệt'),
    phone: z.string().regex(phoneRegex, 'Số điện thoại không hợp lệ'),
    dateOfBirth: z
        .string()
        .optional()
        .refine((v) => !v || /^\d{4}-\d{2}-\d{2}$/.test(v), {
            message: 'Ngày sinh không hợp lệ (yyyy-MM-dd)',
        }),
})

type FormValues = z.infer<typeof schema>

export default function Register() {
    const navigate = useNavigate()
    const {
        register,
        handleSubmit,
        formState: { errors, isSubmitting },
    } = useForm<FormValues>({ resolver: zodResolver(schema) })
    const [serverMsg, setServerMsg] = useState<string | null>(null)
    const [serverErr, setServerErr] = useState<string | null>(null)

    const onSubmit = async (data: FormValues) => {
        setServerErr(null)
        setServerMsg(null)
        try {
            const res = await AuthApi.register(data)
            setServerMsg(res.message)

            // Show message that email verification is required
            if (res.emailVerificationRequired) {
                setServerMsg('Đăng ký thành công! Vui lòng kiểm tra email để xác thực tài khoản.')
                setTimeout(() => navigate('/resend-verification', {
                    replace: true,
                    state: { email: data.email }
                }), 2000)
            } else {
                setTimeout(() => navigate('/login', { replace: true }), 800)
            }
        } catch (e) {
            const responseData = typeof e === 'object' && e && 'response' in e
                ? (e as { response?: { data?: unknown } }).response?.data
                : undefined
            const fallback = typeof e === 'object' && e && 'message' in e && typeof (e as { message?: unknown }).message === 'string'
                ? (e as { message: string }).message
                : 'Đăng ký thất bại'
            setServerErr(extractProblemMessage(responseData, fallback))
        }
    }

    const handleOAuthLogin = (provider: 'google' | 'facebook') => {
        // Redirect đến backend OAuth endpoint
        const backendUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8089'
        window.location.href = `${backendUrl}/oauth2/authorization/${provider}`
    }

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-50 p-4">
            <form onSubmit={handleSubmit(onSubmit)} className="w-full max-w-md bg-white shadow rounded p-6 space-y-4">
                <h1 className="text-xl font-semibold text-center">Tạo tài khoản</h1>
                {serverErr && (
                    <div className="bg-red-50 border border-red-200 rounded p-3">
                        <p className="text-red-600 text-sm">{serverErr}</p>
                    </div>
                )}
                {serverMsg && (
                    <div className="bg-green-50 border border-green-200 rounded p-3">
                        <p className="text-green-600 text-sm">{serverMsg}</p>
                    </div>
                )}

                <div>
                    <label className="block text-sm font-medium mb-1">Email</label>
                    <input className="w-full border rounded px-3 py-2" type="email" {...register('email')} />
                    {errors.email && <p className="text-xs text-red-600">{errors.email.message}</p>}
                </div>
                <div>
                    <label className="block text-sm font-medium mb-1">Họ tên</label>
                    <input className="w-full border rounded px-3 py-2" type="text" {...register('fullName')} />
                    {errors.fullName && <p className="text-xs text-red-600">{errors.fullName.message}</p>}
                </div>
                <div>
                    <label className="block text-sm font-medium mb-1">Mật khẩu</label>
                    <input className="w-full border rounded px-3 py-2" type="password" {...register('password')} />
                    {errors.password && <p className="text-xs text-red-600">{errors.password.message}</p>}
                </div>
                <div>
                    <label className="block text-sm font-medium mb-1">Số điện thoại</label>
                    <input className="w-full border rounded px-3 py-2" type="tel" {...register('phone')} />
                    {errors.phone && <p className="text-xs text-red-600">{errors.phone.message}</p>}
                </div>
                <div>
                    <label className="block text-sm font-medium mb-1">Ngày sinh (tùy chọn)</label>
                    <input className="w-full border rounded px-3 py-2" type="date" {...register('dateOfBirth')} />
                    {errors.dateOfBirth && <p className="text-xs text-red-600">{errors.dateOfBirth.message}</p>}
                </div>
                <button
                    type="submit"
                    disabled={isSubmitting}
                    className="w-full bg-black text-white rounded py-2 disabled:opacity-60"
                >
                    {isSubmitting ? 'Đang tạo tài khoản…' : 'Đăng ký'}
                </button>

                {/* OAuth Divider */}
                <div className="relative w-full flex items-center justify-center my-4">
                    <div className="flex-grow h-[1px] bg-gray-300"></div>
                    <span className="mx-4 text-sm text-gray-500 flex-shrink">
                        Hoặc đăng ký bằng
                    </span>
                    <div className="flex-grow h-[1px] bg-gray-300"></div>
                </div>

                {/* OAuth Buttons */}
                <div className="grid grid-cols-2 gap-3">
                    <button
                        type="button"
                        onClick={() => handleOAuthLogin('google')}
                        className="flex items-center justify-center gap-2 p-3 border border-gray-300 rounded-md hover:bg-gray-50 transition"
                    >
                        <SiGoogle className="text-blue-600 text-lg" />
                        <span className="text-sm font-medium text-gray-700">Google</span>
                    </button>
                    <button
                        type="button"
                        onClick={() => handleOAuthLogin('facebook')}
                        className="flex items-center justify-center gap-2 p-3 border border-gray-300 rounded-md hover:bg-gray-50 transition"
                    >
                        <SiFacebook className="text-blue-600 text-lg" />
                        <span className="text-sm font-medium text-gray-700">Facebook</span>
                    </button>
                </div>

                <p className="text-sm text-center">
                    Đã có tài khoản? <Link className="text-blue-600" to="/login">Đăng nhập</Link>
                </p>
            </form>
        </div>
    )
}
