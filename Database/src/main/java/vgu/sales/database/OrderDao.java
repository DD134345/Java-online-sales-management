package vgu.sales.database;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * DAO interface for Order records, extending the generic Dao.
 */
public interface OrderDao extends Dao<OrderRecord, Integer> {
    /**
     * Find all orders for a specific customer.
     * @param customerId the customer ID
     * @return a List of OrderRecords
     */
    List<OrderRecord> findByCustomerId(Integer customerId);

    /**
     * Find all orders with summary info, newest first, with pagination.
     * Overload of Dao.findAll() with limit and offset.
     * @param limit maximum number of records to return
     * @param offset number of records to skip
     * @return a List of OrderSummaryRecords
     */
    List<OrderSummaryRecord> findAll(int limit, int offset);

    /**
     * Search orders with optional filters.
     * @param status order status filter, null = no filter
     * @param fromDate start date filter (inclusive), null = no filter
     * @param toDate end date filter (inclusive), null = no filter
     * @param keyword search keyword (order code, customer name), null = no filter
     * @return a List of matching OrderSummaryRecords
     */
    List<OrderSummaryRecord> search(String status, LocalDate fromDate, LocalDate toDate, String keyword);

    /**
     * Create an order with its items and payment in a single transaction.
     * @param order the OrderRecord to create
     * @param items the list of OrderItemRecords
     * @param payment the PaymentRecord
     * @return the created OrderRecord with generated ID
     */
    OrderRecord createOrderWithTransaction(OrderRecord order, List<OrderItemRecord> items, PaymentRecord payment);

    /**
     * Update order status with stock rollback if cancelling.
     * @param orderId the order ID
     * @param newStatus the new status
     * @param cancelReason the cancellation reason (required if newStatus is CANCELLED)
     * @return true if a row was changed, false otherwise
     */
    boolean updateStatusWithStockRollback(Integer orderId, String newStatus, String cancelReason);

    /**
     * Generate the next order code for a given day.
     * @param day the date for the order code
     * @return the next order code (e.g., ORD-20261020-001)
     */
    String nextOrderCode(LocalDate day);
}