package freshlink.database.dao;

import freshlink.database.DBConnection;
import freshlink.database.model.Order;
import freshlink.database.model.OrderItem;
import freshlink.database.model.OrderTracking;
import freshlink.database.model.Voucher;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Queries for orders, order_items and order_tracking. This is also where checkout happens.
public class OrderDAO {

    // ---------- checkout ----------

    // Turns the customer's whole cart into an order.
    // In orderInfo only these fields are used: userId, receiverName, receiverPhone, deliveryAddress,
    // deliveryDate, deliverySlot, shippingFee, paymentMethod and note. Prices and totals are worked out here.
    // voucherCode can be null or "" if the customer has no voucher.
    //
    // Everything happens in one transaction. If something is wrong (empty cart, not enough stock,
    // bad voucher) we roll back, nothing is saved, and the exception message says why (in Vietnamese).
    public Order placeOrder(Order orderInfo, String voucherCode) throws SQLException {
        int userId = orderInfo.userId();
        Connection conn = DBConnection.getConnection();
        try {
            conn.setAutoCommit(false);

            // 1. read the cart. FOR UPDATE locks the products so two people can't buy the last item at the same time
            String cartSql = "SELECT p.product_id, p.name, COALESCE(p.sale_price, p.price) AS price, p.stock, "
                    + "p.active, p.voucher_eligible, c.quantity FROM cart_items c "
                    + "JOIN products p ON p.product_id = c.product_id WHERE c.user_id = ? "
                    + "ORDER BY p.product_id FOR UPDATE OF p";
            List<OrderItem> items = new ArrayList<>();
            long subtotal = 0;
            long voucherValue = 0;   // only products that allow vouchers count for the discount
            try (PreparedStatement ps = conn.prepareStatement(cartSql)) {
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String name = rs.getString("name");
                    int quantity = rs.getInt("quantity");
                    int stock = rs.getInt("stock");
                    long price = rs.getLong("price");
                    if (!rs.getBoolean("active")) {
                        throw new IllegalStateException(name + " đã ngừng bán");
                    }
                    if (stock < quantity) {
                        throw new IllegalStateException(name + " chỉ còn " + stock + ", không đủ số lượng");
                    }
                    items.add(new OrderItem(0, 0, rs.getInt("product_id"), name, price, quantity));
                    subtotal += price * quantity;
                    if (rs.getBoolean("voucher_eligible")) {
                        voucherValue += price * quantity;
                    }
                }
            }
            if (items.isEmpty()) {
                throw new IllegalStateException("Giỏ hàng đang trống");
            }

            // 2. voucher
            Integer voucherId = null;
            long discount = 0;
            if (voucherCode != null && !voucherCode.isBlank()) {
                Voucher v;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT * FROM vouchers WHERE UPPER(code) = UPPER(?) FOR UPDATE")) {
                    ps.setString(1, voucherCode.trim());
                    List<Voucher> found = VoucherDAO.readVouchers(ps.executeQuery());
                    if (found.isEmpty()) {
                        throw new IllegalArgumentException("Mã giảm giá " + voucherCode + " không tồn tại");
                    }
                    v = found.get(0);
                }
                String problem = VoucherDAO.checkVoucher(v, voucherValue);
                if (problem != null) {
                    throw new IllegalArgumentException(problem);
                }
                voucherId = v.voucherId();
                discount = VoucherDAO.calculateDiscount(v, voucherValue);
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE vouchers SET used_count = used_count + 1 WHERE voucher_id = ?")) {
                    ps.setInt(1, voucherId);
                    ps.executeUpdate();
                }
            }

            // 3. the order itself
            long total = subtotal - discount + orderInfo.shippingFee();
            int orderId;
            String orderSql = "INSERT INTO orders (user_id, receiver_name, receiver_phone, delivery_address, "
                    + "delivery_date, delivery_slot, voucher_id, subtotal, discount, shipping_fee, total, "
                    + "payment_method, note) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING order_id";
            try (PreparedStatement ps = conn.prepareStatement(orderSql)) {
                ps.setInt(1, userId);
                ps.setString(2, orderInfo.receiverName());
                ps.setString(3, orderInfo.receiverPhone());
                ps.setString(4, orderInfo.deliveryAddress());
                ps.setDate(5, Date.valueOf(orderInfo.deliveryDate()));
                ps.setString(6, orderInfo.deliverySlot());
                ps.setObject(7, voucherId);
                ps.setLong(8, subtotal);
                ps.setLong(9, discount);
                ps.setLong(10, orderInfo.shippingFee());
                ps.setLong(11, total);
                ps.setString(12, orderInfo.paymentMethod());
                ps.setString(13, orderInfo.note());
                ResultSet rs = ps.executeQuery();
                rs.next();
                orderId = rs.getInt(1);
            }

            // 4. order items, and take them out of stock
            for (OrderItem item : items) {
                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO order_items "
                        + "(order_id, product_id, product_name, unit_price, quantity) VALUES (?, ?, ?, ?, ?)")) {
                    ps.setInt(1, orderId);
                    ps.setInt(2, item.productId());
                    ps.setString(3, item.productName());
                    ps.setLong(4, item.unitPrice());
                    ps.setInt(5, item.quantity());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE products SET stock = stock - ? WHERE product_id = ?")) {
                    ps.setInt(1, item.quantity());
                    ps.setInt(2, item.productId());
                    ps.executeUpdate();
                }
            }

            // 5. first tracking step, then empty the cart
            addTracking(conn, orderId, "PENDING", "Đơn hàng đã được đặt");
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM cart_items WHERE user_id = ?")) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }

            conn.commit();
            return getOrderById(orderId);
        } catch (SQLException | RuntimeException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.close();
        }
    }

    // ---------- reading orders ----------

    public Order getOrderById(int orderId) throws SQLException {
        List<Order> list = findOrders("SELECT * FROM orders WHERE order_id = ?", orderId);
        return list.isEmpty() ? null : list.get(0);
    }

    // order history of one customer, newest first
    public List<Order> getOrdersByUser(int userId) throws SQLException {
        return findOrders("SELECT * FROM orders WHERE user_id = ? ORDER BY created_at DESC", userId);
    }

    // admin: every order, newest first
    public List<Order> getAllOrders() throws SQLException {
        return findOrders("SELECT * FROM orders ORDER BY created_at DESC", null);
    }

    // admin: e.g. getOrdersByStatus("PENDING") for the orders that still need confirming
    public List<Order> getOrdersByStatus(String status) throws SQLException {
        return findOrders("SELECT * FROM orders WHERE status = ? ORDER BY created_at", status);
    }

    public List<OrderItem> getOrderItems(int orderId) throws SQLException {
        List<OrderItem> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM order_items WHERE order_id = ? ORDER BY order_item_id")) {
            ps.setInt(1, orderId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new OrderItem(rs.getInt("order_item_id"), rs.getInt("order_id"), rs.getInt("product_id"),
                        rs.getString("product_name"), rs.getLong("unit_price"), rs.getInt("quantity")));
            }
        }
        return list;
    }

    // the delivery timeline, oldest step first
    public List<OrderTracking> getTracking(int orderId) throws SQLException {
        String sql = "SELECT * FROM order_tracking WHERE order_id = ? ORDER BY created_at, tracking_id";
        List<OrderTracking> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new OrderTracking(rs.getInt("tracking_id"), rs.getInt("order_id"), rs.getString("status"),
                        rs.getString("note"), rs.getTimestamp("created_at").toLocalDateTime()));
            }
        }
        return list;
    }

    // ---------- changing the status ----------

    // Moves the order to its next status and adds a tracking step.
    // Order of steps: PENDING -> CONFIRMED -> PACKING -> SHIPPING -> DELIVERED (or CANCELLED before SHIPPING).
    // Cancelling puts the products back in stock, gives back the voucher use and refunds a paid order.
    // When a COD order is delivered, the cash has been collected, so it becomes PAID.
    // Returns false if the order doesn't exist or the step isn't allowed.
    public boolean updateStatus(int orderId, String newStatus, String note) throws SQLException {
        Order order = getOrderById(orderId);
        if (order == null || !canChangeStatus(order.status(), newStatus)) {
            return false;
        }
        String paymentStatus = order.paymentStatus();

        Connection conn = DBConnection.getConnection();
        try {
            conn.setAutoCommit(false);
            if (newStatus.equals("CANCELLED")) {
                try (PreparedStatement ps = conn.prepareStatement("UPDATE products p SET stock = p.stock + oi.quantity "
                        + "FROM order_items oi WHERE oi.order_id = ? AND oi.product_id = p.product_id")) {
                    ps.setInt(1, orderId);
                    ps.executeUpdate();
                }
                if (order.voucherId() != null) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE vouchers SET used_count = used_count - 1 WHERE voucher_id = ?")) {
                        ps.setInt(1, order.voucherId());
                        ps.executeUpdate();
                    }
                }
                if (paymentStatus.equals("PAID")) {
                    paymentStatus = "REFUNDED";
                }
            } else if (newStatus.equals("DELIVERED") && order.paymentMethod().equals("COD")) {
                paymentStatus = "PAID";
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE orders SET status = ?, payment_status = ? WHERE order_id = ?")) {
                ps.setString(1, newStatus);
                ps.setString(2, paymentStatus);
                ps.setInt(3, orderId);
                ps.executeUpdate();
            }
            addTracking(conn, orderId, newStatus, note);
            conn.commit();
            return true;
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.close();
        }
    }

    // paymentStatus is "UNPAID", "PAID" or "REFUNDED"
    public boolean updatePaymentStatus(int orderId, String paymentStatus) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE orders SET payment_status = ? WHERE order_id = ?")) {
            ps.setString(1, paymentStatus);
            ps.setInt(2, orderId);
            return ps.executeUpdate() > 0;
        }
    }

    public static boolean canChangeStatus(String from, String to) {
        switch (from) {
            case "PENDING":   return to.equals("CONFIRMED") || to.equals("CANCELLED");
            case "CONFIRMED": return to.equals("PACKING") || to.equals("CANCELLED");
            case "PACKING":   return to.equals("SHIPPING") || to.equals("CANCELLED");
            case "SHIPPING":  return to.equals("DELIVERED");
            default:          return false; // DELIVERED and CANCELLED are the end
        }
    }

    // ---------- helpers ----------

    private void addTracking(Connection conn, int orderId, String status, String note) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO order_tracking (order_id, status, note) VALUES (?, ?, ?)")) {
            ps.setInt(1, orderId);
            ps.setString(2, status);
            ps.setString(3, note);
            ps.executeUpdate();
        }
    }

    // runs an orders query that has one parameter (or none, if param is null)
    private List<Order> findOrders(String sql, Object param) throws SQLException {
        List<Order> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (param != null) {
                ps.setObject(1, param);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Order(rs.getInt("order_id"), rs.getInt("user_id"), rs.getString("receiver_name"),
                        rs.getString("receiver_phone"), rs.getString("delivery_address"),
                        rs.getDate("delivery_date").toLocalDate(), rs.getString("delivery_slot"),
                        rs.getObject("voucher_id", Integer.class), rs.getLong("subtotal"), rs.getLong("discount"),
                        rs.getLong("shipping_fee"), rs.getLong("total"), rs.getString("payment_method"),
                        rs.getString("payment_status"), rs.getString("status"), rs.getString("note"),
                        rs.getTimestamp("created_at").toLocalDateTime()));
            }
        }
        return list;
    }
}
