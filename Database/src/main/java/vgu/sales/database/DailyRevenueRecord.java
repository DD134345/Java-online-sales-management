package vgu.sales.database;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Immutable carrier for daily revenue report row.
 * @param date day
 * @param orderCount # orders
 * @param totalRevenue sum revenue
 * @param averageOrderValue mean order value
 */
public record DailyRevenueRecord(
    LocalDate date,
    Integer orderCount,
    BigDecimal totalRevenue,
    BigDecimal averageOrderValue
) {}
