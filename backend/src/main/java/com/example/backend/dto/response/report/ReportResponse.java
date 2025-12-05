package com.example.backend.dto.response.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class ReportResponse {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardOverviewDTO {
        private Long totalOrders;
        private BigDecimal totalRevenue;
        private BigDecimal totalDiscount;
        private Long pendingOrders;
        private Instant startDate;
        private Instant endDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueChartDTO {
        private List<ReportProjections.RevenueChartPoint> dataPoints;
        private Instant startDate;
        private Instant endDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryRevenueDTO {
        private List<ReportProjections.CategoryRevenue> categories;
        private Instant startDate;
        private Instant endDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopProductsDTO {
        private List<ReportProjections.TopProductStats> products;
        private Instant startDate;
        private Instant endDate;
        private Integer limit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LowStockDTO {
        private List<ReportProjections.LowStockItem> items;
        private Integer limit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopCustomersDTO {
        private List<ReportProjections.CustomerSpending> customers;
        private Instant startDate;
        private Instant endDate;
        private Integer limit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComprehensiveReportDTO {
        private DashboardOverviewDTO overview;
        private RevenueChartDTO revenueChart;
        private CategoryRevenueDTO topCategories;
        private TopProductsDTO topProducts;
        private TopCustomersDTO topCustomers;
        private LowStockDTO lowStockItems;
        private Instant generatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfitReportDTO {
        private Instant startDate;
        private Instant endDate;
        private BigDecimal totalRevenue;
        private BigDecimal totalCost;
        private BigDecimal grossProfit;
    }
}
