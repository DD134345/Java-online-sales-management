package vgu.sales.database;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Package-private: the read-only order list and search queries that return OrderSummaryRecord rows.
 * Moved out of PgOrderDao unchanged so both classes stay under 300 lines; only PgOrderDao uses it.
 */
final class PgOrderSummaryQueries {

    /** Find all orders with summary info, newest first, with pagination. */
    private static final String FIND_ALL_SUMMARY = "SELECT o.order_id, o.order_code, c.full_name AS customer_name, o.order_date, o.total_amount, o.status, COUNT(oi.order_item_id)::int AS item_count "
            + "FROM orders o "
            + "JOIN customers c ON c.customer_id = o.customer_id "
            + "LEFT JOIN order_items oi ON oi.order_id = o.order_id "
            + "GROUP BY o.order_id, c.full_name "
            + "ORDER BY o.order_date DESC "
            + "LIMIT ? OFFSET ?";

    /** Search orders with optional filters. */
    private static final String SEARCH = "SELECT o.order_id, o.order_code, c.full_name AS customer_name, o.order_date, o.total_amount, o.status, COUNT(oi.order_item_id)::int AS item_count "
            + "FROM orders o "
            + "JOIN customers c ON c.customer_id = o.customer_id "
            + "LEFT JOIN order_items oi ON oi.order_id = o.order_id "
            + "WHERE 1=1 "
            + "%s "
            + "GROUP BY o.order_id, c.full_name "
            + "ORDER BY o.order_date DESC";

    /** Private and final: set once by the constructor. */
    private final ConnectionProvider connections;

    /**
     * Package-private: created only by PgOrderDao.
     *
     * @param connections connection source
     */
    PgOrderSummaryQueries(ConnectionProvider connections) {
        this.connections = connections;
    }

    /**
     * Loads a page of order summaries (joined with customer, item count) newest first.
     *
     * @param limit  max rows to return
     * @param offset number of rows to skip
     * @return list of order summaries (may be empty)
     */
    List<OrderSummaryRecord> findAll(int limit, int offset) {
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(FIND_ALL_SUMMARY)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                List<OrderSummaryRecord> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapSummaryRow(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load order summaries", e);
        }
    }

    /**
     * Searches orders with optional filters on status, date range, and keyword.
     *
     * @param status   exact status filter (null or blank = no filter)
     * @param fromDate inclusive start date (null = no lower bound)
     * @param toDate   inclusive end date (null = no upper bound)
     * @param keyword  searches order_code, customer full_name, or phone (null or blank = no filter)
     * @return list of matching order summaries (may be empty)
     */
    List<OrderSummaryRecord> search(String status, LocalDate fromDate, LocalDate toDate, String keyword) {
        StringBuilder where = new StringBuilder();
        List<Object> params = new ArrayList<>();

        if (status != null && !status.isBlank()) {
            where.append(" AND o.status = ?");
            params.add(status);
        }
        if (fromDate != null) {
            where.append(" AND o.order_date >= ?");
            params.add(fromDate.atStartOfDay());
        }
        if (toDate != null) {
            where.append(" AND o.order_date < ?");
            params.add(toDate.plusDays(1).atStartOfDay());
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = "%" + keyword.toLowerCase().trim() + "%";
            where.append(" AND (lower(o.order_code) LIKE ? OR lower(c.full_name) LIKE ? OR c.phone LIKE ?)");
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }

        // Safe from SQL injection: 'where' only contains fixed SQL fragments; every value is bound with ? below.
        String sql = String.format(SEARCH, where);
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof LocalDateTime) {
                    ps.setObject(i + 1, param, Types.TIMESTAMP);
                } else if (param instanceof String) {
                    ps.setString(i + 1, (String) param);
                } else {
                    ps.setObject(i + 1, param);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<OrderSummaryRecord> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapSummaryRow(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not search orders", e);
        }
    }

    /**
     * Maps a ResultSet row to an OrderSummaryRecord. Private: only used by this class.
     */
    private OrderSummaryRecord mapSummaryRow(ResultSet rs) throws SQLException {
        return new OrderSummaryRecord(
                rs.getObject("order_id", Integer.class),
                rs.getString("order_code"),
                rs.getString("customer_name"),
                rs.getObject("order_date", LocalDateTime.class),
                rs.getObject("total_amount", BigDecimal.class),
                rs.getString("status"),
                rs.getInt("item_count")
        );
    }
}