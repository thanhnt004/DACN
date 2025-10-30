import { useEffect, useState } from 'react'
import { Edit, Facebook } from 'lucide-react'
import { useSearchParams } from 'react-router-dom'
import * as ProfileApi from '../../api/profile'
import type { UserProfile, Address, OAuthAccount } from '../../api/profile'

export default function ProfilePage() {
    const [searchParams, setSearchParams] = useSearchParams()
    const [profile, setProfile] = useState<UserProfile | null>(null)
    const [addresses, setAddresses] = useState<Address[]>([])
    const [oauthAccounts, setOAuthAccounts] = useState<OAuthAccount[]>([])
    const [loading, setLoading] = useState(true)
    const [editingProfile, setEditingProfile] = useState(false)
    const [editingAddress, setEditingAddress] = useState<string | null>(null)
    const [addingAddress, setAddingAddress] = useState(false)
    const [changingPassword, setChangingPassword] = useState(false)

    // Loading states for operations
    const [updatingProfile, setUpdatingProfile] = useState(false)
    const [savingAddress, setSavingAddress] = useState(false)
    const [deletingAddress, setDeletingAddress] = useState<string | null>(null)
    const [savingPassword, setSavingPassword] = useState(false)
    const [unlinkingOAuth, setUnlinkingOAuth] = useState<string | null>(null)

    // Form states
    const [profileForm, setProfileForm] = useState({
        fullName: '',
        phone: '',
        gender: '' as 'M' | 'F' | 'O' | '',
        dateOfBirth: '',
        avatarUrl: ''
    })

    const [addressForm, setAddressForm] = useState<Omit<Address, 'id'>>({
        fullName: '',
        phone: '',
        line1: '',
        line2: '',
        ward: '',
        district: '',
        province: '',
        city: '',
        isDefaultShipping: false
    })

    const [passwordForm, setPasswordForm] = useState({
        currentPassword: '',
        newPassword: '',
        newPasswordConfirm: ''
    })

    useEffect(() => {
        loadData()

        // Check for OAuth link callback
        const linkedProvider = searchParams.get('linked')
        if (linkedProvider) {
            alert(`✅ Liên kết ${linkedProvider} thành công!`)
            // Remove the query param
            searchParams.delete('linked')
            setSearchParams(searchParams)
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [])

    const loadData = async () => {
        setLoading(true)
        try {
            const [profileData, addressesData, oauthData] = await Promise.all([
                ProfileApi.getProfile(),
                ProfileApi.getAddresses(),
                ProfileApi.getOAuthAccounts()
            ])
            setProfile(profileData)
            setAddresses(addressesData)
            setOAuthAccounts(oauthData)
            setProfileForm({
                fullName: profileData.fullName,
                phone: profileData.phone,
                gender: profileData.gender || '',
                dateOfBirth: profileData.dateOfBirth || '',
                avatarUrl: profileData.avatarUrl || ''
            })
        } catch (error) {
            console.error('Failed to load profile:', error)
            alert('Không thể tải thông tin tài khoản')
        } finally {
            setLoading(false)
        }
    }

    const handleUpdateProfile = async () => {
        setUpdatingProfile(true)
        try {
            await ProfileApi.updateProfile({
                fullName: profileForm.fullName,
                phone: profileForm.phone,
                gender: profileForm.gender || undefined,
                dateOfBirth: profileForm.dateOfBirth || undefined,
                avatarUrl: profileForm.avatarUrl || undefined
            })
            await loadData() // Reload data
            setEditingProfile(false)
            alert('✅ Cập nhật thông tin thành công!')
        } catch (error) {
            console.error('Failed to update profile:', error)
            alert('❌ Không thể cập nhật thông tin. Vui lòng thử lại.')
        } finally {
            setUpdatingProfile(false)
        }
    }

    const handleAddAddress = async () => {
        setSavingAddress(true)
        try {
            await ProfileApi.addAddress(addressForm)
            await loadData() // Reload addresses
            setAddingAddress(false)
            setAddressForm({
                fullName: '',
                phone: '',
                line1: '',
                line2: '',
                ward: '',
                district: '',
                province: '',
                city: '',
                isDefaultShipping: false
            })
            alert('✅ Thêm địa chỉ thành công!')
        } catch (error) {
            console.error('Failed to add address:', error)
            alert('❌ Không thể thêm địa chỉ. Vui lòng thử lại.')
        } finally {
            setSavingAddress(false)
        }
    }

    const handleUpdateAddress = async (id: string) => {
        setSavingAddress(true)
        try {
            await ProfileApi.updateAddress(id, addressForm)
            await loadData() // Reload addresses
            setEditingAddress(null)
            alert('✅ Cập nhật địa chỉ thành công!')
        } catch (error) {
            console.error('Failed to update address:', error)
            alert('❌ Không thể cập nhật địa chỉ. Vui lòng thử lại.')
        } finally {
            setSavingAddress(false)
        }
    }

    const handleDeleteAddress = async (id: string) => {
        if (!confirm('Bạn có chắc chắn muốn xóa địa chỉ này?')) return
        setDeletingAddress(id)
        try {
            await ProfileApi.deleteAddress(id)
            await loadData() // Reload addresses
            alert('✅ Xóa địa chỉ thành công!')
        } catch (error) {
            console.error('Failed to delete address:', error)
            alert('❌ Không thể xóa địa chỉ. Vui lòng thử lại.')
        } finally {
            setDeletingAddress(null)
        }
    }

    const handleChangePassword = async () => {
        if (passwordForm.newPassword !== passwordForm.newPasswordConfirm) {
            alert('❌ Mật khẩu xác nhận không khớp')
            return
        }
        setSavingPassword(true)
        try {
            await ProfileApi.changePassword({
                currentPassword: passwordForm.currentPassword,
                newPassword: passwordForm.newPassword,
                newPasswordConfirm: passwordForm.newPasswordConfirm
            })
            setChangingPassword(false)
            setPasswordForm({ currentPassword: '', newPassword: '', newPasswordConfirm: '' })
            alert('✅ Đổi mật khẩu thành công!')
        } catch (error) {
            console.error('Failed to change password:', error)
            alert('❌ Không thể đổi mật khẩu. Vui lòng kiểm tra mật khẩu cũ.')
        } finally {
            setSavingPassword(false)
        }
    }

    const handleUnlinkOAuth = async (provider: 'GOOGLE' | 'FACEBOOK') => {
        if (!confirm(`Bạn có chắc chắn muốn hủy liên kết tài khoản ${provider}?`)) return
        setUnlinkingOAuth(provider)
        try {
            await ProfileApi.unlinkOAuthAccount(provider)
            await loadData() // Reload OAuth accounts
            alert('✅ Hủy liên kết thành công!')
        } catch (error) {
            console.error('Failed to unlink OAuth:', error)
            alert('❌ Không thể hủy liên kết. Vui lòng thử lại.')
        } finally {
            setUnlinkingOAuth(null)
        }
    }

    const handleLinkOAuth = (provider: 'google' | 'facebook') => {
        // Redirect to OAuth link flow
        ProfileApi.linkOAuthAccount(provider)
    }

    if (loading) {
        return (
            <div className="bg-white rounded-lg shadow-sm p-8">
                <div className="text-center text-gray-600">Đang tải...</div>
            </div>
        )
    }

    if (!profile) {
        return (
            <div className="bg-white rounded-lg shadow-sm p-8">
                <div className="text-center text-red-600">Không thể tải thông tin tài khoản</div>
            </div>
        )
    }

    return (
        <div className="space-y-6">
            {/* Personal Information */}
            <div className="bg-white rounded-lg shadow-sm">
                <div className="p-6 border-b border-gray-200 flex items-center justify-between">
                    <h2 className="text-xl font-bold text-gray-900">Thông tin cá nhân</h2>
                    <button
                        onClick={() => setEditingProfile(!editingProfile)}
                        className="text-red-600 hover:text-red-700 flex items-center gap-2"
                    >
                        <Edit className="w-4 h-4" />
                        <span className="text-sm font-medium">Cập nhật</span>
                    </button>
                </div>

                <div className="p-6">
                    {editingProfile ? (
                        <div className="space-y-4">
                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Họ và tên:
                                    </label>
                                    <input
                                        type="text"
                                        value={profileForm.fullName}
                                        onChange={(e) => setProfileForm({ ...profileForm, fullName: e.target.value })}
                                        className="w-full border border-gray-300 rounded px-3 py-2"
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Số điện thoại:
                                    </label>
                                    <input
                                        type="tel"
                                        value={profileForm.phone}
                                        onChange={(e) => setProfileForm({ ...profileForm, phone: e.target.value })}
                                        className="w-full border border-gray-300 rounded px-3 py-2"
                                    />
                                </div>
                            </div>

                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Giới tính:
                                    </label>
                                    <select
                                        value={profileForm.gender}
                                        onChange={(e) => setProfileForm({ ...profileForm, gender: e.target.value as 'M' | 'F' | 'O' | '' })}
                                        className="w-full border border-gray-300 rounded px-3 py-2"
                                    >
                                        <option value="">Chọn giới tính</option>
                                        <option value="M">Nam</option>
                                        <option value="F">Nữ</option>
                                        <option value="O">Khác</option>
                                    </select>
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Email:
                                    </label>
                                    <input
                                        type="email"
                                        value={profile.email}
                                        disabled
                                        className="w-full border border-gray-300 rounded px-3 py-2 bg-gray-50"
                                    />
                                </div>
                            </div>

                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Ngày sinh:
                                    </label>
                                    <input
                                        type="date"
                                        value={profileForm.dateOfBirth}
                                        onChange={(e) => setProfileForm({ ...profileForm, dateOfBirth: e.target.value })}
                                        className="w-full border border-gray-300 rounded px-3 py-2"
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Địa chỉ mặc định:
                                    </label>
                                    <input
                                        type="text"
                                        value={addresses.find(a => a.isDefaultShipping)
                                            ? `${addresses.find(a => a.isDefaultShipping)?.line1}, ${addresses.find(a => a.isDefaultShipping)?.ward}, ${addresses.find(a => a.isDefaultShipping)?.district}, ${addresses.find(a => a.isDefaultShipping)?.city}`
                                            : 'Chưa có'}
                                        disabled
                                        className="w-full border border-gray-300 rounded px-3 py-2 bg-gray-50"
                                    />
                                </div>
                            </div>

                            <div className="flex gap-2">
                                <button
                                    onClick={handleUpdateProfile}
                                    disabled={updatingProfile}
                                    className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed"
                                >
                                    {updatingProfile ? 'Đang lưu...' : 'Lưu thay đổi'}
                                </button>
                                <button
                                    onClick={() => setEditingProfile(false)}
                                    disabled={updatingProfile}
                                    className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                                >
                                    Hủy
                                </button>
                            </div>
                        </div>
                    ) : (
                        <div className="grid grid-cols-2 gap-x-8 gap-y-4 text-sm">
                            <div>
                                <span className="text-gray-600">Họ và tên:</span>
                                <span className="ml-4 font-medium">{profile.fullName}</span>
                            </div>
                            <div>
                                <span className="text-gray-600">Số điện thoại:</span>
                                <span className="ml-4 font-medium">{profile.phone}</span>
                            </div>
                            <div>
                                <span className="text-gray-600">Giới tính:</span>
                                <span className="ml-4 font-medium">
                                    {profile.gender === 'M' ? 'Nam' : profile.gender === 'F' ? 'Nữ' : profile.gender === 'O' ? 'Khác' : 'Chưa cập nhật'}
                                </span>
                            </div>
                            <div>
                                <span className="text-gray-600">Email:</span>
                                <span className="ml-4 font-medium">{profile.email}</span>
                            </div>
                            <div>
                                <span className="text-gray-600">Ngày sinh:</span>
                                <span className="ml-4 font-medium">
                                    {profile.dateOfBirth ? new Date(profile.dateOfBirth).toLocaleDateString('vi-VN') : 'Chưa cập nhật'}
                                </span>
                            </div>
                            <div>
                                <span className="text-gray-600">Địa chỉ mặc định:</span>
                                <span className="ml-4 font-medium">
                                    {addresses.find(a => a.isDefaultShipping)
                                        ? `${addresses.find(a => a.isDefaultShipping)?.line1}, ${addresses.find(a => a.isDefaultShipping)?.ward}, ${addresses.find(a => a.isDefaultShipping)?.district}, ${addresses.find(a => a.isDefaultShipping)?.city}`
                                        : 'Chưa có'}
                                </span>
                            </div>
                        </div>
                    )}
                </div>
            </div>

            {/* Addresses */}
            <div className="bg-white rounded-lg shadow-sm">
                <div className="p-6 border-b border-gray-200 flex items-center justify-between">
                    <h2 className="text-xl font-bold text-gray-900">Sổ địa chỉ</h2>
                    <button
                        onClick={() => setAddingAddress(true)}
                        className="text-red-600 hover:text-red-700 text-sm font-medium"
                    >
                        + Thêm địa chỉ
                    </button>
                </div>

                <div className="p-6 space-y-4">
                    {addingAddress && (
                        <div className="border border-gray-200 rounded-lg p-4 bg-gray-50">
                            <div className="space-y-3">
                                <input
                                    type="text"
                                    placeholder="Họ và tên"
                                    value={addressForm.fullName}
                                    onChange={(e) => setAddressForm({ ...addressForm, fullName: e.target.value })}
                                    className="w-full border border-gray-300 rounded px-3 py-2"
                                />
                                <input
                                    type="tel"
                                    placeholder="Số điện thoại"
                                    value={addressForm.phone}
                                    onChange={(e) => setAddressForm({ ...addressForm, phone: e.target.value })}
                                    className="w-full border border-gray-300 rounded px-3 py-2"
                                />
                                <input
                                    type="text"
                                    placeholder="Địa chỉ chi tiết (số nhà, đường...)"
                                    value={addressForm.line1}
                                    onChange={(e) => setAddressForm({ ...addressForm, line1: e.target.value })}
                                    className="w-full border border-gray-300 rounded px-3 py-2"
                                />
                                <input
                                    type="text"
                                    placeholder="Địa chỉ bổ sung (tùy chọn)"
                                    value={addressForm.line2}
                                    onChange={(e) => setAddressForm({ ...addressForm, line2: e.target.value })}
                                    className="w-full border border-gray-300 rounded px-3 py-2"
                                />
                                <div className="grid grid-cols-2 gap-3">
                                    <input
                                        type="text"
                                        placeholder="Phường/Xã"
                                        value={addressForm.ward}
                                        onChange={(e) => setAddressForm({ ...addressForm, ward: e.target.value })}
                                        className="w-full border border-gray-300 rounded px-3 py-2"
                                    />
                                    <input
                                        type="text"
                                        placeholder="Quận/Huyện"
                                        value={addressForm.district}
                                        onChange={(e) => setAddressForm({ ...addressForm, district: e.target.value })}
                                        className="w-full border border-gray-300 rounded px-3 py-2"
                                    />
                                </div>
                                <div className="grid grid-cols-2 gap-3">
                                    <input
                                        type="text"
                                        placeholder="Tỉnh/Thành phố"
                                        value={addressForm.province}
                                        onChange={(e) => setAddressForm({ ...addressForm, province: e.target.value })}
                                        className="w-full border border-gray-300 rounded px-3 py-2"
                                    />
                                    <input
                                        type="text"
                                        placeholder="Thành phố"
                                        value={addressForm.city}
                                        onChange={(e) => setAddressForm({ ...addressForm, city: e.target.value })}
                                        className="w-full border border-gray-300 rounded px-3 py-2"
                                    />
                                </div>
                                <label className="flex items-center gap-2">
                                    <input
                                        type="checkbox"
                                        checked={addressForm.isDefaultShipping}
                                        onChange={(e) => setAddressForm({ ...addressForm, isDefaultShipping: e.target.checked })}
                                    />
                                    <span className="text-sm">Đặt làm địa chỉ giao hàng mặc định</span>
                                </label>
                                <div className="flex gap-2">
                                    <button
                                        onClick={handleAddAddress}
                                        disabled={savingAddress}
                                        className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700 text-sm disabled:opacity-50 disabled:cursor-not-allowed"
                                    >
                                        {savingAddress ? 'Đang lưu...' : 'Lưu'}
                                    </button>
                                    <button
                                        onClick={() => {
                                            setAddingAddress(false)
                                            setAddressForm({
                                                fullName: '',
                                                phone: '',
                                                line1: '',
                                                line2: '',
                                                ward: '',
                                                district: '',
                                                province: '',
                                                city: '',
                                                isDefaultShipping: false
                                            })
                                        }}
                                        disabled={savingAddress}
                                        className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50 text-sm disabled:opacity-50 disabled:cursor-not-allowed"
                                    >
                                        Hủy
                                    </button>
                                </div>
                            </div>
                        </div>
                    )}

                    {addresses.map((address) => (
                        <div key={address.id} className="border border-gray-200 rounded-lg p-4">
                            <div className="flex items-start justify-between mb-2">
                                <div className="flex items-center gap-2">
                                    <span className="font-medium text-gray-900">{address.fullName}</span>
                                    {address.isDefaultShipping && (
                                        <span className="px-2 py-1 bg-green-100 text-green-700 text-xs rounded">
                                            Mặc định
                                        </span>
                                    )}
                                </div>
                                <div className="flex gap-2">
                                    <button
                                        onClick={() => {
                                            setEditingAddress(address.id!)
                                            setAddressForm({
                                                fullName: address.fullName,
                                                phone: address.phone,
                                                line1: address.line1,
                                                line2: address.line2,
                                                ward: address.ward,
                                                district: address.district,
                                                province: address.province,
                                                city: address.city,
                                                isDefaultShipping: address.isDefaultShipping
                                            })
                                        }}
                                        className="text-blue-600 hover:text-blue-700 text-sm"
                                    >
                                        Cập nhật
                                    </button>
                                    <button
                                        onClick={() => handleDeleteAddress(address.id!)}
                                        disabled={deletingAddress === address.id}
                                        className="text-red-600 hover:text-red-700 text-sm disabled:opacity-50 disabled:cursor-not-allowed"
                                    >
                                        {deletingAddress === address.id ? 'Đang xóa...' : 'Xóa'}
                                    </button>
                                </div>
                            </div>

                            {editingAddress === address.id ? (
                                <div className="space-y-3 mt-3">
                                    <input
                                        type="text"
                                        placeholder="Họ và tên"
                                        value={addressForm.fullName}
                                        onChange={(e) => setAddressForm({ ...addressForm, fullName: e.target.value })}
                                        className="w-full border border-gray-300 rounded px-3 py-2"
                                    />
                                    <input
                                        type="tel"
                                        placeholder="Số điện thoại"
                                        value={addressForm.phone}
                                        onChange={(e) => setAddressForm({ ...addressForm, phone: e.target.value })}
                                        className="w-full border border-gray-300 rounded px-3 py-2"
                                    />
                                    <input
                                        type="text"
                                        placeholder="Địa chỉ chi tiết"
                                        value={addressForm.line1}
                                        onChange={(e) => setAddressForm({ ...addressForm, line1: e.target.value })}
                                        className="w-full border border-gray-300 rounded px-3 py-2"
                                    />
                                    <input
                                        type="text"
                                        placeholder="Địa chỉ bổ sung (tùy chọn)"
                                        value={addressForm.line2}
                                        onChange={(e) => setAddressForm({ ...addressForm, line2: e.target.value })}
                                        className="w-full border border-gray-300 rounded px-3 py-2"
                                    />
                                    <div className="grid grid-cols-2 gap-3">
                                        <input
                                            type="text"
                                            placeholder="Phường/Xã"
                                            value={addressForm.ward}
                                            onChange={(e) => setAddressForm({ ...addressForm, ward: e.target.value })}
                                            className="w-full border border-gray-300 rounded px-3 py-2"
                                        />
                                        <input
                                            type="text"
                                            placeholder="Quận/Huyện"
                                            value={addressForm.district}
                                            onChange={(e) => setAddressForm({ ...addressForm, district: e.target.value })}
                                            className="w-full border border-gray-300 rounded px-3 py-2"
                                        />
                                    </div>
                                    <div className="grid grid-cols-2 gap-3">
                                        <input
                                            type="text"
                                            placeholder="Tỉnh/Thành phố"
                                            value={addressForm.province}
                                            onChange={(e) => setAddressForm({ ...addressForm, province: e.target.value })}
                                            className="w-full border border-gray-300 rounded px-3 py-2"
                                        />
                                        <input
                                            type="text"
                                            placeholder="Thành phố"
                                            value={addressForm.city}
                                            onChange={(e) => setAddressForm({ ...addressForm, city: e.target.value })}
                                            className="w-full border border-gray-300 rounded px-3 py-2"
                                        />
                                    </div>
                                    <div className="flex gap-2">
                                        <button
                                            onClick={() => handleUpdateAddress(address.id!)}
                                            disabled={savingAddress}
                                            className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 text-sm disabled:opacity-50 disabled:cursor-not-allowed"
                                        >
                                            {savingAddress ? 'Đang lưu...' : 'Cập nhật'}
                                        </button>
                                        <button
                                            onClick={() => setEditingAddress(null)}
                                            disabled={savingAddress}
                                            className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50 text-sm disabled:opacity-50 disabled:cursor-not-allowed"
                                        >
                                            Hủy
                                        </button>
                                    </div>
                                </div>
                            ) : (
                                <div className="text-sm text-gray-600">
                                    <p className="font-medium text-gray-900">{address.fullName} - {address.phone}</p>
                                    <p className="mt-1">
                                        {address.line1}
                                        {address.line2 && `, ${address.line2}`}
                                    </p>
                                    <p>{address.ward}, {address.district}, {address.city}</p>
                                    {address.province && <p>{address.province}</p>}
                                </div>
                            )}
                        </div>
                    ))}
                </div>
            </div>

            {/* Password */}
            <div className="bg-white rounded-lg shadow-sm">
                <div className="p-6 border-b border-gray-200 flex items-center justify-between">
                    <h2 className="text-xl font-bold text-gray-900">Mật khẩu</h2>
                    <button
                        onClick={() => setChangingPassword(!changingPassword)}
                        className="text-red-600 hover:text-red-700 text-sm font-medium"
                    >
                        Thay đổi mật khẩu
                    </button>
                </div>

                {changingPassword && (
                    <div className="p-6">
                        <div className="space-y-4 max-w-md">
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    Cập nhật lần cuối lúc:
                                </label>
                                <input
                                    type="text"
                                    value={profile.passwordChangedAt ? new Date(profile.passwordChangedAt).toLocaleString('vi-VN') : 'Chưa đổi'}
                                    disabled
                                    className="w-full border border-gray-300 rounded px-3 py-2 bg-gray-50"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    Mật khẩu hiện tại:
                                </label>
                                <input
                                    type="password"
                                    value={passwordForm.currentPassword}
                                    onChange={(e) => setPasswordForm({ ...passwordForm, currentPassword: e.target.value })}
                                    className="w-full border border-gray-300 rounded px-3 py-2"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    Mật khẩu mới:
                                </label>
                                <input
                                    type="password"
                                    value={passwordForm.newPassword}
                                    onChange={(e) => setPasswordForm({ ...passwordForm, newPassword: e.target.value })}
                                    className="w-full border border-gray-300 rounded px-3 py-2"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    Xác nhận mật khẩu mới:
                                </label>
                                <input
                                    type="password"
                                    value={passwordForm.newPasswordConfirm}
                                    onChange={(e) => setPasswordForm({ ...passwordForm, newPasswordConfirm: e.target.value })}
                                    className="w-full border border-gray-300 rounded px-3 py-2"
                                />
                            </div>
                            <div className="flex gap-2">
                                <button
                                    onClick={handleChangePassword}
                                    disabled={savingPassword}
                                    className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed"
                                >
                                    {savingPassword ? 'Đang đổi...' : 'Đổi mật khẩu'}
                                </button>
                                <button
                                    onClick={() => {
                                        setChangingPassword(false)
                                        setPasswordForm({ currentPassword: '', newPassword: '', newPasswordConfirm: '' })
                                    }}
                                    disabled={savingPassword}
                                    className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                                >
                                    Hủy
                                </button>
                            </div>
                        </div>
                    </div>
                )}
            </div>

            {/* OAuth Accounts */}
            <div className="bg-white rounded-lg shadow-sm">
                <div className="p-6 border-b border-gray-200">
                    <h2 className="text-xl font-bold text-gray-900">Tài khoản liên kết</h2>
                </div>

                <div className="p-6 space-y-4">
                    {/* Google */}
                    <div className="flex items-center justify-between">
                        <div className="flex items-center gap-3">
                            <div className="w-10 h-10 bg-white rounded-full flex items-center justify-center shadow">
                                <svg className="w-6 h-6" viewBox="0 0 24 24">
                                    <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
                                    <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
                                    <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" />
                                    <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
                                </svg>
                            </div>
                            <div>
                                <p className="font-medium">Google</p>
                                {oauthAccounts.find(acc => acc.provider === 'GOOGLE') ? (
                                    <p className="text-sm text-green-600">Đã liên kết</p>
                                ) : (
                                    <p className="text-sm text-gray-500">Chưa liên kết</p>
                                )}
                            </div>
                        </div>
                        {oauthAccounts.find(acc => acc.provider === 'GOOGLE') ? (
                            <button
                                onClick={() => handleUnlinkOAuth('GOOGLE')}
                                disabled={unlinkingOAuth === 'GOOGLE'}
                                className="text-red-600 hover:text-red-700 text-sm font-medium disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                {unlinkingOAuth === 'GOOGLE' ? 'Đang hủy...' : 'Hủy liên kết'}
                            </button>
                        ) : (
                            <button
                                onClick={() => handleLinkOAuth('google')}
                                className="text-blue-600 hover:text-blue-700 text-sm font-medium"
                            >
                                Liên kết
                            </button>
                        )}
                    </div>

                    {/* Facebook */}
                    <div className="flex items-center justify-between">
                        <div className="flex items-center gap-3">
                            <div className="w-10 h-10 bg-blue-600 rounded-full flex items-center justify-center">
                                <Facebook className="w-6 h-6 text-white" />
                            </div>
                            <div>
                                <p className="font-medium">Facebook</p>
                                {oauthAccounts.find(acc => acc.provider === 'FACEBOOK') ? (
                                    <p className="text-sm text-green-600">Đã liên kết</p>
                                ) : (
                                    <p className="text-sm text-gray-500">Chưa liên kết</p>
                                )}
                            </div>
                        </div>
                        {oauthAccounts.find(acc => acc.provider === 'FACEBOOK') ? (
                            <button
                                onClick={() => handleUnlinkOAuth('FACEBOOK')}
                                disabled={unlinkingOAuth === 'FACEBOOK'}
                                className="text-red-600 hover:text-red-700 text-sm font-medium disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                {unlinkingOAuth === 'FACEBOOK' ? 'Đang hủy...' : 'Hủy liên kết'}
                            </button>
                        ) : (
                            <button
                                onClick={() => handleLinkOAuth('facebook')}
                                className="text-blue-600 hover:text-blue-700 text-sm font-medium"
                            >
                                Liên kết
                            </button>
                        )}
                    </div>
                </div>
            </div>
        </div>
    )
}
