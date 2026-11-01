package freshlink.database;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Opens connections to the PostgreSQL database.
 * The URL, user and password are read from db.properties (copy db.properties.example).
 */
public class DBConnection {

    private static final Properties config = new Properties();
    private static boolean useTestDatabase = false;

    static {
        try (InputStream in = DBConnection.class.getResourceAsStream("/db.properties")) {
            if (in == null) {
                throw new RuntimeException("db.properties not found. Copy db.properties.example and add your password.");
            }
            config.load(in);
        } catch (Exception e) {
            throw new RuntimeException("Cannot read db.properties", e);
        }
    }

    /** Call this in tests so all DAOs use the test database instead. */
    public static void useTestDatabase() {
        useTestDatabase = true;
    }

    public static Connection getConnection() throws SQLException {
        String prefix = useTestDatabase ? "db.test." : "db.";
        return DriverManager.getConnection(
                config.getProperty(prefix + "url"),
                config.getProperty(prefix + "user"),
                config.getProperty(prefix + "password"));
    }
}
