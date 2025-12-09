package com.example.backend.service.report;

import com.example.backend.dto.response.report.ReportProjections;
import com.example.backend.dto.response.report.ReportResponse;
import com.example.backend.exception.BadRequestException;
import com.example.backend.repository.report.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
        long pendingOrders = reportRepository.countByStatus(com.example.backend.model.order.Order.OrderStatus.PENDING);

        return ReportResponse.DashboardOverviewDTO.builder()
                .totalOrders(data.getTotalOrders())
                .totalRevenue(data.getTotalRevenue())
                .totalDiscount(data.getTotalDiscount())
                .pendingOrders(pendingOrders)
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
     * Get profit by month report
     */
    public ReportResponse.ProfitByMonthDTO getProfitByMonth(LocalDate startDate, LocalDate endDate, String timezone) {
        validateDateRange(startDate, endDate);

        Instant start = convertToInstant(startDate, timezone, true);
        Instant end = convertToInstant(endDate, timezone, false);
        String tz = timezone != null ? timezone : DEFAULT_TIMEZONE;

        List<ReportProjections.MonthlyProfitProjection> dataPoints = reportRepository.findMonthlyProfit(start, end, tz);

        return ReportResponse.ProfitByMonthDTO.builder()
                .dataPoints(dataPoints)
                .startDate(start)
                .endDate(end)
                .build();
    }

    /**
     * Get profit by category report
     */
    public ReportResponse.ProfitByCategoryDTO getProfitByCategory(LocalDate startDate, LocalDate endDate, String timezone) {
        validateDateRange(startDate, endDate);

        Instant start = convertToInstant(startDate, timezone, true);
        Instant end = convertToInstant(endDate, timezone, false);

        List<ReportProjections.CategoryProfitProjection> categories = reportRepository.findCategoryProfit(start, end);

        return ReportResponse.ProfitByCategoryDTO.builder()
                .categories(categories)
                .startDate(start)
                .endDate(end)
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

    /**
     * Export top selling products to Excel
     */
    public byte[] exportTopSellingProductsToExcel(LocalDate startDate, LocalDate endDate, String timezone, Integer limit) {
        validateDateRange(startDate, endDate);

        Instant start = convertToInstant(startDate, timezone, true);
        Instant end = convertToInstant(endDate, timezone, false);
        int topLimit = limit != null && limit > 0 ? limit : DEFAULT_TOP_LIMIT;

        List<ReportProjections.TopProductStats> products = reportRepository.getTopSellingProducts(start, end, topLimit);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Top Sản Phẩm Bán Chạy");

            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Create currency style
            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            currencyStyle.setDataFormat(format.getFormat("#,##0 ₫"));

            // Create number style
            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.setDataFormat(format.getFormat("#,##0"));

            // Create title row
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BÁO CÁO TOP SẢN PHẨM BÁN CHẠY");
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 4));

            // Create info rows
            Row dateRangeRow = sheet.createRow(1);
            dateRangeRow.createCell(0).setCellValue(String.format("Từ ngày: %s đến %s", 
                startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), 
                endDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));

            Row generatedRow = sheet.createRow(2);
            generatedRow.createCell(0).setCellValue("Ngày xuất: " + 
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

            // Empty row
            sheet.createRow(3);

            // Create header row
            Row headerRow = sheet.createRow(4);
            String[] headers = {"STT", "Mã sản phẩm", "Tên sản phẩm", "Số lượng bán", "Doanh thu"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Create data rows
            int rowNum = 5;
            int stt = 1;
            long totalQuantity = 0;
            double totalRevenue = 0;

            for (ReportProjections.TopProductStats product : products) {
                Row row = sheet.createRow(rowNum++);
                
                row.createCell(0).setCellValue(stt++);
                row.createCell(1).setCellValue(product.getProductId());
                row.createCell(2).setCellValue(product.getProductName());
                
                Cell quantityCell = row.createCell(3);
                quantityCell.setCellValue(product.getQuantitySold());
                quantityCell.setCellStyle(numberStyle);
                
                Cell revenueCell = row.createCell(4);
                revenueCell.setCellValue(product.getRevenueGenerated().doubleValue());
                revenueCell.setCellStyle(currencyStyle);

                totalQuantity += product.getQuantitySold();
                totalRevenue += product.getRevenueGenerated().doubleValue();
            }

            // Create total row
            Row totalRow = sheet.createRow(rowNum);
            Cell totalLabelCell = totalRow.createCell(2);
            totalLabelCell.setCellValue("TỔNG CỘNG");
            CellStyle boldStyle = workbook.createCellStyle();
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldStyle.setFont(boldFont);
            totalLabelCell.setCellStyle(boldStyle);

            Cell totalQuantityCell = totalRow.createCell(3);
            totalQuantityCell.setCellValue(totalQuantity);
            CellStyle boldNumberStyle = workbook.createCellStyle();
            boldNumberStyle.cloneStyleFrom(numberStyle);
            boldNumberStyle.setFont(boldFont);
            totalQuantityCell.setCellStyle(boldNumberStyle);

            Cell totalRevenueCell = totalRow.createCell(4);
            totalRevenueCell.setCellValue(totalRevenue);
            CellStyle boldCurrencyStyle = workbook.createCellStyle();
            boldCurrencyStyle.cloneStyleFrom(currencyStyle);
            boldCurrencyStyle.setFont(boldFont);
            totalRevenueCell.setCellStyle(boldCurrencyStyle);

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000); // Add some padding
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            log.error("Error exporting top selling products to Excel", e);
            throw new RuntimeException("Không thể xuất file Excel", e);
        }
    }
}
