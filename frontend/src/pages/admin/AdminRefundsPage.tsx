import { useState, useEffect, useCallback } from 'react'
import { CheckCircle, XCircle, Clock, AlertCircle, Package, RefreshCw, LucideIcon, DollarSign, UploadCloud } from 'lucide-react'
import { toast } from 'react-toastify'
import * as ChangeRequestsApi from '../../api/admin/changeRequests'
import { uploadImagesToCloudinary } from '../../api/media'
import ErrorModal from '../../components/common/ErrorModal'
import ConfirmModal from '../../components/common/ConfirmModal'
import { useModals } from '../../hooks/useModals'
import { formatInstant } from '../../lib/dateUtils'

export default function AdminRefundsPage() {
    const [requests, setRequests] = useState<ChangeRequestsApi.ChangeRequest[]>([])
    const [loading, setLoading] = useState(false)
    const [page, setPage] = useState(0)
    const [totalPages, setTotalPages] = useState(1)
    const [selectedRequest, setSelectedRequest] = useState<ChangeRequestsApi.ChangeRequest | null>(null)
    const [showRefundModal, setShowRefundModal] = useState(false)
    const [refundData, setRefundData] = useState({ imageProof: '', note: '' })
    const [refundImageFile, setRefundImageFile] = useState<File | null>(null)
    const [submitting, setSubmitting] = useState(false)
    
    const { errorModal, showError, closeError, confirmModal, closeConfirm } = useModals()

    const loadRequests = useCallback(async () => {
        setLoading(true)
        try {
            // Use the new WAITING_REFUND status/tab
            const response = await ChangeRequestsApi.getChangeRequests({
                page,
                size: 20,
                status: 'WAITING_REFUND'
            })
            setRequests(response.content || [])
            setTotalPages(response.totalPages || 1)
        } catch (error: unknown) {
            const errorMessage = error && typeof error === 'object' && 'response' in error
                ? String((error.response as { data?: { message?: string } })?.data?.message || 'Không thể tải danh sách hoàn tiền')
                : 'Không thể tải danh sách hoàn tiền'
            toast.error(errorMessage)
        } finally {
            setLoading(false)
        }
    }, [page])

    useEffect(() => {
        loadRequests()
    }, [loadRequests])

    const handleRefund = (request: ChangeRequestsApi.ChangeRequest) => {
        setSelectedRequest(request)
        setRefundData({ imageProof: '', note: '' })
        setRefundImageFile(null)
        setShowRefundModal(true)
    }

    const submitRefund = async () => {
        if (!selectedRequest) return
        
        if ((!refundData.imageProof && !refundImageFile) || !refundData.note) {
            toast.warning('Vui lòng nhập đầy đủ thông tin hoàn tiền')
            return
        }
        
        setSubmitting(true)
        try {
            let imageUrl = refundData.imageProof;
            if (refundImageFile) {
                const uploadedUrls = await uploadImagesToCloudinary([refundImageFile], 'refund_proof', selectedRequest.id);
                if (uploadedUrls && uploadedUrls.length > 0) {
                    imageUrl = uploadedUrls[0];
                }
            }

            await ChangeRequestsApi.confirmRefund(selectedRequest.id, {
                ...refundData,
                imageProof: imageUrl
            })
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

    const formatDate = (dateString: string) => {
        return formatInstant(dateString, 'vi-VN')
    }

    return (
        <div className="p-6">
            <div className="mb-6 flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-bold text-gray-900">Xác nhận Hoàn tiền</h1>
                    <p className="text-gray-600 mt-1">Danh sách các đơn hàng chờ hoàn tiền</p>
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

            {/* Requests List */}
            {loading ? (
                <div className="flex items-center justify-center py-12">
                    <RefreshCw className="w-8 h-8 animate-spin text-gray-400" />
                </div>
            ) : requests.length === 0 ? (
                <div className="text-center py-12 bg-white rounded-lg shadow">
                    <DollarSign className="w-16 h-16 text-gray-300 mx-auto mb-4" />
                    <p className="text-gray-600">Không có yêu cầu hoàn tiền nào</p>
                </div>
            ) : (
                <div className="space-y-4">
                    {requests.map((request) => (
                        <div key={request.id} className="bg-white rounded-lg shadow p-6">
                            <div className="flex items-start justify-between mb-4">
                                <div className="flex-1">
                                    <div className="flex items-center gap-3 mb-2">
                                        <h3 className="text-lg font-semibold">Đơn hàng: {request.orderNumber}</h3>
                                        <span className="inline-flex items-center gap-1 px-3 py-1 rounded-full text-sm font-medium bg-blue-100 text-blue-800">
                                            <AlertCircle className="w-4 h-4" />
                                            Chờ hoàn tiền
                                        </span>
                                    </div>
                                    <p className="text-sm text-gray-600">
                                        Thời gian yêu cầu: {formatDate(request.createdAt)}
                                    </p>
                                </div>
                                <div className="flex gap-2">
                                    <button
                                        onClick={() => handleRefund(request)}
                                        className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition flex items-center gap-2"
                                    >
                                        <DollarSign className="w-4 h-4" />
                                        Xác nhận hoàn tiền
                                    </button>
                                </div>
                            </div>
                            
                            <div className="grid grid-cols-2 gap-4 text-sm border-t pt-4">
                                <div>
                                    <span className="text-gray-600">Loại yêu cầu:</span>
                                    <p className="font-medium mt-1">
                                        {request.type === 'CANCEL' ? 'Hủy đơn' : 'Trả hàng'}
                                    </p>
                                </div>
                                <div>
                                    <span className="text-gray-600">Lý do:</span>
                                    <p className="font-medium mt-1">{request.reason || 'Không có'}</p>
                                </div>
                                {request.metadata && request.metadata.refundMethod && (
                                    <div className="col-span-2 bg-gray-50 p-3 rounded">
                                        <span className="text-gray-600 font-medium">Thông tin hoàn tiền:</span>
                                        <div className="mt-1 grid grid-cols-2 gap-4">
                                            <div>
                                                <span className="text-gray-500 text-xs">Phương thức:</span>
                                                <p>{request.metadata.refundMethod}</p>
                                            </div>
                                            {request.metadata.refundData && (
                                                <div>
                                                    <span className="text-gray-500 text-xs">Chi tiết:</span>
                                                    <p>{JSON.stringify(request.metadata.refundData)}</p>
                                                </div>
                                            )}
                                        </div>
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
                        <h3 className="text-xl font-bold mb-4">Xác nhận hoàn tiền - {selectedRequest.orderNumber}</h3>
                        
                        <div className="space-y-4 mb-6">
                            <div className="bg-blue-50 p-4 rounded-lg text-sm text-blue-800 mb-4">
                                <p className="font-bold mb-1">Thông tin khách hàng cung cấp:</p>
                                <p>Phương thức: {selectedRequest.metadata?.refundMethod || 'Không có'}</p>
                                {selectedRequest.metadata?.refundData && (
                                    <div className="mt-1">
                                        <p className="font-semibold">Chi tiết:</p>
                                        <pre className="whitespace-pre-wrap text-xs mt-1">
                                            {JSON.stringify(selectedRequest.metadata.refundData, null, 2)}
                                        </pre>
                                    </div>
                                )}
                            </div>

                            <div>
                                <label className="block text-sm font-medium mb-2">Link ảnh chứng từ chuyển khoản *</label>
                                <div className="mt-2 p-6 border-2 border-dashed rounded-lg text-center hover:bg-gray-50 transition-colors relative">
                                    {refundImageFile ? (
                                        <div className="relative inline-block">
                                            <img 
                                                src={URL.createObjectURL(refundImageFile)} 
                                                alt="Preview" 
                                                className="h-32 object-contain rounded"
                                            />
                                            <button 
                                                onClick={() => setRefundImageFile(null)}
                                                className="absolute -top-2 -right-2 bg-red-500 text-white rounded-full p-1 hover:bg-red-600"
                                            >
                                                <XCircle className="w-4 h-4" />
                                            </button>
                                            <p className="text-sm text-gray-500 mt-2">{refundImageFile.name}</p>
                                        </div>
                                    ) : (
                                        <>
                                            <UploadCloud className="mx-auto h-12 w-12 text-gray-400" />
                                            <div className="mt-2">
                                                <label htmlFor="file-upload" className="cursor-pointer">
                                                    <span className="mt-2 block text-sm font-medium text-gray-900">
                                                        Kéo thả hoặc chọn ảnh
                                                    </span>
                                                    <input 
                                                        id="file-upload"
                                                        type="file" 
                                                        accept="image/*" 
                                                        onChange={(e) => {
                                                            if (e.target.files && e.target.files[0]) {
                                                                setRefundImageFile(e.target.files[0])
                                                            }
                                                        }} 
                                                        className="absolute inset-0 w-full h-full opacity-0 cursor-pointer" 
                                                    />
                                                </label>
                                            </div>
                                            <p className="text-xs text-gray-500 mt-1">PNG, JPG, GIF up to 10MB</p>
                                        </>
                                    )}
                                </div>
                                <div className="mt-2 flex items-center gap-2">
                                    <span className="text-xs text-gray-500">Hoặc nhập URL:</span>
                                    <input
                                        type="text"
                                        value={refundData.imageProof}
                                        onChange={(e) => setRefundData({ ...refundData, imageProof: e.target.value })}
                                        className="flex-1 px-3 py-1 text-sm border rounded"
                                        placeholder="https://..."
                                    />
                                </div>
                            </div>
                            
                            <div>
                                <label className="block text-sm font-medium mb-2">Ghi chú giao dịch *</label>
                                <textarea
                                    value={refundData.note}
                                    onChange={(e) => setRefundData({ ...refundData, note: e.target.value })}
                                    rows={4}
                                    className="w-full px-4 py-2 border rounded-lg"
                                    placeholder="Nhập mã giao dịch, nội dung chuyển khoản..."
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