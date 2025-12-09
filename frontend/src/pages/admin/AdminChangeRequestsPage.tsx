import { useState, useEffect, useCallback } from 'react'
import { CheckCircle, XCircle, Clock, AlertCircle, Package, RefreshCw, LucideIcon } from 'lucide-react'
import { toast } from 'react-toastify'
import * as ChangeRequestsApi from '../../api/admin/changeRequests'
import ErrorModal from '../../components/common/ErrorModal'
import ConfirmModal from '../../components/common/ConfirmModal'
import { useModals } from '../../hooks/useModals'
import { formatInstant } from '../../lib/dateUtils'

type TabType = 'CANCEL' | 'RETURN'

export default function AdminChangeRequestsPage() {
    const [activeTab, setActiveTab] = useState<TabType>('CANCEL')
    const [requests, setRequests] = useState<ChangeRequestsApi.ChangeRequest[]>([])
    const [loading, setLoading] = useState(false)
    const [page, setPage] = useState(0)
    const [totalPages, setTotalPages] = useState(1)
    const [selectedRequest, setSelectedRequest] = useState<ChangeRequestsApi.ChangeRequest | null>(null)
    const [showReviewModal, setShowReviewModal] = useState(false)
    const [showRefundModal, setShowRefundModal] = useState(false)
    const [reviewData, setReviewData] = useState({ status: 'APPROVED', adminNote: '' })
    const [refundData, setRefundData] = useState({ imageProof: '', note: '' })
    const [submitting, setSubmitting] = useState(false)
    
    const { errorModal, showError, closeError, confirmModal, closeConfirm } = useModals()

    const loadRequests = useCallback(async () => {
        setLoading(true)
        try {
            const response = await ChangeRequestsApi.getChangeRequests({
                page,
                size: 20,
                type: activeTab
            })
            setRequests(response.content || [])
            setTotalPages(response.totalPages || 1)
        } catch (error: unknown) {
            const errorMessage = error && typeof error === 'object' && 'response' in error
                ? String((error.response as { data?: { message?: string } })?.data?.message || 'Không thể tải danh sách yêu cầu')
                : 'Không thể tải danh sách yêu cầu'
            toast.error(errorMessage)
        } finally {
            setLoading(false)
        }
    }, [activeTab, page])

    useEffect(() => {
        loadRequests()
    }, [loadRequests])

    const handleReview = (request: ChangeRequestsApi.ChangeRequest) => {
        setSelectedRequest(request)
        setReviewData({ status: 'APPROVED', adminNote: '' })
        setShowReviewModal(true)
    }

    const handleRefund = (request: ChangeRequestsApi.ChangeRequest) => {
        setSelectedRequest(request)
        setRefundData({ imageProof: '', note: '' })
        setShowRefundModal(true)
    }

    const submitReview = async () => {
        if (!selectedRequest) return
        
        setSubmitting(true)
        try {
            await ChangeRequestsApi.reviewChangeRequest(selectedRequest.id, {
                status: reviewData.status as 'APPROVED' | 'REJECTED',
                adminNote: reviewData.adminNote
            })
            toast.success(`Đã ${reviewData.status === 'APPROVED' ? 'phê duyệt' : 'từ chối'} yêu cầu`)
            loadRequests()
        } catch (error: unknown) {
            const errorMessage = error && typeof error === 'object' && 'response' in error
                ? String((error.response as { data?: { message?: string } })?.data?.message || 'Không thể xử lý yêu cầu')
                : 'Không thể xử lý yêu cầu'
            showError(errorMessage)
        } finally {
            setSubmitting(false)
            setShowReviewModal(false)
        }
    }

    const submitRefund = async () => {
        if (!selectedRequest) return
        
        if (!refundData.imageProof || !refundData.note) {
            toast.warning('Vui lòng nhập đầy đủ thông tin hoàn tiền')
            return
        }
        
        setSubmitting(true)
        try {
            await ChangeRequestsApi.confirmRefund(selectedRequest.id, refundData)
            toast.success('Đã xác nhận hoàn tiền')
            loadRequests()
        } catch (error: unknown) {
            const errorMessage = error && typeof error === 'object' && 'response' in error
                ? String((error.response as { data?: { message?: string } })?.data?.message || 'Không thể xác nhận hoàn tiền')
                : 'Không thể xác nhận hoàn tiền'
            showError(errorMessage)
        } finally {
            setSubmitting(false)
            setShowRefundModal(false)
        }
    }

    const getStatusBadge = (status: string) => {
        const badges: Record<string, { bg: string; text: string; icon: LucideIcon }> = {
            PENDING: { bg: 'bg-yellow-100', text: 'text-yellow-800', icon: Clock },
            APPROVED: { bg: 'bg-green-100', text: 'text-green-800', icon: CheckCircle },
            REJECTED: { bg: 'bg-red-100', text: 'text-red-800', icon: XCircle },
            WAITING_REFUND: { bg: 'bg-blue-100', text: 'text-blue-800', icon: AlertCircle },
            REFUNDED: { bg: 'bg-purple-100', text: 'text-purple-800', icon: Package }
        }
        
        const badge = badges[status] || badges.PENDING
        const Icon = badge.icon
        
        return (
            <span className={`inline-flex items-center gap-1 px-3 py-1 rounded-full text-sm font-medium ${badge.bg} ${badge.text}`}>
                <Icon className="w-4 h-4" />
                {status}
            </span>
        )
    }

    const formatDate = (dateString: string) => {
        return formatInstant(dateString, 'vi-VN')
    }

    return (
        <div className="p-6">
            <div className="mb-6 flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-bold text-gray-900">Quản lý Yêu cầu Hủy/Trả hàng</h1>
                    <p className="text-gray-600 mt-1">Xem và xử lý các yêu cầu từ khách hàng</p>
                </div>
                <button
                    onClick={loadRequests}
                    disabled={loading}
                    className="flex items-center gap-2 px-4 py-2 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 transition disabled:opacity-50"
                >
                    <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
                    Làm mới
                </button>
            </div>

            {/* Tabs */}
            <div className="mb-6 border-b border-gray-200">
                <div className="flex gap-4">
                    <button
                        onClick={() => { setActiveTab('CANCEL'); setPage(0); }}
                        className={`px-4 py-2 font-medium border-b-2 transition ${
                            activeTab === 'CANCEL'
                                ? 'border-red-600 text-red-600'
                                : 'border-transparent text-gray-600 hover:text-gray-900'
                        }`}
                    >
                        Yêu cầu Hủy đơn
                    </button>
                    <button
                        onClick={() => { setActiveTab('RETURN'); setPage(0); }}
                        className={`px-4 py-2 font-medium border-b-2 transition ${
                            activeTab === 'RETURN'
                                ? 'border-red-600 text-red-600'
                                : 'border-transparent text-gray-600 hover:text-gray-900'
                        }`}
                    >
                        Yêu cầu Trả hàng
                    </button>
                </div>
            </div>

            {/* Requests List */}
            {loading ? (
                <div className="flex items-center justify-center py-12">
                    <RefreshCw className="w-8 h-8 animate-spin text-gray-400" />
                </div>
            ) : requests.length === 0 ? (
                <div className="text-center py-12 bg-white rounded-lg shadow">
                    <Package className="w-16 h-16 text-gray-300 mx-auto mb-4" />
                    <p className="text-gray-600">Không có yêu cầu nào</p>
                </div>
            ) : (
                <div className="space-y-4">
                    {requests.map((request) => (
                        <div key={request.id} className="bg-white rounded-lg shadow p-6">
                            <div className="flex items-start justify-between mb-4">
                                <div className="flex-1">
                                    <div className="flex items-center gap-3 mb-2">
                                        <h3 className="text-lg font-semibold">Đơn hàng: {request.orderNumber}</h3>
                                        {getStatusBadge(request.status)}
                                    </div>
                                    <p className="text-sm text-gray-600">
                                        Thời gian: {formatDate(request.createdAt)}
                                    </p>
                                </div>
                                <div className="flex gap-2">
                                    {request.status === 'PENDING' && (
                                        <>
                                            <button
                                                onClick={() => handleReview(request)}
                                                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition"
                                            >
                                                Xem xét
                                            </button>
                                        </>
                                    )}
                                    {request.status === 'WAITING_REFUND' && (
                                        <button
                                            onClick={() => handleRefund(request)}
                                            className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition"
                                        >
                                            Xác nhận hoàn tiền
                                        </button>
                                    )}
                                </div>
                            </div>
                            
                            <div className="grid grid-cols-2 gap-4 text-sm">
                                <div>
                                    <span className="text-gray-600">Lý do:</span>
                                    <p className="font-medium mt-1">{request.reason || 'Không có'}</p>
                                </div>
                                <div>
                                    <span className="text-gray-600">Ghi chú khách hàng:</span>
                                    <p className="font-medium mt-1">{request.note || 'Không có'}</p>
                                </div>
                                {request.adminNote && (
                                    <div className="col-span-2">
                                        <span className="text-gray-600">Ghi chú Admin:</span>
                                        <p className="font-medium mt-1 text-blue-600">{request.adminNote}</p>
                                    </div>
                                )}
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {/* Pagination */}
            {totalPages > 1 && (
                <div className="mt-6 flex justify-center gap-2">
                    <button
                        onClick={() => setPage(Math.max(0, page - 1))}
                        disabled={page === 0}
                        className="px-4 py-2 border rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
                    >
                        Trước
                    </button>
                    <span className="px-4 py-2">
                        Trang {page + 1} / {totalPages}
                    </span>
                    <button
                        onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
                        disabled={page >= totalPages - 1}
                        className="px-4 py-2 border rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
                    >
                        Sau
                    </button>
                </div>
            )}

            {/* Review Modal */}
            {showReviewModal && selectedRequest && (
                <div 
                    className="fixed inset-0 flex items-center justify-center z-[9999] p-4"
                    onClick={() => setShowReviewModal(false)}
                >
                    <div 
                        className="bg-white rounded-lg shadow-2xl max-w-2xl w-full p-6"
                        onClick={(e) => e.stopPropagation()}
                    >
                        <h3 className="text-xl font-bold mb-4">Xem xét yêu cầu {activeTab === 'CANCEL' ? 'hủy' : 'trả'} hàng</h3>
                        
                        <div className="space-y-4 mb-6">
                            <div>
                                <label className="block text-sm font-medium mb-2">Quyết định</label>
                                <select
                                    value={reviewData.status}
                                    onChange={(e) => setReviewData({ ...reviewData, status: e.target.value })}
                                    className="w-full px-4 py-2 border rounded-lg"
                                >
                                    <option value="APPROVED">Phê duyệt</option>
                                    <option value="REJECTED">Từ chối</option>
                                </select>
                            </div>
                            
                            <div>
                                <label className="block text-sm font-medium mb-2">Ghi chú Admin</label>
                                <textarea
                                    value={reviewData.adminNote}
                                    onChange={(e) => setReviewData({ ...reviewData, adminNote: e.target.value })}
                                    rows={4}
                                    className="w-full px-4 py-2 border rounded-lg"
                                    placeholder="Nhập ghi chú..."
                                />
                            </div>
                        </div>
                        
                        <div className="flex gap-3 justify-end">
                            <button
                                onClick={() => setShowReviewModal(false)}
                                disabled={submitting}
                                className="px-6 py-2 border rounded-lg hover:bg-gray-50"
                            >
                                Hủy
                            </button>
                            <button
                                onClick={submitReview}
                                disabled={submitting}
                                className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
                            >
                                {submitting ? 'Đang xử lý...' : 'Xác nhận'}
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Refund Modal */}
            {showRefundModal && selectedRequest && (
                <div 
                    className="fixed inset-0 flex items-center justify-center z-[9999] p-4"
                    onClick={() => setShowRefundModal(false)}
                >
                    <div 
                        className="bg-white rounded-lg shadow-2xl max-w-2xl w-full p-6"
                        onClick={(e) => e.stopPropagation()}
                    >
                        <h3 className="text-xl font-bold mb-4">Xác nhận hoàn tiền</h3>
                        
                        <div className="space-y-4 mb-6">
                            <div>
                                <label className="block text-sm font-medium mb-2">Link ảnh chứng từ *</label>
                                <input
                                    type="text"
                                    value={refundData.imageProof}
                                    onChange={(e) => setRefundData({ ...refundData, imageProof: e.target.value })}
                                    className="w-full px-4 py-2 border rounded-lg"
                                    placeholder="Nhập URL ảnh chứng từ chuyển khoản..."
                                />
                            </div>
                            
                            <div>
                                <label className="block text-sm font-medium mb-2">Ghi chú *</label>
                                <textarea
                                    value={refundData.note}
                                    onChange={(e) => setRefundData({ ...refundData, note: e.target.value })}
                                    rows={4}
                                    className="w-full px-4 py-2 border rounded-lg"
                                    placeholder="Nhập thông tin giao dịch hoàn tiền..."
                                />
                            </div>
                        </div>
                        
                        <div className="flex gap-3 justify-end">
                            <button
                                onClick={() => setShowRefundModal(false)}
                                disabled={submitting}
                                className="px-6 py-2 border rounded-lg hover:bg-gray-50"
                            >
                                Hủy
                            </button>
                            <button
                                onClick={submitRefund}
                                disabled={submitting}
                                className="px-6 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50"
                            >
                                {submitting ? 'Đang xử lý...' : 'Xác nhận hoàn tiền'}
                            </button>
                        </div>
                    </div>
                </div>
            )}

            <ErrorModal
                isOpen={errorModal.isOpen}
                onClose={closeError}
                title={errorModal.title}
                message={errorModal.message}
            />

            <ConfirmModal
                isOpen={confirmModal.isOpen}
                onClose={closeConfirm}
                onConfirm={confirmModal.onConfirm}
                title={confirmModal.title}
                message={confirmModal.message}
            />
        </div>
    )
}
