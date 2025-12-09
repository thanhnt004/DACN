import api from '../http'

export interface DashboardOverview {
    totalOrders: number
    totalRevenue: number
    totalDiscount: number
    pendingOrders: number
    startDate: string
    endDate: string
}

export interface RevenueChartPoint {
    reportDate: string
    orderCount: number
    revenue: number
}

export interface RevenueChart {
    dataPoints: RevenueChartPoint[]
    startDate: string
    endDate: string
}

export interface CategoryRevenue {
    categoryName: string
    totalRevenue: number
    totalSold: number
}

export interface CategoryRevenueReport {
    categories: CategoryRevenue[]
    startDate: string
    endDate: string
}

export interface TopProduct {
    productId: string
    productName: string
    primaryImageUrl: string | null
    quantitySold: number
    revenueGenerated: number
}

export interface TopProductsReport {
    products: TopProduct[]
    startDate: string
    endDate: string
    limit: number
}

export interface ProfitReport {
    startDate: string
    endDate: string
    totalRevenue: number
    totalCost: number
    grossProfit: number
}

export interface MonthlyProfitPoint {
    month: string
    totalRevenue: number
    totalCost: number
    grossProfit: number
    orderCount: number
}

export interface MonthlyProfitReport {
    dataPoints: MonthlyProfitPoint[]
    startDate: string
    endDate: string
}

export interface CategoryProfitItem {
    categoryName: string
    totalRevenue: number
    totalCost: number
    grossProfit: number
    profitMargin: number
}

export interface CategoryProfitReport {
    categories: CategoryProfitItem[]
    startDate: string
    endDate: string
}

export interface ReportParams {
    startDate: string // ISO date string YYYY-MM-DD
    endDate: string
    timezone?: string
    limit?: number
}

export const getDashboardOverview = async (params: ReportParams): Promise<DashboardOverview> => {
    const response = await api.get<DashboardOverview>('/api/v1/reports/dashboard-overview', { params })
    return response.data
}

export const getRevenueChart = async (params: ReportParams): Promise<RevenueChart> => {
    const response = await api.get<RevenueChart>('/api/v1/reports/revenue-chart', { params })
    return response.data
}

export const getTopCategoryRevenue = async (params: ReportParams): Promise<CategoryRevenueReport> => {
    const response = await api.get<CategoryRevenueReport>('/api/v1/reports/top-category-revenue', { params })
    return response.data
}

export const getTopSellingProducts = async (params: ReportParams): Promise<TopProductsReport> => {
    const response = await api.get<TopProductsReport>('/api/v1/reports/top-selling-products', { params })
    return response.data
}

export const getProfitReport = async (params: ReportParams): Promise<ProfitReport> => {
    const response = await api.get<ProfitReport>('/api/v1/reports/profit-report', { params })
    return response.data
}

export const getMonthlyProfitReport = async (params: ReportParams): Promise<MonthlyProfitReport> => {
    const response = await api.get<MonthlyProfitReport>('/api/v1/reports/monthly-profit', { params })
    return response.data
}

export const getCategoryProfitReport = async (params: ReportParams): Promise<CategoryProfitReport> => {
    const response = await api.get<CategoryProfitReport>('/api/v1/reports/category-profit', { params })
    return response.data
}

export const exportTopSellingProductsToExcel = async (params: ReportParams): Promise<Blob> => {
    const response = await api.get('/api/v1/reports/top-selling-products/export', {
        params,
        responseType: 'blob'
    })
    return response.data
}
