package vgu.sales.database;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Immutable carrier for one row of table orders summary; id is null before insert.
 */
public record OrderSummaryRecord(
    Integer orderId,
    String orderCode,
    String customerName,
    LocalDateTime orderDate,
    BigDecimal totalAmount,
    String status,
    Integer itemCount
) {}