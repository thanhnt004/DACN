import { useState, useEffect, useCallback, useRef, useMemo } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { toast } from 'react-toastify'
import { Header } from '../components/layout/Header'
import Footer from '../components/layout/Footer'
import { useCartStore } from '../store/cart'
import * as CheckoutApi from '../api/checkout'
import * as ProfileApi from '../api/profile'
import * as DiscountsApi from '../api/discounts'
import type { Address } from '../api/profile'
import { formatInstant } from '../lib/dateUtils'
import { useAuthStore } from '../store/auth'
import { Plus, X, Ticket, Search, Tag, Calendar, Percent } from 'lucide-react'
import { useGhnLocationSelector } from '../hooks/useGhnLocationSelector'
import type { ProvinceOption, DistrictOption, WardOption } from '../api/location'
import { extractProblemMessage } from '../lib/problemDetails'

export default function CheckoutPage() {
    const navigate = useNavigate()
    const location = useLocation()
    const { cart, loading: cartLoading, fetchCart } = useCartStore()
    const { isAuthenticated } = useAuthStore()

    const [sessionId, setSessionId] = useState<string | null>(null)
    const [sessionToken, setSessionToken] = useState<string | null>(null)
    const [sessionData, setSessionData] = useState<CheckoutApi.CheckoutSession | null>(null)
    const [initializing, setInitializing] = useState(true)
    const initializingRef = useRef(false)

    const idempotencyKeyRef = useRef<string | null>(null)
    const ensureIdempotencyKey = useCallback((): string => {
        if (!idempotencyKeyRef.current) {
            idempotencyKeyRef.current = CheckoutApi.createIdempotencyKey()
        }
        return idempotencyKeyRef.current
    }, [])
    const memberAddressSignatureRef = useRef<string | null>(null)
    const guestAddressSignatureRef = useRef<string | null>(null)

    // User Address State
    const [addresses, setAddresses] = useState<Address[]>([])
    const [selectedAddressId, setSelectedAddressId] = useState<string>('')
    const [showAddAddressModal, setShowAddAddressModal] = useState(false)
    const [showAddressListModal, setShowAddressListModal] = useState(false)
    const [newAddressForm, setNewAddressForm] = useState<Omit<Address, 'id'>>({
        fullName: '',
        phone: '',
        line1: '',
        line2: '',
        ward: '',
        district: '',
        province: '',
        isDefaultShipping: false
    })
    const [addingAddress, setAddingAddress] = useState(false)

    // Voucher State
    const [voucherCode, setVoucherCode] = useState('')
    const [applyingVoucher, setApplyingVoucher] = useState(false)
    const [showDiscountModal, setShowDiscountModal] = useState(false)
    const [availableDiscounts, setAvailableDiscounts] = useState<DiscountsApi.DiscountResponse[]>([])
    const [loadingDiscounts, setLoadingDiscounts] = useState(false)
    const [discountSearchTerm, setDiscountSearchTerm] = useState('')

    const [formData, setFormData] = useState({
        customerName: '',
        customerPhone: '',
        line1: '',
        ward: '',
        district: '',
        province: '',
        paymentMethod: '',
        note: ''
    })
    const [submitting, setSubmitting] = useState(false)
    const [paymentMethods, setPaymentMethods] = useState<CheckoutApi.PaymentMethodResponse[]>([])
    const [addressUpdating, setAddressUpdating] = useState(false)
    const [paymentUpdating, setPaymentUpdating] = useState(false)
    const { customerName, customerPhone, line1, ward, district, province } = formData

    const updateGuestLocation = useCallback((updates: Partial<Record<'province' | 'district' | 'ward', string>>) => {
        setFormData(prev => ({ ...prev, ...updates }))
    }, [])

    const guestLocationSelector = useGhnLocationSelector(updateGuestLocation)
    const {
        provinceOptions: guestProvinceOptions,
        districtOptions: guestDistrictOptions,
        wardOptions: guestWardOptions,
        selectedProvinceId: guestSelectedProvinceId,
        selectedDistrictId: guestSelectedDistrictId,
        selectedWardCode: guestSelectedWardCode,
        handleProvinceChange: handleGuestProvinceChange,
        handleDistrictChange: handleGuestDistrictChange,
        handleWardChange: handleGuestWardChange,
        initializeFromNames: initializeGuestLocation,
        resetSelections: resetGuestLocation
    } = guestLocationSelector

    const updateModalLocation = useCallback((updates: Partial<Record<'province' | 'district' | 'ward', string>>) => {
        setNewAddressForm(prev => ({ ...prev, ...updates }))
    }, [])

    const modalLocationSelector = useGhnLocationSelector(updateModalLocation)
    const {
        provinceOptions: modalProvinceOptions,
        districtOptions: modalDistrictOptions,
        wardOptions: modalWardOptions,
        selectedProvinceId: modalSelectedProvinceId,
        selectedDistrictId: modalSelectedDistrictId,
        selectedWardCode: modalSelectedWardCode,
        handleProvinceChange: handleModalProvinceChange,
        handleDistrictChange: handleModalDistrictChange,
        handleWardChange: handleModalWardChange,
        initializeFromNames: initializeModalLocation,
        resetSelections: resetModalLocation
    } = modalLocationSelector

    const syncPaymentState = useCallback((session: CheckoutApi.CheckoutSession) => {
        const available = session.availablePaymentMethods || []
        setPaymentMethods(available)
        setFormData(prev => {
            const hasValidSelection = prev.paymentMethod && available.some(method => {
                return method.id === prev.paymentMethod && (method.isAvailable ?? true)
            })
            const fallbackId = session.selectedPaymentMethod?.id
                ?? available.find(method => method.isAvailable ?? true)?.id
                ?? ''
            const nextId = hasValidSelection ? prev.paymentMethod : fallbackId
            if (nextId === prev.paymentMethod) {
                return prev
            }
            return { ...prev, paymentMethod: nextId }
        })
    }, [setPaymentMethods, setFormData])

    const selectedAddress = useMemo(() => {
        if (!selectedAddressId) return undefined
        return addresses.find(addr => String(addr.id) === String(selectedAddressId))
    }, [addresses, selectedAddressId])

    const persistAddress = useCallback(async (address: CheckoutApi.UpdateAddressRequest) => {
        if (!sessionId || !sessionToken) {
            return null
        }
        const updatedSession = await CheckoutApi.updateAddress(sessionId, sessionToken, address)
        setSessionData(updatedSession)
        syncPaymentState(updatedSession)
        return updatedSession
    }, [sessionId, sessionToken, syncPaymentState])

    const openAddAddressModal = useCallback(() => {
        setShowAddAddressModal(true)
        void initializeModalLocation(
            newAddressForm.province,
            newAddressForm.district,
            newAddressForm.ward
        )
    }, [initializeModalLocation, newAddressForm.province, newAddressForm.district, newAddressForm.ward])

    const closeAddAddressModal = useCallback(() => {
        setShowAddAddressModal(false)
        resetModalLocation()
    }, [resetModalLocation])

    const loadAddresses = useCallback(() => {
        if (!isAuthenticated) return
        ProfileApi.getAddresses().then(data => {
            setAddresses(data)
            const defaultAddr = data.find(a => a.isDefaultShipping)
            if (defaultAddr?.id) {
                setSelectedAddressId(defaultAddr.id)
            } else if (data.length > 0 && data[0]?.id) {
                setSelectedAddressId(prev => prev || data[0].id!)
            }
        }).catch(console.error)
    }, [isAuthenticated])

    const handleAddAddress = async () => {
        if (!newAddressForm.fullName || !newAddressForm.phone || !newAddressForm.line1 || 
            !newAddressForm.province || !newAddressForm.district || !newAddressForm.ward) {
            toast.warning('Vui lòng điền đầy đủ thông tin địa chỉ')
            return
        }

        setAddingAddress(true)
        try {
            const newAddr = await ProfileApi.addAddress(newAddressForm)
            setAddresses(prev => [...prev, newAddr])
            if (newAddr.id) {
                setSelectedAddressId(newAddr.id)
            }
            
            // Reset form
            setNewAddressForm({
                fullName: '',
                phone: '',
                line1: '',
                line2: '',
                ward: '',
                district: '',
                province: '',
                isDefaultShipping: false
            })
            
            closeAddAddressModal()
            toast.success('Thêm địa chỉ thành công')
        } catch (error) {
            console.error('Failed to add address:', error)
            toast.error('Không thể thêm địa chỉ, vui lòng thử lại')
        } finally {
            setAddingAddress(false)
        }
    }

    // Load user addresses
    useEffect(() => {
        loadAddresses()
    }, [loadAddresses])

    useEffect(() => {
        if (!sessionId || !sessionToken) return

        let isCancelled = false
        const updateAddress = async () => {
            setAddressUpdating(true)
            try {
                let signature: string | null = null
                let payload: CheckoutApi.UpdateAddressRequest | null = null

                if (isAuthenticated && selectedAddress) {
                    payload = {
                        id: selectedAddress.id,
                        fullName: selectedAddress.fullName,
                        phone: selectedAddress.phone,
                        line1: selectedAddress.line1,
                        ward: selectedAddress.ward,
                        district: selectedAddress.district,
                        province: selectedAddress.province,
                        ...(selectedAddress.line2 && { line2: selectedAddress.line2 })
                    }
                    signature = JSON.stringify(payload)
                    if (memberAddressSignatureRef.current === signature) return
                } else if (!isAuthenticated && customerName && customerPhone && line1 && province && district && ward) {
                    payload = {
                        fullName: customerName,
                        phone: customerPhone,
                        line1,
                        ward,
                        district,
                        province,
                    }
                    signature = JSON.stringify(payload)
                    if (guestAddressSignatureRef.current === signature) return
                }

                if (payload && signature) {
                    const result = await persistAddress(payload)
                    if (isCancelled) return

                    if (result) {
                        if (isAuthenticated) {
                            memberAddressSignatureRef.current = signature
                        } else {
                            guestAddressSignatureRef.current = signature
                        }
                    }
                }
            } catch (error) {
                if (isCancelled) return
                console.error('Failed to update shipping address:', error)
                toast.error('Không thể cập nhật địa chỉ giao hàng, vui lòng thử lại.')
            } finally {
                if (!isCancelled) {
                    setAddressUpdating(false)
                }
            }
        }

        updateAddress()

        return () => {
            isCancelled = true
        }
    }, [isAuthenticated, persistAddress, selectedAddress, sessionId, sessionToken, customerName, customerPhone, line1, ward, district, province])

    useEffect(() => {
        idempotencyKeyRef.current = null
        memberAddressSignatureRef.current = null
        guestAddressSignatureRef.current = null
    }, [sessionId])

    const initSession = useCallback(async (mounted: { value: boolean }) => {
        // Prevent multiple simultaneous initializations
        if (initializingRef.current) {
            console.log('Session initialization already in progress, skipping...');
            return;
        }

        console.log('[DEBUG] Starting session initialization...');
        initializingRef.current = true;
        const buyNowItem = location.state?.buyNowItem;
        const selectedItems = location.state?.selectedItems;
        console.log('[DEBUG] buyNowItem:', buyNowItem);
        console.log('[DEBUG] selectedItems:', selectedItems);

        const getSessionPayload = (): CheckoutApi.CheckoutSessionCreateRequest | null => {
            if (buyNowItem) {
                return {
                    items: [{
                        variantId: buyNowItem.variantId,
                        quantity: buyNowItem.quantity
                    }]
                };
            }
            if (cart?.id && cart.items.length > 0) {
                // Use selectedItems if provided from cart page, otherwise use all cart items
                const itemsToCheckout = selectedItems && selectedItems.length > 0 
                    ? cart.items.filter(item => selectedItems.includes(item.id))
                    : cart.items;
                    
                return {
                    cartId: cart.id,
                    items: itemsToCheckout.map(item => ({
                        cartItemId: item.id,
                        variantId: item.variantId,
                        quantity: item.quantity
                    }))
                };
            }
            return null;
        };

        const payload = getSessionPayload();
        if (!payload) {
            if (mounted.value) {
                setInitializing(false);
                initializingRef.current = false;
            }
            return;
        }

        try {
            console.log('[DEBUG] Calling createSession with payload:', payload);
            const session = await CheckoutApi.createSession(payload);
            console.log('[DEBUG] Session created successfully:', session);
            
            // Always set session state even if unmounted - this is critical
            console.log('[DEBUG] Setting session state...');
            setSessionId(session.id);
            setSessionToken(session.sessionToken);
            setSessionData(session);
            syncPaymentState(session);

            if (mounted.value) {
                if (session.shippingAddress) {
                    setFormData(prev => ({
                        ...prev,
                        customerName: session.shippingAddress?.fullName || prev.customerName,
                        customerPhone: session.shippingAddress?.phone || prev.customerPhone,
                        line1: session.shippingAddress?.line1 || prev.line1,
                        ward: session.shippingAddress?.ward || prev.ward,
                        district: session.shippingAddress?.district || prev.district,
                        province: session.shippingAddress?.province || prev.province
                    }));
                    void initializeGuestLocation(
                        session.shippingAddress?.province,
                        session.shippingAddress?.district,
                        session.shippingAddress?.ward
                    );
                } else {
                    resetGuestLocation();
                }
            }
        } catch (error) {
            console.error('Failed to init checkout session:', error);
            toast.error('Không thể khởi tạo phiên thanh toán');
        } finally {
            console.log('[DEBUG] Finally block - mounted:', mounted.value);
            // Always set initializing to false
            console.log('[DEBUG] Setting initializing to false');
            setInitializing(false);
            initializingRef.current = false;
            console.log('[DEBUG] Session initialization complete');
        }
    }, [location.state, cart, syncPaymentState, initializeGuestLocation, resetGuestLocation]);

    useEffect(() => {
        // Only initialize once when we don't have a session
        if (sessionId) {
            return;
        }

        const mounted = { value: true };
        const hasBuyNowItem = !!location.state?.buyNowItem;

        // Priority 1: BuyNow item (direct purchase) - Don't need cart
        if (hasBuyNowItem) {
            initSession(mounted);
            return () => { mounted.value = false; };
        }

        // For cart checkout flow only (no buyNowItem)
        if (!hasBuyNowItem) {
            // Priority 2: Cart has items - init checkout
            if (cart?.items && cart.items.length > 0) {
                initSession(mounted);
                return () => { mounted.value = false; };
            }

            // Priority 3: Cart not loaded yet - fetch it
            if (!cartLoading && !cart) {
                fetchCart();
                return () => { mounted.value = false; };
            }

            // Priority 4: Cart loaded but empty - stop loading
            if (cart !== undefined && !cartLoading) {
                setInitializing(false);
            }
        }

        return () => {
            mounted.value = false;
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [sessionId, location.state?.buyNowItem, cart, cartLoading]);

    // This useEffect is no longer needed as the session pre-fill handles this.
    // useEffect(() => {
    //     if (user && !formData.customerName) {
    //         setFormData(prev => ({
    //             ...prev,
    //             customerName: (user as any).fullName || '',
    //             customerPhone: (user as any).phoneNumber || ''
    //         }))
    //     }
    // }, [user, formData.customerName])

    const handlePaymentMethodChange = useCallback(async (methodId: string) => {
        if (paymentUpdating) return

        const previousMethod = formData.paymentMethod
        if (methodId === previousMethod) return

        const method = paymentMethods.find(item => item.id === methodId)
        if (!method || method.isAvailable === false) return

        setFormData(prev => ({ ...prev, paymentMethod: methodId }))

        if (!sessionId || !sessionToken) {
            return
        }

        setPaymentUpdating(true)
        try {
            const updatedSession = await CheckoutApi.updatePaymentMethod(sessionId, sessionToken, {
                paymentMethodId: methodId
            })
            setSessionData(updatedSession)
            syncPaymentState(updatedSession)
        } catch (error) {
            console.error('Failed to update payment method:', error)
            toast.error('Không thể cập nhật phương thức thanh toán, vui lòng thử lại.')
            setFormData(prev => ({ ...prev, paymentMethod: previousMethod }))
        } finally {
            setPaymentUpdating(false)
        }
    }, [formData.paymentMethod, paymentMethods, paymentUpdating, sessionId, sessionToken, syncPaymentState])

    const handleSubmit = async (e?: React.FormEvent | React.MouseEvent) => {
        if (e) e.preventDefault()
        if (!sessionId || !sessionToken) {
            toast.error('Phiên thanh toán không hợp lệ')
            return
        }

        if (!formData.paymentMethod) {
            toast.warning('Vui lòng chọn phương thức thanh toán')
            return
        }

        const selectedMethod = paymentMethods.find(method => method.id === formData.paymentMethod)
        if (!selectedMethod) {
            toast.error('Phương thức thanh toán không khả dụng')
            return
        }

        if (selectedMethod.isAvailable === false) {
            toast.error(selectedMethod.unavailableReason || 'Phương thức thanh toán không khả dụng')
            return
        }

        if (addressUpdating || paymentUpdating) {
            toast.info('Thông tin đang được cập nhật, vui lòng đợi')
            return
        }

        if (applyingVoucher) {
            toast.info('Đang áp dụng mã giảm giá, vui lòng đợi')
            return
        }

        if (isAuthenticated) {
            if (!selectedAddress) {
                toast.warning('Vui lòng chọn địa chỉ giao hàng')
                return
            }
        } else {
            if (!customerName || !customerPhone || !line1 || !province || !district || !ward) {
                toast.warning('Vui lòng điền đầy đủ thông tin giao hàng')
                return
            }
        }

        // Validate sessionId and sessionToken
        if (!sessionId || !sessionToken) {
            console.error('[CHECKOUT ERROR] Missing sessionId or sessionToken:', { sessionId, sessionToken });
            toast.error('Phiên thanh toán không hợp lệ. Vui lòng thử lại.');
            return;
        }

        console.log('[CHECKOUT] Confirming order with:', { sessionId, sessionToken, idempotencyKey: ensureIdempotencyKey() });

        setSubmitting(true)
        try {
            const order = await CheckoutApi.confirmCheckout(
                sessionId,
                sessionToken,
                ensureIdempotencyKey(),
                formData.note
            )

            // Refresh cart (should be empty now)
            await fetchCart()

            toast.success(`Đặt hàng thành công! Mã đơn hàng: ${order.orderNumber}`)

            idempotencyKeyRef.current = null

            if (!isAuthenticated) {
                resetGuestLocation()
            }

            if (order.paymentUrl) {
                localStorage.setItem('pendingOrderId', order.id);
                window.location.href = order.paymentUrl
            } else {
                navigate('/member/orders')
            }
        } catch (error) {
            console.error('Checkout error:', error)
            const responseData = typeof error === 'object' && error && 'response' in error
                ? (error as { response?: { data?: unknown } }).response?.data
                : undefined
            console.error('[CHECKOUT ERROR] Response data:', responseData);
            const message = extractProblemMessage(responseData, 'Lỗi đặt hàng')
            toast.error(message)
        } finally {
            setSubmitting(false)
        }
    }

    const handleApplyVoucher = async () => {
        if (!sessionId || !sessionToken) return
        if (!voucherCode.trim()) {
            toast.warning('Vui lòng nhập mã giảm giá')
            return
        }

        setApplyingVoucher(true)
        try {
            const updatedSession = await CheckoutApi.updateDiscount(sessionId, sessionToken, voucherCode)
            setSessionData(updatedSession)
            syncPaymentState(updatedSession)
            toast.success('Áp dụng mã giảm giá thành công')
            setShowDiscountModal(false)
        } catch (error) {
            console.error('Failed to apply voucher:', error)
            toast.error('Mã giảm giá không hợp lệ hoặc đã hết hạn')
            setVoucherCode('')
        } finally {
            setApplyingVoucher(false)
        }
    }

    const handleVoucherInputFocus = async () => {
        if (showDiscountModal) return // Đã mở rồi thì không gọi lại
        
        setShowDiscountModal(true)
        setLoadingDiscounts(true)
        try {
            // Lấy product IDs từ cart hoặc buy now item
            const productIds: string[] = []
            if (location.state?.buyNowItem) {
                productIds.push(location.state.buyNowItem.productId)
            } else if (cart?.items) {
                productIds.push(...cart.items.map(item => item.productId))
            }
            
            const discounts = await DiscountsApi.getAvailableForProducts(productIds)
            setAvailableDiscounts(discounts)
        } catch (error) {
            console.error('Failed to load discounts:', error)
            toast.error('Không thể tải danh sách mã giảm giá')
        } finally {
            setLoadingDiscounts(false)
        }
    }

    const handleSearchDiscounts = async () => {
        if (!discountSearchTerm.trim()) {
            // Reload all if search is empty
            handleOpenDiscountModal()
            return
        }
        
        setLoadingDiscounts(true)
        try {
            const response = await DiscountsApi.searchDiscounts(discountSearchTerm)
            setAvailableDiscounts(response.content)
        } catch (error) {
            console.error('Failed to search discounts:', error)
            toast.error('Không thể tìm kiếm mã giảm giá')
        } finally {
            setLoadingDiscounts(false)
        }
    }

    const handleSelectDiscount = (code: string) => {
        setVoucherCode(code)
        setShowDiscountModal(false)
    }

    const formatDiscountValue = (discount: DiscountsApi.DiscountResponse) => {
        if (discount.discountType === 'PERCENTAGE') {
            return `Giảm ${discount.discountValue}%${discount.maxDiscountAmount ? ` (tối đa ${discount.maxDiscountAmount.toLocaleString()}đ)` : ''}`
        } else {
            return `Giảm ${discount.discountValue.toLocaleString()}đ`
        }
    }

    const isDiscountValid = (discount: DiscountsApi.DiscountResponse) => {
        const now = new Date()
        const endDate = new Date(discount.endDate)
        return discount.isActive && endDate > now && 
               (!discount.usageLimit || discount.usageCount < discount.usageLimit)
    }

    const calculateTotal = () => {
        if (sessionData) return sessionData.totalAmount
        if (location.state?.buyNowItem) {
            return 0
        }
        if (!cart || !cart.items) return 0
        return cart.items.reduce((sum, item) => sum + (item.unitPriceAmount * item.quantity), 0)
    }





    const hasBuyNowItem = location.state?.buyNowItem
    const hasCartItems = cart && cart.items && cart.items.length > 0

    // Show loading only when:
    // 1. Still initializing AND no session created yet
    // 2. For cart flow (not buyNow), also check cartLoading
    const isLoading = (initializing && !sessionId) || (!hasBuyNowItem && cartLoading && !sessionId)
    
    console.log('[DEBUG] Render state:', { 
        initializing, 
        sessionId, 
        cartLoading, 
        hasBuyNowItem, 
        isLoading,
        hasCartItems 
    });
    
    if (isLoading) {
        console.log('[DEBUG] Showing loading screen');
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

    console.log('[DEBUG] Not loading, checking cart...');

    // Allow checkout if there's a buyNow item or if cart has items
    if (!hasBuyNowItem && !hasCartItems) {
        return (
            <div className="min-h-screen flex flex-col">
                <Header />
                <div className="flex-1 flex flex-col items-center justify-center p-8">
                    <h2 className="text-2xl font-bold mb-4">Giỏ hàng trống</h2>
                    <button
                        onClick={() => navigate('/products')}
                        className="bg-red-600 text-white px-6 py-2 rounded hover:bg-red-700"
                    >
                        Tiếp tục mua sắm
                    </button>
                </div>
                <Footer />
            </div>
        )
    }

    return (
        <div className="min-h-screen flex flex-col bg-gray-50">
            <Header />

            <div className="flex-1 max-w-7xl mx-auto w-full px-4 py-8">
                <h1 className="text-3xl font-bold mb-8">Thanh toán</h1>

                <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                    {/* Checkout Form */}
                    <div className="lg:col-span-2">
                        <div className="bg-white rounded-lg shadow-sm p-6 mb-6">
                            <h2 className="text-xl font-semibold mb-4">Thông tin giao hàng</h2>

                            {isAuthenticated ? (
                                <div className="bg-white p-6 rounded-lg border border-gray-200 shadow-sm">
                                    {addresses.length > 0 && selectedAddressId ? (
                                        (() => {
                                            const addr = addresses.find(a => String(a.id) === String(selectedAddressId))
                                            if (!addr) return (
                                                <div className="text-center py-4">
                                                    <p className="text-gray-500 mb-2">Vui lòng chọn địa chỉ giao hàng</p>
                                                    <button
                                                        onClick={() => setShowAddressListModal(true)}
                                                        className="text-red-600 font-medium hover:underline"
                                                    >
                                                        Chọn từ sổ địa chỉ
                                                    </button>
                                                </div>
                                            )
                                            return (
                                                <>
                                                    <div className="flex justify-between mb-6 pb-4 border-b border-gray-100">
                                                        <div>
                                                            <p className="text-xs text-gray-500 uppercase mb-1 font-semibold">Tên người nhận</p>
                                                            <p className="font-medium text-lg text-gray-900">{addr.fullName}</p>
                                                        </div>
                                                        <div className="text-right">
                                                            <p className="text-xs text-gray-500 uppercase mb-1 font-semibold">SĐT người nhận</p>
                                                            <p className="font-medium text-lg text-gray-900">{addr.phone}</p>
                                                        </div>
                                                    </div>

                                                    <div
                                                        onClick={() => setShowAddressListModal(true)}
                                                        className="flex items-start gap-3 cursor-pointer group hover:opacity-80 transition-opacity"
                                                    >
                                                        <div className="mt-1">
                                                            <input
                                                                type="radio"
                                                                checked={true}
                                                                readOnly
                                                                className="w-5 h-5 text-red-600 border-gray-300 focus:ring-red-500 cursor-pointer"
                                                            />
                                                        </div>
                                                        <div className="flex-1">
                                                            <div className="flex flex-wrap items-center gap-2 mb-1">
                                                                <span className="font-medium text-gray-900 text-lg">
                                                                    {addr.line1}
                                                                </span>
                                                                <span className="px-2 py-0.5 rounded border border-blue-200 text-blue-600 text-xs bg-blue-50 font-medium">
                                                                    Nhà riêng
                                                                </span>
                                                                {addr.isDefaultShipping && (
                                                                    <span className="px-2 py-0.5 rounded border border-red-200 text-red-600 text-xs bg-red-50 font-medium">
                                                                        Mặc định
                                                                    </span>
                                                                )}
                                                            </div>
                                                            <p className="text-gray-600">
                                                                {addr.ward}, {addr.district}, {addr.province}
                                                            </p>
                                                        </div>
                                                    </div>

                                                    <div className="mt-6 pt-4 border-t border-gray-100">
                                                        <span className="text-gray-600">hoặc </span>
                                                        <button
                                                            type="button"
                                                            onClick={openAddAddressModal}
                                                            className="text-red-600 hover:underline font-medium"
                                                        >
                                                            nhập địa chỉ mới
                                                        </button>
                                                    </div>
                                                </>
                                            )
                                        })()
                                    ) : (
                                        <div className="text-center py-6">
                                            <p className="text-gray-500 mb-4">Bạn chưa có địa chỉ giao hàng nào.</p>
                                            <button
                                                type="button"
                                                onClick={openAddAddressModal}
                                                className="inline-flex items-center justify-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-red-600 hover:bg-red-700"
                                            >
                                                <Plus className="w-4 h-4 mr-2" /> Thêm địa chỉ mới
                                            </button>
                                        </div>
                                    )}
                                </div>
                            ) : (
                                <div className="space-y-4">
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-1">Họ và tên</label>
                                        <input
                                            type="text"
                                            required
                                            value={formData.customerName}
                                            onChange={e => setFormData({ ...formData, customerName: e.target.value })}
                                            className="w-full border rounded-md px-3 py-2 focus:ring-red-500 focus:border-red-500"
                                            placeholder="Nguyễn Văn A"
                                        />
                                    </div>
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-1">Số điện thoại</label>
                                        <input
                                            type="tel"
                                            required
                                            value={formData.customerPhone}
                                            onChange={e => setFormData({ ...formData, customerPhone: e.target.value })}
                                            className="w-full border rounded-md px-3 py-2 focus:ring-red-500 focus:border-red-500"
                                            placeholder="0901234567"
                                        />
                                    </div>
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-1">Địa chỉ chi tiết</label>
                                        <input
                                            type="text"
                                            required
                                            value={formData.line1}
                                            onChange={e => setFormData({ ...formData, line1: e.target.value })}
                                            className="w-full border rounded-md px-3 py-2 focus:ring-red-500 focus:border-red-500"
                                            placeholder="Số nhà, tên đường"
                                        />
                                    </div>
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-1">Tỉnh/Thành phố</label>
                                        <select
                                            required
                                            value={guestSelectedProvinceId}
                                            onChange={(e) => void handleGuestProvinceChange(e.target.value)}
                                            className="w-full border rounded-md px-3 py-2 focus:ring-red-500 focus:border-red-500"
                                        >
                                            <option value="">Chọn tỉnh/Thành phố</option>
                                            {guestProvinceOptions.map((province: ProvinceOption) => (
                                                <option key={province.id} value={province.id}>
                                                    {province.name}
                                                </option>
                                            ))}
                                        </select>
                                    </div>
                                    <div className="grid grid-cols-2 gap-4">
                                        <div>
                                            <label className="block text-sm font-medium text-gray-700 mb-1">Quận/Huyện</label>
                                            <select
                                                required
                                                value={guestSelectedDistrictId}
                                                onChange={(e) => void handleGuestDistrictChange(e.target.value)}
                                                className="w-full border rounded-md px-3 py-2 focus:ring-red-500 focus:border-red-500"
                                                disabled={!guestSelectedProvinceId}
                                            >
                                                <option value="">Chọn quận/Huyện</option>
                                                {guestDistrictOptions.map((district: DistrictOption) => (
                                                    <option key={district.id} value={district.id}>
                                                        {district.name}
                                                    </option>
                                                ))}
                                            </select>
                                        </div>
                                        <div>
                                            <label className="block text-sm font-medium text-gray-700 mb-1">Phường/Xã</label>
                                            <select
                                                required
                                                value={guestSelectedWardCode}
                                                onChange={(e) => handleGuestWardChange(e.target.value)}
                                                className="w-full border rounded-md px-3 py-2 focus:ring-red-500 focus:border-red-500"
                                                disabled={!guestSelectedDistrictId}
                                            >
                                                <option value="">Chọn phường/Xã</option>
                                                {guestWardOptions.map((ward: WardOption) => (
                                                    <option key={ward.code} value={ward.code}>
                                                        {ward.name}
                                                    </option>
                                                ))}
                                            </select>
                                        </div>
                                    </div>
                                </div>
                            )}

                            <div className="mt-4">
                                <label className="block text-sm font-medium text-gray-700 mb-1">Ghi chú đơn hàng (tùy chọn)</label>
                                <textarea
                                    rows={2}
                                    value={formData.note}
                                    onChange={e => setFormData({ ...formData, note: e.target.value })}
                                    className="w-full border rounded-md px-3 py-2 focus:ring-red-500 focus:border-red-500"
                                    placeholder="Ví dụ: Giao hàng trong giờ hành chính"
                                />
                            </div>
                        </div>

                        <div className="bg-white rounded-lg shadow-sm p-6">
                            <h2 className="text-xl font-semibold mb-4">Phương thức thanh toán</h2>
                            <div className="space-y-3">
                                {paymentMethods.length === 0 ? (
                                    <p className="text-sm text-gray-500">Không có phương thức thanh toán khả dụng. Vui lòng thử lại sau.</p>
                                ) : (
                                    paymentMethods.map(method => {
                                        const isDisabled = method.isAvailable === false
                                        const isSelected = formData.paymentMethod === method.id
                                        return (
                                            <label
                                                key={method.id}
                                                className={`flex items-center p-4 border rounded-lg transition hover:bg-gray-50 ${isDisabled ? 'opacity-60 cursor-not-allowed hover:bg-white' : 'cursor-pointer'}`}
                                            >
                                                <input
                                                    type="radio"
                                                    name="paymentMethod"
                                                    value={method.id}
                                                    checked={isSelected}
                                                    onChange={() => { void handlePaymentMethodChange(method.id) }}
                                                    className="h-4 w-4 text-red-600 focus:ring-red-500"
                                                    disabled={isDisabled || paymentUpdating}
                                                />
                                                <div className="ml-3 flex-1">
                                                    <div className="flex items-center gap-2">
                                                        <span className="block text-sm font-medium text-gray-900">{method.name}</span>
                                                        {method.isRecommended && method.isAvailable !== false && (
                                                            <span className="text-xs font-semibold text-green-600 bg-green-50 border border-green-200 rounded px-2 py-0.5">
                                                                Gợi ý
                                                            </span>
                                                        )}
                                                    </div>
                                                    {method.description && (
                                                        <span className="block text-sm text-gray-500">{method.description}</span>
                                                    )}
                                                    {isDisabled && method.unavailableReason && (
                                                        <span className="block text-xs text-red-500 mt-2">{method.unavailableReason}</span>
                                                    )}
                                                    {method.feeAmount && method.feeAmount > 0 && method.isAvailable !== false && (
                                                        <span className="block text-xs text-gray-500 mt-2">Phí giao dịch: {method.feeAmount.toLocaleString()} đ</span>
                                                    )}
                                                </div>
                                                {method.iconUrl && (
                                                    <img
                                                        src={method.iconUrl}
                                                        alt={method.name}
                                                        className="w-12 h-12 object-contain ml-4 hidden sm:block"
                                                    />
                                                )}
                                            </label>
                                        )
                                    })
                                )}
                            </div>
                        </div>
                    </div>

                    {/* Order Summary */}
                    <div className="lg:col-span-1">
                        <div className="bg-white rounded-lg shadow-sm p-6 sticky top-4">
                            <h2 className="text-lg font-semibold mb-4">Đơn hàng của bạn</h2>
                            <div className="space-y-4 mb-6 max-h-96 overflow-y-auto">
                                {/* Display items from sessionData if available, otherwise from cart */}
                                {sessionData?.items ? (
                                    sessionData.items.map((item) => (
                                        <div key={item.variantId} className="flex gap-3">
                                            <img
                                                src={item.imageUrl || 'https://placehold.co/100x100?text=No+Image'}
                                                alt={item.productName}
                                                className="w-16 h-16 object-cover rounded border"
                                            />
                                            <div className="flex-1">
                                                <h4 className="text-sm font-medium line-clamp-2">{item.productName}</h4>
                                                <p className="text-xs text-gray-500">
                                                    {item.variantName} x {item.quantity}
                                                </p>
                                                <p className="text-sm font-medium text-red-600">
                                                    {(item.unitPriceAmount * item.quantity).toLocaleString()} đ
                                                </p>
                                            </div>
                                        </div>
                                    ))
                                ) : (
                                    (cart?.items || []).map(item => (
                                        <div key={item.id} className="flex gap-3">
                                            <img
                                                src={item.imageUrl || 'https://placehold.co/100x100?text=No+Image'}
                                                alt={item.productName}
                                                className="w-16 h-16 object-cover rounded border"
                                            />
                                            <div className="flex-1">
                                                <h4 className="text-sm font-medium line-clamp-2">{item.productName}</h4>
                                                <p className="text-xs text-gray-500">
                                                    {item.variantName} x {item.quantity}
                                                </p>
                                                <p className="text-sm font-medium text-red-600">
                                                    {(item.unitPriceAmount * item.quantity).toLocaleString()} đ
                                                </p>
                                            </div>
                                        </div>
                                    ))
                                )}
                            </div>

                            <div className="border-t pt-4 mb-4">
                                <div className="flex gap-2">
                                    <div className="relative flex-1">
                                        <Ticket className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                                        <input
                                            type="text"
                                            placeholder="Mã giảm giá"
                                            value={voucherCode}
                                            onChange={(e) => setVoucherCode(e.target.value)}
                                            onKeyPress={(e) => e.key === 'Enter' && handleApplyVoucher()}
                                            onFocus={handleVoucherInputFocus}
                                            className="w-full pl-9 pr-3 py-2 border rounded-lg text-sm focus:ring-red-500 focus:border-red-500"
                                        />
                                    </div>
                                    <button
                                        onClick={handleApplyVoucher}
                                        disabled={applyingVoucher || !voucherCode}
                                        className="px-4 py-2 bg-gray-900 text-white text-sm rounded-lg hover:bg-gray-800 disabled:opacity-50"
                                    >
                                        {applyingVoucher ? '...' : 'Áp dụng'}
                                    </button>
                                </div>
                            </div>

                            <div className="border-t pt-4 space-y-2">
                                <div className="flex justify-between text-sm">
                                    <span className="text-gray-600">Tạm tính</span>
                                    <span className="font-medium">{sessionData?.subtotalAmount?.toLocaleString() || calculateTotal().toLocaleString()} đ</span>
                                </div>
                                <div className="flex justify-between text-sm">
                                    <span className="text-gray-600">Phí vận chuyển</span>
                                    <span className="font-medium">{sessionData?.shippingAmount ? `${sessionData.shippingAmount.toLocaleString()} đ` : 'Miễn phí'}</span>
                                </div>
                                {sessionData?.discountAmount ? (
                                    <div className="flex justify-between text-sm text-green-600">
                                        <span>Giảm giá</span>
                                        <span>-{sessionData.discountAmount.toLocaleString()} đ</span>
                                    </div>
                                ) : null}
                                <div className="flex justify-between text-lg font-bold pt-2 border-t mt-2">
                                    <span>Tổng cộng</span>
                                    <span className="text-red-600">{sessionData?.totalAmount?.toLocaleString() || calculateTotal().toLocaleString()} đ</span>
                                </div>
                            </div>

                            <button
                                type="button"
                                onClick={(e) => handleSubmit(e)}
                                disabled={submitting || addressUpdating || paymentUpdating || applyingVoucher}
                                className="w-full mt-6 bg-red-600 text-white py-3 rounded-lg font-medium hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                {submitting ? 'Đang xử lý...' : 'Đặt hàng'}
                            </button>
                        </div>
                    </div>
                </div>
            </div>

            {/* Address List Modal */}
            {showAddressListModal && (
                <div className="fixed inset-0 bg-transparent flex items-center justify-center z-50 p-4">
                    <div className="bg-white rounded-lg shadow-xl max-w-md w-full p-6 max-h-[90vh] flex flex-col">
                        <div className="flex justify-between items-center mb-4">
                            <h3 className="text-lg font-semibold">Chọn địa chỉ</h3>
                            <button onClick={() => setShowAddressListModal(false)} className="text-gray-400 hover:text-gray-600">
                                <X className="w-5 h-5" />
                            </button>
                        </div>

                        <div className="flex-1 overflow-y-auto space-y-3 mb-4">
                            {addresses.length === 0 ? (
                                <p className="text-center text-gray-500 py-4">Bạn chưa có địa chỉ nào</p>
                            ) : (
                                addresses.map(addr => (
                                    <div
                                        key={addr.id}
                                        onClick={() => {
                                            if (addr.id) {
                                                setSelectedAddressId(addr.id)
                                                setShowAddressListModal(false)
                                            }
                                        }}
                                        className={`flex items-start p-3 border rounded-lg cursor-pointer hover:bg-gray-50 ${String(selectedAddressId) === String(addr.id) ? 'border-red-500 ring-1 ring-red-500 bg-red-50' : ''}`}
                                    >
                                        <input
                                            type="radio"
                                            name="modalShippingAddress"
                                            checked={String(selectedAddressId) === String(addr.id)}
                                            readOnly
                                            className="mt-1 h-4 w-4 text-red-600 focus:ring-red-500 pointer-events-none"
                                        />
                                        <div className="ml-3">
                                            <p className="font-medium">{addr.fullName} <span className="text-gray-500 font-normal">| {addr.phone}</span></p>
                                            <p className="text-sm text-gray-600">{addr.line1}, {addr.line2 && `${addr.line2}, `}{addr.ward}, {addr.district}, {addr.province}</p>
                                            {addr.isDefaultShipping && <span className="inline-block mt-1 text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded">Mặc định</span>}
                                        </div>
                                    </div>
                                ))
                            )}
                        </div>                        <button
                            onClick={() => {
                                setShowAddressListModal(false)
                                openAddAddressModal()
                            }}
                            className="w-full flex items-center justify-center py-2 border-2 border-dashed border-gray-300 rounded-lg text-gray-600 hover:border-red-500 hover:text-red-600 font-medium transition-colors"
                        >
                            <Plus className="w-5 h-5 mr-2" /> Thêm địa chỉ mới
                        </button>
                    </div>
                </div>
            )}

            {/* Add Address Modal */}
            {showAddAddressModal && (
                <div className="fixed inset-0 bg-transparent flex items-center justify-center z-50 p-4">
                    <div className="bg-white rounded-lg shadow-xl max-w-md w-full p-6">
                        <div className="flex justify-between items-center mb-4">
                            <h3 className="text-lg font-semibold">Thêm địa chỉ mới</h3>
                            <button onClick={closeAddAddressModal} className="text-gray-400 hover:text-gray-600">
                                <X className="w-5 h-5" />
                            </button>
                        </div>
                        <div className="space-y-3">
                            <input
                                type="text"
                                placeholder="Họ và tên"
                                className="w-full border rounded px-3 py-2"
                                value={newAddressForm.fullName}
                                onChange={e => setNewAddressForm({ ...newAddressForm, fullName: e.target.value })}
                            />
                            <input
                                type="tel"
                                placeholder="Số điện thoại"
                                className="w-full border rounded px-3 py-2"
                                value={newAddressForm.phone}
                                onChange={e => setNewAddressForm({ ...newAddressForm, phone: e.target.value })}
                            />
                            <input
                                type="text"
                                placeholder="Địa chỉ chi tiết (số nhà, đường...)"
                                className="w-full border rounded px-3 py-2"
                                value={newAddressForm.line1}
                                onChange={e => setNewAddressForm({ ...newAddressForm, line1: e.target.value })}
                            />
                            <input
                                type="text"
                                placeholder="Địa chỉ bổ sung (tùy chọn)"
                                className="w-full border rounded px-3 py-2"
                                value={newAddressForm.line2}
                                onChange={e => setNewAddressForm({ ...newAddressForm, line2: e.target.value })}
                            />
                            <select
                                value={modalSelectedProvinceId}
                                onChange={(e) => void handleModalProvinceChange(e.target.value)}
                                className="w-full border rounded px-3 py-2"
                            >
                                <option value="">Chọn tỉnh/Thành phố</option>
                                {modalProvinceOptions.map((province: ProvinceOption) => (
                                    <option key={province.id} value={province.id}>
                                        {province.name}
                                    </option>
                                ))}
                            </select>
                            <div className="grid grid-cols-2 gap-3">
                                <select
                                    value={modalSelectedDistrictId}
                                    onChange={(e) => void handleModalDistrictChange(e.target.value)}
                                    className="w-full border rounded px-3 py-2"
                                    disabled={!modalSelectedProvinceId}
                                >
                                    <option value="">Chọn quận/Huyện</option>
                                    {modalDistrictOptions.map((district: DistrictOption) => (
                                        <option key={district.id} value={district.id}>
                                            {district.name}
                                        </option>
                                    ))}
                                </select>
                                <select
                                    value={modalSelectedWardCode}
                                    onChange={(e) => handleModalWardChange(e.target.value)}
                                    className="w-full border rounded px-3 py-2"
                                    disabled={!modalSelectedDistrictId}
                                >
                                    <option value="">Chọn phường/Xã</option>
                                    {modalWardOptions.map((ward: WardOption) => (
                                        <option key={ward.code} value={ward.code}>
                                            {ward.name}
                                        </option>
                                    ))}
                                </select>
                            </div>
                            <label className="flex items-center">
                                <input
                                    type="checkbox"
                                    className="rounded text-red-600 focus:ring-red-500"
                                    checked={newAddressForm.isDefaultShipping}
                                    onChange={e => setNewAddressForm({ ...newAddressForm, isDefaultShipping: e.target.checked })}
                                />
                                <span className="ml-2 text-sm text-gray-600">Đặt làm địa chỉ mặc định</span>
                            </label>
                        </div>
                        <div className="mt-6 flex justify-end gap-3">
                            <button
                                onClick={closeAddAddressModal}
                                className="px-4 py-2 text-gray-600 hover:bg-gray-100 rounded"
                            >
                                Hủy
                            </button>
                            <button
                                onClick={handleAddAddress}
                                disabled={addingAddress}
                                className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700 disabled:opacity-50"
                            >
                                {addingAddress ? 'Đang lưu...' : 'Lưu địa chỉ'}
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Discount Modal */}
            {showDiscountModal && (
                <div className="fixed inset-0 bg-transparent flex items-center justify-center z-50 p-4">
                    <div className="bg-white rounded-lg shadow-xl w-full max-w-2xl max-h-[90vh] overflow-hidden flex flex-col">
                        {/* Header */}
                        <div className="flex items-center justify-between p-6 border-b">
                            <h2 className="text-xl font-semibold">Chọn mã giảm giá</h2>
                            <button
                                onClick={() => setShowDiscountModal(false)}
                                className="text-gray-400 hover:text-gray-600"
                            >
                                <X className="w-6 h-6" />
                            </button>
                        </div>

                        {/* Search */}
                        <div className="p-4 border-b">
                            <div className="flex gap-2">
                                <div className="relative flex-1">
                                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                                    <input
                                        type="text"
                                        placeholder="Tìm kiếm mã giảm giá..."
                                        value={discountSearchTerm}
                                        onChange={(e) => setDiscountSearchTerm(e.target.value)}
                                        onKeyPress={(e) => e.key === 'Enter' && handleSearchDiscounts()}
                                        className="w-full pl-10 pr-3 py-2 border rounded-lg focus:ring-blue-500 focus:border-blue-500"
                                    />
                                </div>
                                <button
                                    onClick={handleSearchDiscounts}
                                    className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
                                >
                                    Tìm
                                </button>
                            </div>
                        </div>

                        {/* Discount List */}
                        <div className="flex-1 overflow-y-auto p-4">
                            {loadingDiscounts ? (
                                <div className="flex justify-center items-center py-12">
                                    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
                                </div>
                            ) : availableDiscounts.length === 0 ? (
                                <div className="text-center py-12 text-gray-500">
                                    <Tag className="w-12 h-12 mx-auto mb-3 text-gray-300" />
                                    <p>Không tìm thấy mã giảm giá</p>
                                </div>
                            ) : (
                                <div className="space-y-3">
                                    {availableDiscounts.map((discount) => {
                                        const isValid = isDiscountValid(discount)
                                        return (
                                            <div
                                                key={discount.id}
                                                className={`border rounded-lg p-4 transition-all ${
                                                    isValid
                                                        ? 'hover:border-blue-500 hover:shadow-md cursor-pointer'
                                                        : 'opacity-60 cursor-not-allowed'
                                                }`}
                                                onClick={() => isValid && handleSelectDiscount(discount.code)}
                                            >
                                                <div className="flex items-start justify-between gap-4">
                                                    <div className="flex-1">
                                                        <div className="flex items-center gap-2 mb-2">
                                                            <div className="bg-red-100 text-red-600 px-3 py-1 rounded font-mono font-bold text-sm">
                                                                {discount.code}
                                                            </div>
                                                            {!isValid && (
                                                                <span className="text-xs text-red-500 font-medium">
                                                                    Hết hạn
                                                                </span>
                                                            )}
                                                        </div>
                                                        <p className="text-sm text-gray-600 mb-2">
                                                            {discount.description}
                                                        </p>
                                                        <div className="flex items-center gap-4 text-xs text-gray-500">
                                                            <div className="flex items-center gap-1">
                                                                <Percent className="w-3 h-3" />
                                                                <span>{formatDiscountValue(discount)}</span>
                                                            </div>
                                                            {discount.minOrderAmount && (
                                                                <div className="flex items-center gap-1">
                                                                    <span>Đơn tối thiểu: {discount.minOrderAmount.toLocaleString()}đ</span>
                                                                </div>
                                                            )}
                                                            <div className="flex items-center gap-1">
                                                                <Calendar className="w-3 h-3" />
                                                                <span>HSD: {formatInstant(discount.endDate, 'vi-VN', { year: 'numeric', month: '2-digit', day: '2-digit' })}</span>
                                                            </div>
                                                        </div>
                                                        {discount.usageLimit && (
                                                            <div className="mt-2">
                                                                <div className="flex items-center justify-between text-xs text-gray-500 mb-1">
                                                                    <span>Đã dùng: {discount.usageCount}/{discount.usageLimit}</span>
                                                                    <span>{Math.round((discount.usageCount / discount.usageLimit) * 100)}%</span>
                                                                </div>
                                                                <div className="w-full bg-gray-200 rounded-full h-1.5">
                                                                    <div
                                                                        className="bg-blue-600 h-1.5 rounded-full"
                                                                        style={{ width: `${(discount.usageCount / discount.usageLimit) * 100}%` }}
                                                                    />
                                                                </div>
                                                            </div>
                                                        )}
                                                    </div>
                                                    {isValid && (
                                                        <button
                                                            onClick={(e) => {
                                                                e.stopPropagation()
                                                                handleSelectDiscount(discount.code)
                                                            }}
                                                            className="px-4 py-2 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700 whitespace-nowrap"
                                                        >
                                                            Chọn
                                                        </button>
                                                    )}
                                                </div>
                                            </div>
                                        )
                                    })}
                                </div>
                            )}
                        </div>

                        {/* Footer */}
                        <div className="p-4 border-t bg-gray-50">
                            <p className="text-xs text-gray-500 text-center">
                                Nhấn vào mã giảm giá để áp dụng cho đơn hàng
                            </p>
                        </div>
                    </div>
                </div>
            )}

            <Footer />
        </div>
    )
}