package vgu.sales.database;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to initialize the PostgreSQL database schema and seed data.
 * Public so it can be executed from the command line or invoked by integration test setups.
 */
public final class DbSetup {

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private DbSetup() {
    }

    /**
     * Application entry point for database setup.
     * Public to allow command-line and maven exec invocation.
     *
     * @param args command-line arguments (pass "--test" to initialize test database)
     */
    public static void main(String[] args) {
        ConnectionProvider provider = (args.length > 0 && args[0].equals("--test"))
                ? DatabaseConnection.test()
                : DatabaseConnection.production();
        run(provider);
    }

    /**
     * Executes the schema and seed scripts using the given connection provider.
     * Public so test suites can reset the test database before test runs.
     *
     * @param provider the connection provider
     */
    public static void run(ConnectionProvider provider) {
        String schemaSql = readResource("/db/schema.sql");
        String seedSql = readResource("/db/seed.sql");

        List<String> statements = new ArrayList<>();
        statements.addAll(splitStatements(schemaSql));
        statements.addAll(splitStatements(seedSql));

        try (Connection conn = provider.getConnection()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                for (String sql : statements) {
                    stmt.execute(sql);
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw new DataAccessException("Failed to execute database setup script", e);
            }
            System.out.println("Database ready: " + statements.size() + " statements executed.");
        } catch (SQLException e) {
            throw new DataAccessException("Database setup connection error", e);
        }
    }

    /**
     * Splits a raw SQL script into executable SQL statements.
     * Package-private so unit tests can verify statement parsing logic without database.
     *
     * @param sql raw SQL script text
     * @return list of trimmed, non-empty SQL statements
     */
    static List<String> splitStatements(String sql) {
        if (sql == null || sql.isBlank()) {
            return List.of();
        }
        StringBuilder sb = new StringBuilder();
        sql.lines().forEach(line -> {
            if (!line.trim().startsWith("--")) {
                sb.append(line).append("\n");
            }
        });
        String cleaned = sb.toString();
        String[] parts = cleaned.split(";\\s*(?:\\r?\\n|$)");
        List<String> statements = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                statements.add(trimmed);
            }
        }
        return statements;
    }

    /**
     * Reads a classpath text resource into a String.
     * Private helper internal to DbSetup.
     *
     * @param resourcePath classpath path
     * @return content of the resource
     */
    private static String readResource(String resourcePath) {
        try (InputStream in = DbSetup.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new DataAccessException("Resource not found on classpath: " + resourcePath);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DataAccessException("Failed to read classpath resource: " + resourcePath, e);
        }
    }
}
