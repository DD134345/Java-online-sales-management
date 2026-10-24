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
 * JDBC implementation of StaffDao for PostgreSQL.
 * Public because ControllerFactory creates it.
 */
public class PgStaffDao implements StaffDao {

    /** Columns selected for staff queries. */
    private static final String COLUMNS = "staff_id, username, password_hash, password_salt, full_name, email, phone, role, active, created_at";

    /** Find a staff member by primary key. */
    private static final String FIND_BY_ID = "SELECT " + COLUMNS + " FROM staff WHERE staff_id = ?";

    /** Retrieve all staff ordered by staff_id. */
    private static final String FIND_ALL = "SELECT " + COLUMNS + " FROM staff ORDER BY staff_id";

    /** Find a staff member by username (case-insensitive). */
    private static final String FIND_BY_USERNAME = "SELECT " + COLUMNS + " FROM staff WHERE lower(username) = lower(?)";

    /** Insert a new staff member and return generated ID and created_at. */
    private static final String INSERT = "INSERT INTO staff (username, password_hash, password_salt, full_name, email, phone, role, active) "
            + "VALUES (lower(?), ?, ?, ?, ?, ?, ?, ?) RETURNING staff_id, username, created_at";

    /** Update a staff member (includes password hash/salt for password reset; never changes username). */
    private static final String UPDATE = "UPDATE staff SET full_name = ?, email = ?, phone = ?, role = ?, active = ?, password_hash = ?, password_salt = ? WHERE staff_id = ?";

    /** Connection provider for obtaining database connections. */
    private final ConnectionProvider connections;

    /**
     * Uses the production database from db.properties.
     */
    public PgStaffDao() {
        this(DatabaseConnection.production());
    }

    /**
     * Lets tests pass DatabaseConnection.test().
     *
     * @param connections the connection provider to use
     */
    public PgStaffDao(ConnectionProvider connections) {
        this.connections = connections;
    }

    /**
     * Loads one staff row by primary key with a parameterised SELECT.
     *
     * @param id the staff_id to look up
     * @return Optional containing the StaffRecord if found, empty otherwise
     * Public: part of the StaffDao contract used by the Controller tier.
     */
    @Override
    public Optional<StaffRecord> findById(Integer id) {
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(FIND_BY_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load staff " + id, e);
        }
    }

    /**
     * Loads all staff rows ordered by staff_id with a parameterised SELECT.
     *
     * @return List of all StaffRecord objects
     * Public: part of the StaffDao contract used by the Controller tier.
     */
    @Override
    public List<StaffRecord> findAll() {
        List<StaffRecord> result = new ArrayList<>();
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load all staff", e);
        }
        return result;
    }

    /**
     * Inserts a new staff row and returns the generated staff_id, username, and created_at.
     *
     * @param record the StaffRecord to insert (staffId must be null, will be generated)
     * @return a new StaffRecord with generated staff_id, username, and created_at
     * Public: part of the StaffDao contract used by the Controller tier.
     */
    @Override
    public StaffRecord insert(StaffRecord record) {
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(INSERT)) {
            ps.setObject(1, record.username(), Types.VARCHAR);
            ps.setObject(2, record.passwordHash(), Types.VARCHAR);
            ps.setObject(3, record.passwordSalt(), Types.VARCHAR);
            ps.setObject(4, record.fullName(), Types.VARCHAR);
            ps.setObject(5, record.email(), Types.VARCHAR);
            ps.setObject(6, record.phone(), Types.VARCHAR);
            ps.setObject(7, record.role(), Types.VARCHAR);
            boolean active = record.active() != null ? record.active() : true;
            ps.setObject(8, active, Types.BOOLEAN);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Integer generatedId = rs.getObject("staff_id", Integer.class);
                    String generatedUsername = rs.getObject("username", String.class);
                    LocalDateTime createdAt = rs.getObject("created_at", LocalDateTime.class);
                    // Return a new record with generated values
                    return new StaffRecord(
                            generatedId,
                            generatedUsername,
                            record.passwordHash(),
                            record.passwordSalt(),
                            record.fullName(),
                            record.email(),
                            record.phone(),
                            record.role(),
                            active,
                            createdAt
                    );
                }
                throw new DataAccessException("Insert failed, no ID returned");
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not insert staff", e);
        }
    }

    /**
     * Updates contact data, role, active flag and password hash/salt of one staff row;
     * the username never changes.
     *
     * @param record the StaffRecord with updated fields (staffId must not be null)
     * @return true if a row was updated, false if no matching staff_id
     * Public: part of the StaffDao contract used by the Controller tier.
     */
    @Override
    public boolean update(StaffRecord record) {
        if (record.staffId() == null) {
            throw new IllegalArgumentException("Staff ID must not be null for update");
        }
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(UPDATE)) {
            ps.setObject(1, record.fullName(), Types.VARCHAR);
            ps.setObject(2, record.email(), Types.VARCHAR);
            ps.setObject(3, record.phone(), Types.VARCHAR);
            ps.setObject(4, record.role(), Types.VARCHAR);
            ps.setObject(5, record.active() != null ? record.active() : true, Types.BOOLEAN);
            ps.setObject(6, record.passwordHash(), Types.VARCHAR);
            ps.setObject(7, record.passwordSalt(), Types.VARCHAR);
            ps.setObject(8, record.staffId(), Types.INTEGER);

            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not update staff " + record.staffId(), e);
        }
    }

    /**
     * Loads one staff row by username with a parameterised case-insensitive SELECT.
     *
     * @param username the username to look up (trimmed, case-insensitive)
     * @return Optional containing the StaffRecord if found, empty otherwise
     * Public: part of the StaffDao contract used by the Controller tier.
     */
    @Override
    public Optional<StaffRecord> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        String trimmed = username.trim();
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(FIND_BY_USERNAME)) {
            ps.setObject(1, trimmed, Types.VARCHAR);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not find staff by username " + trimmed, e);
        }
    }

    /**
     * Map a ResultSet row to a StaffRecord.
     * Only this class knows the column names.
     *
     * @param rs the ResultSet positioned at a valid row
     * @return a StaffRecord with the row data
     * @throws SQLException if a database access error occurs
     */
    private StaffRecord mapRow(ResultSet rs) throws SQLException {
        return new StaffRecord(
                rs.getObject("staff_id", Integer.class),
                rs.getObject("username", String.class),
                rs.getObject("password_hash", String.class),
                rs.getObject("password_salt", String.class),
                rs.getObject("full_name", String.class),
                rs.getObject("email", String.class),
                rs.getObject("phone", String.class),
                rs.getObject("role", String.class),
                rs.getObject("active", Boolean.class),
                rs.getObject("created_at", LocalDateTime.class)
        );
    }
}