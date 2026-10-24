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
 * JDBC implementation of CategoryDao for PostgreSQL.
 * Public because ControllerFactory creates it.
 */
public class PgCategoryDao implements CategoryDao {

    /** Private: SQL text is an implementation detail of this class. */
    private static final String FIND_BY_ID = """
        SELECT category_id, name, description, active
        FROM categories
        WHERE category_id = ?
        """;

    /** Private: SQL text is an implementation detail of this class. */
    private static final String FIND_ALL = """
        SELECT category_id, name, description, active
        FROM categories
        ORDER BY name
        """;

    /** Private: SQL text is an implementation detail of this class. */
    private static final String FIND_ACTIVE = """
        SELECT category_id, name, description, active
        FROM categories
        WHERE active = true
        ORDER BY name
        """;

    /** Private: SQL text is an implementation detail of this class. */
    private static final String INSERT = """
        INSERT INTO categories (name, description, active)
        VALUES (?, ?, ?)
        RETURNING category_id
        """;

    /** Private: SQL text is an implementation detail of this class. */
    private static final String UPDATE = """
        UPDATE categories
        SET name = ?, description = ?, active = ?
        WHERE category_id = ?
        """;

    /** Private and final: set once by the constructor, never exposed. */
    private final ConnectionProvider connections;

    /**
     * Uses the production database from db.properties.
     * Public so ControllerFactory can instantiate it.
     */
    public PgCategoryDao() {
        this(DatabaseConnection.production());
    }

    /**
     * Lets tests pass DatabaseConnection.test().
     * Public so tests can inject a test ConnectionProvider.
     *
     * @param connections the connection provider to use
     */
    public PgCategoryDao(ConnectionProvider connections) {
        this.connections = connections;
    }

    /**
     * Loads one row by id with a parameterised SELECT.
     *
     * @param id the category primary key
     * @return the CategoryRecord if found, else empty
     * Public: part of the CategoryDao contract used by the Controller tier.
     */
    @Override
    public Optional<CategoryRecord> findById(Integer id) {
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(FIND_BY_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load category " + id, e);
        }
    }

    /**
     * Loads all rows with a parameterised SELECT ordered by name.
     *
     * @return list of all CategoryRecords (empty if none)
     * Public: part of the CategoryDao contract used by the Controller tier.
     */
    @Override
    public List<CategoryRecord> findAll() {
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            List<CategoryRecord> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load all categories", e);
        }
    }

    /**
     * Loads active rows with a parameterised SELECT ordered by name.
     *
     * @return list of active CategoryRecords (empty if none)
     * Public: part of the CategoryDao contract used by the Controller tier.
     */
    @Override
    public List<CategoryRecord> findActive() {
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(FIND_ACTIVE);
             ResultSet rs = ps.executeQuery()) {
            List<CategoryRecord> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load active categories", e);
        }
    }

    /**
     * Inserts a new row and returns the generated-key record.
     *
     * @param record the CategoryRecord to insert (categoryId is ignored)
     * @return the inserted CategoryRecord with generated categoryId
     * Public: part of the CategoryDao contract used by the Controller tier.
     */
    @Override
    public CategoryRecord insert(CategoryRecord record) {
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(INSERT)) {
            // active null -> true
            Boolean active = record.active() != null ? record.active() : true;
            ps.setString(1, record.name());
            ps.setObject(2, record.description(), Types.VARCHAR);
            ps.setObject(3, active, Types.BOOLEAN);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Integer generatedId = rs.getObject("category_id", Integer.class);
                    return new CategoryRecord(generatedId, record.name(), record.description(), active);
                }
                throw new DataAccessException("Insert returned no generated key");
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not insert category", e);
        }
    }

    /**
     * Updates an existing row by primary key.
     *
     * @param record the CategoryRecord to update (categoryId must not be null)
     * @return true if a row was updated, false if not found
     * Public: part of the CategoryDao contract used by the Controller tier.
     */
    @Override
    public boolean update(CategoryRecord record) {
        if (record.categoryId() == null) {
            throw new DataAccessException("Cannot update category without ID");
        }
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(UPDATE)) {
            ps.setString(1, record.name());
            ps.setObject(2, record.description(), Types.VARCHAR);
            ps.setObject(3, record.active(), Types.BOOLEAN);
            ps.setInt(4, record.categoryId());
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not update category " + record.categoryId(), e);
        }
    }

    /**
     * Private: converts the current ResultSet row into a record;
     * only this class knows the column names.
     *
     * @param rs the result set positioned at a valid row
     * @return a CategoryRecord populated from the row
     * @throws SQLException if a database access error occurs
     */
    private CategoryRecord mapRow(ResultSet rs) throws SQLException {
        Integer categoryId = rs.getObject("category_id", Integer.class);
        String name = rs.getObject("name", String.class);
        String description = rs.getObject("description", String.class);
        Boolean active = rs.getObject("active", Boolean.class);
        return new CategoryRecord(categoryId, name, description, active);
    }
}