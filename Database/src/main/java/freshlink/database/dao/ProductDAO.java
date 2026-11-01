package freshlink.database.dao;

import freshlink.database.DBConnection;
import freshlink.database.model.Category;
import freshlink.database.model.Product;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** SQL for the categories and products tables. */
public class ProductDAO {

    // ===================== Categories =====================

    /** Main categories (no parent), for the menu. */
    public List<Category> getMainCategories() throws SQLException {
        return getCategories("SELECT * FROM categories WHERE parent_id IS NULL ORDER BY category_id", null);
    }

    public List<Category> getSubCategories(int parentId) throws SQLException {
        return getCategories("SELECT * FROM categories WHERE parent_id = ? ORDER BY category_id", parentId);
    }

    public int addCategory(String name, Integer parentId) throws SQLException {
        String sql = "INSERT INTO categories (name, parent_id) VALUES (?, ?) RETURNING category_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setObject(2, parentId);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt("category_id");
        }
    }

    private List<Category> getCategories(String sql, Integer parentId) throws SQLException {
        List<Category> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (parentId != null) {
                ps.setInt(1, parentId);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Category(rs.getInt("category_id"),
                        rs.getObject("parent_id", Integer.class), rs.getString("name")));
            }
        }
        return list;
    }

    // ===================== Products =====================

    /** Admin: every product, including hidden ones. */
    public List<Product> getAllProducts() throws SQLException {
        return getProducts("SELECT * FROM products ORDER BY product_id");
    }

    public Product getProductById(int productId) throws SQLException {
        String sql = "SELECT * FROM products WHERE product_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? toProduct(rs) : null;
        }
    }

    /** Products of a category. For a main category this also includes its sub-categories. */
    public List<Product> getProductsByCategory(int categoryId) throws SQLException {
        String sql = "SELECT * FROM products WHERE active AND (category_id = ? "
                + "OR category_id IN (SELECT category_id FROM categories WHERE parent_id = ?)) ORDER BY name";
        List<Product> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            ps.setInt(2, categoryId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(toProduct(rs));
            }
        }
        return list;
    }

    /** Search by name, not case-sensitive. */
    public List<Product> searchProducts(String keyword) throws SQLException {
        String sql = "SELECT * FROM products WHERE active AND LOWER(name) LIKE LOWER(?) ORDER BY name";
        List<Product> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword.trim() + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(toProduct(rs));
            }
        }
        return list;
    }

    /** "Khuyen mai hot": products on sale, biggest discount first. */
    public List<Product> getSaleProducts() throws SQLException {
        return getProducts("SELECT * FROM products WHERE active AND sale_price IS NOT NULL "
                + "ORDER BY (price - sale_price) * 1.0 / price DESC");
    }

    /** Admin warning: products with stock at or below the limit. */
    public List<Product> getLowStockProducts(int limit) throws SQLException {
        String sql = "SELECT * FROM products WHERE active AND stock <= ? ORDER BY stock";
        List<Product> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(toProduct(rs));
            }
        }
        return list;
    }

    public int addProduct(Product p) throws SQLException {
        String sql = "INSERT INTO products (category_id, name, unit, price, sale_price, stock, origin, "
                + "description, image_url, voucher_eligible) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING product_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, p.categoryId());
            ps.setString(2, p.name());
            ps.setString(3, p.unit());
            ps.setLong(4, p.price());
            ps.setObject(5, p.salePrice());
            ps.setInt(6, p.stock());
            ps.setString(7, p.origin());
            ps.setString(8, p.description());
            ps.setString(9, p.imageUrl());
            ps.setBoolean(10, p.voucherEligible());
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt("product_id");
        }
    }

    public boolean updateProduct(Product p) throws SQLException {
        String sql = "UPDATE products SET category_id = ?, name = ?, unit = ?, price = ?, sale_price = ?, "
                + "stock = ?, origin = ?, description = ?, image_url = ?, voucher_eligible = ?, active = ? "
                + "WHERE product_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, p.categoryId());
            ps.setString(2, p.name());
            ps.setString(3, p.unit());
            ps.setLong(4, p.price());
            ps.setObject(5, p.salePrice());
            ps.setInt(6, p.stock());
            ps.setString(7, p.origin());
            ps.setString(8, p.description());
            ps.setString(9, p.imageUrl());
            ps.setBoolean(10, p.voucherEligible());
            ps.setBoolean(11, p.active());
            ps.setInt(12, p.productId());
            return ps.executeUpdate() > 0;
        }
    }

    /** Admin: add stock (positive number) or remove stock (negative). Stock cannot go below 0. */
    public boolean changeStock(int productId, int amount) throws SQLException {
        String sql = "UPDATE products SET stock = stock + ? WHERE product_id = ? AND stock + ? >= 0";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, amount);
            ps.setInt(2, productId);
            ps.setInt(3, amount);
            return ps.executeUpdate() > 0;
        }
    }

    private List<Product> getProducts(String sql) throws SQLException {
        List<Product> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(toProduct(rs));
            }
        }
        return list;
    }

    private Product toProduct(ResultSet rs) throws SQLException {
        return new Product(rs.getInt("product_id"), rs.getInt("category_id"), rs.getString("name"),
                rs.getString("unit"), rs.getLong("price"), rs.getObject("sale_price", Long.class),
                rs.getInt("stock"), rs.getString("origin"), rs.getString("description"),
                rs.getString("image_url"), rs.getBoolean("voucher_eligible"), rs.getBoolean("active"));
    }
}
