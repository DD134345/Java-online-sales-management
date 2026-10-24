package vgu.sales.database;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Immutable carrier for one row of table orders; id is null before insert.
 */
public record OrderRecord(
    Integer orderId,
    String orderCode,
    Integer customerId,
    Integer staffId,
    LocalDateTime orderDate,
    String status,
    BigDecimal totalAmount,
    String shippingAddress,
    String notes,
    String cancelReason
) {}