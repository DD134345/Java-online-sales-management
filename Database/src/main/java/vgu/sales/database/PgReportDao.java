package vgu.sales.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * JDBC implementation of ReportDao for PostgreSQL. Public because ControllerFactory creates it.
 */
public class PgReportDao implements ReportDao {
    /** Private: SQL text is an implementation detail of this class. */
    private static final String DAILY_REVENUE = "SELECT CAST(o.order_date AS DATE) AS day, COUNT(*)::int AS order_count, SUM(o.total_amount) AS total_revenue, ROUND(AVG(o.total_amount), 0) AS average_order_value FROM orders o WHERE o.status IN ('PAID', 'SHIPPED') AND o.order_date >= ? AND o.order_date < ? GROUP BY CAST(o.order_date AS DATE) ORDER BY day";
    private static final String TOP_PRODUCTS = "SELECT p.product_id, p.name AS product_name, c.name AS category_name, SUM(oi.quantity)::int AS quantity_sold, SUM(oi.quantity * oi.unit_price) AS total_revenue FROM order_items oi JOIN orders o ON o.order_id = oi.order_id JOIN products p ON p.product_id = oi.product_id JOIN categories c ON c.category_id = p.category_id WHERE o.status IN ('PAID', 'SHIPPED') AND o.order_date >= ? AND o.order_date < ? GROUP BY p.product_id, p.name, c.name ORDER BY quantity_sold DESC, total_revenue DESC LIMIT ?";

    /** Private and final: set once by the constructor, never exposed. */
    private final ConnectionProvider connections;

    /** Uses the production database from db.properties. */
    public PgReportDao() { this(DatabaseConnection.production()); }

    /** Lets tests pass DatabaseConnection.test(). */
    public PgReportDao(ConnectionProvider connections) { this.connections = connections; }

    /** {@inheritDoc} */
    @Override
    public List<DailyRevenueRecord> getDailyRevenue(LocalDate start, LocalDate end) {
        if (start == null || end == null || start.isAfter(end))
            throw new IllegalArgumentException("Valid date range required: start= " + start + ", end= " + end);
        List<DailyRevenueRecord> result = new ArrayList<>();
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(DAILY_REVENUE)) {
            ps.setObject(1, start.atStartOfDay());
            ps.setObject(2, end.plusDays(1).atStartOfDay());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapDailyRevenueRow(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load daily revenue", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<TopProductRecord> getTopSellingProducts(LocalDate start, LocalDate end, int limit) {
        if (start == null || end == null || start.isAfter(end))
            throw new IllegalArgumentException("Valid date range required: start= " + start + ", end= " + end);
        int useLimit = (limit <= 0) ? 10 : limit;
        List<TopProductRecord> result = new ArrayList<>();
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(TOP_PRODUCTS)) {
            ps.setObject(1, start.atStartOfDay());
            ps.setObject(2, end.plusDays(1).atStartOfDay());
            ps.setInt(3, useLimit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapTopProductRow(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load top selling products", e);
        }
    }

    /** Private: converts current ResultSet row into DailyRevenueRecord. */
    private DailyRevenueRecord mapDailyRevenueRow(ResultSet rs) throws SQLException {
        return new DailyRevenueRecord(
                rs.getObject("day", LocalDate.class),
                rs.getObject("order_count", Integer.class),
                rs.getObject("total_revenue", BigDecimal.class),
                rs.getObject("average_order_value", BigDecimal.class)
        );
    }

    /** Private: converts current ResultSet row into TopProductRecord. */
    private TopProductRecord mapTopProductRow(ResultSet rs) throws SQLException {
        return new TopProductRecord(
                rs.getObject("product_id", Integer.class),
                rs.getString("product_name"),
                rs.getString("category_name"),
                rs.getObject("quantity_sold", Integer.class),
                rs.getObject("total_revenue", BigDecimal.class)
        );
    }
}
