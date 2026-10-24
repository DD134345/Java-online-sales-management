package vgu.sales.database;

import java.util.List;
import java.util.Optional;

/**
 * DAO interface for Customer records, extending the generic Dao.
 */
public interface CustomerDao extends Dao<CustomerRecord, Integer> {
    /**
     * Search customers by keyword (name, phone, or email), case-insensitive.
     * @param keyword the search keyword
     * @return a List of matching CustomerRecords
     */
    List<CustomerRecord> search(String keyword);

    /**
     * Find a customer by phone number.
     * @param phone the phone number
     * @return an Optional containing the CustomerRecord if found, empty otherwise
     */
    Optional<CustomerRecord> findByPhone(String phone);
}