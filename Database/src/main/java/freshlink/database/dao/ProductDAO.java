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

// Queries for the categories and products tables
public class ProductDAO {

    // ---------- categories ----------

    // the main menu: categories without a parent
    public List<Category> getMainCategories() throws SQLException {
        String sql = "SELECT * FROM categories WHERE parent_id IS NULL ORDER BY category_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            return readCategories(ps.executeQuery());
        }
    }

    public List<Category> getSubCategories(int parentId) throws SQLException {
        String sql = "SELECT * FROM categories WHERE parent_id = ? ORDER BY category_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, parentId);
            return readCategories(ps.executeQuery());
        }
    }

    // parentId = null to make a main category
    public int addCategory(String name, Integer parentId) throws SQLException {
        String sql = "INSERT INTO categories (name, parent_id) VALUES (?, ?) RETURNING category_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setObject(2, parentId);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1);
        }
    }

    // ---------- products ----------

    // for the admin screen, hidden products included
    public List<Product> getAllProducts() throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM products ORDER BY product_id")) {
            return readProducts(ps.executeQuery());
        }
    }

    public Product getProductById(int productId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM products WHERE product_id = ?")) {
            ps.setInt(1, productId);
            List<Product> list = readProducts(ps.executeQuery());
            return list.isEmpty() ? null : list.get(0);
        }
    }

    // for a main category this also returns the products of its sub-categories
    public List<Product> getProductsByCategory(int categoryId) throws SQLException {
        String sql = "SELECT * FROM products WHERE active AND (category_id = ? "
                + "OR category_id IN (SELECT category_id FROM categories WHERE parent_id = ?)) ORDER BY name";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            ps.setInt(2, categoryId);
            return readProducts(ps.executeQuery());
        }
    }

    public List<Product> searchProducts(String keyword) throws SQLException {
        String sql = "SELECT * FROM products WHERE active AND LOWER(name) LIKE LOWER(?) ORDER BY name";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword.trim() + "%");
            return readProducts(ps.executeQuery());
        }
    }

    // "Khuyến mãi hot" - products on sale, biggest discount first
    public List<Product> getSaleProducts() throws SQLException {
        String sql = "SELECT * FROM products WHERE active AND sale_price IS NOT NULL "
                + "ORDER BY (price - sale_price) * 1.0 / price DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            return readProducts(ps.executeQuery());
        }
    }

    // for the admin's low stock warning
    public List<Product> getLowStockProducts(int limit) throws SQLException {
        String sql = "SELECT * FROM products WHERE active AND stock <= ? ORDER BY stock";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            return readProducts(ps.executeQuery());
        }
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
            return rs.getInt(1);
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

    // amount > 0 adds stock, amount < 0 removes it. Returns false if stock would go below 0.
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

    private List<Category> readCategories(ResultSet rs) throws SQLException {
        List<Category> list = new ArrayList<>();
        while (rs.next()) {
            list.add(new Category(rs.getInt("category_id"), rs.getObject("parent_id", Integer.class),
                    rs.getString("name")));
        }
        return list;
    }

    private List<Product> readProducts(ResultSet rs) throws SQLException {
        List<Product> list = new ArrayList<>();
        while (rs.next()) {
            list.add(new Product(rs.getInt("product_id"), rs.getInt("category_id"), rs.getString("name"),
                    rs.getString("unit"), rs.getLong("price"), rs.getObject("sale_price", Long.class),
                    rs.getInt("stock"), rs.getString("origin"), rs.getString("description"),
                    rs.getString("image_url"), rs.getBoolean("voucher_eligible"), rs.getBoolean("active")));
        }
        return list;
    }
}
