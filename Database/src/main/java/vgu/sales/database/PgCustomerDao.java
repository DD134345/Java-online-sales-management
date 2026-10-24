package vgu.sales.database;

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
 * JDBC implementation of CustomerDao for PostgreSQL.
 * Public because ControllerFactory creates it.
 */
public class PgCustomerDao implements CustomerDao {

    /** Columns selected for customer queries. */
    private static final String COLUMNS = "customer_id, full_name, phone, email, address, created_at";

    /** Find a customer by its primary key. */
    private static final String FIND_BY_ID = "SELECT " + COLUMNS + " FROM customers WHERE customer_id = ?";

    /** Retrieve all customers ordered by name. */
    private static final String FIND_ALL = "SELECT " + COLUMNS + " FROM customers ORDER BY full_name";

    /** Search customers by keyword in name, phone, or email. */
    private static final String SEARCH = "SELECT " + COLUMNS
            + " FROM customers"
            + " WHERE lower(full_name) LIKE ?"
            + "    OR phone LIKE ?"
            + "    OR lower(coalesce(email, '')) LIKE ?"
            + " ORDER BY full_name";

    /** Find a customer by phone number. */
    private static final String FIND_BY_PHONE = "SELECT " + COLUMNS + " FROM customers WHERE phone = ?";

    /** Insert a new customer and return generated ID and created_at. */
    private static final String INSERT = "INSERT INTO customers (full_name, phone, email, address)"
            + " VALUES (?, ?, ?, ?)"
            + " RETURNING customer_id, created_at";

    /** Update a customer. */
    private static final String UPDATE = "UPDATE customers"
            + " SET full_name = ?, phone = ?, email = ?, address = ?"
            + " WHERE customer_id = ?";

    /** Private and final: set once by the constructor, never exposed. */
    private final ConnectionProvider connections;

    /** Uses the production database from db.properties. */
    public PgCustomerDao() {
        this(DatabaseConnection.production());
    }

    /** Lets tests pass DatabaseConnection.test(). */
    public PgCustomerDao(ConnectionProvider connections) {
        this.connections = connections;
    }

    /**
     * Loads one row by id with a parameterised SELECT.
     *
     * @param id the customer primary key
     * @return an Optional containing the record if found, empty otherwise
     * Public: part of the CustomerDao contract used by the Controller tier.
     */
    @Override
    public Optional<CustomerRecord> findById(Integer id) {
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(FIND_BY_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load customer " + id, e);
        }
    }

    /**
     * Loads all rows ordered by name with a parameterised SELECT.
     *
     * @return a list of all customer records, possibly empty
     * Public: part of the CustomerDao contract used by the Controller tier.
     */
    @Override
    public List<CustomerRecord> findAll() {
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            List<CustomerRecord> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load all customers", e);
        }
    }

    /**
     * Searches customers by keyword in name, phone, or email with a parameterised SELECT.
     *
     * @param keyword the search term (case-insensitive for name and email)
     * @return a list of matching customer records, possibly empty
     * Public: part of the CustomerDao contract used by the Controller tier.
     */
    @Override
    public List<CustomerRecord> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return findAll();
        }
        String pattern = "%" + keyword.toLowerCase().trim() + "%";
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(SEARCH)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                List<CustomerRecord> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not search customers with keyword: " + keyword, e);
        }
    }

    /**
     * Loads one row by phone with a parameterised SELECT.
     *
     * @param phone the phone number to look up
     * @return an Optional containing the record if found, empty otherwise
     * Public: part of the CustomerDao contract used by the Controller tier.
     */
    @Override
    public Optional<CustomerRecord> findByPhone(String phone) {
        String trimmedPhone = phone == null ? null : phone.trim();
        if (trimmedPhone == null || trimmedPhone.isBlank()) {
            return Optional.empty();
        }
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(FIND_BY_PHONE)) {
            ps.setString(1, trimmedPhone);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not find customer by phone: " + trimmedPhone, e);
        }
    }

    /**
     * Inserts a new customer and returns the generated id and created_at.
     *
     * @param record the customer data (id and created_at must be null)
     * @return a new CustomerRecord with the generated id and created_at populated
     * Public: part of the CustomerDao contract used by the Controller tier.
     */
    @Override
    public CustomerRecord insert(CustomerRecord record) {
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(INSERT)) {
            ps.setString(1, record.fullName());
            ps.setString(2, record.phone());
            ps.setObject(3, record.email(), Types.VARCHAR);
            ps.setObject(4, record.address(), Types.VARCHAR);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Integer id = rs.getObject("customer_id", Integer.class);
                    LocalDateTime createdAt = rs.getObject("created_at", LocalDateTime.class);
                    return new CustomerRecord(id, record.fullName(), record.phone(), record.email(), record.address(), createdAt);
                }
                throw new DataAccessException("Insert customer returned no generated key");
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not insert customer: " + record.fullName(), e);
        }
    }

    /**
     * Updates an existing customer by id.
     *
     * @param record the customer data with a non-null id
     * @return true if a row was updated, false if the id was not found
     * Public: part of the CustomerDao contract used by the Controller tier.
     */
    @Override
    public boolean update(CustomerRecord record) {
        if (record.customerId() == null) {
            throw new DataAccessException("Cannot update customer without id");
        }
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(UPDATE)) {
            ps.setString(1, record.fullName());
            ps.setString(2, record.phone());
            ps.setObject(3, record.email(), Types.VARCHAR);
            ps.setObject(4, record.address(), Types.VARCHAR);
            ps.setInt(5, record.customerId());
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not update customer: " + record.customerId(), e);
        }
    }

    /**
     * Private: converts the current ResultSet row into a record; only this class knows the column names.
     *
     * @param rs the result set positioned at a valid row
     * @return a populated CustomerRecord
     * @throws SQLException if column access fails
     */
    private CustomerRecord mapRow(ResultSet rs) throws SQLException {
        Integer customerId = rs.getObject("customer_id", Integer.class);
        String fullName = rs.getString("full_name");
        String phone = rs.getString("phone");
        // phone is CHAR(10) in the database; trim it when mapping
        if (phone != null) {
            phone = phone.trim();
        }
        String email = rs.getString("email");
        String address = rs.getString("address");
        LocalDateTime createdAt = rs.getObject("created_at", LocalDateTime.class);
        return new CustomerRecord(customerId, fullName, phone, email, address, createdAt);
    }
}