package vgu.sales.database;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Package-private: the two multi-table JDBC transactions of the orders table;
 * only PgOrderDao uses it.
 */
final class PgOrderTransactions {

    /** Select product for update (lock row). */
    private static final String SELECT_PRODUCT_FOR_UPDATE =
            "SELECT stock_quantity, active FROM products WHERE product_id = ? FOR UPDATE";

    /** Update product stock quantity. */
    private static final String UPDATE_PRODUCT_STOCK =
            "UPDATE products SET stock_quantity = stock_quantity - ? WHERE product_id = ?";

    /** Batch insert order items. */
    private static final String INSERT_ORDER_ITEM =
            "INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?)";

    /** Insert payment. */
    private static final String INSERT_PAYMENT =
            "INSERT INTO payments (order_id, method, amount, paid_at, cash_received, change_amount, bank_reference, cardholder_name) "
            + "VALUES (?, ?, ?, COALESCE(?, NOW()), ?, ?, ?, ?)";

    /** Select current order status for update (lock row). */
    private static final String SELECT_ORDER_STATUS_FOR_UPDATE =
            "SELECT status FROM orders WHERE order_id = ? FOR UPDATE";

    /** Update order status and cancel reason. */
    private static final String UPDATE_ORDER_STATUS =
            "UPDATE orders SET status = ?, cancel_reason = ? WHERE order_id = ?";

    /** Restore stock when cancelling an order. */
    private static final String RESTORE_STOCK_ON_CANCEL =
            "UPDATE products p SET stock_quantity = p.stock_quantity + oi.quantity "
            + "FROM order_items oi "
            + "WHERE oi.order_id = ? AND p.product_id = oi.product_id";

    /** Get the maximum order sequence number for a given day prefix. */
    private static final String MAX_SEQUENCE_FOR_DAY =
            "SELECT COALESCE(MAX(CAST(SUBSTRING(order_code FROM 14 FOR 3) AS INTEGER)), 0) + 1 "
            + "FROM orders WHERE order_code LIKE ?";

    /** Insert a new order and return generated ID and order_date. */
    private static final String INSERT =
            "INSERT INTO orders (order_code, customer_id, staff_id, order_date, status, total_amount, shipping_address, notes, cancel_reason) "
            + "VALUES (?, ?, ?, COALESCE(?, NOW()), ?, ?, ?, ?, ?) RETURNING order_id, order_date";

    private final ConnectionProvider connections;

    /**
     * Creates a new PgOrderTransactions with the given connection provider.
     *
     * @param connections the source of JDBC connections
     */
    PgOrderTransactions(ConnectionProvider connections) {
        this.connections = connections;
    }

