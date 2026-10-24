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

/**
 * Package-private: the read-only product search and low-stock report queries; only PgProductDao uses it.
 */
final class PgProductQueries {

    /** Columns selected for product queries. */
    private static final String COLUMNS = "product_id, category_id, name, description, price, stock_quantity, active, created_at";

    /**
     * Find products with stock at or below a threshold.
     * Joins with categories to get the category name.
     */
    private static final String LOW_STOCK = "SELECT p.product_id, p.name AS product_name, c.name AS category_name, "
            + "p.stock_quantity, p.price, "
            + "CASE WHEN p.stock_quantity = 0 THEN 'OUT OF STOCK' ELSE 'LOW STOCK' END AS reorder_status "
            + "FROM products p JOIN categories c ON c.category_id = p.category_id "
            + "WHERE p.active = true AND p.stock_quantity <= ? ORDER BY p.stock_quantity, p.name";

    /**
     * SQL fragments for dynamic search query.
     * The query is built from fixed fragments only; VALUES always go through ? placeholders.
     * This is injection-safe because:
     * 1) The SQL structure (keywords, column names, operators) is hard-coded in constants
     * 2) Only parameter VALUES are bound via PreparedStatement.setObject()
     * 3) No user input is ever concatenated into the SQL string
     */
    private static final String SEARCH_BASE = "SELECT " + COLUMNS + " FROM products WHERE 1=1";
    private static final String SEARCH_KEYWORD = " AND (lower(name) LIKE ? OR lower(coalesce(description, '')) LIKE ?)";
    private static final String SEARCH_CATEGORY = " AND category_id = ?";
    private static final String SEARCH_MIN_PRICE = " AND price >= ?";
    private static final String SEARCH_MAX_PRICE = " AND price <= ?";
    private static final String SEARCH_IN_STOCK = " AND stock_quantity > 0";
    private static final String SEARCH_ORDER_BY = " ORDER BY name";

    /** Connection provider for obtaining database connections. */
    private final ConnectionProvider connections;

    /**
     * Creates a new PgProductQueries with the given connection provider.
     *
     * @param connections the connection provider to use
     */
    PgProductQueries(ConnectionProvider connections) {
        this.connections = connections;
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
     */
    List<ProductRecord> search(String keyword, Integer categoryId, BigDecimal minPrice, BigDecimal maxPrice, Boolean inStockOnly) {
        StringBuilder sql = new StringBuilder(SEARCH_BASE);
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            sql.append(SEARCH_KEYWORD);
            String kw = "%" + keyword.toLowerCase().trim() + "%";
            params.add(kw);
            params.add(kw);
        }
        if (categoryId != null) {
            sql.append(SEARCH_CATEGORY);
            params.add(categoryId);
        }
        if (minPrice != null) {
            sql.append(SEARCH_MIN_PRICE);
            params.add(minPrice);
        }
        if (maxPrice != null) {
            sql.append(SEARCH_MAX_PRICE);
            params.add(maxPrice);
        }
        if (Boolean.TRUE.equals(inStockOnly)) {
            sql.append(SEARCH_IN_STOCK);
        }
        sql.append(SEARCH_ORDER_BY);

        List<ProductRecord> result = new ArrayList<>();
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not search products", e);
        }
        return result;
    }

    /**
     * Loads products whose stock is at or below the given threshold.
     *
     * @param threshold the maximum stock_quantity to include
     * @return a List of LowStockRecord rows (empty if none match)
     */
    List<LowStockRecord> findLowStock(int threshold) {
        List<LowStockRecord> result = new ArrayList<>();
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(LOW_STOCK)) {
            ps.setObject(1, threshold, Types.INTEGER);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapLowStockRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load low stock products", e);
        }
        return result;
    }

    /**
     * Map a ResultSet row to a ProductRecord.
     * Package-private so PgProductQueries can call it without duplicating the mapping logic.
     * Only this class and PgProductDao know the column names.
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

    /**
     * Map a ResultSet row to a LowStockRecord.
     *
     * @param rs the ResultSet positioned at a valid row
     * @return a LowStockRecord with the row data
     * @throws SQLException if a database access error occurs
     */
    private LowStockRecord mapLowStockRow(ResultSet rs) throws SQLException {
        return new LowStockRecord(
                rs.getObject("product_id", Integer.class),
                rs.getObject("product_name", String.class),
                rs.getObject("category_name", String.class),
                rs.getObject("stock_quantity", Integer.class),
                rs.getObject("price", BigDecimal.class),
                rs.getObject("reorder_status", String.class)
        );
    }
}