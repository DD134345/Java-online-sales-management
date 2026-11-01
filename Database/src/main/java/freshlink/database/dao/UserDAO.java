package freshlink.database.dao;

import freshlink.database.DBConnection;
import freshlink.database.model.Address;
import freshlink.database.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** SQL for the users and addresses tables. */
public class UserDAO {

    // ===================== Users =====================

    /** Used for login. Returns null if the username does not exist. */
    public User getUserByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE LOWER(username) = LOWER(?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? toUser(rs) : null;
        }
    }

    public User getUserById(int userId) throws SQLException {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? toUser(rs) : null;
        }
    }

    public List<User> getAllUsers() throws SQLException {
        String sql = "SELECT * FROM users ORDER BY user_id";
        List<User> users = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                users.add(toUser(rs));
            }
        }
        return users;
    }

    /** Registers a new account and returns its new user_id. */
    public int addUser(User user) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, password_salt, full_name, email, phone, role) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING user_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.username());
            ps.setString(2, user.passwordHash());
            ps.setString(3, user.passwordSalt());
            ps.setString(4, user.fullName());
            ps.setString(5, user.email());
            ps.setString(6, user.phone());
            ps.setString(7, user.role() == null ? "CUSTOMER" : user.role());
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt("user_id");
        }
    }

    /** Updates name, email and phone. */
    public boolean updateProfile(User user) throws SQLException {
        String sql = "UPDATE users SET full_name = ?, email = ?, phone = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.fullName());
            ps.setString(2, user.email());
            ps.setString(3, user.phone());
            ps.setInt(4, user.userId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean changePassword(int userId, String newHash, String newSalt) throws SQLException {
        String sql = "UPDATE users SET password_hash = ?, password_salt = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setString(2, newSalt);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        }
    }

    /** Admin: lock (false) or unlock (true) an account. */
    public boolean setActive(int userId, boolean active) throws SQLException {
        String sql = "UPDATE users SET active = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    // ===================== Addresses =====================

    /** All addresses of a customer, the default one first. */
    public List<Address> getAddresses(int userId) throws SQLException {
        String sql = "SELECT * FROM addresses WHERE user_id = ? ORDER BY is_default DESC, address_id";
        List<Address> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Address(rs.getInt("address_id"), rs.getInt("user_id"),
                        rs.getString("receiver_name"), rs.getString("phone"), rs.getString("street"),
                        rs.getString("ward"), rs.getString("district"), rs.getString("city"),
                        rs.getBoolean("is_default")));
            }
        }
        return list;
    }

    /** Adds an address. The first address of a customer becomes the default. Returns the new id. */
    public int addAddress(Address a) throws SQLException {
        boolean isDefault = a.isDefault() || getAddresses(a.userId()).isEmpty();
        if (isDefault) {
            clearDefaultAddress(a.userId());
        }
        String sql = "INSERT INTO addresses (user_id, receiver_name, phone, street, ward, district, city, is_default) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING address_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, a.userId());
            ps.setString(2, a.receiverName());
            ps.setString(3, a.phone());
            ps.setString(4, a.street());
            ps.setString(5, a.ward());
            ps.setString(6, a.district());
            ps.setString(7, a.city());
            ps.setBoolean(8, isDefault);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt("address_id");
        }
    }

    public boolean setDefaultAddress(int userId, int addressId) throws SQLException {
        clearDefaultAddress(userId);
        String sql = "UPDATE addresses SET is_default = TRUE WHERE address_id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, addressId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteAddress(int userId, int addressId) throws SQLException {
        String sql = "DELETE FROM addresses WHERE address_id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, addressId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    private void clearDefaultAddress(int userId) throws SQLException {
        String sql = "UPDATE addresses SET is_default = FALSE WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    private User toUser(ResultSet rs) throws SQLException {
        return new User(rs.getInt("user_id"), rs.getString("username"), rs.getString("password_hash"),
                rs.getString("password_salt"), rs.getString("full_name"), rs.getString("email"),
                rs.getString("phone"), rs.getString("role"), rs.getBoolean("active"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }
}
