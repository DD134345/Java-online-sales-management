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

/** SQL for orders, order_items and order_tracking, including checkout. */
public class OrderDAO {

    // ===================== Checkout =====================

    /**
     * Turns the customer's cart into an order.
     * Fill in userId, receiver, address, delivery date/slot, payment method, shipping fee and note
     * in orderInfo; the other fields (ids, prices, totals, status) are ignored and calculated here.
     *
     * Everything runs in ONE transaction: if a product is out of stock or the voucher is
     * not valid, nothing is saved and an exception with the reason is thrown.
     *
     * @param voucherCode null or "" for no voucher
     * @return the saved order (with its new orderId and totals)
     */
    public Order placeOrder(Order orderInfo, String voucherCode) throws SQLException {
        int userId = orderInfo.userId();
        Connection conn = DBConnection.getConnection();
        try {
            conn.setAutoCommit(false); // start transaction

            // 1. Read the cart and lock the product rows so nobody else can buy the same stock.
            String cartSql = "SELECT p.product_id, p.name, COALESCE(p.sale_price, p.price) AS price, p.stock, "
                    + "p.active, p.voucher_eligible, c.quantity FROM cart_items c "
                    + "JOIN products p ON p.product_id = c.product_id WHERE c.user_id = ? "
                    + "ORDER BY p.product_id FOR UPDATE OF p";
            List<OrderItem> items = new ArrayList<>();
            long subtotal = 0;
            long voucherValue = 0; // value of products that accept vouchers
            try (PreparedStatement ps = conn.prepareStatement(cartSql)) {
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String name = rs.getString("name");
                    int quantity = rs.getInt("quantity");
                    long price = rs.getLong("price");
                    if (!rs.getBoolean("active")) {
                        throw new IllegalStateException(name + " is no longer sold");
                    }
                    if (rs.getInt("stock") < quantity) {
                        throw new IllegalStateException("Not enough stock for " + name
                                + " (only " + rs.getInt("stock") + " left)");
                    }
                    items.add(new OrderItem(0, 0, rs.getInt("product_id"), name, price, quantity));
                    subtotal += price * quantity;
                    if (rs.getBoolean("voucher_eligible")) {
                        voucherValue += price * quantity;
                    }
                }
            }
            if (items.isEmpty()) {
                throw new IllegalStateException("The cart is empty");
            }

            // 2. Voucher
            Integer voucherId = null;
            long discount = 0;
            if (voucherCode != null && !voucherCode.isBlank()) {
                Voucher v;
                String voucherSql = "SELECT * FROM vouchers WHERE UPPER(code) = UPPER(?) FOR UPDATE";
                try (PreparedStatement ps = conn.prepareStatement(voucherSql)) {
                    ps.setString(1, voucherCode.trim());
                    ResultSet rs = ps.executeQuery();
                    if (!rs.next()) {
                        throw new IllegalArgumentException("Voucher " + voucherCode + " does not exist");
                    }
                    v = VoucherDAO.toVoucher(rs);
                }
                String error = VoucherDAO.checkVoucher(v, voucherValue);
                if (error != null) {
                    throw new IllegalArgumentException(error);
                }
                voucherId = v.voucherId();
                discount = VoucherDAO.calculateDiscount(v, voucherValue);
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE vouchers SET used_count = used_count + 1 WHERE voucher_id = ?")) {
                    ps.setInt(1, voucherId);
                    ps.executeUpdate();
                }
            }

            // 3. Save the order
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
                orderId = rs.getInt("order_id");
            }

            // 4. Save the order items and take the products out of stock
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

            // 5. First tracking step, then empty the cart
            addTracking(conn, orderId, "PENDING", "Đơn hàng đã được đặt");
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM cart_items WHERE user_id = ?")) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }

            conn.commit(); // everything OK: save
            return getOrderById(orderId);
        } catch (SQLException | RuntimeException e) {
            conn.rollback(); // something failed: undo everything
            throw e;
        } finally {
            conn.close();
        }
    }

    // ===================== Read orders =====================

    public Order getOrderById(int orderId) throws SQLException {
        List<Order> list = getOrders("SELECT * FROM orders WHERE order_id = ?", orderId);
        return list.isEmpty() ? null : list.get(0);
    }

    /** A customer's order history, newest first. */
    public List<Order> getOrdersByUser(int userId) throws SQLException {
        return getOrders("SELECT * FROM orders WHERE user_id = ? ORDER BY created_at DESC", userId);
    }

    /** Admin: all orders, newest first. */
    public List<Order> getAllOrders() throws SQLException {
        return getOrders("SELECT * FROM orders ORDER BY created_at DESC", null);
    }

    /** Admin: orders with one status, e.g. all "PENDING" orders. */
    public List<Order> getOrdersByStatus(String status) throws SQLException {
        return getOrders("SELECT * FROM orders WHERE status = ? ORDER BY created_at", status);
    }

    public List<OrderItem> getOrderItems(int orderId) throws SQLException {
        String sql = "SELECT * FROM order_items WHERE order_id = ? ORDER BY order_item_id";
        List<OrderItem> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new OrderItem(rs.getInt("order_item_id"), rs.getInt("order_id"), rs.getInt("product_id"),
                        rs.getString("product_name"), rs.getLong("unit_price"), rs.getInt("quantity")));
            }
        }
        return list;
    }

    /** Delivery timeline of an order, oldest step first. */
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

    // ===================== Change status =====================

    /**
     * Moves an order to the next status and adds a tracking step.
     * Allowed: PENDING -> CONFIRMED -> PACKING -> SHIPPING -> DELIVERED,
     * and CANCELLED from any status before SHIPPING.
     * Cancelling puts the products back in stock, gives the voucher use back, and refunds a paid order.
     * Delivering a COD (cash) order marks it as PAID.
     *
     * @return false if the order does not exist or the change is not allowed
     */
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

    /** paymentStatus: "UNPAID", "PAID" or "REFUNDED". */
    public boolean updatePaymentStatus(int orderId, String paymentStatus) throws SQLException {
        String sql = "UPDATE orders SET payment_status = ? WHERE order_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
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
            default:          return false; // DELIVERED and CANCELLED are final
        }
    }

    // ===================== Helpers =====================

    private void addTracking(Connection conn, int orderId, String status, String note) throws SQLException {
        String sql = "INSERT INTO order_tracking (order_id, status, note) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setString(2, status);
            ps.setString(3, note);
            ps.executeUpdate();
        }
    }

    /** Runs an order query with zero or one parameter. */
    private List<Order> getOrders(String sql, Object param) throws SQLException {
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
