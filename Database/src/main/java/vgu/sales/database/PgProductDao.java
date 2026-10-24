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
 * JDBC implementation of ProductDao for PostgreSQL.
 * Public because ControllerFactory creates it.
 */
public class PgProductDao implements ProductDao {

    /** Columns selected for product queries. */
    private static final String COLUMNS = "product_id, category_id, name, description, price, stock_quantity, active, created_at";

    /** Find a product by its primary key. */
    private static final String FIND_BY_ID = "SELECT " + COLUMNS + " FROM products WHERE product_id = ?";

    /** Retrieve all products ordered by name. */
    private static final String FIND_ALL = "SELECT " + COLUMNS + " FROM products ORDER BY name";

    /** Insert a new product and return generated ID and created_at. */
    private static final String INSERT = "INSERT INTO products (category_id, name, description, price, stock_quantity, active) "
            + "VALUES (?, ?, ?, ?, ?, ?) RETURNING product_id, created_at";

    /** Update a product (does NOT change stock; stock only changes through updateStock or an order transaction). */
    private static final String UPDATE = "UPDATE products SET category_id = ?, name = ?, description = ?, price = ?, active = ? WHERE product_id = ?";

    /** Update the stock quantity of a product. */
    private static final String UPDATE_STOCK = "UPDATE products SET stock_quantity = ? WHERE product_id = ?";

    /** Connection provider for obtaining database connections. */
    private final ConnectionProvider connections;

    /** Read-only query delegate for search and low-stock operations. */
    private final PgProductQueries queries;

    /**
     * Uses the production database from db.properties.
     */
    public PgProductDao() {
        this(DatabaseConnection.production());
    }

    /**
     * Lets tests pass DatabaseConnection.test().
     *
     * @param connections the connection provider to use
     */
    public PgProductDao(ConnectionProvider connections) {
        this.connections = connections;
        this.queries = new PgProductQueries(connections);
    }

    /**
     * Loads one product row by primary key using a parameterised SELECT.
     *
     * @param id the product_id to look up
     * @return an Optional containing the ProductRecord if found, otherwise empty
     * Public: part of the ProductDao contract used by the Controller tier.
     */
    @Override
    public Optional<ProductRecord> findById(Integer id) {
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(FIND_BY_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load product " + id, e);
        }
    }

    /**
     * Loads all products ordered by name using a parameterised SELECT.
     *
     * @return a List of all ProductRecords, ordered by name
     * Public: part of the ProductDao contract used by the Controller tier.
     */
    @Override
    public List<ProductRecord> findAll() {
        List<ProductRecord> result = new ArrayList<>();
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load all products", e);
        }
        return result;
    }

    /**
     * Inserts a new product row and returns the record with generated ID and created_at.
     *
     * @param record the ProductRecord to insert (productId and createdAt are ignored)
     * @return a new ProductRecord with the generated product_id and created_at
     * Public: part of the ProductDao contract used by the Controller tier.
     */
    @Override
    public ProductRecord insert(ProductRecord record) {
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(INSERT)) {
            ps.setObject(1, record.categoryId(), Types.INTEGER);
            ps.setObject(2, record.name(), Types.VARCHAR);
            ps.setObject(3, record.description(), Types.VARCHAR);
            ps.setObject(4, record.price(), Types.NUMERIC);
            int stock = record.stockQuantity() != null ? record.stockQuantity() : 0;
            ps.setObject(5, stock, Types.INTEGER);
            boolean active = record.active() != null ? record.active() : true;
            ps.setObject(6, active, Types.BOOLEAN);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Integer generatedId = rs.getObject("product_id", Integer.class);
                    LocalDateTime createdAt = rs.getObject("created_at", LocalDateTime.class);
                    // Return a new record with generated values
                    return new ProductRecord(
                            generatedId,
                            record.categoryId(),
                            record.name(),
                            record.description(),
                            record.price(),
                            stock,
                            active,
                            createdAt
                    );
                }
                throw new DataAccessException("Insert failed, no ID returned");
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not insert product", e);
        }
    }

    /**
     * Updates an existing product (excludes stock and created_at).
     *
     * @param record the ProductRecord with fields to update (productId required)
     * @return true if a row was updated, false if no matching product_id
     * Public: part of the ProductDao contract used by the Controller tier.
     */
    @Override
    public boolean update(ProductRecord record) {
        if (record.productId() == null) {
            throw new IllegalArgumentException("Product ID must not be null for update");
        }
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(UPDATE)) {
            ps.setObject(1, record.categoryId(), Types.INTEGER);
            ps.setObject(2, record.name(), Types.VARCHAR);
            ps.setObject(3, record.description(), Types.VARCHAR);
            ps.setObject(4, record.price(), Types.NUMERIC);
            ps.setObject(5, record.active() != null ? record.active() : true, Types.BOOLEAN);
            ps.setObject(6, record.productId(), Types.INTEGER);

            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not update product " + record.productId(), e);
        }
    }

    /**
     * Searches products with optional keyword, category, price range, and stock filter.
     *
     * @param keyword      optional keyword to match against name or description
     * @param categoryId   optional category filter
     * @param minPrice     optional minimum price (inclusive)
     * @param maxPrice     optional maximum price (inclusive)
     * @param inStockOnly  optional filter for products with stock > 0
     * @return a List of matching ProductRecord rows (empty if no matches)
     * Public: part of the ProductDao contract used by the Controller tier.
     */
    @Override
    public List<ProductRecord> search(String keyword, Integer categoryId, BigDecimal minPrice, BigDecimal maxPrice, Boolean inStockOnly) {
        return queries.search(keyword, categoryId, minPrice, maxPrice, inStockOnly);
    }

    /**
     * Updates the stock quantity of a single product.
     *
     * @param productId the product_id to update
     * @param newStock  the new stock quantity (must be non-negative)
     * @return true if a row was updated, false if no matching product_id
     * Public: part of the ProductDao contract used by the Controller tier.
     */
    @Override
    public boolean updateStock(Integer productId, Integer newStock) {
        if (newStock == null || newStock < 0) {
            throw new IllegalArgumentException("newStock must be non-negative");
        }
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(UPDATE_STOCK)) {
            ps.setObject(1, newStock, Types.INTEGER);
            ps.setObject(2, productId, Types.INTEGER);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not update stock for product " + productId, e);
        }
    }

    /**
     * Loads products whose stock is at or below the given threshold.
     *
     * @param threshold the maximum stock_quantity to include
     * @return a List of LowStockRecord rows (empty if none match)
     * Public: part of the ProductDao contract used by the Controller tier.
     */
    @Override
    public List<LowStockRecord> findLowStock(int threshold) {
        return queries.findLowStock(threshold);
    }

    /**
     * Map a ResultSet row to a ProductRecord.
     * Package-private so PgProductQueries can call it without duplicating the mapping logic.
     * Only this class and PgProductQueries know the column names.
     *
     * @param rs the ResultSet positioned at a valid row
     * @return a ProductRecord with the row data
     * @throws SQLException if a database access error occurs
     */
    static ProductRecord mapRow(ResultSet rs) throws SQLException {
        return new ProductRecord(
                rs.getObject("product_id", Integer.class),
                rs.getObject("category_id", Integer.class),
                rs.getObject("name", String.class),
                rs.getObject("description", String.class),
                rs.getObject("price", BigDecimal.class),
                rs.getObject("stock_quantity", Integer.class),
                rs.getObject("active", Boolean.class),
                rs.getObject("created_at", LocalDateTime.class)
        );
    }
}