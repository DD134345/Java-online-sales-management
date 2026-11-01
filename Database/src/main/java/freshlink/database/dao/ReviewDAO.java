package freshlink.database.dao;

import freshlink.database.DBConnection;
import freshlink.database.model.Review;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Queries for the reviews table
public class ReviewDAO {

    // newest first, with the name of the person who wrote it
    public List<Review> getReviews(int productId) throws SQLException {
        String sql = "SELECT r.*, u.full_name FROM reviews r JOIN users u ON u.user_id = r.user_id "
                + "WHERE r.product_id = ? ORDER BY r.created_at DESC";
        List<Review> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Review(rs.getInt("review_id"), rs.getInt("product_id"), rs.getInt("user_id"),
                        rs.getString("full_name"), rs.getInt("rating"), rs.getString("comment"),
                        rs.getTimestamp("created_at").toLocalDateTime()));
            }
        }
        return list;
    }

    // 0 if nobody reviewed it yet
    public double getAverageRating(int productId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(AVG(rating), 0) FROM reviews WHERE product_id = ?")) {
            ps.setInt(1, productId);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getDouble(1);
        }
    }

    // you can only review something you actually received (an order with status DELIVERED)
    public boolean canReview(int userId, int productId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM orders o JOIN order_items oi ON oi.order_id = o.order_id "
                + "WHERE o.user_id = ? AND oi.product_id = ? AND o.status = 'DELIVERED'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, productId);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    // Saves the review. If the customer already reviewed this product, the old one is replaced.
    // Returns false if they're not allowed to review it (see canReview).
    public boolean addReview(Review review) throws SQLException {
        if (!canReview(review.userId(), review.productId())) {
            return false;
        }
        String sql = "INSERT INTO reviews (product_id, user_id, rating, comment) VALUES (?, ?, ?, ?) "
                + "ON CONFLICT (product_id, user_id) DO UPDATE "
                + "SET rating = EXCLUDED.rating, comment = EXCLUDED.comment, created_at = NOW()";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, review.productId());
            ps.setInt(2, review.userId());
            ps.setInt(3, review.rating());
            ps.setString(4, review.comment());
            ps.executeUpdate();
            return true;
        }
    }

    public boolean deleteReview(int reviewId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM reviews WHERE review_id = ?")) {
            ps.setInt(1, reviewId);
            return ps.executeUpdate() > 0;
        }
    }
}
