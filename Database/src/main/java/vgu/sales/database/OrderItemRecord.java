package vgu.sales.database;

import java.math.BigDecimal;

/**
 * Immutable carrier for one row of table order_items; id is null before insert.
 */
public record OrderItemRecord(
    Integer orderItemId,
    Integer orderId,
    Integer productId,
    Integer quantity,
    BigDecimal unitPrice
) {
    /**
     * Compact constructor validating quantity and unitPrice.
     * @param quantity must be non-null and > 0
     * @param unitPrice must be non-null and >= 0
     * @throws IllegalArgumentException if validation fails
     */
    public OrderItemRecord {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("quantity must be > 0");
        }
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("unitPrice must be >= 0");
        }
    }
}