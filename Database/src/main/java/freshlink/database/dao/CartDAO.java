package freshlink.database.dao;

import freshlink.database.DBConnection;
import freshlink.database.model.CartItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** SQL for the cart_items table. */
public class CartDAO {

    /** The customer's cart with product names and current prices (sale price if any). */
    public List<CartItem> getCart(int userId) throws SQLException {
        String sql = "SELECT c.user_id, c.product_id, p.name, p.unit, COALESCE(p.sale_price, p.price) AS price, "
                + "c.quantity, p.stock FROM cart_items c JOIN products p ON p.product_id = c.product_id "
                + "WHERE c.user_id = ? ORDER BY p.name";
        List<CartItem> cart = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                cart.add(new CartItem(rs.getInt("user_id"), rs.getInt("product_id"), rs.getString("name"),
                        rs.getString("unit"), rs.getLong("price"), rs.getInt("quantity"), rs.getInt("stock")));
            }
        }
        return cart;
    }

    /** Adds a product. If it is already in the cart, the quantity is increased. */
    public void addToCart(int userId, int productId, int quantity) throws SQLException {
        String sql = "INSERT INTO cart_items (user_id, product_id, quantity) VALUES (?, ?, ?) "
                + "ON CONFLICT (user_id, product_id) DO UPDATE SET quantity = cart_items.quantity + EXCLUDED.quantity";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, productId);
            ps.setInt(3, quantity);
            ps.executeUpdate();
        }
    }

    /** Sets the quantity. A quantity of 0 removes the product. */
    public void updateQuantity(int userId, int productId, int quantity) throws SQLException {
        if (quantity <= 0) {
            removeFromCart(userId, productId);
            return;
        }
        String sql = "UPDATE cart_items SET quantity = ? WHERE user_id = ? AND product_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setInt(2, userId);
            ps.setInt(3, productId);
            ps.executeUpdate();
        }
    }

    public void removeFromCart(int userId, int productId) throws SQLException {
        String sql = "DELETE FROM cart_items WHERE user_id = ? AND product_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, productId);
            ps.executeUpdate();
        }
    }

    public void clearCart(int userId) throws SQLException {
        String sql = "DELETE FROM cart_items WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }
}
