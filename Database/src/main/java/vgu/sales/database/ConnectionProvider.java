package vgu.sales.database;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Functional interface supplying database connections.
 * Lets every PostgreSQL DAO receive its connection source,
 * so tests can point DAOs at the test database.
 * Public to allow injection across the Database package and tests.
 */
@FunctionalInterface
public interface ConnectionProvider {

    /**
     * Obtains a JDBC Connection.
     * Public so callers and DAO implementations can acquire connections.
     *
     * @return an open {@link Connection}
     * @throws SQLException if a database access error occurs
     */
    Connection getConnection() throws SQLException;
}
