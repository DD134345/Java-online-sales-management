package vgu.sales.database;

import java.math.BigDecimal;

/**
 * Immutable carrier for one row of low stock report; id is null before insert.
 */
public record LowStockRecord(
    Integer productId,
    String productName,
    String categoryName,
    Integer currentStock,
    BigDecimal price,
    String reorderStatus
) {}