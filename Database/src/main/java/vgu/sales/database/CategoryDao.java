package vgu.sales.database;

import java.util.List;

/**
 * DAO interface for Category records, extending the generic Dao.
 */
public interface CategoryDao extends Dao<CategoryRecord, Integer> {
    /**
     * Find all active categories sorted by name.
     * @return a List of active CategoryRecords
     */
    List<CategoryRecord> findActive();
}