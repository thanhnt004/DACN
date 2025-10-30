import Banner from '../components/layout/Banner'
import { Header } from '../components/layout/Header'
import Footer from '../components/layout/Footer'
import { Link } from 'react-router-dom'

export default function HomePage() {
    return (
        <div className="min-h-screen bg-gray-50">
            <Header />
            <Banner />

            {/* Featured Categories */}
            <section className="py-16">
                <div className="container mx-auto px-4">
                    <h2 className="text-3xl font-bold text-center mb-12">Danh Mục Nổi Bật</h2>
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
                        {[
                            { name: 'Thời Trang Nam', path: '/products?gender=men', image: 'https://via.placeholder.com/300x400?text=Nam' },
                            { name: 'Thời Trang Nữ', path: '/products?gender=women', image: 'https://via.placeholder.com/300x400?text=Nữ' },
                            { name: 'Unisex', path: '/products?gender=unisex', image: 'https://via.placeholder.com/300x400?text=Trẻ+Em' },
                            { name: 'Phụ Kiện', path: '/categories/phu-kien', image: 'https://via.placeholder.com/300x400?text=Phụ+Kiện' },
                        ].map((category) => (
                            <Link
                                key={category.name}
                                to={category.path}
                                className="group relative overflow-hidden rounded-lg shadow-lg hover:shadow-xl transition-shadow duration-300"
                            >
                                <div className="aspect-[3/4] relative">
                                    <img
                                        src={category.image}
                                        alt={category.name}
                                        className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-500"
                                    />
                                    <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent" />
                                    <div className="absolute bottom-0 left-0 right-0 p-4">
                                        <h3 className="text-white text-xl font-bold">{category.name}</h3>
                                    </div>
                                </div>
                            </Link>
                        ))}
                    </div>
                </div>
            </section>

            {/* Call to Action */}
            <section className="py-16 bg-red-600 text-white">
                <div className="container mx-auto px-4 text-center">
                    <h2 className="text-4xl font-bold mb-4">Khám Phá Bộ Sưu Tập Mới</h2>
                    <p className="text-xl mb-8">Thời trang xu hướng, giá cả hợp lý</p>
                    <Link
                        to="/products"
                        className="inline-block bg-white text-red-600 px-8 py-4 rounded-full font-bold hover:bg-gray-100 transition-colors duration-200"
                    >
                        Xem Tất Cả Sản Phẩm
                    </Link>
                </div>
            </section>

            {/* Footer */}
            <Footer />
        </div>
    )
}
