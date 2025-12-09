import { useEffect, useState } from 'react'
import { toast } from 'react-toastify'
import * as BrandCategoryApi from '../../api/admin/brandCategory'
import type { CategoryResponse, CategoryCreateRequest, CategoryUpdateRequest } from '../../api/admin/brandCategory'
import { ChevronRight, ChevronDown, Plus, Edit, Trash2, GripVertical } from 'lucide-react'
import { resolveErrorMessage } from '../../lib/problemDetails'

interface TreeItemProps {
    category: CategoryResponse
    level: number
    onEdit: (category: CategoryResponse) => void
    onDelete: (id: string) => void
    onMove: (categoryId: string, newParentId: string | null) => void
    allCategories: CategoryResponse[]
}

function CategoryTreeItem({ category, level, onEdit, onDelete, onMove, allCategories }: TreeItemProps) {
    const [isExpanded, setIsExpanded] = useState(true)
    const [isDragging, setIsDragging] = useState(false)
    const [isDragOver, setIsDragOver] = useState(false)

    const hasChildren = category.children && category.children.length > 0

    const handleDragStart = (e: React.DragEvent) => {
        setIsDragging(true)
        e.dataTransfer.effectAllowed = 'move'
        e.dataTransfer.setData('categoryId', category.id)
    }

    const handleDragEnd = () => {
        setIsDragging(false)
    }

    const handleDragOver = (e: React.DragEvent) => {
        e.preventDefault()
        e.stopPropagation()
        setIsDragOver(true)
    }

    const handleDragLeave = (e: React.DragEvent) => {
        e.preventDefault()
        e.stopPropagation()
        setIsDragOver(false)
    }

    const handleDrop = (e: React.DragEvent) => {
        e.preventDefault()
        e.stopPropagation()
        setIsDragOver(false)

        const draggedCategoryId = e.dataTransfer.getData('categoryId')

        // Don't allow dropping on itself
        if (draggedCategoryId === category.id) {
            return
        }

        // Don't allow dropping a parent into its own child
        const isDescendant = (parent: CategoryResponse, childId: string): boolean => {
            if (parent.id === childId) return true
            if (!parent.children) return false
            return parent.children.some(child => isDescendant(child, childId))
        }

        const draggedCategory = allCategories.find(c => c.id === draggedCategoryId)
        if (draggedCategory && isDescendant(draggedCategory, category.id)) {
            toast.error('Không thể di chuyển danh mục cha vào danh mục con của nó')
            return
        }

        onMove(draggedCategoryId, category.id)
    }

    return (
        <div className="select-none">
            <div
                draggable
                onDragStart={handleDragStart}
                onDragEnd={handleDragEnd}
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
                onDrop={handleDrop}
                className={`
                    flex items-center gap-2 p-2 rounded hover:bg-gray-50 cursor-move
                    ${isDragging ? 'opacity-50' : ''}
                    ${isDragOver ? 'bg-blue-50 border-2 border-blue-400' : 'border-2 border-transparent'}
                `}
                style={{ paddingLeft: `${level * 24 + 8}px` }}
            >
                {/* Drag Handle */}
                <GripVertical className="w-4 h-4 text-gray-400" />

                {/* Expand/Collapse Button */}
                <button
                    onClick={() => setIsExpanded(!isExpanded)}
                    className="flex-shrink-0"
                    disabled={!hasChildren}
                >
                    {hasChildren ? (
                        isExpanded ? (
                            <ChevronDown className="w-4 h-4" />
                        ) : (
                            <ChevronRight className="w-4 h-4" />
                        )
                    ) : (
                        <span className="w-4 h-4 inline-block" />
                    )}
                </button>

                {/* Category Info */}
                <div className="flex-1 flex items-center gap-4">
                    <span className="font-medium">{category.name}</span>
                    <span className="text-sm text-gray-500">({category.slug})</span>
                    <span className="text-xs bg-blue-100 text-blue-700 px-2 py-1 rounded">
                        Level {category.level}
                    </span>
                    <span className="text-xs bg-green-100 text-green-700 px-2 py-1 rounded">
                        {category.productsCount ?? 0} sản phẩm
                    </span>
                </div>

                {/* Action Buttons */}
                <div className="flex gap-2">
                    <button
                        onClick={(e) => {
                            e.stopPropagation()
                            onEdit(category)
                        }}
                        className="p-1 text-blue-600 hover:bg-blue-50 rounded"
                        title="Sửa"
                    >
                        <Edit className="w-4 h-4" />
                    </button>
                    <button
                        onClick={(e) => {
                            e.stopPropagation()
                            onDelete(category.id)
                        }}
                        className="p-1 text-red-600 hover:bg-red-50 rounded"
                        title="Xóa"
                    >
                        <Trash2 className="w-4 h-4" />
                    </button>
                </div>
            </div>

            {/* Children */}
            {hasChildren && isExpanded && (
                <div>
                    {category.children!.map((child) => (
                        <CategoryTreeItem
                            key={child.id}
                            category={child}
                            level={level + 1}
                            onEdit={onEdit}
                            onDelete={onDelete}
                            onMove={onMove}
                            allCategories={allCategories}
                        />
                    ))}
                </div>
            )}
        </div>
    )
}

