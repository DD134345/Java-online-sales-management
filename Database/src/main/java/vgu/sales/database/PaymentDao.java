package vgu.sales.database;

import java.util.Optional;

/**
 * DAO interface for Payment records, extending the generic Dao.
 */
public interface PaymentDao extends Dao<PaymentRecord, Integer> {
    /**
     * Find a payment by order ID.
     * @param orderId the order ID
     * @return an Optional containing the PaymentRecord if found, empty otherwise
     */
    Optional<PaymentRecord> findByOrderId(Integer orderId);
}