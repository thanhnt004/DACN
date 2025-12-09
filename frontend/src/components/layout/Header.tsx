import React, { useState, useEffect, useRef } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Search, User, ShoppingBag, ChevronDown, Package, Loader2 } from 'lucide-react';
import { useAuthStore } from '../../store/auth';
import { useCartStore } from '../../store/cart';
import { useCategoriesStore } from '../../store/categories';
import * as ProductSearchApi from '../../api/productSearch';

// --- Icons ---
const SearchIcon = Search;
const UserIcon = User;
const ShoppingBagIcon = ShoppingBag;

// --- Sub-components ---
const AnnouncementBar = () => {
    const [isVisible, setIsVisible] = useState(true);
    if (!isVisible) return null;

    return (
        <div className="relative bg-gray-900 text-white text-xs text-center py-2 px-8">
            <span>
                Giảm giá tới 50% cho bộ sưu tập mới, chỉ trong thời gian có hạn!
                <a href="#" className="underline ml-2 font-semibold">Mua ngay</a>
            </span>
            <button
                onClick={() => setIsVisible(false)}
                className="absolute right-2 top-1/2 -translate-y-1/2 text-gray-400 hover:text-white"
                aria-label="Đóng thông báo"
            >
                <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
            </button>
        </div>
    );
};

