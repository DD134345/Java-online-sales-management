package vgu.sales.database;

import java.time.LocalDateTime;

/**
 * Immutable carrier for one row of table customers; id is null before insert.
 */
public record CustomerRecord(
    Integer customerId,
    String fullName,
    String phone,
    String email,
    String address,
    LocalDateTime createdAt
) {}