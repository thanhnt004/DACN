import './App.css'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { ToastContainer } from 'react-toastify'
import 'react-toastify/dist/ReactToastify.css'
import Login from './pages/Login'
import Register from './pages/Register'
import HomePage from './pages/HomePage'
import { useAuthStore } from './store/auth'

import type { ReactElement } from 'react'
import { useLocation } from 'react-router-dom'
import AdminLayout from './components/layout/AdminLayout'
import MemberLayout from './components/layout/MemberLayout'
import AdminDashboard from './pages/admin/AdminDashboard'
import ProductManager from './pages/admin/ProductManager'
import ProductCreate from './pages/admin/ProductCreate'
import ProductCreateNew from './pages/admin/ProductCreateNew'
import ProductEdit from './pages/admin/ProductEdit'
import ProductVariantEdit from './pages/admin/ProductVariantEdit'
import ProductDetail from './pages/ProductDetail'
import ProductsPage from './pages/ProductsPage'
import CategoryProductsPage from './pages/CategoryProductsPage'
import CartPage from './pages/CartPage'
import CheckoutPage from './pages/CheckoutPage'
import OrderManager from './pages/admin/OrderManager'
import UserManager from './pages/admin/UserManager'
import DiscountManager from './pages/admin/DiscountManager'
import SizeManager from './pages/admin/SizeManager'
import ColorManager from './pages/admin/ColorManager'
import BrandManager from './pages/admin/BrandManager'
import CategoryManager from './pages/admin/CategoryManager'
import BannerManager from './pages/admin/BannerManager'
import AdminChangeRequestsPage from './pages/admin/AdminChangeRequestsPage'
import AdminRefundsPage from './pages/admin/AdminRefundsPage'
import ProfilePage from './pages/member/ProfilePage'
import DeactivateAccount from './pages/member/DeactivateAccount'
import VerifyEmail from './pages/VerifyEmail'
import ResendVerification from './pages/ResendVerification'
import EmailVerificationRequired from './pages/EmailVerificationRequired'
import MemberOrders from './pages/member/MemberOrders'
import PaymentReturnPage from './pages/PaymentReturnPage'
import GuestOrderLookupPage from './pages/GuestOrderLookupPage'
import ChatWidget from './components/ChatWidget'
import ForgotPasswordPage from './pages/ForgotPasswordPage'
import ResetPasswordPage from './pages/ResetPasswordPage'
import TermsOfUse from './pages/TermsOfUse'
import FeedbackSupport from './pages/FeedbackSupport'

function ProtectedRoute({ children }: { children: ReactElement }) {
  const isAuth = useAuthStore((s) => s.isAuthenticated)
  return isAuth ? children : <Navigate to="/login" replace />
}

function AdminRoute({ children }: { children: ReactElement }) {
  const { isAuthenticated, isAdmin } = useAuthStore()
  if (!isAuthenticated) return <Navigate to="/login" replace />
  if (!isAdmin) return <Navigate to="/" replace />
  return children
}

function App() {
  return (
    <BrowserRouter>
      <AppContent />
    </BrowserRouter>
  )
}

function AppContent() {
  const location = useLocation()
  const isAdminPage = location.pathname.startsWith('/admin')

  return (
    <>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/verify-email" element={<VerifyEmail />} />
        <Route path="/resend-verification" element={<ResendVerification />} />
        <Route path="/email-verification-required" element={<EmailVerificationRequired />} />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        <Route path="/reset-password" element={<ResetPasswordPage />} />
        <Route path="/" element={<HomePage />} />
        <Route path="/products" element={<ProductsPage />} />
        <Route path="/categories/:slug" element={<CategoryProductsPage />} />
        <Route path="/products/:slug" element={<ProductDetail />} />
        <Route path="/cart" element={<CartPage />} />
        <Route path="/checkout" element={<CheckoutPage />} />
        <Route path="/payment_return" element={<PaymentReturnPage />} />
        <Route path="/orders/track" element={<GuestOrderLookupPage />} />
        <Route path="/terms" element={<TermsOfUse />} />
        <Route path="/feedback" element={<FeedbackSupport />} />

        {/* Member Routes */}
        <Route
          path="/member"
          element={
            <ProtectedRoute>
              <MemberLayout />
            </ProtectedRoute>
          }
        >
          <Route index element={<Navigate to="/member/orders" replace />} />
          <Route path="profile" element={<ProfilePage />} />
          <Route path="deactivate" element={<DeactivateAccount />} />
          <Route path="orders" element={<MemberOrders />} />
          <Route path="support" element={<div className="bg-white rounded-lg shadow-sm p-8">Góp ý - Phản hồi - Hỗ trợ</div>} />
          <Route path="terms" element={<div className="bg-white rounded-lg shadow-sm p-8">Điều khoản sử dụng</div>} />
        </Route>

        {/* Admin Routes */}
        <Route
          path="/admin"
          element={
            <AdminRoute>
              <AdminLayout />
            </AdminRoute>
          }
        >
          <Route index element={<AdminDashboard />} />
          <Route path="products" element={<ProductManager />} />
          <Route path="products/create" element={<ProductCreateNew />} />
          <Route path="products/:productId/edit" element={<ProductEdit />} />
          <Route path="products/:productId/variants/:variantId/edit" element={<ProductVariantEdit />} />
          <Route path="orders" element={<OrderManager />} />
          <Route path="users" element={<UserManager />} />
          <Route path="discounts" element={<DiscountManager />} />
          <Route path="products/sizes" element={<SizeManager />} />
          <Route path="products/colors" element={<ColorManager />} />
          <Route path="brands" element={<BrandManager />} />
          <Route path="categories" element={<CategoryManager />} />
          <Route path="banners" element={<BannerManager />} />
          <Route path="change-requests" element={<AdminChangeRequestsPage />} />
          <Route path="refunds" element={<AdminRefundsPage />} />
        </Route>
        <Route path="*" element={<Navigate to="/" />} />
      </Routes>
      
      {/* ChatWidget - Only on homepage and product browsing pages */}
      {!isAdminPage && (
        location.pathname === '/' ||
        location.pathname.startsWith('/products') ||
        location.pathname.startsWith('/category')
      ) && <ChatWidget />}
      
      {/* Toast notifications */}
      <ToastContainer
        position="top-right"
        autoClose={3000}
        hideProgressBar={false}
        newestOnTop
        closeOnClick
        rtl={false}
        pauseOnFocusLoss
        draggable
        pauseOnHover
        theme="light"
      />
    </>
  )
}

export default App
