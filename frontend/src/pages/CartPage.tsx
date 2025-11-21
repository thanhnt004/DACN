import { useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Header } from '../components/layout/Header'
import Footer from '../components/layout/Footer'
import { useCartStore } from '../store/cart'
import { Minus, Plus, Trash2, ShoppingBag, ArrowRight } from 'lucide-react'

export default function CartPage() {
    const navigate = useNavigate()
    const { cart, loading, fetchCart, removeFromCart, updateCartItem, clearCart } = useCartStore()

    useEffect(() => {
        fetchCart()
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [])

    const handleUpdateQuantity = async (itemId: string, variantId: string, newQuantity: number) => {
        if (newQuantity < 1) return
        try {
            await updateCartItem(itemId, variantId, newQuantity)
        } catch (error) {
            console.error('Failed to update quantity:', error)
        }
    }

    const handleRemoveItem = async (itemId: string) => {
        if (!confirm('Bạn có chắc muốn xóa sản phẩm này khỏi giỏ hàng?')) return
        try {
            await removeFromCart(itemId)
        } catch (error) {
            console.error('Failed to remove item:', error)
        }
    }

    const handleClearCart = async () => {
        if (!confirm('Bạn có chắc muốn xóa tất cả sản phẩm khỏi giỏ hàng?')) return
        try {
            await clearCart()
        } catch (error) {
            console.error('Failed to clear cart:', error)
        }
    }

    const calculateSubtotal = () => {
        if (!cart || !cart.items) return 0
        return cart.items.reduce((sum, item) => sum + (item.unitPriceAmount * item.quantity), 0)
    }

    const calculateTotal = () => {
        const subtotal = calculateSubtotal()
        // Add shipping, tax, discounts here if needed
        return subtotal
    }

    if (loading && !cart) {
        return (
            <div className="min-h-screen flex flex-col">
                <Header />
                <div className="flex-1 flex items-center justify-center">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-red-600"></div>
                </div>
                <Footer />
            </div>
        )
    }

    const isEmpty = !cart || !cart.items || cart.items.length === 0

    return (
        <div className="min-h-screen flex flex-col">
            <Header />

            <div className="flex-1 bg-gray-50 py-8">
                <div className="max-w-7xl mx-auto px-4">
                    <h1 className="text-3xl font-bold mb-8">Giỏ hàng của bạn</h1>

                    {isEmpty ? (
                        <div className="bg-white rounded-lg shadow-sm p-12 text-center">
                            <ShoppingBag className="w-16 h-16 mx-auto text-gray-300 mb-4" />
                            <h2 className="text-2xl font-semibold text-gray-900 mb-2">
                                Giỏ hàng trống
                            </h2>
                            <p className="text-gray-600 mb-6">
                                Bạn chưa có sản phẩm nào trong giỏ hàng
                            </p>
                            <Link
                                to="/products"
                                className="inline-flex items-center gap-2 bg-red-600 text-white px-6 py-3 rounded-lg font-medium hover:bg-red-700 transition"
                            >
                                Tiếp tục mua sắm
                                <ArrowRight className="w-5 h-5" />
                            </Link>
                        </div>
                    ) : (
                        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                            {/* Cart Items */}
                            <div className="lg:col-span-2 space-y-4">
                                <div className="bg-white rounded-lg shadow-sm">
                                    <div className="p-6 border-b flex items-center justify-between">
                                        <h2 className="text-lg font-semibold">
                                            Sản phẩm ({cart.items.length})
                                        </h2>
                                        <button
                                            onClick={handleClearCart}
                                            className="text-sm text-red-600 hover:underline"
                                        >
                                            Xóa tất cả
                                        </button>
                                    </div>

                                    <div className="divide-y">
                                        {cart.items.map((item) => (
                                            <div key={item.id} className="p-6">
                                                <div className="flex gap-4">
                                                    {/* Product Image */}
                                                    <div className="flex-shrink-0">
                                                        <Link to={`/products/${item.productId}`}>
                                                            {item.imageUrl ? (
                                                                <img
                                                                    src={item.imageUrl}
                                                                    alt={item.productName}
                                                                    className="w-24 h-24 object-cover rounded-lg"
                                                                />
                                                            ) : (
                                                                <div className="w-24 h-24 bg-gray-200 rounded-lg flex items-center justify-center">
                                                                    <ShoppingBag className="w-8 h-8 text-gray-400" />
                                                                </div>
                                                            )}
                                                        </Link>
                                                    </div>

                                                    {/* Product Info */}
                                                    <div className="flex-1">
                                                        <Link
                                                            to={`/products/${item.productId}`}
                                                            className="font-medium text-gray-900 hover:text-red-600"
                                                        >
                                                            {item.productName}
                                                        </Link>
                                                        {item.variantName && (
                                                            <p className="text-sm text-gray-600 mt-1">
                                                                {item.variantName}
                                                            </p>
                                                        )}
                                                        <p className="text-lg font-semibold text-red-600 mt-2">
                                                            {item.unitPriceAmount.toLocaleString('vi-VN')} ₫
                                                        </p>

                                                        {/* Stock Status */}
                                                        {!item.stockStatus.inStock && (
                                                            <p className="text-sm text-red-600 mt-1">
                                                                Hết hàng
                                                            </p>
                                                        )}
                                                        {item.stockStatus.inStock && item.quantity > item.stockStatus.availableQuantity && (
                                                            <p className="text-sm text-orange-600 mt-1">
                                                                Chỉ còn {item.stockStatus.availableQuantity} sản phẩm
                                                            </p>
                                                        )}
                                                    </div>

                                                    {/* Quantity & Actions */}
                                                    <div className="flex flex-col items-end gap-4">
                                                        <button
                                                            onClick={() => handleRemoveItem(item.id)}
                                                            className="text-gray-400 hover:text-red-600"
                                                            title="Xóa sản phẩm"
                                                        >
                                                            <Trash2 className="w-5 h-5" />
                                                        </button>

                                                        <div className="flex items-center border-2 border-gray-300 rounded-lg">
                                                            <button
                                                                onClick={() =>
                                                                    handleUpdateQuantity(
                                                                        item.id,
                                                                        item.variantId,
                                                                        item.quantity - 1
                                                                    )
                                                                }
                                                                disabled={item.quantity <= 1}
                                                                className="px-3 py-2 hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed"
                                                            >
                                                                <Minus className="w-4 h-4" />
                                                            </button>
                                                            <span className="px-4 py-2 font-medium min-w-[3rem] text-center">
                                                                {item.quantity}
                                                            </span>
                                                            <button
                                                                onClick={() =>
                                                                    handleUpdateQuantity(
                                                                        item.id,
                                                                        item.variantId,
                                                                        item.quantity + 1
                                                                    )
                                                                }
                                                                disabled={!item.stockStatus.inStock || item.quantity >= item.stockStatus.availableQuantity}
                                                                className="px-3 py-2 hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed"
                                                            >
                                                                <Plus className="w-4 h-4" />
                                                            </button>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                </div>

                                {/* Continue Shopping */}
                                <Link
                                    to="/products"
                                    className="inline-flex items-center gap-2 text-red-600 hover:underline font-medium"
                                >
                                    ← Tiếp tục mua sắm
                                </Link>
                            </div>

                            {/* Order Summary */}
                            <div className="lg:col-span-1">
                                <div className="bg-white rounded-lg shadow-sm p-6 sticky top-4">
                                    <h2 className="text-lg font-semibold mb-4">Tóm tắt đơn hàng</h2>

                                    <div className="space-y-3 mb-4">
                                        <div className="flex justify-between text-gray-600">
                                            <span>Tạm tính</span>
                                            <span>{calculateSubtotal().toLocaleString('vi-VN')} ₫</span>
                                        </div>
                                        <div className="flex justify-between text-gray-600">
                                            <span>Phí vận chuyển</span>
                                            <span>Miễn phí</span>
                                        </div>
                                    </div>

                                    <div className="border-t pt-4 mb-6">
                                        <div className="flex justify-between text-lg font-semibold">
                                            <span>Tổng cộng</span>
                                            <span className="text-red-600">
                                                {calculateTotal().toLocaleString('vi-VN')} ₫
                                            </span>
                                        </div>
                                    </div>

                                    <button
                                        onClick={() => navigate('/checkout')}
                                        className="w-full bg-red-600 text-white py-4 rounded-lg font-medium hover:bg-red-700 transition"
                                    >
                                        Tiến hành thanh toán
                                    </button>

                                    <div className="mt-4 space-y-2 text-sm text-gray-600">
                                        <p className="flex items-start gap-2">
                                            <span className="text-green-600">✓</span>
                                            Miễn phí vận chuyển cho đơn hàng trên 599.000₫
                                        </p>
                                        <p className="flex items-start gap-2">
                                            <span className="text-green-600">✓</span>
                                            Đổi trả miễn phí trong 30 ngày
                                        </p>
                                        <p className="flex items-start gap-2">
                                            <span className="text-green-600">✓</span>
                                            Thanh toán an toàn & bảo mật
                                        </p>
                                    </div>
                                </div>
                            </div>
                        </div>
                    )}
                </div>
            </div>

            <Footer />
        </div>
    )
}
