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
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of OrderDao for PostgreSQL.
 * Public because ControllerFactory creates it.
 */
public class PgOrderDao implements OrderDao {

    /** Columns selected for order queries. */
    private static final String ORDER_COLUMNS = "order_id, order_code, customer_id, staff_id, order_date, status, total_amount, shipping_address, notes, cancel_reason";

    /** Find an order by its primary key. */
    private static final String FIND_BY_ID = "SELECT " + ORDER_COLUMNS + " FROM orders WHERE order_id = ?";

    /** Retrieve all orders ordered by order_date DESC. */
    private static final String FIND_ALL = "SELECT " + ORDER_COLUMNS + " FROM orders ORDER BY order_date DESC";

    /** Find all orders for a specific customer ordered by order_date DESC. */
    private static final String FIND_BY_CUSTOMER_ID = "SELECT " + ORDER_COLUMNS + " FROM orders WHERE customer_id = ? ORDER BY order_date DESC";

    /** Update an order's status, cancel_reason, shipping_address, notes. */
    private static final String UPDATE = "UPDATE orders SET status = ?, cancel_reason = ?, shipping_address = ?, notes = ? WHERE order_id = ?";

    

    /** Connection provider. */
    private final ConnectionProvider connections;

    /** Transactional helper for multi-table order operations. */
    private final PgOrderTransactions transactions;

    /** Private and final: the read-only list/search queries this DAO delegates to. */
    private final PgOrderSummaryQueries summaries;

    /**
     * Creates a new PgOrderDao with the given connection provider.
     *
     * @param connections the source of JDBC connections
     */
    public PgOrderDao(ConnectionProvider connections) {
        this.connections = connections;
        this.transactions = new PgOrderTransactions(connections);
        this.summaries = new PgOrderSummaryQueries(connections);
    }

    /**
     * Loads one order by id with a parameterised SELECT.
     *
     * @param id the order primary key
     * @return the order if found, otherwise empty
     * Public: part of the OrderDao contract used by the Controller tier.
     */
    @Override
    public Optional<OrderRecord> findById(Integer id) {
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(FIND_BY_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load order " + id, e);
        }
    }

