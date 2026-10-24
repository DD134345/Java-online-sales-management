package vgu.sales.database;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of OrderItemDao for PostgreSQL.
 * Public because ControllerFactory creates it.
 */
public class PgOrderItemDao implements OrderItemDao {

    /** Columns selected for order item queries. */
    private static final String COLUMNS = "order_item_id, order_id, product_id, quantity, unit_price";

    /** Find an order item by its primary key. */
    private static final String FIND_BY_ID = "SELECT " + COLUMNS + " FROM order_items WHERE order_item_id = ?";

    /** Retrieve all order items ordered by ID. */
    private static final String FIND_ALL = "SELECT " + COLUMNS + " FROM order_items ORDER BY order_item_id";

    /** Find all order items for a specific order. */
    private static final String FIND_BY_ORDER_ID = "SELECT " + COLUMNS + " FROM order_items WHERE order_id = ? ORDER BY order_item_id";

    /** Insert a new order item and return generated ID. */
    private static final String INSERT = "INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?) RETURNING order_item_id";

    /** Update an order item's quantity and unit price. */
    private static final String UPDATE = "UPDATE order_items SET quantity = ?, unit_price = ? WHERE order_item_id = ?";

    /** Connection provider for obtaining database connections. */
    private final ConnectionProvider connections;

    /**
     * Uses the production database from db.properties.
     */
    public PgOrderItemDao() {
        this(DatabaseConnection.production());
    }

    /**
     * Lets tests pass DatabaseConnection.test().
     *
     * @param connections the connection provider to use
     */
    public PgOrderItemDao(ConnectionProvider connections) {
        this.connections = connections;
    }

    /**
     * Loads one row by id with a parameterised SELECT.
     *
     * @param id the order item primary key
     * @return an Optional containing the record if found, otherwise empty
     * Public: part of the OrderItemDao contract used by the Controller tier.
     */
    @Override
    public Optional<OrderItemRecord> findById(Integer id) {
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(FIND_BY_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load order item " + id, e);
        }
    }

    /**
     * Returns all order items ordered by primary key.
     *
     * @return a list of all order item records (never null)
     * Public: part of the OrderItemDao contract used by the Controller tier.
     */
    @Override
    public List<OrderItemRecord> findAll() {
        List<OrderItemRecord> result = new ArrayList<>();
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load all order items", e);
        }
        return result;
    }

    /**
     * Inserts a new order item and returns the record with the generated ID.
     *
     * @param record the order item data (orderId, productId, quantity, unitPrice must not be null)
     * @return a new OrderItemRecord including the generated orderItemId
     * Public: part of the OrderItemDao contract used by the Controller tier.
     */
    @Override
    public OrderItemRecord insert(OrderItemRecord record) {
        if (record.orderId() == null || record.productId() == null || record.quantity() == null || record.unitPrice() == null) {
            throw new IllegalArgumentException("orderId, productId, quantity, and unitPrice must not be null");
        }
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(INSERT)) {
            ps.setObject(1, record.orderId(), Types.INTEGER);
            ps.setObject(2, record.productId(), Types.INTEGER);
            ps.setObject(3, record.quantity(), Types.INTEGER);
            ps.setObject(4, record.unitPrice(), Types.NUMERIC);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Integer generatedId = rs.getObject("order_item_id", Integer.class);
                    return new OrderItemRecord(
                            generatedId,
                            record.orderId(),
                            record.productId(),
                            record.quantity(),
                            record.unitPrice()
                    );
                }
                throw new DataAccessException("Insert failed, no ID returned");
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not insert order item", e);
        }
    }

    /**
     * Updates the quantity and unit price of an existing order item.
     *
     * @param record the order item with a non-null orderItemId and new quantity/unitPrice
     * @return true if a row was updated, false if no matching ID was found
     * Public: part of the OrderItemDao contract used by the Controller tier.
     */
    @Override
    public boolean update(OrderItemRecord record) {
        if (record.orderItemId() == null) {
            throw new IllegalArgumentException("Order item ID must not be null for update");
        }
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(UPDATE)) {
            ps.setObject(1, record.quantity(), Types.INTEGER);
            ps.setObject(2, record.unitPrice(), Types.NUMERIC);
            ps.setObject(3, record.orderItemId(), Types.INTEGER);

            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not update order item " + record.orderItemId(), e);
        }
    }

    /**
     * Returns all order items belonging to a specific order, ordered by ID.
     *
     * @param orderId the order primary key
     * @return a list of order item records for that order (never null, empty if none)
     * Public: part of the OrderItemDao contract used by the Controller tier.
     */
    @Override
    public List<OrderItemRecord> findByOrderId(Integer orderId) {
        List<OrderItemRecord> result = new ArrayList<>();
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(FIND_BY_ORDER_ID)) {
            ps.setObject(1, orderId, Types.INTEGER);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load order items for order " + orderId, e);
        }
        return result;
    }

    /**
     * Map a ResultSet row to an OrderItemRecord.
     * Only this class knows the column names.
     *
     * @param rs the ResultSet positioned at a valid row
     * @return an OrderItemRecord with the row data
     * @throws SQLException if a database access error occurs
     */
    private OrderItemRecord mapRow(ResultSet rs) throws SQLException {
        Integer orderItemId = rs.getObject("order_item_id", Integer.class);
        Integer orderId = rs.getObject("order_id", Integer.class);
        Integer productId = rs.getObject("product_id", Integer.class);
        Integer quantity = rs.getObject("quantity", Integer.class);
        BigDecimal unitPrice = rs.getObject("unit_price", BigDecimal.class);

        return new OrderItemRecord(
                orderItemId,
                orderId,
                productId,
                quantity,
                unitPrice
        );
    }
}