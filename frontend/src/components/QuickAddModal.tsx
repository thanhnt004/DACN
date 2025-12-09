import { useState, useEffect } from 'react';
import { X, Plus, Minus, ShoppingBag, Loader2 } from 'lucide-react';
import * as ProductsApi from '../api/admin/products';
import { useCartStore } from '../store/cart';
import { toast } from 'react-toastify';

interface QuickAddModalProps {
    productId: string;
    onClose: () => void;
}

export default function QuickAddModal({ productId, onClose }: QuickAddModalProps) {
    const [product, setProduct] = useState<ProductsApi.ProductDetailResponse | null>(null);
    const [loading, setLoading] = useState(true);
    const [selectedSize, setSelectedSize] = useState<string>('');
    const [selectedColor, setSelectedColor] = useState<string>('');
    const [quantity, setQuantity] = useState(1);
    const [selectedImageIndex, setSelectedImageIndex] = useState(0);
    const [addingToCart, setAddingToCart] = useState(false);
    const { addToCart } = useCartStore();

    useEffect(() => {
        const loadProduct = async () => {
            setLoading(true);
            try {
                const data = await ProductsApi.getProductDetail(productId, ['images', 'variants', 'options']);
                setProduct(data);
                // Don't auto-select - user must choose
            } catch (error) {
                console.error('Failed to load product:', error);
                toast.error('Không thể tải thông tin sản phẩm');
                onClose();
            } finally {
                setLoading(false);
            }
        };

        loadProduct();
    }, [productId, onClose]);

    // Auto-switch to variant image when variant is selected
    useEffect(() => {
        const variant = getSelectedVariant();
        if (variant?.image?.imageUrl && product?.images) {
            // Find the index of variant image in the gallery
            const variantImageIndex = product.images.findIndex(img => img.imageUrl === variant.image!.imageUrl);
            if (variantImageIndex !== -1) {
                setSelectedImageIndex(variantImageIndex);
            }
        }
    }, [selectedSize, selectedColor, product]);

    const getSelectedVariant = () => {
        if (!product || !product.variants) return null;
        return product.variants.find(v => v.sizeId === selectedSize && v.colorId === selectedColor);
    };

    const isVariantAvailable = (sizeId: string, colorId: string) => {
        const variant = product?.variants?.find(v => v.sizeId === sizeId && v.colorId === colorId);
        return variant?.inventory?.available && variant.inventory.available > 0;
    };

    const getCurrentPrice = () => {
        const variant = getSelectedVariant();
        return variant?.priceAmount || product?.priceAmount || 0;
    };

    const handleAddToCart = async () => {
        if (!selectedSize || !selectedColor) {
            toast.error('Vui lòng chọn size và màu sắc');
            return;
        }

        const variant = getSelectedVariant();
        if (!variant) {
            toast.error('Không tìm thấy biến thể sản phẩm');
            return;
        }

        if (!variant.inventory?.available || variant.inventory.available < quantity) {
            toast.error('Sản phẩm không đủ số lượng trong kho');
            return;
        }

        setAddingToCart(true);
        try {
            await addToCart(variant.id, quantity);
            toast.success('Đã thêm sản phẩm vào giỏ hàng!');
            onClose();
        } catch (error) {
            console.error('Failed to add to cart:', error);
            toast.error('Không thể thêm vào giỏ hàng');
        } finally {
            setAddingToCart(false);
        }
    };

    const formatPrice = (price: number) => {
        return new Intl.NumberFormat('vi-VN', {
            style: 'currency',
            currency: 'VND'
        }).format(price);
    };

    if (loading) {
        return (
            <div 
                className="fixed inset-0 bg-transparent flex items-center justify-center z-50"
                onClick={onClose}
            >
                <div className="bg-white rounded-lg p-8" onClick={(e) => e.stopPropagation()}>
                    <Loader2 className="w-12 h-12 animate-spin text-red-600 mx-auto" />
                </div>
            </div>
        );
    }

    if (!product) return null;

    const images = product.images || [];
    const mainImage = images[selectedImageIndex]?.imageUrl || product.primaryImageUrl || 'https://placehold.co/400x400';
    const maxQuantity = getSelectedVariant()?.inventory?.available || 99;

    return (
        <div 
            className="fixed inset-0 bg-transparent flex items-center justify-center z-50 p-4"
            onClick={onClose}
        >
            <div 
                className="bg-white rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto"
                onClick={(e) => e.stopPropagation()}
            >
                {/* Header */}
                <div className="sticky top-0 bg-white border-b p-4 flex justify-between items-center">
                    <h3 className="text-lg font-bold">Thêm vào giỏ hàng</h3>
                    <button
                        onClick={onClose}
                        className="text-gray-400 hover:text-gray-600"
                        disabled={addingToCart}
                    >
                        <X className="w-6 h-6" />
                    </button>
                </div>

                {/* Content */}
                <div className="p-6">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        {/* Product Image */}
                        <div className="aspect-square bg-gray-100 rounded-lg overflow-hidden">
                            <img
                                src={mainImage}
                                alt={product.name}
                                className="w-full h-full object-cover"
                            />
                        </div>

                        {/* Product Info */}
                        <div className="space-y-4">
                            <div>
                                <h4 className="text-xl font-bold text-gray-900">{product.name}</h4>
                                <p className="text-2xl font-bold text-red-600 mt-2">
                                    {formatPrice(getCurrentPrice())}
                                </p>
                            </div>

                            {/* Color Selection */}
                            {product.options?.color && product.options.color.length > 0 && (
                                <div>
                                    <label className="block text-sm font-medium text-gray-900 mb-2">
                                        Màu sắc: {selectedColor ? product.options.color.find(c => c.id === selectedColor)?.name : 'Chọn màu sắc'}
                                    </label>
                                    <div className="flex gap-2 flex-wrap">
                                        {product.options.color.map(color => {
                                            const isAvailable = selectedSize ? isVariantAvailable(selectedSize, color.id) : true;
                                            return (
                                                <button
                                                    key={color.id}
                                                    onClick={() => isAvailable && setSelectedColor(color.id)}
                                                    disabled={!isAvailable}
                                                    className={`relative w-10 h-10 rounded-lg border-2 transition ${
                                                        selectedColor === color.id
                                                            ? 'border-red-600 ring-2 ring-red-200'
                                                            : isAvailable
                                                            ? 'border-gray-300 hover:border-gray-400'
                                                            : 'border-gray-200 opacity-40 cursor-not-allowed'
                                                    }`}
                                                    title={color.name}
                                                >
                                                    <div
                                                        className="w-full h-full rounded-md"
                                                        style={{ backgroundColor: color.hexCode }}
                                                    />
                                                    {!isAvailable && (
                                                        <div className="absolute inset-0 flex items-center justify-center">
                                                            <div className="w-full h-0.5 bg-gray-400 rotate-45" />
                                                        </div>
                                                    )}
                                                </button>
                                            );
                                        })}
                                    </div>
                                </div>
                            )}

                            {/* Size Selection */}
                            {product.options?.size && product.options.size.length > 0 && (
                                <div>
                                    <label className="block text-sm font-medium text-gray-900 mb-2">
                                        Kích cỡ: {selectedSize ? product.options.size.find(s => s.id === selectedSize)?.code : 'Chọn kích cỡ'}
                                    </label>
                                    <div className="flex gap-2 flex-wrap">
                                        {product.options.size.map(size => {
                                            const isAvailable = selectedColor ? isVariantAvailable(size.id, selectedColor) : true;
                                            return (
                                                <button
                                                    key={size.id}
                                                    onClick={() => isAvailable && setSelectedSize(size.id)}
                                                    disabled={!isAvailable}
                                                    className={`px-4 py-2 rounded-lg border-2 transition font-medium ${
                                                        selectedSize === size.id
                                                            ? 'border-red-600 bg-red-50 text-red-600'
                                                            : isAvailable
                                                            ? 'border-gray-300 hover:border-gray-400 text-gray-900'
                                                            : 'border-gray-200 text-gray-400 opacity-50 cursor-not-allowed line-through'
                                                    }`}
                                                >
                                                    {size.code}
                                                </button>
                                            );
                                        })}
                                    </div>
                                </div>
                            )}

                            {/* Quantity */}
                            <div>
                                <label className="block text-sm font-medium text-gray-900 mb-2">
                                    Số lượng
                                </label>
                                <div className="flex items-center gap-4">
                                    <div className="flex items-center border-2 border-gray-300 rounded-lg">
                                        <button
                                            onClick={() => setQuantity(Math.max(1, quantity - 1))}
                                            disabled={quantity <= 1}
                                            className="px-3 py-2 hover:bg-gray-100 transition disabled:opacity-50"
                                        >
                                            <Minus className="w-4 h-4" />
                                        </button>
                                        <input
                                            type="number"
                                            value={quantity}
                                            onChange={(e) => {
                                                const val = Math.max(1, Math.min(maxQuantity, parseInt(e.target.value) || 1));
                                                setQuantity(val);
                                            }}
                                            className="w-16 text-center border-x-2 border-gray-300 py-2 focus:outline-none"
                                        />
                                        <button
                                            onClick={() => setQuantity(Math.min(maxQuantity, quantity + 1))}
                                            disabled={quantity >= maxQuantity}
                                            className="px-3 py-2 hover:bg-gray-100 transition disabled:opacity-50"
                                        >
                                            <Plus className="w-4 h-4" />
                                        </button>
                                    </div>
                                    {maxQuantity < 10 && (
                                        <span className="text-sm text-orange-600">
                                            Chỉ còn {maxQuantity} sản phẩm
                                        </span>
                                    )}
                                </div>
                            </div>

                            {/* Add to Cart Button */}
                            <button
                                onClick={handleAddToCart}
                                disabled={addingToCart || !selectedSize || !selectedColor}
                                className="w-full bg-red-600 text-white py-3 rounded-lg font-medium hover:bg-red-700 transition disabled:bg-gray-400 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                            >
                                {addingToCart ? (
                                    <>
                                        <Loader2 className="w-5 h-5 animate-spin" strokeWidth={2} />
                                        <span>Đang thêm...</span>
                                    </>
                                ) : (
                                    <>
                                        <ShoppingBag className="w-5 h-5" strokeWidth={2} />
                                        <span>Thêm vào giỏ hàng</span>
                                    </>
                                )}
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
