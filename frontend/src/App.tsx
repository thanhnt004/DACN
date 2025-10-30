import './App.css'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import Login from './pages/Login'
import Register from './pages/Register'
import HomePage from './pages/HomePage'
import { useAuthStore } from './store/auth'

import type { ReactElement } from 'react'
import AdminLayout from './components/layout/AdminLayout'
import MemberLayout from './components/layout/MemberLayout'
import AdminDashboard from './pages/admin/AdminDashboard'
import ProductManager from './pages/admin/ProductManager'
import ProductCreate from './pages/admin/ProductCreate'
import ProductEdit from './pages/admin/ProductEdit'
import ProductVariantEdit from './pages/admin/ProductVariantEdit'
import ProductDetail from './pages/ProductDetail'
import ProductsPage from './pages/ProductsPage'
import CategoryProductsPage from './pages/CategoryProductsPage'
import CartPage from './pages/CartPage'
import OrderManager from './pages/admin/OrderManager'
import UserManager from './pages/admin/UserManager'
import DiscountManager from './pages/admin/DiscountManager'
import SizeManager from './pages/admin/SizeManager'
import ColorManager from './pages/admin/ColorManager'
import BrandManager from './pages/admin/BrandManager'
import CategoryManager from './pages/admin/CategoryManager'
import MemberDashboard from './pages/member/MemberDashboard'
import ProfilePage from './pages/member/ProfilePage'
import DeactivateAccount from './pages/member/DeactivateAccount'
import VerifyEmail from './pages/VerifyEmail'
import ResendVerification from './pages/ResendVerification'
import EmailVerificationRequired from './pages/EmailVerificationRequired'

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
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/verify-email" element={<VerifyEmail />} />
        <Route path="/resend-verification" element={<ResendVerification />} />
        <Route path="/email-verification-required" element={<EmailVerificationRequired />} />
        <Route path="/" element={<HomePage />} />
        <Route path="/products" element={<ProductsPage />} />
        <Route path="/categories/:slug" element={<CategoryProductsPage />} />
        <Route path="/products/:slug" element={<ProductDetail />} />
        <Route path="/cart" element={<CartPage />} />

        {/* Member Routes */}
        <Route
          path="/member"
          element={
            <ProtectedRoute>
              <MemberLayout />
            </ProtectedRoute>
          }
        >
          <Route index element={<MemberDashboard />} />
          <Route path="profile" element={<ProfilePage />} />
          <Route path="deactivate" element={<DeactivateAccount />} />
          <Route path="orders" element={<div className="bg-white rounded-lg shadow-sm p-8">Lịch sử mua hàng</div>} />
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
          <Route path="products/create" element={<ProductCreate />} />
          <Route path="products/:productId/edit" element={<ProductEdit />} />
          <Route path="products/:productId/variants/:variantId/edit" element={<ProductVariantEdit />} />
          <Route path="orders" element={<OrderManager />} />
          <Route path="users" element={<UserManager />} />
          <Route path="discounts" element={<DiscountManager />} />
          <Route path="products/sizes" element={<SizeManager />} />
          <Route path="products/colors" element={<ColorManager />} />
          <Route path="brands" element={<BrandManager />} />
          <Route path="categories" element={<CategoryManager />} />
        </Route>
        <Route path="*" element={<Navigate to="/" />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
