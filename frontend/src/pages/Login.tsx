import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { useAuthStore } from '../store/auth'
import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { SiFacebook, SiGoogle } from 'react-icons/si'
import { FaRegEye, FaRegEyeSlash } from 'react-icons/fa'

const schema = z.object({
    identifier: z.string().min(3, 'Vui lòng nhập email hoặc số điện thoại'),
    password: z.string().min(6, 'Mật khẩu tối thiểu 6 ký tự'),
})

type FormValues = z.infer<typeof schema>

export default function Login() {
    const [showPassword, setShowPassword] = useState(true);
    const [searchParams] = useSearchParams()
    const togglePasswordView = () => {
        setShowPassword(!showPassword);
    }
    const { login, loading, error, refreshToken } = useAuthStore()
    const [success, setSuccess] = useState<string | null>(null)
    const navigate = useNavigate()
    const {
        register,
        handleSubmit,
        formState: { errors },
    } = useForm<FormValues>({ resolver: zodResolver(schema) })

    // Xử lý OAuth callback
    useEffect(() => {
        const provider = searchParams.get('provider')
        if (provider) {
            // OAuth callback - refresh để lấy access token
            const handleOAuthCallback = async () => {
                try {
                    await refreshToken()
                    setSuccess(`Đăng nhập bằng ${provider} thành công!`)
                    setTimeout(() => {
                        navigate('/', { replace: true })
                    }, 1000)
                } catch (err) {
                    console.error('OAuth callback error:', err)
                }
            }
            handleOAuthCallback()
        }
    }, [searchParams, refreshToken, navigate])

    const handleOAuthLogin = (provider: 'google' | 'facebook') => {
        // Redirect đến backend OAuth endpoint
        const backendUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8089'
        window.location.href = `${backendUrl}/oauth2/authorization/${provider}`
    }

    const onSubmit = async (data: FormValues) => {
        setSuccess(null)
        try {
            const res = await login(data.identifier, data.password)

            // Check if email verification is required
            if (res.requireEmailVerification) {
                navigate('/email-verification-required', { replace: true })
                return
            }

            setSuccess('Đăng nhập thành công')
            navigate('/', { replace: true })
        } catch {
            // error handled in store
        }
    }

    return (
        <div className="w-full min-h-screen flex bg-white">
            <div className="hidden md:flex flex-col w-1/2 items-center justify-center py-12 bg-gray-50">
                <div className='text-red-600 text-3xl font-bold'>WearWave Store</div>
                <img src="/src/assets/img/logo.svg"
                    alt="WearWave Logo"
                    className="h-1/2 w-2/3" />

            </div>

            <div className="w-full md:w-1/2 py-8 md:py-12 shadow-lg" >
                <h1 className="text-2xl md:text-3xl font-bold text-red-600 text-center mb-6">
                    Đăng nhập
                </h1>
                <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4 p-5">

                    <div>
                        <label
                            htmlFor="identifier"
                            className="block text-sm font-medium text-gray-700 mb-1"
                        >
                            Số điện thoại
                        </label>
                        <input
                            id="identifier"
                            placeholder="Nhập email hoặc số điện thoại  của bạn"
                            {...register('identifier')}
                            className="w-full p-3 border border-gray-300 rounded-md outline-none focus:ring-2 focus:ring-red-500"
                        />
                        {errors.identifier && (<p className="text-xs text-red-600 mt-1">{errors.identifier.message}</p>)}
                    </div>
                    <div>
                        <label
                            htmlFor="password"
                            className="block text-sm font-medium text-gray-700 mb-1"
                        >
                            Mật khẩu
                        </label>
                        <div className="relative">
                            <input
                                id="password"
                                type={showPassword ? "text" : "password"}
                                placeholder="Nhập mật khẩu của bạn"
                                {...register('password')}
                                className="w-full p-3 border border-gray-300 rounded-md outline-none focus:ring-2 focus:ring-red-500"
                            />
                            <div
                                className="absolute right-4 top-1/2 -translate-y-1/2 cursor-pointer text-gray-500"
                                onClick={togglePasswordView}
                            >
                                {showPassword ? <FaRegEye /> : <FaRegEyeSlash />}
                            </div>
                        </div>
                        {errors.password && (<p className="text-xs text-red-600 mt-1">{errors.password.message}</p>)}
                    </div>

                    <div className="text-right">
                        <Link to="/forgot-password" className="text-sm text-blue-600 hover:underline">
                            Quên mật khẩu?
                        </Link>
                    </div>
                    <button
                        type="submit"
                        className="w-1/2 p-3 text-black rounded-md bg-blue-600 font-semibold disabled:bg-red-400 self-center"
                        disabled={loading}
                    >
                        {loading ? 'Đang đăng nhập…' : 'Đăng nhập'}
                    </button>

                    {error && <div className="text-red-600 text-sm text-center">{error}</div>}
                    {success && <div className="text-green-600 text-sm text-center">{success}</div>}
                </form>

                <div className="relative w-full flex items-center justify-center my-6">
                    <div className="flex-grow h-[1px] bg-gray-300"></div>
                    <span className="mx-4 text-sm text-gray-500 flex-shrink">
                        Hoặc đăng nhập bằng
                    </span>
                    <div className="flex-grow h-[1px] bg-gray-300"></div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 justify-items-center">
                    <div
                        onClick={() => handleOAuthLogin('google')}
                        className="flex items-center justify-center w-3/4 gap-2 p-3 border border-gray-300 rounded-md hover:bg-gray-50 cursor-pointer"
                    >
                        <SiGoogle className="text-blue-600 text-xl" />
                        <span className="text-sm font-medium text-gray-700">Google</span>
                    </div>

                    <div
                        onClick={() => handleOAuthLogin('facebook')}
                        className="flex items-center w-3/4 justify-center gap-2 p-3 border border-gray-300 rounded-md hover:bg-gray-50 cursor-pointer"
                    >
                        <SiFacebook className="text-blue-600 text-xl" />
                        <span className="text-sm font-medium text-gray-700">Facebook</span>
                    </div>
                </div>

                {/* --- LINK ĐĂNG KÝ --- */}
                <p className="text-center mt-6 text-sm text-gray-600">
                    Bạn chưa có tài khoản?{' '}
                    <Link to="/register" className="font-semibold text-red-600 hover:underline">
                        Đăng ký ngay
                    </Link>
                </p>

            </div>
        </div>
    )
}
