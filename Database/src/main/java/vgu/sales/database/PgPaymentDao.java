package vgu.sales.database;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of PaymentDao for PostgreSQL.
 * Public because ControllerFactory creates it.
 */
public class PgPaymentDao implements PaymentDao {

    /** Find a payment by its primary key. */
    private static final String FIND_BY_ID = "SELECT payment_id, order_id, method, amount, paid_at, cash_received, change_amount, bank_reference, cardholder_name FROM payments WHERE payment_id = ?";

    /** Retrieve all payments ordered by payment_id. */
    private static final String FIND_ALL = "SELECT payment_id, order_id, method, amount, paid_at, cash_received, change_amount, bank_reference, cardholder_name FROM payments ORDER BY payment_id";

    /** Find a payment by order ID. */
    private static final String FIND_BY_ORDER_ID = "SELECT payment_id, order_id, method, amount, paid_at, cash_received, change_amount, bank_reference, cardholder_name FROM payments WHERE order_id = ?";

    /** Insert a new payment and return generated payment_id and paid_at. */
    private static final String INSERT = "INSERT INTO payments (order_id, method, amount, paid_at, cash_received, change_amount, bank_reference, cardholder_name) VALUES (?, ?, ?, COALESCE(?, NOW()), ?, ?, ?, ?) RETURNING payment_id, paid_at";

    /** Update a payment (amount and method-specific fields). */
    private static final String UPDATE = "UPDATE payments SET amount = ?, cash_received = ?, change_amount = ?, bank_reference = ?, cardholder_name = ? WHERE payment_id = ?";

    /** Private and final: set once by the constructor, never exposed. */
    private final ConnectionProvider connections;

    /**
     * Uses the production database from db.properties.
     */
    public PgPaymentDao() {
        this(DatabaseConnection.production());
    }

    /**
     * Lets tests pass DatabaseConnection.test().
     * @param connections the connection provider
     */
    public PgPaymentDao(ConnectionProvider connections) {
        this.connections = connections;
    }

    /**
     * Loads one payment row by id with a parameterised SELECT.
     * @param id the payment primary key
     * @return the payment record if found, empty otherwise
     * Public: part of the PaymentDao contract used by the Controller tier.
     */
    @Override
    public Optional<PaymentRecord> findById(Integer id) {
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(FIND_BY_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load payment " + id, e);
        }
    }

    /**
     * Loads all payment rows ordered by id with a parameterised SELECT.
     * @return list of all payment records (empty if none)
     * Public: part of the PaymentDao contract used by the Controller tier.
     */
    @Override
    public List<PaymentRecord> findAll() {
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            List<PaymentRecord> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load all payments", e);
        }
    }

    /**
     * Loads a payment row by order id with a parameterised SELECT.
     * @param orderId the order foreign key
     * @return the payment record if found, empty otherwise
     * Public: part of the PaymentDao contract used by the Controller tier.
     */
    @Override
    public Optional<PaymentRecord> findByOrderId(Integer orderId) {
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(FIND_BY_ORDER_ID)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load payment for order " + orderId, e);
        }
    }

    /**
     * Inserts a new payment row and returns the generated id and paid_at.
     * @param record the payment data (orderId, method, amount, paidAt, cashReceived, changeAmount, bankReference, cardholderName)
     * @return the inserted payment record with generated payment_id and paid_at
     * Public: part of the PaymentDao contract used by the Controller tier.
     */
    @Override
    public PaymentRecord insert(PaymentRecord record) {
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(INSERT)) {
            ps.setInt(1, record.orderId());
            ps.setObject(2, record.method(), Types.VARCHAR);
            ps.setObject(3, record.amount(), Types.NUMERIC);
            ps.setObject(4, record.paidAt(), Types.TIMESTAMP);
            ps.setObject(5, record.cashReceived(), Types.NUMERIC);
            ps.setObject(6, record.changeAmount(), Types.NUMERIC);
            ps.setObject(7, record.bankReference(), Types.VARCHAR);
            ps.setObject(8, record.cardholderName(), Types.VARCHAR);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Integer generatedId = rs.getObject("payment_id", Integer.class);
                    LocalDateTime generatedPaidAt = rs.getObject("paid_at", LocalDateTime.class);
                    return new PaymentRecord(
                            generatedId,
                            record.orderId(),
                            record.method(),
                            record.amount(),
                            generatedPaidAt,
                            record.cashReceived(),
                            record.changeAmount(),
                            record.bankReference(),
                            record.cardholderName()
                    );
                }
                throw new DataAccessException("Insert returned no generated key");
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not insert payment", e);
        }
    }

    /**
     * Updates an existing payment row (amount and method-specific fields).
     * @param record the payment with id and new values
     * @return true if a row was updated, false if not found
     * Public: part of the PaymentDao contract used by the Controller tier.
     */
    @Override
    public boolean update(PaymentRecord record) {
        if (record.paymentId() == null) {
            throw new IllegalArgumentException("Cannot update payment without ID");
        }
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(UPDATE)) {
            ps.setObject(1, record.amount(), Types.NUMERIC);
            ps.setObject(2, record.cashReceived(), Types.NUMERIC);
            ps.setObject(3, record.changeAmount(), Types.NUMERIC);
            ps.setObject(4, record.bankReference(), Types.VARCHAR);
            ps.setObject(5, record.cardholderName(), Types.VARCHAR);
            ps.setInt(6, record.paymentId());

            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not update payment " + record.paymentId(), e);
        }
    }

    /**
     * Private: converts the current ResultSet row into a record; only this class knows the column names.
     * @param rs the result set positioned at a valid row
     * @return a new PaymentRecord
     * @throws SQLException if column access fails
     */
    private PaymentRecord mapRow(ResultSet rs) throws SQLException {
        return new PaymentRecord(
                rs.getObject("payment_id", Integer.class),
                rs.getObject("order_id", Integer.class),
                rs.getObject("method", String.class),
                rs.getObject("amount", BigDecimal.class),
                rs.getObject("paid_at", LocalDateTime.class),
                rs.getObject("cash_received", BigDecimal.class),
                rs.getObject("change_amount", BigDecimal.class),
                rs.getObject("bank_reference", String.class),
                rs.getObject("cardholder_name", String.class)
        );
    }
}