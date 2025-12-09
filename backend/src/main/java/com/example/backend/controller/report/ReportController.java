package com.example.backend.controller.report;

import com.example.backend.dto.response.report.ReportResponse;
import com.example.backend.service.report.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
// @PreAuthorize("hasRole('ADMIN')") // TEMP: Disabled for testing
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/dashboard-overview")
    public ResponseEntity<ReportResponse.DashboardOverviewDTO> getDashboardOverview(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String timezone) {
        return ResponseEntity.ok(reportService.getDashboardOverview(startDate, endDate, timezone));
    }

    @GetMapping("/revenue-chart")
    public ResponseEntity<ReportResponse.RevenueChartDTO> getRevenueChart(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String timezone) {
        return ResponseEntity.ok(reportService.getRevenueChart(startDate, endDate, timezone));
    }

    @GetMapping("/top-category-revenue")
    public ResponseEntity<ReportResponse.CategoryRevenueDTO> getTopCategoryRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String timezone) {
        return ResponseEntity.ok(reportService.getTopCategoryRevenue(startDate, endDate, timezone));
    }

    @GetMapping("/top-selling-products")
    public ResponseEntity<ReportResponse.TopProductsDTO> getTopSellingProducts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String timezone,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(reportService.getTopSellingProducts(startDate, endDate, timezone, limit));
    }

    @GetMapping("/profit-report")
    public ResponseEntity<ReportResponse.ProfitReportDTO> getProfitReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String timezone) {
        return ResponseEntity.ok(reportService.getProfitReport(startDate, endDate, timezone));
    }

    @PreAuthorize("permitAll()")
    @GetMapping("/monthly-profit")
    public ResponseEntity<ReportResponse.ProfitByMonthDTO> getProfitByMonth(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String timezone) {
        return ResponseEntity.ok(reportService.getProfitByMonth(startDate, endDate, timezone));
    }

    @PreAuthorize("permitAll()")
    @GetMapping("/category-profit")
    public ResponseEntity<ReportResponse.ProfitByCategoryDTO> getProfitByCategory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String timezone) {
        return ResponseEntity.ok(reportService.getProfitByCategory(startDate, endDate, timezone));
    }

    @GetMapping("/top-selling-products/export")
    public ResponseEntity<byte[]> exportTopSellingProducts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String timezone,
            @RequestParam(required = false) Integer limit) {
        byte[] excelData = reportService.exportTopSellingProductsToExcel(startDate, endDate, timezone, limit);
        
        String filename = String.format("top-selling-products_%s_%s.xlsx", 
                startDate.toString(), endDate.toString());
        
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(excelData);
    }
}
