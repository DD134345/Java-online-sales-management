package freshlink.database.dao;

import freshlink.database.DBConnection;
import freshlink.database.model.Voucher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** SQL for the vouchers table, plus the voucher rules used at checkout. */
public class VoucherDAO {

    public List<Voucher> getAllVouchers() throws SQLException {
        return getVouchers("SELECT * FROM vouchers ORDER BY end_date DESC");
    }

    /** Vouchers a customer can use right now. */
    public List<Voucher> getAvailableVouchers() throws SQLException {
        return getVouchers("SELECT * FROM vouchers WHERE active AND NOW() BETWEEN start_date AND end_date "
                + "AND (usage_limit IS NULL OR used_count < usage_limit) ORDER BY min_order_value");
    }

    /** Returns null if the code does not exist. Not case-sensitive. */
    public Voucher getVoucherByCode(String code) throws SQLException {
        String sql = "SELECT * FROM vouchers WHERE UPPER(code) = UPPER(?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code.trim());
            ResultSet rs = ps.executeQuery();
            return rs.next() ? toVoucher(rs) : null;
        }
    }

    public int addVoucher(Voucher v) throws SQLException {
        String sql = "INSERT INTO vouchers (code, description, discount_type, discount_value, min_order_value, "
                + "max_discount, start_date, end_date, usage_limit) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING voucher_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, v.code().toUpperCase());
            ps.setString(2, v.description());
            ps.setString(3, v.discountType());
            ps.setLong(4, v.discountValue());
            ps.setLong(5, v.minOrderValue());
            ps.setObject(6, v.maxDiscount());
            ps.setTimestamp(7, Timestamp.valueOf(v.startDate()));
            ps.setTimestamp(8, Timestamp.valueOf(v.endDate()));
            ps.setObject(9, v.usageLimit());
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt("voucher_id");
        }
    }

    public boolean setActive(int voucherId, boolean active) throws SQLException {
        String sql = "UPDATE vouchers SET active = ? WHERE voucher_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setInt(2, voucherId);
            return ps.executeUpdate() > 0;
        }
    }

    // ===================== Voucher rules =====================
    // These are static so the Controller can also use them to preview the discount in the cart.

    /** Returns an error message if the voucher cannot be used, or null if it is OK. */
    public static String checkVoucher(Voucher v, long orderValue) {
        LocalDateTime now = LocalDateTime.now();
        if (!v.active()) {
            return "This voucher is no longer available";
        }
        if (now.isBefore(v.startDate()) || now.isAfter(v.endDate())) {
            return "This voucher has expired or is not valid yet";
        }
        if (v.usageLimit() != null && v.usedCount() >= v.usageLimit()) {
            return "This voucher has been fully used";
        }
        if (orderValue < v.minOrderValue()) {
            return "The order must be at least " + v.minOrderValue() + " VND to use this voucher";
        }
        return null;
    }

    /** How many VND the voucher takes off an order of the given value. */
    public static long calculateDiscount(Voucher v, long orderValue) {
        long discount;
        if (v.discountType().equals("PERCENT")) {
            discount = orderValue * v.discountValue() / 100;
        } else {
            discount = v.discountValue();
        }
        if (v.maxDiscount() != null && discount > v.maxDiscount()) {
            discount = v.maxDiscount();
        }
        return Math.min(discount, orderValue);
    }

    private List<Voucher> getVouchers(String sql) throws SQLException {
        List<Voucher> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(toVoucher(rs));
            }
        }
        return list;
    }

    /** Package-private so OrderDAO can reuse it. */
    static Voucher toVoucher(ResultSet rs) throws SQLException {
        return new Voucher(rs.getInt("voucher_id"), rs.getString("code"), rs.getString("description"),
                rs.getString("discount_type"), rs.getLong("discount_value"), rs.getLong("min_order_value"),
                rs.getObject("max_discount", Long.class),
                rs.getTimestamp("start_date").toLocalDateTime(), rs.getTimestamp("end_date").toLocalDateTime(),
                rs.getObject("usage_limit", Integer.class), rs.getInt("used_count"), rs.getBoolean("active"));
    }
}
