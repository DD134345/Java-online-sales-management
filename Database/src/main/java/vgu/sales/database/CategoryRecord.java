package vgu.sales.database;

/**
 * Immutable carrier for one row of table categories; id is null before insert.
 */
public record CategoryRecord(
    Integer categoryId,
    String name,
    String description,
    Boolean active
) {}