package com.example.backend.dto.response.report;

import java.math.BigDecimal;

public class ReportProjections {

    // 1. Dashboard Overview
    public interface DashboardOverview {
        Long getTotalOrders();
        BigDecimal getTotalRevenue();
        BigDecimal getTotalDiscount();
        Long getPendingOrders();
    }

    // 2. Biểu đồ doanh thu
    public interface RevenueChartPoint {
        String getReportDate(); // Trả về chuỗi YYYY-MM-DD từ SQL
        Long getOrderCount();
        BigDecimal getRevenue();
    }

    // 3. Doanh thu theo danh mục
    public interface CategoryRevenue {
        String getCategoryName();
        BigDecimal getTotalRevenue();
        Long getTotalSold();
    }

    // 4. Top sản phẩm bán chạy
    public interface TopProductStats {
        String getProductId(); // UUID dạng String
        String getProductName();
        String getPrimaryImageUrl();
        Long getQuantitySold();
        BigDecimal getRevenueGenerated();
    }

    // 5. Cảnh báo tồn kho thấp
    public interface LowStockItem {
        String getProductName();
        String getSku();
        String getSizeName();
        String getColorName();
        Integer getQuantityOnHand();
        Integer getReorderLevel();
    }

    // 6. Top khách hàng (VIP)
    public interface CustomerSpending {
        String getUserId();
        String getFullName();
        String getEmail();
        Long getTotalOrders();
        BigDecimal getTotalSpent();
    }

    // 7. Thống kê lợi nhuận
    public interface ProfitStats {
        BigDecimal getTotalRevenue();
        BigDecimal getTotalCost();
    }
}