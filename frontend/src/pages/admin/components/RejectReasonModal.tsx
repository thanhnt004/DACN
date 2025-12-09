import { useState } from 'react';
import { toast } from 'react-toastify';

interface RejectReasonModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSubmit: (reason: string) => void;
    title: string;
    prompt: string;
}

export default function RejectReasonModal({ isOpen, onClose, onSubmit, title, prompt }: RejectReasonModalProps) {
    const [reason, setReason] = useState('');

    if (!isOpen) {
        return null;
    }

    const handleSubmit = () => {
        if (reason.trim()) {
            onSubmit(reason);
        } else {
            toast.warning('Vui lòng nhập lý do từ chối.');
        }
    };

    return (
        <div className="fixed inset-0 bg-transparent backdrop-blur-sm flex items-center justify-center z-[100]">
            <div className="bg-white rounded-lg shadow-xl w-full max-w-md">
                <div className="p-6">
                    <h3 className="text-xl font-semibold mb-4">{title}</h3>
                    <p className="text-gray-600 mb-4">{prompt}</p>
                    <textarea
                        value={reason}
                        onChange={(e) => setReason(e.target.value)}
                        placeholder="Nhập lý do..."
                        className="w-full p-2 border rounded-lg"
                        rows={4}
                    />
                </div>
                <div className="flex justify-end gap-3 p-4 bg-gray-50 rounded-b-lg">
                    <button onClick={onClose} className="px-5 py-2 rounded-lg text-gray-700 bg-gray-100 hover:bg-gray-200">
                        Hủy bỏ
                    </button>
                    <button onClick={handleSubmit} className="px-5 py-2 rounded-lg text-white bg-red-600 hover:bg-red-700">
                        Xác nhận
                    </button>
                </div>
            </div>
        </div>
    );
}
