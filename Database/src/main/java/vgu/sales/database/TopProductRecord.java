package vgu.sales.database;

import java.math.BigDecimal;

/**
 * Immutable carrier for top selling product rows.
 * @param productId PK
 * @param productName product name
 * @param categoryName name
 * @param quantitySold sold
 * @param totalRevenue revenue
 */
public record TopProductRecord(
    Integer productId,
    String productName,
    String categoryName,
    Integer quantitySold,
    BigDecimal totalRevenue
) {}
