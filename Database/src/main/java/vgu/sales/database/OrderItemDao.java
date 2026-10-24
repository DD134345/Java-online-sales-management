package vgu.sales.database;

import java.util.List;

/**
 * DAO interface for OrderItem records, extending the generic Dao.
 */
public interface OrderItemDao extends Dao<OrderItemRecord, Integer> {
    /**
     * Find all order items for a specific order.
     * @param orderId the order ID
     * @return a List of OrderItemRecords
     */
    List<OrderItemRecord> findByOrderId(Integer orderId);
}