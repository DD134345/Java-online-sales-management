package vgu.sales.database;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Factory for JDBC connections loaded from {@code db.properties} on the classpath.
 * Public to provide connection access across the Database package and tests.
 */
public final class DatabaseConnection {

    /** Classpath resource path for database properties. Private constant internal to loader. */
    private static final String PROPERTIES_FILE = "/db.properties";

    /** Configuration properties loaded once at class initialization. Private to prevent tampering. */
    private static final Properties PROPS = loadProperties();

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private DatabaseConnection() {
    }

    /**
     * Loads database configuration properties from the classpath.
     * Private static helper encapsulated inside the connection loader.
     *
     * @return loaded Properties instance
     */
    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream in = DatabaseConnection.class.getResourceAsStream(PROPERTIES_FILE)) {
            if (in == null) {
                throw new DataAccessException(
                    "db.properties not found on the classpath. Copy db.properties.example to db.properties and fill in your password."
                );
            }
            properties.load(in);
            return properties;
        } catch (IOException e) {
            throw new DataAccessException("Failed to load db.properties from classpath", e);
        }
    }

    /**
     * Obtains a live connection to the production database.
     * Public static method providing connections to DAOs.
     *
     * @return an open {@link Connection}
     * @throws SQLException if connection cannot be established
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
            PROPS.getProperty("db.url"),
            PROPS.getProperty("db.user"),
            PROPS.getProperty("db.password")
        );
    }

    /**
     * Obtains a live connection to the test database.
     * Public static method providing connections to test suites.
     *
     * @return an open test {@link Connection}
     * @throws SQLException if connection cannot be established
     */
    public static Connection getTestConnection() throws SQLException {
        return DriverManager.getConnection(
            PROPS.getProperty("db.test.url"),
            PROPS.getProperty("db.test.user"),
            PROPS.getProperty("db.test.password")
        );
    }

    /**
     * Checks if a test database URL is configured and non-blank.
     * Public static method for test runners to conditionally execute integration tests.
     *
     * @return true if db.test.url is set and not blank, false otherwise
     */
    public static boolean hasTestDatabase() {
        String testUrl = PROPS.getProperty("db.test.url");
        return testUrl != null && !testUrl.isBlank();
    }

    /**
     * Returns a ConnectionProvider pointing to the production database.
     * Public static factory method for default DAO instantiation.
     *
     * @return connection provider for production database
     */
    public static ConnectionProvider production() {
        return DatabaseConnection::getConnection;
    }

    /**
     * Returns a ConnectionProvider pointing to the test database.
     * Public static factory method for test harnesses.
     *
     * @return connection provider for test database
     */
    public static ConnectionProvider test() {
        return DatabaseConnection::getTestConnection;
    }
}
