import { Facebook, Instagram, Youtube, Mail, Phone, MapPin, CreditCard, Wallet, Smartphone, Building2 } from 'lucide-react';

export default function Footer() {
    return (
        <footer className="bg-gray-800 text-white">
            <div className="container mx-auto px-4 py-12">
                <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
                    {/* Company Info */}
                    <div>
                        <div className="flex items-center gap-3 mb-4">
                            <img
                                src="/img/logo.svg"
                                alt="WearWave Logo"
                                className="h-12 w-12"
                            />
                            <div>
                                <h3 className="text-lg font-bold uppercase">Cửa hàng thời trang</h3>
                                <h2 className="text-xl font-bold text-red-500">WearWave</h2>
                            </div>
                        </div>
                        <div className="space-y-2 text-sm text-gray-300">
                            <p className="flex items-start gap-2">
                                <MapPin className="w-4 h-4 mt-1 flex-shrink-0" />
                                <span>Số 123, Đường Nguyễn Văn Linh, Phường Tân Phú, Quận 7, TP. Hồ Chí Minh</span>
                            </p>
                            <p className="flex items-center gap-2">
                                <Phone className="w-4 h-4 flex-shrink-0" />
                                <span>Điện thoại: 0909.123.456 - 0987.654.321</span>
                            </p>
                            <p className="flex items-center gap-2">
                                <Mail className="w-4 h-4 flex-shrink-0" />
                                <span>Email: contact@wearwave.vn</span>
                            </p>
                        </div>
                    </div>

                    {/* Thương hiệu (removed as requested) */}

                    {/* Hỗ trợ */}
                    <div>
                        <h4 className="text-lg font-bold mb-4 uppercase">Hỗ Trợ</h4>
                        <ul className="space-y-2 text-sm text-gray-300">
                            <li>
                                <a href="/terms" className="hover:text-red-500 transition-colors">
                                    Điều khoản sử dụng
                                </a>
                            </li>
                            <li>
                                <a href="/feedback" className="hover:text-red-500 transition-colors">
                                    Góp ý - Phản hồi
                                </a>
                            </li>
                            <li>
                                <a href="#" className="hover:text-red-500 transition-colors">
                                    Hệ thống cửa hàng
                                </a>
                            </li>
                            <li>
                                <a href="#" className="hover:text-red-500 transition-colors">
                                    Tin tức
                                </a>
                            </li>
                            <li>
                                <a href="#" className="hover:text-red-500 transition-colors">
                                    Tuyển dụng
                                </a>
                            </li>
                            <li>
                                <a href="#" className="hover:text-red-500 transition-colors">
                                    Liên hệ
                                </a>
                            </li>
                        </ul>
                    </div>

                    {/* Tài khoản */}
                    <div>
                        <h4 className="text-lg font-bold mb-4 uppercase">Tài Khoản</h4>
                        <ul className="space-y-2 text-sm text-gray-300">
                            <li>
                                <a href="/login" className="hover:text-red-500 transition-colors">
                                    Đăng nhập / Đăng ký
                                </a>
                            </li>
                            <li>
                                <a href="/profile" className="hover:text-red-500 transition-colors">
                                    Tài khoản
                                </a>
                            </li>
                            <li>
                                <a href="/orders" className="hover:text-red-500 transition-colors">
                                    Lịch sử đặt hàng
                                </a>
                            </li>
                            <li>
                                <a href="/orders/track" className="hover:text-red-500 transition-colors">
                                    Tra cứu đơn hàng
                                </a>
                            </li>
                        </ul>
                    </div>

                    {/* Theo dõi chúng tôi */}
                    <div>
                        <h4 className="text-lg font-bold mb-4 uppercase">Theo Dõi Chúng Tôi</h4>
                        <div className="flex gap-3 mb-6">
                            <a
                                href="#"
                                className="w-10 h-10 bg-gray-700 rounded-full flex items-center justify-center hover:bg-red-500 transition-colors"
                                aria-label="Facebook"
                            >
                                <Facebook className="w-5 h-5" />
                            </a>
                            <a
                                href="#"
                                className="w-10 h-10 bg-gray-700 rounded-full flex items-center justify-center hover:bg-red-500 transition-colors"
                                aria-label="Instagram"
                            >
                                <Instagram className="w-5 h-5" />
                            </a>
                            <a
                                href="#"
                                className="w-10 h-10 bg-gray-700 rounded-full flex items-center justify-center hover:bg-red-500 transition-colors"
                                aria-label="YouTube"
                            >
                                <Youtube className="w-5 h-5" />
                            </a>
                            <a
                                href="#"
                                className="w-10 h-10 bg-gray-700 rounded-full flex items-center justify-center hover:bg-red-500 transition-colors"
                                aria-label="TikTok"
                            >
                                <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
                                    <path d="M19.59 6.69a4.83 4.83 0 0 1-3.77-4.25V2h-3.45v13.67a2.89 2.89 0 0 1-5.2 1.74 2.89 2.89 0 0 1 2.31-4.64 2.93 2.93 0 0 1 .88.13V9.4a6.84 6.84 0 0 0-1-.05A6.33 6.33 0 0 0 5 20.1a6.34 6.34 0 0 0 10.86-4.43v-7a8.16 8.16 0 0 0 4.77 1.52v-3.4a4.85 4.85 0 0 1-1-.1z" />
                                </svg>
                            </a>
                        </div>

                        <div>
                            <h5 className="text-sm font-semibold mb-3 uppercase">Phương Thức Thanh Toán</h5>
                            <div className="flex flex-wrap gap-2">
                                <div className="bg-white rounded px-3 py-2 flex items-center gap-1.5 shadow-sm">
                                    <CreditCard className="w-4 h-4 text-blue-600" />
                                    <span className="text-xs font-medium text-gray-700">VISA</span>
                                </div>
                                <div className="bg-white rounded px-3 py-2 flex items-center gap-1.5 shadow-sm">
                                    <CreditCard className="w-4 h-4 text-orange-600" />
                                    <span className="text-xs font-medium text-gray-700">Master</span>
                                </div>
                                <div className="bg-white rounded px-3 py-2 flex items-center gap-1.5 shadow-sm">
                                    <Building2 className="w-4 h-4 text-green-600" />
                                    <span className="text-xs font-medium text-gray-700">ATM</span>
                                </div>
                                <div className="bg-white rounded px-3 py-2 flex items-center gap-1.5 shadow-sm">
                                    <Smartphone className="w-4 h-4 text-pink-600" />
                                    <span className="text-xs font-medium text-gray-700">MoMo</span>
                                </div>
                                <div className="bg-white rounded px-3 py-2 flex items-center gap-1.5 shadow-sm">
                                    <Wallet className="w-4 h-4 text-blue-500" />
                                    <span className="text-xs font-medium text-gray-700">ZaloPay</span>
                                </div>
                                <div className="bg-white rounded px-3 py-2 flex items-center gap-1.5 shadow-sm">
                                    <Wallet className="w-4 h-4 text-red-600" />
                                    <span className="text-xs font-medium text-gray-700">VNPay</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Copyright */}
                <div className="border-t border-gray-700 mt-8 pt-6 flex flex-col md:flex-row justify-between items-center text-sm text-gray-400">
                    <p>© 2025 WearWave. All rights reserved.</p>
                    <div className="flex gap-4 mt-4 md:mt-0">
                        <a href="#" className="hover:text-white transition-colors">
                            Chính sách bảo mật
                        </a>
                        <a href="#" className="hover:text-white transition-colors">
                            Điều khoản sử dụng
                        </a>
                        <a href="#" className="hover:text-white transition-colors">
                            Chính sách đổi trả
                        </a>
                    </div>
                </div>
            </div>
        </footer>
    )
}
