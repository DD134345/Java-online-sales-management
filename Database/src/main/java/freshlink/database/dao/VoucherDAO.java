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
import java.util.Locale;

// Queries for the vouchers table + the rules for using a voucher
public class VoucherDAO {

    public List<Voucher> getAllVouchers() throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM vouchers ORDER BY end_date DESC")) {
            return readVouchers(ps.executeQuery());
        }
    }

    // vouchers that can be used right now
    public List<Voucher> getAvailableVouchers() throws SQLException {
        String sql = "SELECT * FROM vouchers WHERE active AND NOW() BETWEEN start_date AND end_date "
                + "AND (usage_limit IS NULL OR used_count < usage_limit) ORDER BY min_order_value";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            return readVouchers(ps.executeQuery());
        }
    }

    // returns null if the code doesn't exist ("fresh50k" works the same as "FRESH50K")
    public Voucher getVoucherByCode(String code) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM vouchers WHERE UPPER(code) = UPPER(?)")) {
            ps.setString(1, code.trim());
            List<Voucher> list = readVouchers(ps.executeQuery());
            return list.isEmpty() ? null : list.get(0);
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
            return rs.getInt(1);
        }
    }

    public boolean setActive(int voucherId, boolean active) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE vouchers SET active = ? WHERE voucher_id = ?")) {
            ps.setBoolean(1, active);
            ps.setInt(2, voucherId);
            return ps.executeUpdate() > 0;
        }
    }

    // ---------- voucher rules ----------
    // static, so the Controller can also use them to show the discount in the cart before checkout

    // returns null if the voucher is OK, otherwise the reason (in Vietnamese, ready to show to the customer)
    public static String checkVoucher(Voucher v, long orderValue) {
        LocalDateTime now = LocalDateTime.now();
        if (!v.active()) {
            return "Mã giảm giá này không còn sử dụng được";
        }
        if (now.isBefore(v.startDate()) || now.isAfter(v.endDate())) {
            return "Mã giảm giá đã hết hạn hoặc chưa đến ngày sử dụng";
        }
        if (v.usageLimit() != null && v.usedCount() >= v.usageLimit()) {
            return "Mã giảm giá đã hết lượt sử dụng";
        }
        if (orderValue < v.minOrderValue()) {
            return String.format(Locale.GERMANY, "Đơn hàng cần tối thiểu %,dđ để dùng mã này", v.minOrderValue());
        }
        return null;
    }

    // how much money the voucher takes off (never more than the order itself)
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

    // not private because OrderDAO uses it too
    static List<Voucher> readVouchers(ResultSet rs) throws SQLException {
        List<Voucher> list = new ArrayList<>();
        while (rs.next()) {
            list.add(new Voucher(rs.getInt("voucher_id"), rs.getString("code"), rs.getString("description"),
                    rs.getString("discount_type"), rs.getLong("discount_value"), rs.getLong("min_order_value"),
                    rs.getObject("max_discount", Long.class),
                    rs.getTimestamp("start_date").toLocalDateTime(), rs.getTimestamp("end_date").toLocalDateTime(),
                    rs.getObject("usage_limit", Integer.class), rs.getInt("used_count"), rs.getBoolean("active")));
        }
        return list;
    }
}
