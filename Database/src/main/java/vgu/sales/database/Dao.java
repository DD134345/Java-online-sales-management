package vgu.sales.database;

import java.util.List;
import java.util.Optional;

/**
 * Generic DAO interface defining basic CRUD operations for any entity record.
 * @param <R> the record type
 * @param <ID> the identifier type
 */
public interface Dao<R, ID> {
    /**
     * Find a record by its primary key.
     * @param id the identifier
     * @return an Optional containing the record if found, empty otherwise
     */
    Optional<R> findById(ID id);

    /**
     * Retrieve all records from the table.
     * @return a List of all records (empty if none)
     */
    List<R> findAll();

    /**
     * Insert a new record and return it with generated ID and DB defaults filled in.
     * @param record the record to insert (without ID)
     * @return the inserted record with generated ID and defaults
     */
    R insert(R record);

    /**
     * Update an existing record.
     * @param record the record to update (must have valid ID)
     * @return true if a row was changed, false otherwise
     */
    boolean update(R record);
}