# Database Tier Changelog

## 2026-10-20
1. Added `ConnectionProvider` functional interface (`Connection getConnection() throws SQLException`). Every `PgXxxDao` now has two public constructors: no-arg (production DB via `DatabaseConnection.production()`) and `(ConnectionProvider)` (tests use `DatabaseConnection.test()`). `DatabaseConnection` also has `hasTestDatabase()`, `production()`, `test()`.
2. Created `OrderStatus` final class with String constants (PENDING/PAID/SHIPPED/CANCELLED) and `canTransition(from, to)` to avoid Controller tier enum dependency.
3. Introduced package-private final class `PgOrderTransactions` holding the two multi-table transactions (`createOrderWithTransaction`, `updateStatusWithStockRollback`) and shared `nextOrderCode(Connection, LocalDate)`; `PgOrderDao` delegates to it so both classes stay under 300 lines.
4. Defined low-stock `reorderStatus` values as "OUT OF STOCK" and "LOW STOCK" (with spaces) per requirements F7.3.
5. Extended order search keyword to match customer's phone; report/search date ranges are inclusive (implemented as end + 1 day, exclusive bound).
6. Added `DbSetup.run(ConnectionProvider)` and `main` (`--test` targets the test DB); `mvn -q -f Database/pom.xml exec:java` sets up the DB.
7. Implemented tests: `TierBoundaryTest`, `DbSetupTest`, `PgDaoIntegrationTest` (skipped automatically when db.test.url is not set).