    /**
     * Loads all orders newest first with a parameterised SELECT.
     *
     * @return list of all orders (may be empty)
     * Public: part of the OrderDao contract used by the Controller tier.
     */
    @Override
    public List<OrderRecord> findAll() {
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            List<OrderRecord> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load all orders", e);
        }
    }

    /**
     * Loads all orders for a customer newest first with a parameterised SELECT.
     *
     * @param customerId the customer primary key
     * @return list of orders for that customer (may be empty)
     * Public: part of the OrderDao contract used by the Controller tier.
     */
    @Override
    public List<OrderRecord> findByCustomerId(Integer customerId) {
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(FIND_BY_CUSTOMER_ID)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                List<OrderRecord> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load orders for customer " + customerId, e);
        }
    }

    /**
     * Inserts a new order and returns the generated order_id and order_date.
     *
     * @param order the order to insert (order_id must be null)
     * @return a new OrderRecord with generated id and date
     * Public: part of the OrderDao contract used by the Controller tier.
     */
    @Override
    public OrderRecord insert(OrderRecord order) {
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement("INSERT INTO orders (order_code, customer_id, staff_id, order_date, status, total_amount, shipping_address, notes, cancel_reason) "
                      + "VALUES (?, ?, ?, COALESCE(?, NOW()), ?, ?, ?, ?, ?) RETURNING order_id, order_date")) {
            ps.setString(1, order.orderCode());
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
                    Integer generatedId = rs.getObject("order_id", Integer.class);
                    LocalDateTime generatedDate = rs.getObject("order_date", LocalDateTime.class);
                    return new OrderRecord(
                            generatedId,
                            order.orderCode(),
                            order.customerId(),
                            order.staffId(),
                            generatedDate,
                            order.status(),
                            order.totalAmount(),
                            order.shippingAddress(),
                            order.notes(),
                            order.cancelReason()
                    );
                }
                throw new DataAccessException("Insert order returned no generated key");
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not insert order", e);
        }
    }

    /**
     * Updates status, cancel_reason, shipping_address, and notes of an existing order.
     *
     * @param order the order with updated fields (order_id must not be null)
     * @return true if a row was updated
     * Public: part of the OrderDao contract used by the Controller tier.
     */
    @Override
    public boolean update(OrderRecord order) {
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(UPDATE)) {
            ps.setString(1, order.status());
            ps.setObject(2, order.cancelReason(), Types.VARCHAR);
            ps.setObject(3, order.shippingAddress(), Types.VARCHAR);
            ps.setObject(4, order.notes(), Types.VARCHAR);
            ps.setInt(5, order.orderId());
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not update order " + order.orderId(), e);
        }
    }

    /**
     * Loads a page of order summaries (joined with customer, item count) newest first.
     *
     * @param limit  max rows to return
     * @param offset number of rows to skip
     * @return list of order summaries (may be empty)
     * Public: part of the OrderDao contract used by the Controller tier.
     */
    @Override
    public List<OrderSummaryRecord> findAll(int limit, int offset) {
        return summaries.findAll(limit, offset);
    }

    /**
     * Searches orders with optional filters on status, date range, and keyword.
     *
     * @param status   exact status filter (null or blank = no filter)
     * @param fromDate inclusive start date (null = no lower bound)
     * @param toDate   inclusive end date (null = no upper bound)
     * @param keyword  searches order_code, customer full_name, or phone (null or blank = no filter)
     * @return list of matching order summaries (may be empty)
     * Public: part of the OrderDao contract used by the Controller tier.
     */
    @Override
    public List<OrderSummaryRecord> search(String status, LocalDate fromDate, LocalDate toDate, String keyword) {
        return summaries.search(status, fromDate, toDate, keyword);
    }

    /**
     * Generates the next sequential order code for a given day (ORD-YYYYMMDD-NNN).
     *
     * @param day the order date
     * @return the next order code string
     * Public: part of the OrderDao contract used by the Controller tier.
     */
    @Override
    public String nextOrderCode(LocalDate day) {
        String prefix = "ORD-" + day.format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COALESCE(MAX(CAST(SUBSTRING(order_code FROM 14 FOR 3) AS INTEGER)), 0) + 1 FROM orders WHERE order_code LIKE ?")) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int n = rs.getInt(1);
                    return prefix + String.format("%03d", n);
                }
                return prefix + "001";
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not generate next order code for " + day, e);
        }
    }

    /**
     * Creates an order with its items and payment in one transaction.
     *
     * @param order   the order to insert
     * @param items   list of order items for the order
     * @param payment the payment for the order
     * @return the created order with generated id and date
     * Public: part of the OrderDao contract used by the Controller tier.
     */
    @Override
    public OrderRecord createOrderWithTransaction(OrderRecord order, List<OrderItemRecord> items, PaymentRecord payment) {
        return transactions.createOrderWithTransaction(order, items, payment);
    }

    /**
     * Updates order status and rolls back stock if cancelling, all in one transaction.
     *
     * @param orderId     the order to update
     * @param newStatus   the new status
     * @param cancelReason reason if cancelling (ignored for other statuses)
     * @return true if the order was updated
     * Public: part of the OrderDao contract used by the Controller tier.
     */
    @Override
    public boolean updateStatusWithStockRollback(Integer orderId, String newStatus, String cancelReason) {
        return transactions.updateStatusWithStockRollback(orderId, newStatus, cancelReason);
    }

    /**
     * Maps a ResultSet row to an OrderRecord.
     * Private: only used by this class.
     */
    private OrderRecord mapRow(ResultSet rs) throws SQLException {
        return new OrderRecord(
                rs.getObject("order_id", Integer.class),
                rs.getString("order_code"),
                rs.getObject("customer_id", Integer.class),
                rs.getObject("staff_id", Integer.class),
                rs.getObject("order_date", LocalDateTime.class),
                rs.getString("status"),
                rs.getObject("total_amount", BigDecimal.class),
                rs.getString("shipping_address"),
                rs.getString("notes"),
                rs.getString("cancel_reason")
        );
    }
}