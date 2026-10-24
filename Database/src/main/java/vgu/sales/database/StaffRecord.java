package vgu.sales.database;

import java.time.LocalDateTime;

/**
 * Immutable record representing a row in the staff table.
 */
public record StaffRecord(
    Integer staffId,
    String username,
    String passwordHash,
    String passwordSalt,
    String fullName,
    String email,
    String phone,
    String role,
    Boolean active,
    LocalDateTime createdAt
) {}