const MainHeader = () => {
    const navigate = useNavigate();
    const { isAuthenticated, isAdmin, logout } = useAuthStore();
    const { getItemCount, fetchCart } = useCartStore();
    const [searchQuery, setSearchQuery] = useState('');
    const [showUserMenu, setShowUserMenu] = useState(false);
    const [searchResults, setSearchResults] = useState<ProductSearchApi.ProductSearchResult[]>([]);
    const [showSearchResults, setShowSearchResults] = useState(false);
    const [searchLoading, setSearchLoading] = useState(false);
    const searchTimeoutRef = useRef<number | null>(null);
    const searchContainerRef = useRef<HTMLDivElement>(null);
    const cartItemCount = getItemCount();

    // Fetch cart on mount
    useEffect(() => {
        fetchCart().catch(err => console.error('Failed to fetch cart:', err));
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    // Close search results when clicking outside
    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (searchContainerRef.current && !searchContainerRef.current.contains(event.target as Node)) {
                setShowSearchResults(false);
            }
        };

        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    // Debounced search
    useEffect(() => {
        if (searchTimeoutRef.current) {
            clearTimeout(searchTimeoutRef.current);
        }

        if (searchQuery.trim().length >= 2) {
            setSearchLoading(true);
            searchTimeoutRef.current = setTimeout(async () => {
                try {
                    const results = await ProductSearchApi.vectorSearch(searchQuery.trim(), 5, 0.5);
                    setSearchResults(results);
                    setShowSearchResults(true);
                } catch (error) {
                    console.error('Search failed:', error);
                    setSearchResults([]);
                } finally {
                    setSearchLoading(false);
                }
            }, 300);
        } else {
            setSearchResults([]);
            setShowSearchResults(false);
            setSearchLoading(false);
        }

        return () => {
            if (searchTimeoutRef.current) {
                clearTimeout(searchTimeoutRef.current);
            }
        };
    }, [searchQuery]);

    const handleSearch = (e: React.FormEvent) => {
        e.preventDefault();
        if (searchQuery.trim()) {
            navigate(`/products?search=${encodeURIComponent(searchQuery.trim())}`);
            setShowSearchResults(false);
        }
    };

    const handleResultClick = (slug: string) => {
        navigate(`/products/${slug}`);
        setShowSearchResults(false);
        setSearchQuery('');
    };

    const handleLogout = () => {
        logout();
        setShowUserMenu(false);
        navigate('/');
    };

    const formatPrice = (price: number) => {
        return new Intl.NumberFormat('vi-VN', {
            style: 'currency',
            currency: 'VND'
        }).format(price);
    };

    return (
        <header className="bg-white border-b border-gray-200">
            <div className="container mx-auto">
                <div className="flex items-center justify-between h-20 px-4">
                    {/* Logo */}
                    <div className="flex-shrink-0">
                        
                        <Link to="/" className="text-2xl font-bold text-red-600 hover:text-red-700">
                            <div className ="block"><img
                                src="/img/logo.svg"
                                alt="WearWave Logo"
                                className="h-12 w-12"
                            />
                                WEARWAVE STORE</div>    
                        </Link>
                    </div>

                    {/* Search Bar */}
                    <div className="flex-1 max-w-xl mx-8" ref={searchContainerRef}>
                        <form onSubmit={handleSearch} className="relative">
                            <input
                                type="search"
                                placeholder="Bạn muốn tìm gì hôm nay?"
                                value={searchQuery}
                                onChange={(e) => setSearchQuery(e.target.value)}
                                onFocus={() => {
                                    if (searchResults.length > 0) {
                                        setShowSearchResults(true);
                                    }
                                }}
                                className="w-full h-12 pl-5 pr-12 text-sm bg-gray-100 border border-transparent rounded-full focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-transparent"
                                autoComplete="off"
                            />
                            <button
                                type="submit"
                                className="absolute right-0 top-0 h-full px-5 text-gray-500 hover:text-red-600"
                            >
                                {searchLoading ? (
                                    <Loader2 className="w-5 h-5 animate-spin" />
                                ) : (
                                    <SearchIcon className="w-5 h-5" />
                                )}
                            </button>

                            {/* Search Results Dropdown */}
                            {showSearchResults && searchResults.length > 0 && (
                                <div className="absolute top-full left-0 right-0 mt-2 bg-white border border-gray-200 rounded-lg shadow-xl z-50 max-h-96 overflow-y-auto">
                                    <div className="py-2">
                                        {searchResults.map((result) => (
                                            <button
                                                key={result.productId}
                                                onClick={() => handleResultClick(result.product.slug)}
                                                className="w-full px-4 py-3 hover:bg-gray-50 transition-colors flex items-center gap-4 text-left"
                                            >
                                                {/* Product Image */}
                                                <img
                                                    src={result.product.imageUrl || 'https://placehold.co/60x60'}
                                                    alt={result.product.name}
                                                    className="w-16 h-16 object-cover rounded flex-shrink-0"
                                                />
                                                
                                                {/* Product Info */}
                                                <div className="flex-1 min-w-0">
                                                    <h4 className="font-medium text-gray-900 truncate">
                                                        {result.product.name}
                                                    </h4>
                                                    <p className="text-sm text-gray-500 truncate mt-1">
                                                        {result.product.brandName || 'Thương hiệu'}
                                                    </p>
                                                    <div className="flex items-center gap-2 mt-1">
                                                        <span className="font-bold text-red-600">
                                                            {formatPrice(result.product.priceAmount)}
                                                        </span>
                                                        {result.similarity && (
                                                            <span className="text-xs text-gray-400">
                                                                ({Math.round(result.similarity * 100)}% phù hợp)
                                                            </span>
                                                        )}
                                                    </div>
                                                </div>
                                            </button>
                                        ))}
                                    </div>
                                    
                                    {/* View All Results */}
                                    <div className="border-t border-gray-200 p-3">
                                        <button
                                            onClick={(e) => {
                                                e.preventDefault();
                                                handleSearch(e);
                                            }}
                                            className="w-full text-center text-sm text-red-600 hover:text-red-700 font-medium"
                                        >
                                            Xem tất cả kết quả cho "{searchQuery}"
                                        </button>
                                    </div>
                                </div>
                            )}

                            {/* No Results */}
                            {showSearchResults && searchQuery.trim().length >= 2 && searchResults.length === 0 && !searchLoading && (
                                <div className="absolute top-full left-0 right-0 mt-2 bg-white border border-gray-200 rounded-lg shadow-xl z-50 p-6 text-center">
                                    <SearchIcon className="w-12 h-12 text-gray-300 mx-auto mb-2" />
                                    <p className="text-gray-500">Không tìm thấy sản phẩm phù hợp</p>
                                    <p className="text-sm text-gray-400 mt-1">Thử tìm kiếm với từ khóa khác</p>
                                </div>
                            )}
                        </form>
                    </div>

                    {/* User Actions */}
                    <div className="flex items-center space-x-5">
                        {/* User Account */}
                        <div className="relative">
                            {isAuthenticated ? (
                                <div>
                                    <button
                                        onClick={() => setShowUserMenu(!showUserMenu)}
                                        className="flex items-center space-x-2 text-gray-600 hover:text-red-600"
                                    >
                                        <UserIcon className="w-6 h-6" />
                                        <span className="text-sm font-medium hidden md:block">
                                            Tài khoản
                                        </span>
                                        <ChevronDown className="w-4 h-4 hidden md:block" />
                                    </button>

                                    {showUserMenu && (
                                        <>
                                            <div
                                                className="fixed inset-0 z-10"
                                                onClick={() => setShowUserMenu(false)}
                                            />
                                            <div className="absolute right-0 mt-2 w-48 bg-white rounded-lg shadow-lg py-2 z-20">
                                                <Link
                                                    to="/member"
                                                    className="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                                                    onClick={() => setShowUserMenu(false)}
                                                >
                                                    Tài khoản của tôi
                                                </Link>
                                                <Link
                                                    to="/member/profile"
                                                    className="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                                                    onClick={() => setShowUserMenu(false)}
                                                >
                                                    Thông tin cá nhân
                                                </Link>
                                                <Link
                                                    to="/member/orders"
                                                    className="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                                                    onClick={() => setShowUserMenu(false)}
                                                >
                                                    Đơn hàng của tôi
                                                </Link>
                                                {isAdmin && (
                                                    <Link
                                                        to="/admin"
                                                        className="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                                                        onClick={() => setShowUserMenu(false)}
                                                    >
                                                        Quản trị
                                                    </Link>
                                                )}
                                                <hr className="my-2" />
                                                <button
                                                    onClick={handleLogout}
                                                    className="block w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-gray-100"
                                                >
                                                    Đăng xuất
                                                </button>
                                            </div>
                                        </>
                                    )}
                                </div>
                            ) : (
                                <Link
                                    to="/login"
                                    className="flex items-center space-x-1 text-gray-600 hover:text-red-600"
                                >
                                    <UserIcon className="w-6 h-6" />
                                    <span className="text-sm font-medium hidden md:block">Đăng nhập</span>
                                </Link>
                            )}
                        </div>

                        {/* Guest Orders - Show for non-authenticated users */}
                        {!isAuthenticated && (
                            <Link
                                to="/orders/track"
                                className="text-gray-600 hover:text-red-600 relative"
                                title="Đơn hàng của tôi"
                            >
                                <Package className="w-6 h-6" />
                            </Link>
                        )}

                        {/* Shopping Cart */}
                        <Link to="/cart" className="relative text-gray-600 hover:text-red-600" title="Giỏ hàng">
                            <ShoppingBagIcon className="w-6 h-6" />
                            {cartItemCount > 0 && (
                                <span className="absolute -top-2 -right-2 flex items-center justify-center w-5 h-5 text-xs font-medium text-white bg-red-600 rounded-full">
                                    {cartItemCount}
                                </span>
                            )}
                        </Link>
                    </div>
                </div>
            </div>
        </header>
    );
};

const NavigationMenu = () => {
    const { categories, fetchCategories, loaded } = useCategoriesStore()
    const [showCategoryMenu, setShowCategoryMenu] = useState(false)
    const [hoveredCategoryId, setHoveredCategoryId] = useState<string | null>(null)

    useEffect(() => {
        // Only fetch if not already loaded
        if (!loaded) {
            fetchCategories()
        }
    }, [loaded, fetchCategories])

    return (
        <nav className="bg-white border-b border-gray-200 shadow-sm">
            <div className="container mx-auto">
                <div className="flex justify-center items-center h-12 px-4">
                    <ul className="flex space-x-8">
                        {/* Trang chủ */}
                        <li className="flex-shrink-0">
                            <Link
                                to="/"
                                className="text-sm font-semibold text-gray-700 hover:text-red-600 transition-colors duration-200 whitespace-nowrap"
                            >
                                Trang chủ
                            </Link>
                        </li>

                        {/* Categories Mega Menu */}
                        <li
                            className="relative flex-shrink-0"
                            onMouseEnter={() => setShowCategoryMenu(true)}
                            onMouseLeave={() => {
                                setShowCategoryMenu(false)
                                setHoveredCategoryId(null)
                            }}
                        >
                            <span className="flex items-center gap-1 text-sm font-semibold text-gray-700 hover:text-red-600 transition-colors duration-200 cursor-pointer">
                                Danh mục
                                <ChevronDown className="w-4 h-4" />
                            </span>

                            {/* Mega Dropdown Menu */}
                            {showCategoryMenu && categories.length > 0 && (
                                <div className="absolute top-full left-0 mt-0 flex bg-white border border-gray-200 rounded-md shadow-xl z-50">
                                    {/* Left side - Parent Categories */}
                                    <div className="w-56 py-2 border-r border-gray-200">
                                        {categories.map(category => (
                                            <div
                                                key={category.id}
                                                onMouseEnter={() => setHoveredCategoryId(category.id)}
                                            >
                                                <Link
                                                    to={`/categories/${category.slug}`}
                                                    className={`block px-4 py-2 text-sm transition-colors ${hoveredCategoryId === category.id
                                                            ? 'bg-gray-100 text-red-600'
                                                            : 'text-gray-700 hover:bg-gray-50'
                                                        }`}
                                                >
                                                    <div className="flex items-center justify-between">
                                                        <span className="font-medium">{category.name}</span>
                                                        {category.children && category.children.length > 0 && (
                                                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                                                            </svg>
                                                        )}
                                                    </div>
                                                    {category.productsCount !== undefined && category.productsCount > 0 && (
                                                        <span className="text-xs text-gray-500 mt-1">
                                                            {category.productsCount} sản phẩm
                                                        </span>
                                                    )}
                                                </Link>
                                            </div>
                                        ))}
                                    </div>

                                    {/* Right side - Child Categories (subcategories) */}
                                    {hoveredCategoryId && (
                                        <div className="w-64 py-2 px-2">
                                            {categories
                                                .find(cat => cat.id === hoveredCategoryId)
                                                ?.children?.map(subCategory => (
                                                    <Link
                                                        key={subCategory.id}
                                                        to={`/categories/${subCategory.slug}`}
                                                        className="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 hover:text-red-600 rounded transition-colors"
                                                    >
                                                        <div className="font-medium">{subCategory.name}</div>
                                                        {subCategory.productsCount !== undefined && subCategory.productsCount > 0 && (
                                                            <span className="text-xs text-gray-500">
                                                                {subCategory.productsCount} sản phẩm
                                                            </span>
                                                        )}
                                                    </Link>
                                                )) || (
                                                    <div className="px-4 py-3 text-sm text-gray-500 italic">
                                                        Không có danh mục con
                                                    </div>
                                                )}
                                        </div>
                                    )}
                                </div>
                            )}
                        </li>

                        {/* Sản phẩm */}
                        <li className="flex-shrink-0">
                            <Link
                                to="/products"
                                className="text-sm font-semibold text-gray-700 hover:text-red-600 transition-colors duration-200 whitespace-nowrap"
                            >
                                Sản phẩm
                            </Link>
                        </li>

                        {/* Thời Trang Nam */}
                        <li className="flex-shrink-0">
                            <Link
                                to="/products?gender=men"
                                className="text-sm font-semibold text-gray-700 hover:text-red-600 transition-colors duration-200 whitespace-nowrap"
                            >
                                Thời Trang Nam
                            </Link>
                        </li>

                        {/* Thời Trang Nữ */}
                        <li className="flex-shrink-0">
                            <Link
                                to="/products?gender=women"
                                className="text-sm font-semibold text-gray-700 hover:text-red-600 transition-colors duration-200 whitespace-nowrap"
                            >
                                Thời Trang Nữ
                            </Link>
                        </li>

                        <li className="flex-shrink-0">
                            <Link
                                to="/products?gender=unisex"
                                className="text-sm font-semibold text-gray-700 hover:text-red-600 transition-colors duration-200 whitespace-nowrap"
                            >
                                Unisex
                            </Link>
                        </li>

                        {/* Khuyến Mãi */}
                        <li className="flex-shrink-0">
                            <Link
                                to="/sale"
                                className="text-sm font-semibold text-gray-700 hover:text-red-600 transition-colors duration-200 whitespace-nowrap"
                            >
                                Khuyến Mãi
                            </Link>
                        </li>
                    </ul>
                </div>
            </div>
        </nav>
    );
};

// --- Main Header Component ---
export const Header = () => {
    return (
        <div className="sticky top-0 z-50 bg-white">
            <AnnouncementBar />
            <MainHeader />
            <NavigationMenu />
        </div>
    );
};

export default Header;