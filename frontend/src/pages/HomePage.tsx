import { Header } from '../components/layout/Header'
import Footer from '../components/layout/Footer'
import HomeBanner from '../components/HomeBanner'
import ProductCard from '../components/layout/ProductCard'
import { Link } from 'react-router-dom'
import { useEffect, useState, useRef } from 'react'
import { ChevronLeft, ChevronRight, TrendingUp, Sparkles } from 'lucide-react'
import * as ProductsApi from '../api/admin/products'
import * as ReportsApi from '../api/admin/reports'

export default function HomePage() {
    const [bestSellingProducts, setBestSellingProducts] = useState<ReportsApi.TopProduct[]>([])
    const [newProducts, setNewProducts] = useState<ProductsApi.ProductSummaryResponse[]>([])
    const [loadingBestSelling, setLoadingBestSelling] = useState(true)
    const [loadingNewProducts, setLoadingNewProducts] = useState(true)
    const [currentPage, setCurrentPage] = useState(0)
    const [hasMoreProducts, setHasMoreProducts] = useState(true)
    const [loadingMore, setLoadingMore] = useState(false)
    
    const carouselRef = useRef<HTMLDivElement>(null)
    const [scrollPosition, setScrollPosition] = useState(0)
    const [canScrollLeft, setCanScrollLeft] = useState(false)
    const [canScrollRight, setCanScrollRight] = useState(false)
    const autoScrollInterval = useRef<NodeJS.Timeout | null>(null)

    // Fetch best selling products
    useEffect(() => {
        const fetchBestSelling = async () => {
            setLoadingBestSelling(true)
            try {
                const endDate = new Date()
                const startDate = new Date()
                startDate.setMonth(startDate.getMonth() - 3) // Last 3 months
                
                const result = await ReportsApi.getTopSellingProducts({
                    startDate: startDate.toISOString().split('T')[0],
                    endDate: endDate.toISOString().split('T')[0],
                    limit: 10
                })
                
                // Fetch detailed product info to get slug and other data
                const productsWithDetails = await Promise.all(
                    result.products.map(async (product) => {
                        try {
                            const details = await ProductsApi.getProductDetail(product.productId, ['images', 'variants'])
                            return {
                                ...product,
                                productDetails: details
                            }
                        } catch (error) {
                            console.error('Failed to fetch product details:', error)
                            return product
                        }
                    })
                )
                
                setBestSellingProducts(productsWithDetails)
            } catch (error) {
                console.error('Failed to fetch best selling products:', error)
            } finally {
                setLoadingBestSelling(false)
            }
        }
        fetchBestSelling()
    }, [])

    // Fetch new products
    const fetchNewProducts = async (page: number) => {
        if (page === 0) {
            setLoadingNewProducts(true)
        } else {
            setLoadingMore(true)
        }
        
        try {
            const result = await ProductsApi.getProducts({
                page,
                size: 8,
                sortBy: 'createdAt',
                direction: 'DESC',
                status: 'ACTIVE'
            })
            
            if (page === 0) {
                setNewProducts(result.content)
            } else {
                setNewProducts(prev => [...prev, ...result.content])
            }
            
            setHasMoreProducts(!result.last)
        } catch (error) {
            console.error('Failed to fetch new products:', error)
        } finally {
            setLoadingNewProducts(false)
            setLoadingMore(false)
        }
    }

    useEffect(() => {
        fetchNewProducts(0)
    }, [])

    // Check scroll capabilities
    const checkScrollButtons = () => {
        if (carouselRef.current) {
            const { scrollLeft, scrollWidth, clientWidth } = carouselRef.current
            setCanScrollLeft(scrollLeft > 0)
            setCanScrollRight(scrollLeft < scrollWidth - clientWidth - 10)
            setScrollPosition(scrollLeft)
        }
    }

    useEffect(() => {
        checkScrollButtons()
        const carousel = carouselRef.current
        if (carousel) {
            carousel.addEventListener('scroll', checkScrollButtons)
            return () => carousel.removeEventListener('scroll', checkScrollButtons)
        }
    }, [bestSellingProducts])

    // Auto scroll - pause when modal is open or user is hovering
    useEffect(() => {
        if (bestSellingProducts.length > 0) {
            autoScrollInterval.current = setInterval(() => {
                // Skip auto-scroll if modal or dialog is open
                if (document.querySelector('[role="dialog"]') || document.querySelector('.modal')) {
                    return;
                }
                
                if (carouselRef.current) {
                    const { scrollLeft, scrollWidth, clientWidth } = carouselRef.current
                    if (scrollLeft >= scrollWidth - clientWidth - 10) {
                        carouselRef.current.scrollTo({ left: 0, behavior: 'smooth' })
                    } else {
                        carouselRef.current.scrollBy({ left: 300, behavior: 'smooth' })
                    }
                }
            }, 5000) // Tăng từ 3s lên 5s để ít gây khó chịu hơn
        }

        return () => {
            if (autoScrollInterval.current) {
                clearInterval(autoScrollInterval.current)
            }
        }
    }, [bestSellingProducts])

    const scroll = (direction: 'left' | 'right') => {
        if (carouselRef.current) {
            const scrollAmount = 300
            const newPosition = direction === 'left' 
                ? scrollPosition - scrollAmount 
                : scrollPosition + scrollAmount
            
            carouselRef.current.scrollTo({ left: newPosition, behavior: 'smooth' })
            
            // Pause auto scroll temporarily
            if (autoScrollInterval.current) {
                clearInterval(autoScrollInterval.current)
            }
        }
    }

    const handleLoadMore = () => {
        fetchNewProducts(currentPage + 1)
        setCurrentPage(currentPage + 1)
    }

    return (
        <div className="min-h-screen bg-gray-50">
            <Header />
            <HomeBanner />

            {/* Featured Categories */}
            <section className="py-16">
                <div className="container mx-auto px-4">
                    <h2 className="text-3xl font-bold text-center mb-12">Danh Mục Nổi Bật</h2>
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
                        {[
                            { name: 'Thời Trang Nam', path: '/products?gender=men', image: '/img/banner-thoi-trang-nam.jpg' },
                            { name: 'Thời Trang Nữ', path: '/products?gender=women', image: '/img/banner-thoi-trang-nu.jpg' },
                            { name: 'Unisex', path: '/products?gender=unisex', image: '/img/banner-thoi-trang-unisex.jpg' },
                            { name: 'Phụ Kiện', path: '/categories/phu-kien', image: '/img/banner-thoi-trang-phu-kien.webp' },
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

            {/* Best Selling Products */}
            <section className="py-16 bg-white">
                <div className="container mx-auto px-4">
                    <div className="flex items-center justify-center gap-3 mb-12">
                        <TrendingUp className="w-8 h-8 text-red-600" />
                        <h2 className="text-3xl font-bold text-center">Sản Phẩm Bán Chạy</h2>
                    </div>
                    
                    {loadingBestSelling ? (
                        <div className="flex justify-center items-center h-96">
                            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-red-600"></div>
                        </div>
                    ) : bestSellingProducts.length > 0 ? (
                        <div className="relative group">
                            {/* Left Arrow */}
                            <button
                                onClick={() => scroll('left')}
                                disabled={!canScrollLeft}
                                className={`absolute left-0 top-1/2 -translate-y-1/2 z-10 bg-white rounded-full p-3 shadow-lg transition-all ${
                                    canScrollLeft 
                                        ? 'opacity-0 group-hover:opacity-100 hover:bg-red-600 hover:text-white' 
                                        : 'opacity-0 cursor-not-allowed'
                                }`}
                                aria-label="Scroll left"
                            >
                                <ChevronLeft className="w-6 h-6" />
                            </button>

                            {/* Carousel Container */}
                            <div
                                ref={carouselRef}
                                className="flex gap-6 overflow-x-auto scrollbar-hide scroll-smooth pb-4"
                                style={{ scrollbarWidth: 'none', msOverflowStyle: 'none' }}
                                onMouseEnter={() => {
                                    // Pause auto-scroll when hovering
                                    if (autoScrollInterval.current) {
                                        clearInterval(autoScrollInterval.current);
                                    }
                                }}
                                onMouseLeave={() => {
                                    // Resume auto-scroll when leaving, but only if no modal is open
                                    if (!document.querySelector('[role="dialog"]') && !document.querySelector('.modal')) {
                                        autoScrollInterval.current = setInterval(() => {
                                            if (carouselRef.current) {
                                                const { scrollLeft, scrollWidth, clientWidth } = carouselRef.current;
                                                if (scrollLeft >= scrollWidth - clientWidth - 10) {
                                                    carouselRef.current.scrollTo({ left: 0, behavior: 'smooth' });
                                                } else {
                                                    carouselRef.current.scrollBy({ left: 300, behavior: 'smooth' });
                                                }
                                            }
                                        }, 5000);
                                    }
                                }}
                            >
                                {bestSellingProducts.map((product) => {
                                    const details = (product as any).productDetails
                                    return (
                                        <div key={product.productId} className="flex-none w-64">
                                            <ProductCard
                                                id={product.productId}
                                                slug={details?.slug || product.productId}
                                                imageUrl={details?.imageUrl || product.primaryImageUrl || undefined}
                                                name={product.productName}
                                                priceAmount={details?.priceAmount || 0}
                                                colors={details?.colors}
                                                sizes={details?.sizes}
                                                isInStock={details?.isInStock}
                                            />
                                            <div className="mt-2 text-center">
                                                <span className="text-sm text-gray-600">
                                                    Đã bán: <span className="font-semibold text-red-600">{product.quantitySold}</span>
                                                </span>
                                            </div>
                                        </div>
                                    )
                                })}
                            </div>

                            {/* Right Arrow */}
                            <button
                                onClick={() => scroll('right')}
                                disabled={!canScrollRight}
                                className={`absolute right-0 top-1/2 -translate-y-1/2 z-10 bg-white rounded-full p-3 shadow-lg transition-all ${
                                    canScrollRight 
                                        ? 'opacity-0 group-hover:opacity-100 hover:bg-red-600 hover:text-white' 
                                        : 'opacity-0 cursor-not-allowed'
                                }`}
                                aria-label="Scroll right"
                            >
                                <ChevronRight className="w-6 h-6" />
                            </button>
                        </div>
                    ) : (
                        <div className="text-center text-gray-500 py-12">
                            Chưa có dữ liệu sản phẩm bán chạy
                        </div>
                    )}
                </div>
            </section>

            {/* New Products */}
            <section className="py-16 bg-gray-50">
                <div className="container mx-auto px-4">
                    <div className="flex items-center justify-center gap-3 mb-12">
                        <Sparkles className="w-8 h-8 text-red-600" />
                        <h2 className="text-3xl font-bold text-center">Sản Phẩm Mới</h2>
                    </div>
                    
                    {loadingNewProducts ? (
                        <div className="flex justify-center items-center h-96">
                            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-red-600"></div>
                        </div>
                    ) : (
                        <>
                            <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
                                {newProducts.map((product) => (
                                    <ProductCard
                                        key={product.id}
                                        id={product.id}
                                        slug={product.slug}
                                        imageUrl={product.imageUrl}
                                        name={product.name}
                                        gender={product.gender}
                                        priceAmount={product.priceAmount}
                                        colors={product.colors}
                                        sizes={product.sizes}
                                        isInStock={product.isInStock}
                                    />
                                ))}
                            </div>
                            
                            {/* Load More Button */}
                            {hasMoreProducts && (
                                <div className="text-center mt-12">
                                    <button
                                        onClick={handleLoadMore}
                                        disabled={loadingMore}
                                        className="bg-black text-white px-8 py-4 rounded-full font-medium hover:bg-red-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                                    >
                                        {loadingMore ? (
                                            <span className="flex items-center gap-2">
                                                <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></div>
                                                Đang tải...
                                            </span>
                                        ) : (
                                            'Xem Thêm'
                                        )}
                                    </button>
                                </div>
                            )}
                        </>
                    )}
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
