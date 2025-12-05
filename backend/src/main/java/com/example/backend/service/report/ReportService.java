package com.example.backend.service.report;

import com.example.backend.dto.response.report.ReportProjections;
import com.example.backend.dto.response.report.ReportResponse;
import com.example.backend.exception.BadRequestException;
import com.example.backend.repository.report.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private static final String DEFAULT_TIMEZONE = "Asia/Ho_Chi_Minh";
    private static final int DEFAULT_TOP_LIMIT = 10;

    /**
     * Get dashboard overview statistics
     */
    public ReportResponse.DashboardOverviewDTO getDashboardOverview(LocalDate startDate, LocalDate endDate, String timezone) {
        validateDateRange(startDate, endDate);

        Instant start = convertToInstant(startDate, timezone, true);
        Instant end = convertToInstant(endDate, timezone, false);

        ReportProjections.DashboardOverview data = reportRepository.getDashboardOverview(start, end);

        return ReportResponse.DashboardOverviewDTO.builder()
                .totalOrders(data.getTotalOrders())
                .totalRevenue(data.getTotalRevenue())
                .totalDiscount(data.getTotalDiscount())
                .pendingOrders(data.getPendingOrders())
                .startDate(start)
                .endDate(end)
                .build();
    }

    /**
     * Get revenue chart data
     */
    public ReportResponse.RevenueChartDTO getRevenueChart(LocalDate startDate, LocalDate endDate, String timezone) {
        validateDateRange(startDate, endDate);

        Instant start = convertToInstant(startDate, timezone, true);
        Instant end = convertToInstant(endDate, timezone, false);
        String tz = timezone != null ? timezone : DEFAULT_TIMEZONE;

        List<ReportProjections.RevenueChartPoint> chartData = reportRepository.getRevenueChart(start, end, tz);

        return ReportResponse.RevenueChartDTO.builder()
                .dataPoints(chartData)
                .startDate(start)
                .endDate(end)
                .build();
    }

    /**
     * Get top categories by revenue
     */
    public ReportResponse.CategoryRevenueDTO getTopCategoryRevenue(LocalDate startDate, LocalDate endDate, String timezone) {
        validateDateRange(startDate, endDate);

        Instant start = convertToInstant(startDate, timezone, true);
        Instant end = convertToInstant(endDate, timezone, false);

        List<ReportProjections.CategoryRevenue> categories = reportRepository.getTopCategoryRevenue(start, end);

        return ReportResponse.CategoryRevenueDTO.builder()
                .categories(categories)
                .startDate(start)
                .endDate(end)
                .build();
    }

    /**
     * Get top selling products
     */
    public ReportResponse.TopProductsDTO getTopSellingProducts(LocalDate startDate, LocalDate endDate, String timezone, Integer limit) {
        validateDateRange(startDate, endDate);

        Instant start = convertToInstant(startDate, timezone, true);
        Instant end = convertToInstant(endDate, timezone, false);
        int topLimit = limit != null && limit > 0 ? limit : DEFAULT_TOP_LIMIT;

        List<ReportProjections.TopProductStats> products = reportRepository.getTopSellingProducts(start, end, topLimit);

        return ReportResponse.TopProductsDTO.builder()
                .products(products)
                .startDate(start)
                .endDate(end)
                .limit(topLimit)
                .build();
    }

    /**
     * Get low stock items (inventory alert)
     */
    public ReportResponse.LowStockDTO getLowStockItems(Integer limit) {
        int stockLimit = limit != null && limit > 0 ? limit : DEFAULT_TOP_LIMIT;

        List<ReportProjections.LowStockItem> items = reportRepository.getLowStockItems(stockLimit);

        return ReportResponse.LowStockDTO.builder()
                .items(items)
                .limit(stockLimit)
                .build();
    }

    /**
     * Get top spending customers
     */
    public ReportResponse.TopCustomersDTO getTopSpenders(LocalDate startDate, LocalDate endDate, String timezone, Integer limit) {
        validateDateRange(startDate, endDate);

        Instant start = convertToInstant(startDate, timezone, true);
        Instant end = convertToInstant(endDate, timezone, false);
        int topLimit = limit != null && limit > 0 ? limit : DEFAULT_TOP_LIMIT;

        List<ReportProjections.CustomerSpending> customers = reportRepository.getTopSpenders(start, end, topLimit);

        return ReportResponse.TopCustomersDTO.builder()
                .customers(customers)
                .startDate(start)
                .endDate(end)
                .limit(topLimit)
                .build();
    }

    /**
     * Get comprehensive report with all metrics
     */
    public ReportResponse.ComprehensiveReportDTO getComprehensiveReport(LocalDate startDate, LocalDate endDate, String timezone) {
        validateDateRange(startDate, endDate);

        return ReportResponse.ComprehensiveReportDTO.builder()
                .overview(getDashboardOverview(startDate, endDate, timezone))
                .revenueChart(getRevenueChart(startDate, endDate, timezone))
                .topCategories(getTopCategoryRevenue(startDate, endDate, timezone))
                .topProducts(getTopSellingProducts(startDate, endDate, timezone, 10))
                .topCustomers(getTopSpenders(startDate, endDate, timezone, 10))
                .lowStockItems(getLowStockItems(10))
                .generatedAt(Instant.now())
                .build();
    }

    /**
     * Get profit report
     */
    public ReportResponse.ProfitReportDTO getProfitReport(LocalDate startDate, LocalDate endDate, String timezone) {
        validateDateRange(startDate, endDate);

        Instant start = convertToInstant(startDate, timezone, true);
        Instant end = convertToInstant(endDate, timezone, false);

        ReportProjections.ProfitStats stats = reportRepository.getProfitStats(start, end);

        BigDecimal totalRevenue = stats.getTotalRevenue();
        BigDecimal totalCost = stats.getTotalCost();
        BigDecimal grossProfit = totalRevenue.subtract(totalCost);

        return ReportResponse.ProfitReportDTO.builder()
                .startDate(start)
                .endDate(end)
                .totalRevenue(totalRevenue)
                .totalCost(totalCost)
                .grossProfit(grossProfit)
                .build();
    }

    /**
     * Validate date range
     */
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BadRequestException("Ngày bắt đầu và ngày kết thúc không được để trống");
        }

        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("Ngày bắt đầu phải trước ngày kết thúc");
        }

        if (startDate.isAfter(LocalDate.now())) {
            throw new BadRequestException("Ngày bắt đầu không được ở tương lai");
        }
    }

    /**
     * Convert LocalDate to Instant with timezone consideration
     */
    private Instant convertToInstant(LocalDate date, String timezone, boolean startOfDay) {
        String tz = timezone != null ? timezone : DEFAULT_TIMEZONE;
        ZoneId zoneId = ZoneId.of(tz);

        ZonedDateTime zonedDateTime = startOfDay
            ? date.atStartOfDay(zoneId)
            : date.plusDays(1).atStartOfDay(zoneId).minusNanos(1);

        return zonedDateTime.toInstant();
    }
}
