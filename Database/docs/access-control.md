# Access Control & Encapsulation Defense: Database Tier (Report Section VI)

**Author:** Nguyen Duy Duc Trong (Member A — Database Tier Lead)
**System:** Online Sales Management Desktop Application
**Package:** `vgu.sales.database`
**Evaluation Standard:** VGU 61CSE215 OOP in Java

---

## 1. Overview of Tier Access Boundaries

The Database tier encapsulates all direct relational database interactions, SQL text execution, and JDBC connection handling. In accordance with the project's strict 3-tier boundary rules:
1. **Public APIs:** Interfaces (`Dao<R, ID>`, `StaffDao`, `CategoryDao`, `ProductDao`, `CustomerDao`, `OrderDao`, `OrderItemDao`, `PaymentDao`, `ReportDao`), immutable entity records, and domain exceptions are declared `public` so the Controller tier can inject interfaces and read data records without direct coupling to SQL or PostgreSQL.
2. **Implementation Classes:** JDBC implementations (`PgXxxDao`) are declared `public` so `ControllerFactory` can instantiate them via dependency injection.
3. **Internal Helpers & Mappers:** All `ResultSet` extraction methods (`mapRow`), statement builders, and internal fields are declared `private` to ensure database cursors and raw SQL columns never leak outside their enclosing DAO class.
4. **Utility Classes:** `DatabaseConnection` enforces a `private` constructor to prevent instantiation, exposing only static factory methods.

---

## 2. Access Control Table: Data Members (Fields & Constants)

| No. | Member / Field | Enclosing Class / Record | Modifier | Encapsulation & Security Justification |
|---|---|---|---|---|
| 1 | `PROPERTIES_FILE` | `DatabaseConnection` | `private static final String` | Constant classpath resource name; internal to connection loader. |
| 2 | `PROPERTIES` | `DatabaseConnection` | `private static final Properties` | Loaded once at init; prevents tampering with DB credentials. |
| 3 | `connections` | `PgXxxDao` (all) | `private final ConnectionProvider` | Injected connection source; encapsulated per DAO to support test/production swapping. |
| 4 | `COLUMNS`, `FIND_BY_ID`, `FIND_ALL`, `SEARCH`, `FIND_BY_PHONE`, `INSERT`, `UPDATE` | `PgCustomerDao` | `private static final String` | SQL text is an implementation detail; never exposed outside DAO. |
| 5 | `COLUMNS`, `FIND_BY_ID`, `FIND_ALL`, `INSERT`, `UPDATE`, `UPDATE_STOCK`, `SEARCH`, `FIND_LOW_STOCK` | `PgProductDao` | `private static final String` | SQL text is an implementation detail; never exposed outside DAO. |
| 6 | `ORDER_COLUMNS`, `FIND_BY_ID`, `FIND_ALL`, `FIND_BY_CUSTOMER_ID`, `UPDATE`, `FIND_ALL_SUMMARY`, `SEARCH` | `PgOrderDao` | `private static final String` | SQL text is an implementation detail; never exposed outside DAO. |
| 7 | `FIND_BY_ID`, `FIND_ALL`, `INSERT`, `UPDATE`, `DELETE`, `FIND_BY_USERNAME` | `PgStaffDao` | `private static final String` | SQL text is an implementation detail; never exposed outside DAO. |
| 8 | `FIND_BY_ID`, `FIND_ALL`, `FIND_ACTIVE`, `INSERT`, `UPDATE`, `DELETE` | `PgCategoryDao` | `private static final String` | SQL text is an implementation detail; never exposed outside DAO. |
| 9 | `COLUMNS`, `FIND_BY_ID`, `FIND_ALL`, `INSERT`, `UPDATE`, `DELETE`, `FIND_BY_ORDER_ID` | `PgOrderItemDao` | `private static final String` | SQL text is an implementation detail; never exposed outside DAO. |
| 10 | `FIND_BY_ID`, `FIND_ALL`, `FIND_BY_ORDER_ID`, `INSERT`, `UPDATE`, `DELETE` | `PgPaymentDao` | `private static final String` | SQL text is an implementation detail; never exposed outside DAO. |
| 11 | `SELECT_DAILY_REVENUE`, `SELECT_TOP_PRODUCTS` | `PgReportDao` | `private static final String` | Reporting SQL text; internal to implementation. |
| 12 | `SELECT_PRODUCT_FOR_UPDATE`, `UPDATE_PRODUCT_STOCK`, `INSERT_ORDER_ITEM`, `INSERT_PAYMENT`, `SELECT_ORDER_STATUS_FOR_UPDATE`, `UPDATE_ORDER_STATUS_AND_CANCEL_REASON` | `PgOrderTransactions` | `private static final String` | Transaction SQL constants; internal to package-private helper class. |
| 13 | `PENDING`, `PAID`, `SHIPPED`, `CANCELLED` | `OrderStatus` | `public static final String` | Status string constants exposed for cross-tier validation without enum dependency. |
| 14 | `productId` | `InsufficientStockException` | `private final Integer` | Encapsulated identity of the product whose stock fell short. |
| 15 | `requested` | `InsufficientStockException` | `private final int` | Encapsulated demanded quantity that could not be satisfied. |
| 16 | `available` | `InsufficientStockException` | `private final int` | Encapsulated stock ceiling currently available in warehouse. |