    /**
     * Creates an order with its items and payment in a single transaction.
     * Locks product rows in a deterministic order to avoid deadlocks.
     *
     * @param order  the order header
     * @param items  the order lines
     * @param payment the payment record
     * @return the persisted order with generated ID and timestamp
     */
    OrderRecord createOrderWithTransaction(OrderRecord order, List<OrderItemRecord> items, PaymentRecord payment) {
        try (Connection c = connections.getConnection()) {
            c.setAutoCommit(false);
            try {
                // 1. Lock all product rows in a deterministic order (by product_id)
                var sortedItems = new ArrayList<>(items);
                sortedItems.sort(Comparator.comparingInt(OrderItemRecord::productId));
                for (OrderItemRecord item : sortedItems) {
                    try (PreparedStatement ps = c.prepareStatement(SELECT_PRODUCT_FOR_UPDATE)) {
                        ps.setInt(1, item.productId());
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) {
                                throw new DataAccessException("Product " + item.productId() + " does not exist");
                            }
                            int stock = rs.getInt("stock_quantity");
                            boolean active = rs.getBoolean("active");
                            if (!active) {
                                throw new DataAccessException("Product " + item.productId() + " is not active");
                            }
                            if (stock < item.quantity()) {
                                throw new InsufficientStockException(item.productId(), item.quantity(), stock);
                            }
                        }
                    }
                }

                // 2. Decrement stock
                for (OrderItemRecord item : sortedItems) {
                    try (PreparedStatement ps = c.prepareStatement(UPDATE_PRODUCT_STOCK)) {
                        ps.setInt(1, item.quantity());
                        ps.setInt(2, item.productId());
                        ps.executeUpdate();
                    }
                }

                // 3. Generate order code and insert order
                LocalDate today = LocalDate.now();
                String orderCode = (order.orderCode() != null && !order.orderCode().isBlank())
                        ? order.orderCode()
                        : nextOrderCode(c, today);

                int generatedOrderId;
                LocalDateTime generatedOrderDate;
                try (PreparedStatement ps = c.prepareStatement(INSERT)) {
                    ps.setString(1, orderCode);
                    ps.setObject(2, order.customerId(), Types.INTEGER);
                    ps.setObject(3, order.staffId(), Types.INTEGER);
                    ps.setObject(4, order.orderDate(), Types.TIMESTAMP);
                    ps.setString(5, order.status());
                    ps.setObject(6, order.totalAmount(), Types.NUMERIC);
                    ps.setObject(7, order.shippingAddress(), Types.VARCHAR);
                    ps.setObject(8, order.notes(), Types.VARCHAR);
                    ps.setObject(9, order.cancelReason(), Types.VARCHAR);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            generatedOrderId = rs.getObject("order_id", Integer.class);
                            generatedOrderDate = rs.getObject("order_date", LocalDateTime.class);
                        } else {
                            throw new DataAccessException("Insert order returned no generated key");
                        }
                    }
                }

                // 4. Batch insert items
                try (PreparedStatement ps = c.prepareStatement(INSERT_ORDER_ITEM)) {
                    for (OrderItemRecord item : sortedItems) {
                        ps.setInt(1, generatedOrderId);
                        ps.setInt(2, item.productId());
                        ps.setInt(3, item.quantity());
                        ps.setObject(4, item.unitPrice(), Types.NUMERIC);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                // 5. Insert payment
                try (PreparedStatement ps = c.prepareStatement(INSERT_PAYMENT)) {
                    ps.setInt(1, generatedOrderId);
                    ps.setString(2, payment.method());
                    ps.setObject(3, payment.amount(), Types.NUMERIC);
                    ps.setObject(4, payment.paidAt(), Types.TIMESTAMP);
                    ps.setObject(5, payment.cashReceived(), Types.NUMERIC);
                    ps.setObject(6, payment.changeAmount(), Types.NUMERIC);
                    ps.setObject(7, payment.bankReference(), Types.VARCHAR);
                    ps.setObject(8, payment.cardholderName(), Types.VARCHAR);
                    ps.executeUpdate();
                }

                // 6. Commit and return
                c.commit();
                return new OrderRecord(
                        generatedOrderId,
                        orderCode,
                        order.customerId(),
                        order.staffId(),
                        generatedOrderDate,
                        order.status(),
                        order.totalAmount(),
                        order.shippingAddress(),
                        order.notes(),
                        order.cancelReason()
                );
            } catch (RuntimeException e) {
                c.rollback();
                throw e;
            } catch (SQLException e) {
                c.rollback();
                throw new DataAccessException("Order could not be saved; nothing was changed", e);
            } finally {
                try {
                    c.setAutoCommit(true);
                } catch (SQLException ignored) {
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not obtain connection for order transaction", e);
        }
    }

    /**
     * Updates an order's status, rolling back stock if the order is cancelled.
     *
     * @param orderId     the order identifier
     * @param newStatus   the new status value
     * @param cancelReason the reason for cancellation (required if newStatus is CANCELLED)
     * @return true if the order was found and updated, false if not found
     */
    boolean updateStatusWithStockRollback(Integer orderId, String newStatus, String cancelReason) {
        try (Connection c = connections.getConnection()) {
            c.setAutoCommit(false);
            try {
                // 1. Lock order row and get current status
                String currentStatus;
                try (PreparedStatement ps = c.prepareStatement(SELECT_ORDER_STATUS_FOR_UPDATE)) {
                    ps.setInt(1, orderId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            c.rollback();
                            return false;
                        }
                        currentStatus = rs.getString("status");
                    }
                }

                // 2. Validate transition
                if (!OrderStatus.canTransition(currentStatus, newStatus)) {
                    c.rollback();
                    return false;
                }
                if (OrderStatus.CANCELLED.equals(newStatus) && (cancelReason == null || cancelReason.isBlank())) {
                    throw new IllegalArgumentException("Cancel reason required when cancelling an order");
                }

                // 3. If cancelling, restore stock
                if (OrderStatus.CANCELLED.equals(newStatus)) {
                    try (PreparedStatement ps = c.prepareStatement(RESTORE_STOCK_ON_CANCEL)) {
                        ps.setInt(1, orderId);
                        ps.executeUpdate();
                    }
                }

                // 4. Update order status
                try (PreparedStatement ps = c.prepareStatement(UPDATE_ORDER_STATUS)) {
                    ps.setString(1, newStatus);
                    ps.setObject(2, OrderStatus.CANCELLED.equals(newStatus) ? cancelReason : null, Types.VARCHAR);
                    ps.setInt(3, orderId);
                    ps.executeUpdate();
                }

                c.commit();
                return true;
            } catch (RuntimeException e) {
                c.rollback();
                throw e;
            } catch (SQLException e) {
                c.rollback();
                throw new DataAccessException("Could not update order status", e);
            } finally {
                try {
                    c.setAutoCommit(true);
                } catch (SQLException ignored) {
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not obtain connection for status update", e);
        }
    }

    /**
     * Helper to compute next order code on an existing connection (used inside transaction).
     *
     * @param c   the open connection
     * @param day the day for the order code prefix
     * @return the next order code string
     * @throws SQLException if a database access error occurs
     */
    static String nextOrderCode(Connection c, LocalDate day) throws SQLException {
        String prefix = "ORD-" + day.format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        try (PreparedStatement ps = c.prepareStatement(MAX_SEQUENCE_FOR_DAY)) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int n = rs.getInt(1);
                    return prefix + String.format("%03d", n);
                }
                return prefix + "001";
            }
        }
    }
}