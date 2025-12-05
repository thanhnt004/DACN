import { create } from 'zustand'
import * as BrandCategoryApi from '../api/admin/brandCategory'

export interface CategoryWithChildren extends BrandCategoryApi.CategoryResponse {
    children?: BrandCategoryApi.CategoryResponse[]
}

interface CategoriesState {
    categories: CategoryWithChildren[]
    loading: boolean
    loaded: boolean
    error: string | null
    
    fetchCategories: () => Promise<void>
    clearCategories: () => void
}

export const useCategoriesStore = create<CategoriesState>((set, get) => ({
    categories: [],
    loading: false,
    loaded: false,
    error: null,

    fetchCategories: async () => {
        // Don't fetch if already loaded or currently loading
        const { loaded, loading } = get()
        if (loaded || loading) {
            return
        }

        set({ loading: true, error: null })

        try {
            // Load all categories
            const response = await BrandCategoryApi.getCategories({
                size: 100
            })

            // Filter only root categories (level 0 or no parentId)
            const rootCategories = (response.content || []).filter(
                cat => cat.level === 0 || !cat.parentId
            )

            // Load children for each root category in parallel
            const categoriesWithChildren = await Promise.all(
                rootCategories.map(async (rootCategory) => {
                    try {
                        const childrenResponse = await BrandCategoryApi.getCategories({
                            parentId: rootCategory.id,
                            size: 50
                        })
                        return {
                            ...rootCategory,
                            children: childrenResponse.content || []
                        }
                    } catch {
                        return { ...rootCategory, children: [] }
                    }
                })
            )

            set({ 
                categories: categoriesWithChildren, 
                loading: false, 
                loaded: true,
                error: null 
            })
        } catch (error) {
            console.error('Failed to load categories:', error)
            set({ 
                categories: [], 
                loading: false, 
                loaded: true,
                error: 'Failed to load categories' 
            })
        }
    },

    clearCategories: () => {
        set({ categories: [], loading: false, loaded: false, error: null })
    }
}))
