import React, { useState } from 'react';
import Header from '../components/layout/Header';
import Footer from '../components/layout/Footer';
import { toast } from 'react-toastify';
import { Mail, MapPin, Phone } from 'lucide-react';

const FeedbackSupport = () => {
    const [formData, setFormData] = useState({
        name: '',
        email: '',
        subject: '',
        message: ''
    });

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
    };

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        // Giả lập gửi feedback
        console.log('Feedback submitted:', formData);
        toast.success('Cảm ơn bạn đã gửi góp ý. Chúng tôi sẽ phản hồi sớm nhất có thể!');
        setFormData({
            name: '',
            email: '',
            subject: '',
            message: ''
        });
    };

    return (
        <div className="min-h-screen flex flex-col">
            <Header />
            <main className="flex-grow container mx-auto px-4 py-8">
                <h1 className="text-3xl font-bold mb-6 text-center">Góp ý - Phản hồi - Hỗ trợ</h1>
                
                <div className="grid grid-cols-1 md:grid-cols-2 gap-8 max-w-6xl mx-auto">
                    {/* Thông tin liên hệ */}
                    <div className="bg-white p-6 rounded-lg shadow-sm h-fit">
                        <h2 className="text-xl font-semibold mb-4">Thông tin liên hệ</h2>
                        <p className="mb-4 text-gray-600">
                            Chúng tôi luôn sẵn sàng lắng nghe ý kiến của bạn. Nếu bạn có bất kỳ câu hỏi, góp ý hoặc cần hỗ trợ, vui lòng liên hệ với chúng tôi qua các kênh sau:
                        </p>
                        
                        <div className="space-y-4">
                            <div className="flex items-start gap-3">
                                <div className="bg-red-100 p-2 rounded-full text-red-600 flex-shrink-0">
                                    <MapPin className="h-5 w-5" />
                                </div>
                                <div>
                                    <h3 className="font-medium">Địa chỉ</h3>
                                    <p className="text-gray-600">Số 123, Đường Nguyễn Văn Linh, Phường Tân Phú, Quận 7, TP. Hồ Chí Minh</p>
                                </div>
                            </div>

                            <div className="flex items-start gap-3">
                                <div className="bg-green-100 p-2 rounded-full text-green-600 flex-shrink-0">
                                    <Phone className="h-5 w-5" />
                                </div>
                                <div>
                                    <h3 className="font-medium">Điện thoại</h3>
                                    <p className="text-gray-600">0909.123.456 - 0987.654.321</p>
                                </div>
                            </div>

                            <div className="flex items-start gap-3">
                                <div className="bg-blue-100 p-2 rounded-full text-blue-600 flex-shrink-0">
                                    <Mail className="h-5 w-5" />
                                </div>
                                <div>
                                    <h3 className="font-medium">Email</h3>
                                    <p className="text-gray-600">contact@wearwave.vn</p>
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Form Góp ý */}
                    <div className="bg-white p-6 rounded-lg shadow-sm">
                        <h2 className="text-xl font-semibold mb-4">Gửi góp ý cho chúng tôi</h2>
                        <form onSubmit={handleSubmit} className="space-y-4">
                            <div>
                                <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-1">Họ và tên</label>
                                <input
                                    type="text"
                                    id="name"
                                    name="name"
                                    value={formData.name}
                                    onChange={handleChange}
                                    required
                                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    placeholder="Nhập họ tên của bạn"
                                />
                            </div>

                            <div>
                                <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-1">Email</label>
                                <input
                                    type="email"
                                    id="email"
                                    name="email"
                                    value={formData.email}
                                    onChange={handleChange}
                                    required
                                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    placeholder="Nhập email của bạn"
                                />
                            </div>

                            <div>
                                <label htmlFor="subject" className="block text-sm font-medium text-gray-700 mb-1">Chủ đề</label>
                                <select
                                    id="subject"
                                    name="subject"
                                    value={formData.subject}
                                    onChange={handleChange}
                                    required
                                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                                >
                                    <option value="">Chọn chủ đề</option>
                                    <option value="Góp ý">Góp ý cải thiện</option>
                                    <option value="Khiếu nại">Khiếu nại dịch vụ</option>
                                    <option value="Hỗ trợ">Cần hỗ trợ kỹ thuật</option>
                                    <option value="Khác">Khác</option>
                                </select>
                            </div>

                            <div>
                                <label htmlFor="message" className="block text-sm font-medium text-gray-700 mb-1">Nội dung</label>
                                <textarea
                                    id="message"
                                    name="message"
                                    value={formData.message}
                                    onChange={handleChange}
                                    required
                                    rows={4}
                                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    placeholder="Nhập nội dung góp ý của bạn..."
                                ></textarea>
                            </div>

                            <button
                                type="submit"
                                className="w-full bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700 transition-colors font-medium"
                            >
                                Gửi góp ý
                            </button>
                        </form>
                    </div>
                </div>
            </main>
            <Footer />
        </div>
    );
};

export default FeedbackSupport;
