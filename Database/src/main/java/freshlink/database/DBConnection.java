package freshlink.database;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

// Gives out connections to PostgreSQL. Login info comes from db.properties.
public class DBConnection {

    private static Properties props = new Properties();
    private static boolean testMode = false;

    static {
        InputStream in = DBConnection.class.getResourceAsStream("/db.properties");
        if (in == null) {
            throw new RuntimeException("db.properties is missing. Copy db.properties.example and add your password.");
        }
        try (in) {
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Could not read db.properties", e);
        }
    }

    // Tests call this so they use freshlink_test and never touch the real data.
    public static void useTestDatabase() {
        testMode = true;
    }

    public static Connection getConnection() throws SQLException {
        String key = testMode ? "db.test." : "db.";
        return DriverManager.getConnection(props.getProperty(key + "url"),
                props.getProperty(key + "user"), props.getProperty(key + "password"));
    }
}