export default function CategoryManager() {
    const [categoryTree, setCategoryTree] = useState<CategoryResponse | null>(null)
    const [allCategories, setAllCategories] = useState<CategoryResponse[]>([])
    const [loading, setLoading] = useState(false)
    const [showModal, setShowModal] = useState(false)
    const [editingCategory, setEditingCategory] = useState<CategoryResponse | null>(null)
    const [formData, setFormData] = useState<{
        id?: string
        name: string
        slug: string
        description?: string
        parentId?: string
    }>({ name: '', slug: '' })

    // Auto-generate slug from name
    const generateSlug = (name: string) => {
        return name
            .toLowerCase()
            .normalize('NFD')
            .replace(/[\u0300-\u036f]/g, '') // Remove diacritics
            .replace(/đ/g, 'd')
            .replace(/[^a-z0-9]+/g, '-')
            .replace(/^-+|-+$/g, '')
    }

    // Flatten tree to get all categories
    const flattenTree = (node: CategoryResponse): CategoryResponse[] => {
        const result: CategoryResponse[] = [node]
        if (node.children) {
            node.children.forEach(child => {
                result.push(...flattenTree(child))
            })
        }
        return result
    }

    const fetchCategories = async () => {
        setLoading(true)
        try {
            const tree = await BrandCategoryApi.getCategoryTree(undefined, 10)
            setCategoryTree(tree)

            // Flatten for dropdown and validation
            if (tree && tree.children) {
                const flattened = tree.children.flatMap(child => flattenTree(child))
                setAllCategories(flattened)
            }
        } catch (error) {
            console.error('Error fetching categories:', error)
            toast.error('Lỗi tải categories')
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        fetchCategories()
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [])

    const handleSubmit = async () => {
        if (!formData.name.trim()) {
            toast.warning('Vui lòng nhập tên danh mục')
            return
        }
        if (!formData.slug.trim()) {
            toast.warning('Vui lòng nhập slug')
            return
        }

        try {
            if (editingCategory?.id) {
                const updateData: CategoryUpdateRequest = {
                    name: formData.name,
                    slug: formData.slug,
                    description: formData.description,
                    parentId: formData.parentId
                }
                const updated = await BrandCategoryApi.updateCategory(editingCategory.id, updateData)
                // Cập nhật trong danh sách
                setCategories(prev => prev.map(c => c.id === editingCategory.id ? updated : c))
                toast.success('Cập nhật danh mục thành công')
            } else {
                const createData: CategoryCreateRequest = {
                    name: formData.name,
                    slug: formData.slug,
                    description: formData.description,
                    parentId: formData.parentId
                }
                const newCategory = await BrandCategoryApi.createCategory(createData)
                // Thêm vào danh sách
                setCategories(prev => [newCategory, ...prev])
                toast.success('Tạo danh mục thành công')
            }
            setShowModal(false)
            setEditingCategory(null)
            setFormData({ name: '', slug: '' })
        } catch (error) {
            console.error('Error saving category:', error)
            const errorMsg = resolveErrorMessage(error, 'Lỗi lưu category')
            toast.error(errorMsg)
        }
    }

    const handleDelete = async (id: string) => {
        if (!confirm('Bạn có chắc muốn xóa danh mục này?')) return
        try {
            // Optimistic update - xóa khỏi UI
            setCategories(prev => prev.filter(c => c.id !== id))
            await BrandCategoryApi.deleteCategory(id)
            toast.success('Xóa danh mục thành công')
        } catch (error) {
            console.error('Error deleting category:', error)
            // Rollback nếu lỗi
            fetchCategories()
            const errorMsg = resolveErrorMessage(error, 'Lỗi xóa category')
            toast.error(errorMsg)
        }
    }

    const handleMove = async (categoryId: string, newParentId: string | null) => {
        try {
            await BrandCategoryApi.moveCategory(categoryId, newParentId || undefined)
            // Fetch lại vì cấu trúc cây thay đổi
            fetchCategories()
            toast.success('Di chuyển danh mục thành công')
        } catch (error) {
            console.error('Error moving category:', error)
            const errorMsg = resolveErrorMessage(error, 'Lỗi di chuyển danh mục')
            toast.error(errorMsg)
        }
    }

    const openCreateModal = (parentId?: string) => {
        setEditingCategory(null)
        setFormData({ name: '', slug: '', parentId })
        setShowModal(true)
    }

    const openEditModal = (category: CategoryResponse) => {
        setEditingCategory(category)
        setFormData({
            id: category.id,
            name: category.name,
            slug: category.slug,
            description: category.description,
            parentId: category.parentId
        })
        setShowModal(true)
    }

    return (
        <div className="space-y-4">
            <div className="flex justify-between items-center">
                <h1 className="text-2xl font-bold">Quản lý danh mục</h1>
                <button
                    onClick={() => openCreateModal()}
                    className="bg-red-600 text-white px-4 py-2 rounded hover:bg-red-700 flex items-center gap-2"
                >
                    <Plus className="w-4 h-4" />
                    Thêm danh mục
                </button>
            </div>

            {loading ? (
                <div className="bg-white shadow rounded p-6 text-center">
                    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-red-600 mx-auto"></div>
                    <p className="mt-2 text-gray-600">Đang tải...</p>
                </div>
            ) : !categoryTree || !categoryTree.children || categoryTree.children.length === 0 ? (
                <div className="bg-white shadow rounded p-6 text-center text-gray-600">
                    <p>Chưa có danh mục nào.</p>
                    <button
                        onClick={() => openCreateModal()}
                        className="mt-4 text-red-600 hover:underline"
                    >
                        Tạo danh mục đầu tiên
                    </button>
                </div>
            ) : (
                <div className="bg-white shadow rounded p-4">
                    <div className="mb-4 p-3 bg-blue-50 border border-blue-200 rounded">
                        <p className="text-sm text-blue-700">
                            💡 <strong>Hướng dẫn:</strong> Kéo và thả danh mục để di chuyển sang danh mục cha khác.
                            Click vào mũi tên để thu gọn/mở rộng.
                        </p>
                    </div>

                    <div
                        className="border rounded p-2"
                        onDragOver={(e) => e.preventDefault()}
                        onDrop={(e) => {
                            e.preventDefault()
                            const draggedCategoryId = e.dataTransfer.getData('categoryId')
                            handleMove(draggedCategoryId, null)
                        }}
                    >
                        {categoryTree.children.map((category) => (
                            <CategoryTreeItem
                                key={category.id}
                                category={category}
                                level={0}
                                onEdit={openEditModal}
                                onDelete={handleDelete}
                                onMove={handleMove}
                                allCategories={allCategories}
                            />
                        ))}
                    </div>
                </div>
            )}

            {/* Create/Edit Modal */}
            {showModal && (
                <div className="fixed inset-0 bg-transparent flex items-center justify-center z-50">
                    <div className="bg-white rounded-lg p-6 w-full max-w-md space-y-4 shadow-xl">
                        <h2 className="text-xl font-bold">
                            {editingCategory ? 'Sửa danh mục' : 'Thêm danh mục'}
                        </h2>
                        <div>
                            <label className="block text-sm font-medium mb-1">Tên *</label>
                            <input
                                type="text"
                                value={formData.name}
                                onChange={(e) => {
                                    const newName = e.target.value
                                    setFormData({
                                        ...formData,
                                        name: newName,
                                        // Auto-generate slug only when creating new category
                                        ...(editingCategory ? {} : { slug: generateSlug(newName) })
                                    })
                                }}
                                className="w-full border rounded px-3 py-2"
                                placeholder="Tên danh mục"
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium mb-1">Slug *</label>
                            <input
                                type="text"
                                value={formData.slug}
                                onChange={(e) => setFormData({ ...formData, slug: e.target.value })}
                                className="w-full border rounded px-3 py-2"
                                placeholder="vi-du-slug"
                            />
                            <p className="text-xs text-gray-500 mt-1">URL-friendly identifier (tự động tạo từ tên)</p>
                        </div>
                        <div>
                            <label className="block text-sm font-medium mb-1">Mô tả</label>
                            <textarea
                                value={formData.description || ''}
                                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                                className="w-full border rounded px-3 py-2"
                                rows={3}
                                placeholder="Mô tả danh mục..."
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium mb-1">Danh mục cha</label>
                            <select
                                value={formData.parentId || ''}
                                onChange={(e) => setFormData({ ...formData, parentId: e.target.value || undefined })}
                                className="w-full border rounded px-3 py-2"
                            >
                                <option value="">-- Danh mục gốc --</option>
                                {allCategories
                                    .filter(c => c.id !== editingCategory?.id) // Don't allow selecting itself
                                    .map((c) => (
                                        <option key={c.id} value={c.id}>
                                            {'  '.repeat(c.level)} {c.name}
                                        </option>
                                    ))}
                            </select>
                        </div>
                        <div className="flex gap-2 justify-end pt-4">
                            <button
                                onClick={() => {
                                    setShowModal(false)
                                    setEditingCategory(null)
                                    setFormData({ name: '', slug: '' })
                                }}
                                className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50"
                            >
                                Hủy
                            </button>
                            <button
                                onClick={handleSubmit}
                                className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700"
                            >
                                {editingCategory ? 'Cập nhật' : 'Tạo'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    )
}