---

## 3. Access Control Table: Methods & Constructors

| No. | Method / Constructor | Enclosing Class / Interface | Modifier | Encapsulation & Security Justification |
|---|---|---|---|---|
| 1 | `getConnection()` | `DatabaseConnection` | `public static` | Single production entry point; callers receive ready-to-use `Connection`. |
| 2 | `getTestConnection()` | `DatabaseConnection` | `public static` | Test-only entry point returning connection to isolated test schema. |
| 3 | `hasTestDatabase()` | `DatabaseConnection` | `public static` | Boolean probe so integration tests can skip gracefully when test DB is absent. |
| 4 | `production()`, `test()` | `DatabaseConnection` | `public static` | `ConnectionProvider` factory methods for DI; no-arg DAO constructors delegate to `production()`. |
| 5 | `findById(ID)`, `findAll()`, `insert(R)`, `update(R)` | `Dao<R, ID>` | `public` | Generic CRUD contract implemented by every specialized DAO. |
| 6 | `findByUsername(String)` | `StaffDao` | `public` | Authentication lookup contract validating unique staff identity. |
| 7 | `findActive()` | `CategoryDao` | `public` | Category browse contract filtering inactive taxonomy nodes. |
| 8 | `search(String, Integer, BigDecimal, BigDecimal, Boolean)` | `ProductDao` | `public` | Product catalog query contract with multi-criteria filter support. |
| 9 | `updateStock(Integer, Integer)` | `ProductDao` | `public` | Inventory mutation contract to adjust stock quantities atomically. |
| 10 | `findLowStock(int)` | `ProductDao` | `public` | Inventory warning contract retrieving items at or below reorder threshold. |
| 11 | `search(String)` | `CustomerDao` | `public` | Customer query contract matching name substring or telephone prefix. |
| 12 | `findByPhone(String)` | `CustomerDao` | `public` | Uniqueness validation contract ensuring one customer profile per phone. |
| 13 | `findByCustomerId(Integer)` | `OrderDao` | `public` | Customer purchase history query contract. |
| 14 | `findAll(int, int)` | `OrderDao` | `public` | Paginated order overview contract for UI desktop table rendering. |
| 15 | `search(String, LocalDate, LocalDate, String)` | `OrderDao` | `public` | Comprehensive order ledger filter contract for manager review. |
| 16 | `createOrderWithTransaction(OrderRecord, List<OrderItemRecord>, PaymentRecord)` | `OrderDao` | `public` | Critical atomic transaction contract executing multi-table checkout workflow. |
| 17 | `updateStatusWithStockRollback(Integer, String, String)` | `OrderDao` | `public` | Lifecycle state contract restoring product inventory if an order is cancelled. |
| 18 | `nextOrderCode(LocalDate)` | `OrderDao` | `public` | Daily sequence generator contract for `ORD-YYYYMMDD-NNN` identifiers. |
| 19 | `findByOrderId(Integer)` | `OrderItemDao` | `public` | Retrieval contract fetching all purchased line items for an order. |
| 20 | `findByOrderId(Integer)` | `PaymentDao` | `public` | Retrieval contract fetching the single monetary payment linked to an order. |
| 21 | `getDailyRevenue(LocalDate, LocalDate)` | `ReportDao` | `public` | Managerial reporting contract aggregating revenue per calendar date. |
| 22 | `getTopSellingProducts(LocalDate, LocalDate, int)` | `ReportDao` | `public` | Managerial reporting contract ranking products by sold quantity. |
| 23 | `findById(Integer)`, `findAll()`, `insert(...)`, `update(...)`, `delete(Integer)`, `findByUsername(String)` | `PgStaffDao` | `public` | JDBC implementations of `StaffDao`; called via interface by Controller. |
| 24 | `findById(Integer)`, `findAll()`, `insert(...)`, `update(...)`, `delete(Integer)`, `findActive()` | `PgCategoryDao` | `public` | JDBC implementations of `CategoryDao`; called via interface by Controller. |
| 25 | `findById(Integer)`, `findAll()`, `insert(...)`, `update(...)`, `delete(Integer)`, `search(...)`, `updateStock(Integer, Integer)`, `findLowStock(int)` | `PgProductDao` | `public` | JDBC implementations of `ProductDao`; called via interface by Controller. |
| 26 | `findById(Integer)`, `findAll()`, `insert(...)`, `update(...)`, `delete(Integer)`, `search(String)`, `findByPhone(String)` | `PgCustomerDao` | `public` | JDBC implementations of `CustomerDao`; called via interface by Controller. |
| 27 | `findById(Integer)`, `findAll()`, `insert(...)`, `update(...)`, `delete(Integer)`, `findByCustomerId(Integer)`, `findAll(int, int)`, `search(...)`, `createOrderWithTransaction(...)`, `updateStatusWithStockRollback(Integer, String, String)`, `nextOrderCode(LocalDate)` | `PgOrderDao` | `public` | JDBC implementations of `OrderDao`; delegates transactions to `PgOrderTransactions`. |
| 28 | `findById(Integer)`, `findAll()`, `insert(...)`, `update(...)`, `delete(Integer)`, `findByOrderId(Integer)` | `PgOrderItemDao` | `public` | JDBC implementations of `OrderItemDao`; called via interface by Controller. |
| 29 | `findById(Integer)`, `findAll()`, `insert(...)`, `update(...)`, `delete(Integer)`, `findByOrderId(Integer)` | `PgPaymentDao` | `public` | JDBC implementations of `PaymentDao`; called via interface by Controller. |
| 30 | `getDailyRevenue(LocalDate, LocalDate)`, `getTopSellingProducts(LocalDate, LocalDate, int)` | `PgReportDao` | `public` | JDBC implementations of `ReportDao`; called via interface by Controller. |
| 31 | `mapRow(ResultSet)` (all DAOs) | `PgXxxDao` | `private` | Row-to-record mapping hidden inside each DAO; prevents `ResultSet` leakage. |
| 32 | `DataAccessException(String)`, `DataAccessException(String, Throwable)` | `DataAccessException` | `public` | Wraps checked `SQLException` as unchecked for cleaner DAO signatures. |
| 33 | `InsufficientStockException(Integer, int, int)` | `InsufficientStockException` | `public` | Captures product identity, requested qty, available qty for precise error reporting. |
| 34 | `getProductId()`, `getRequested()`, `getAvailable()` | `InsufficientStockException` | `public` | Accessors revealing failure context without exposing internal state mutation. |
| 35 | `getConnection()` | `ConnectionProvider` | `public` | Functional interface method allowing DAO and test code to obtain database connections. |
| 36 | `canTransition(String, String)` | `OrderStatus` | `public static` | Validates order state machine transitions without Controller enum dependency. |
| 37 | `createOrderWithTransaction(...)`, `updateStatusWithStockRollback(...)`, `nextOrderCode(Connection, LocalDate)` | `PgOrderTransactions` | `package-private` | Transactional helpers used ONLY by `PgOrderDao`; hidden from other DAOs and tiers. |
| 38 | `PgXxxDao()` (no-arg) | `PgXxxDao` (all) | `public` | Production constructor using `DatabaseConnection.production()`; allows `ControllerFactory` DI. |
| 39 | `PgXxxDao(ConnectionProvider)` | `PgXxxDao` (all) | `public` | Test constructor accepting injected `ConnectionProvider`; enables `DatabaseConnection.test()` injection. |

(End of file - total 125 lines)