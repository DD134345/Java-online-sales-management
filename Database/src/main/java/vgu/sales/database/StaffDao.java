package vgu.sales.database;

import java.util.Optional;

/**
 * DAO interface for Staff records, extending the generic Dao.
 */
public interface StaffDao extends Dao<StaffRecord, Integer> {
    /**
     * Find a staff member by username (case-insensitive).
     * @param username the username to search for
     * @return an Optional containing the StaffRecord if found, empty otherwise
     */
    Optional<StaffRecord> findByUsername(String username);
}