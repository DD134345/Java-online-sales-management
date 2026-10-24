package vgu.sales.database;

import java.time.LocalDate;
import java.util.List;

/**
 * DAO interface for report queries.
 */
public interface ReportDao {
    /**
     * Get daily revenue for a date range (inclusive).
     * @param start the start date (inclusive)
     * @param end the end date (inclusive)
     * @return a List of DailyRevenueRecords
     */
    List<DailyRevenueRecord> getDailyRevenue(LocalDate start, LocalDate end);

    /**
     * Get top selling products for a date range.
     * @param start the start date (inclusive)
     * @param end the end date (inclusive)
     * @param limit maximum number of products to return
     * @return a List of TopProductRecords
     */
    List<TopProductRecord> getTopSellingProducts(LocalDate start, LocalDate end, int limit);
}