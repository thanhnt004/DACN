package com.example.backend.repository.report;

import com.example.backend.dto.response.report.ReportProjections;
import com.example.backend.model.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Order, UUID> {
    @Query(value = """
    SELECT
        COUNT(o.id) AS totalOrders,
        COALESCE(SUM(o.total_amount), 0) AS totalRevenue,
        COALESCE(SUM(o.discount_amount), 0) AS totalDiscount,
        0 AS pendingOrders
    FROM orders o
    WHERE o.created_at BETWEEN :startDate AND :endDate
      AND o.status IN ('COMPLETED', 'DELIVERED')
    """, nativeQuery = true)
    ReportProjections.DashboardOverview getDashboardOverview(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Query("SELECT count(o) FROM Order o WHERE o.status = :status")
    long countByStatus(@Param("status") Order.OrderStatus status);
    
    @Query(value = """
        SELECT
            TO_CHAR(MAX(created_at) AT TIME ZONE :timeZone, 'YYYY-MM-DD') as reportDate,
            COUNT(id) as orderCount,
            COALESCE(SUM(total_amount), 0) as revenue
        FROM orders
        WHERE created_at BETWEEN :startDate AND :endDate
          AND status IN ('COMPLETED', 'DELIVERED')
        GROUP BY DATE(created_at AT TIME ZONE :timeZone)
        ORDER BY reportDate ASC
        """, nativeQuery = true)
    List<ReportProjections.RevenueChartPoint> getRevenueChart(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("timeZone") String timeZone);

    @Query(value = """
        SELECT
            c.name as categoryName,
            COALESCE(SUM(oi.total_amount), 0) as totalRevenue,
            COALESCE(SUM(oi.quantity), 0) as totalSold
        FROM order_items oi
        JOIN orders o ON oi.order_id = o.id
        JOIN product_variants pv ON oi.variant_id = pv.id
        JOIN products p ON pv.product_id = p.id
        JOIN product_categories pc ON p.id = pc.product_id
        JOIN categories c ON pc.category_id = c.id
        WHERE o.created_at BETWEEN :startDate AND :endDate
          AND o.status IN ('COMPLETED', 'DELIVERED')
        GROUP BY c.id, c.name
        ORDER BY totalRevenue DESC
        LIMIT 10
        """, nativeQuery = true)
    List<ReportProjections.CategoryRevenue> getTopCategoryRevenue(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);
    @Query(value = """
        SELECT
            CAST(p.id AS VARCHAR) as productId,
            p.name as productName,
            p.primary_image_url as primaryImageUrl,
            SUM(oi.quantity) as quantitySold,
            SUM(oi.total_amount) as revenueGenerated
        FROM order_items oi
        JOIN orders o ON oi.order_id = o.id
        JOIN product_variants pv ON oi.variant_id = pv.id
        JOIN products p ON pv.product_id = p.id
        WHERE o.created_at BETWEEN :startDate AND :endDate
          AND o.status IN ('COMPLETED', 'DELIVERED')
        GROUP BY p.id, p.name, p.primary_image_url
        ORDER BY quantitySold DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<ReportProjections.TopProductStats> getTopSellingProducts(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("limit") int limit);
    @Query(value = """
        SELECT
            p.name as productName,
            pv.sku as sku,
            s.name as sizeName,
            c.name as colorName,
            i.quantity_on_hand as quantityOnHand,
            i.reorder_level as reorderLevel
        FROM inventory i
        JOIN product_variants pv ON i.variant_id = pv.id
        JOIN products p ON pv.product_id = p.id
        LEFT JOIN sizes s ON pv.size_id = s.id
        LEFT JOIN colors c ON pv.color_id = c.id
        WHERE i.quantity_on_hand <= i.reorder_level
        ORDER BY i.quantity_on_hand ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<ReportProjections.LowStockItem> getLowStockItems(@Param("limit") int limit);
    @Query(value = """
        SELECT
            CAST(u.id AS VARCHAR) as userId,
            u.full_name as fullName,
            u.email as email,
            COUNT(o.id) as totalOrders,
            COALESCE(SUM(o.total_amount), 0) as totalSpent
        FROM orders o
        JOIN users u ON o.user_id = u.id
        WHERE o.status IN ('COMPLETED', 'DELIVERED')
        AND o.created_at BETWEEN :startDate AND :endDate
        GROUP BY u.id, u.full_name, u.email
        ORDER BY totalSpent DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<ReportProjections.CustomerSpending> getTopSpenders(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("limit") int limit);
    @Query(value = """
            SELECT
                COALESCE(SUM(oi.total_amount), 0) AS totalRevenue,
                COALESCE(SUM(oi.quantity * oi.history_cost), 0) AS totalCost
            FROM orders o
            JOIN order_items oi ON o.id = oi.order_id
            WHERE o.status = 'DELIVERED'
              AND o.placed_at BETWEEN :startDate AND :endDate
            """, nativeQuery = true)
    ReportProjections.ProfitStats getProfitStats(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query(value = """
            SELECT
                TO_CHAR(o.placed_at AT TIME ZONE :timeZone, 'YYYY-MM') as reportMonth,
                COALESCE(SUM(oi.total_amount), 0) AS totalRevenue,
                COALESCE(SUM(oi.quantity * oi.history_cost), 0) AS totalCost,
                COALESCE(SUM(oi.total_amount - (oi.quantity * oi.history_cost)), 0) AS grossProfit,
                COALESCE(COUNT(DISTINCT o.id), 0) AS orderCount
            FROM orders o
            JOIN order_items oi ON o.id = oi.order_id
            WHERE o.status = 'DELIVERED'
              AND o.placed_at BETWEEN :startDate AND :endDate
            GROUP BY reportMonth
            ORDER BY reportMonth ASC
            """, nativeQuery = true)
    List<ReportProjections.MonthlyProfitProjection> findMonthlyProfit(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("timeZone") String timeZone);

    @Query(value = """
            SELECT
                c.name as categoryName,
                COALESCE(SUM(oi.total_amount), 0) AS totalRevenue,
                COALESCE(SUM(oi.quantity * oi.history_cost), 0) AS totalCost,
                COALESCE(SUM(oi.total_amount - (oi.quantity * oi.history_cost)), 0) AS grossProfit
            FROM orders o
            JOIN order_items oi ON o.id = oi.order_id
            JOIN products p ON oi.product_id = p.id
            JOIN product_categories pc ON p.id = pc.product_id
            JOIN categories c ON pc.category_id = c.id
            WHERE o.status = 'DELIVERED'
              AND o.placed_at BETWEEN :startDate AND :endDate
            GROUP BY c.name
            ORDER BY grossProfit DESC
            """, nativeQuery = true)
    List<ReportProjections.CategoryProfitProjection> findCategoryProfit(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);
}
