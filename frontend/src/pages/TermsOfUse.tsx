import React from 'react';
import Header from '../components/layout/Header';
import Footer from '../components/layout/Footer';

const TermsOfUse = () => {
    return (
        <div className="min-h-screen flex flex-col">
            <Header />
            <main className="flex-grow container mx-auto px-4 py-8">
                <h1 className="text-3xl font-bold mb-6 text-center">Điều khoản sử dụng</h1>
                
                <div className="prose max-w-none bg-white p-6 rounded-lg shadow-sm">
                    <section className="mb-6">
                        <h2 className="text-xl font-semibold mb-3">1. Giới thiệu</h2>
                        <p className="mb-2">
                            Chào mừng quý khách hàng đến với website của chúng tôi.
                        </p>
                        <p>
                            Khi quý khách hàng truy cập vào trang web của chúng tôi có nghĩa là quý khách đồng ý với các điều khoản này. Trang web có quyền thay đổi, chỉnh sửa, thêm hoặc lược bỏ bất kỳ phần nào trong Điều khoản mua bán hàng hóa này, vào bất cứ lúc nào. Các thay đổi có hiệu lực ngay khi được đăng trên trang web mà không cần thông báo trước. Và khi quý khách tiếp tục sử dụng trang web, sau khi các thay đổi về Điều khoản này được đăng tải, có nghĩa là quý khách chấp nhận với những thay đổi đó.
                        </p>
                    </section>

                    <section className="mb-6">
                        <h2 className="text-xl font-semibold mb-3">2. Hướng dẫn sử dụng website</h2>
                        <p className="mb-2">
                            Khi vào web của chúng tôi, khách hàng phải đảm bảo đủ 18 tuổi, hoặc truy cập dưới sự giám sát của cha mẹ hay người giám hộ hợp pháp. Khách hàng đảm bảo có đầy đủ hành vi dân sự để thực hiện các giao dịch mua bán hàng hóa theo quy định hiện hành của pháp luật Việt Nam.
                        </p>
                        <p>
                            Chúng tôi sẽ cấp một tài khoản (Account) sử dụng để khách hàng có thể mua sắm trên website trong khuôn khổ Điều khoản và Điều kiện sử dụng đã đề ra.
                        </p>
                    </section>

                    <section className="mb-6">
                        <h2 className="text-xl font-semibold mb-3">3. Thanh toán an toàn và tiện lợi</h2>
                        <p className="mb-2">
                            Người mua có thể tham khảo các phương thức thanh toán sau đây và lựa chọn áp dụng phương thức phù hợp:
                        </p>
                        <ul className="list-disc pl-6">
                            <li>Cách 1: Thanh toán trực tiếp (người mua nhận hàng tại địa chỉ người bán)</li>
                            <li>Cách 2: Thanh toán sau (COD – giao hàng và thu tiền tận nơi)</li>
                            <li>Cách 3: Thanh toán online qua thẻ tín dụng, chuyển khoản</li>
                        </ul>
                    </section>

                    <section className="mb-6">
                        <h2 className="text-xl font-semibold mb-3">4. Chính sách đổi trả</h2>
                        <p>
                            Chúng tôi cam kết cung cấp sản phẩm chất lượng. Nếu sản phẩm có lỗi từ nhà sản xuất, quý khách có thể yêu cầu đổi trả trong vòng 7 ngày kể từ ngày nhận hàng, với điều kiện sản phẩm còn nguyên tem mác và chưa qua sử dụng.
                        </p>
                    </section>
                </div>
            </main>
            <Footer />
        </div>
    );
};

export default TermsOfUse;
