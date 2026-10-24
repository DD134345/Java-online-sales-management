package vgu.sales.database;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Immutable carrier for one row of table products; id is null before insert.
 */
public record ProductRecord(
    Integer productId,
    Integer categoryId,
    String name,
    String description,
    BigDecimal price,
    Integer stockQuantity,
    Boolean active,
    LocalDateTime createdAt
) {}