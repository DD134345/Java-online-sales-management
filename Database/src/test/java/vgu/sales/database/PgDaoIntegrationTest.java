package vgu.sales.database;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests for all PostgreSQL DAOs.
 * Runs only when db.test.url property is set (DatabaseConnection.hasTestDatabase() returns true).
 */
class PgDaoIntegrationTest {

    private static ConnectionProvider testDb;

    private PgStaffDao staffDao;
    private PgCategoryDao categoryDao;
    private PgProductDao productDao;
    private PgCustomerDao customerDao;
    private PgOrderDao orderDao;
    private PgOrderItemDao orderItemDao;
    private PgPaymentDao paymentDao;
    private PgReportDao reportDao;

    @BeforeAll
    static void requireDb() {
        assumeTrue(DatabaseConnection.hasTestDatabase(), "db.test.url not set - skipping DB tests");
        testDb = DatabaseConnection.test();
    }

    @BeforeEach
    void resetDb() {
        DbSetup.run(testDb);

        staffDao = new PgStaffDao(testDb);
        categoryDao = new PgCategoryDao(testDb);
        productDao = new PgProductDao(testDb);
        customerDao = new PgCustomerDao(testDb);
        orderDao = new PgOrderDao(testDb);
        orderItemDao = new PgOrderItemDao(testDb);
        paymentDao = new PgPaymentDao(testDb);
        reportDao = new PgReportDao(testDb);
    }

    @Test
    void staffFindByUsernameAdmin() {
        Optional<StaffRecord> admin = staffDao.findByUsername("ADMIN");
        assertTrue(admin.isPresent(), "ADMIN staff should exist");
        assertEquals("ADMIN", admin.get().role());
    }

    @Test
    void categoryFindActiveSortedByName() {
        List<CategoryRecord> active = categoryDao.findActive();
        assertFalse(active.isEmpty());
        for (CategoryRecord c : active) {
            assertTrue(c.active());
        }
        // Verify sorted by name
        for (int i = 0; i < active.size() - 1; i++) {
            assertTrue(active.get(i).name().compareToIgnoreCase(active.get(i + 1).name()) <= 0,
                    "Categories should be sorted by name");
        }
    }

    @Test
    void productSearchFilters() {
        // inStockOnly = true returns only stock > 0
        List<ProductRecord> inStock = productDao.search(null, null, null, null, true);
        assertFalse(inStock.isEmpty());
        for (ProductRecord p : inStock) {
            assertTrue(p.stockQuantity() > 0, "All products should have stock > 0");
        }

        // keyword with no match returns empty
        List<ProductRecord> noMatch = productDao.search("zzzz-no-match", null, null, null, null);
        assertTrue(noMatch.isEmpty());

        // minPrice and maxPrice filter
        List<ProductRecord> priceRange = productDao.search(null, null,
                new BigDecimal("1000000"), new BigDecimal("5000000"), null);
        for (ProductRecord p : priceRange) {
            assertTrue(p.price().compareTo(new BigDecimal("1000000")) >= 0);
            assertTrue(p.price().compareTo(new BigDecimal("5000000")) <= 0);
        }
    }

    @Test
    void productInsertUpdateRoundTrip() {
        // Insert a new product in category 1
        ProductRecord newProduct = new ProductRecord(
                null, 1, "Test Product", "Test Description",
                new BigDecimal("1234567.89"), 10, true, null);
        ProductRecord inserted = productDao.insert(newProduct);
        assertNotNull(inserted.productId());

        // Find by ID
        Optional<ProductRecord> found = productDao.findById(inserted.productId());
        assertTrue(found.isPresent());
        assertEquals("Test Product", found.get().name());
        assertEquals(0, new BigDecimal("1234567.89").compareTo(found.get().price()));
    }

    @Test
    void lowStockSortedByStockAscending() {
        List<LowStockRecord> lowStock = productDao.findLowStock(5);
        assertFalse(lowStock.isEmpty());

        // Sorted by stock ascending
        for (int i = 0; i < lowStock.size() - 1; i++) {
            assertTrue(lowStock.get(i).currentStock() <= lowStock.get(i + 1).currentStock(),
                    "Low stock should be sorted by stock ascending");
        }

        // All currentStock <= 5
        for (LowStockRecord l : lowStock) {
            assertTrue(l.currentStock() <= 5);
        }

        // Stock 0 has reorderStatus "OUT OF STOCK"
        for (LowStockRecord l : lowStock) {
            if (l.currentStock() == 0) {
                assertEquals("OUT OF STOCK", l.reorderStatus());
            }
        }
    }

    @Test
    void customerInsertFindByPhoneAndSearch() {
        // Insert a customer
        CustomerRecord newCustomer = new CustomerRecord(
                null, "Test Customer", "0999888777", "test@example.com",
                "Test Address", null);
        CustomerRecord inserted = customerDao.insert(newCustomer);
        assertNotNull(inserted.customerId());

        // Find by phone
        Optional<CustomerRecord> byPhone = customerDao.findByPhone("0999888777");
        assertTrue(byPhone.isPresent());
        assertEquals("Test Customer", byPhone.get().fullName());

        // Search by partial phone
        List<CustomerRecord> searchResults = customerDao.search("0999888");
        assertFalse(searchResults.isEmpty());
        assertTrue(searchResults.stream().anyMatch(c -> c.phone().equals("0999888777")));
    }

    @Test
    void orderTransactionSuccess() {
        // Find a product with stock >= 2 (product 1 has stock 15)
        ProductRecord product = productDao.findById(1).orElseThrow();
        int originalStock = product.stockQuantity();

        // Create order with 1 item, quantity 2
        OrderRecord order = new OrderRecord(
                null, null, 1, 2, LocalDateTime.now(), "PAID",
                product.price().multiply(new BigDecimal("2")),
                "Test Address", "Test order", null);

        List<OrderItemRecord> items = List.of(
                new OrderItemRecord(null, null, product.productId(), 2, product.price()));

        PaymentRecord payment = new PaymentRecord(
                null, null, "CASH", order.totalAmount(), LocalDateTime.now(),
                order.totalAmount(), BigDecimal.ZERO, null, null);

        OrderRecord created = orderDao.createOrderWithTransaction(order, items, payment);
        assertNotNull(created.orderId());
        assertTrue(created.orderCode().startsWith("ORD-"));

        // Stock went down by 2
        ProductRecord updatedProduct = productDao.findById(product.productId()).orElseThrow();
        assertEquals(originalStock - 2, updatedProduct.stockQuantity());

        // OrderItem exists
        List<OrderItemRecord> orderItems = orderItemDao.findByOrderId(created.orderId());
        assertEquals(1, orderItems.size());
        assertEquals(2, orderItems.get(0).quantity());

        // Payment exists
        Optional<PaymentRecord> pay = paymentDao.findByOrderId(created.orderId());
        assertTrue(pay.isPresent());
        assertEquals("CASH", pay.get().method());
    }

    @Test
    void orderTransactionRollbackOnInsufficientStock() {
        // Find a product (product 9 has stock 0)
        ProductRecord product = productDao.findById(9).orElseThrow();
        int originalStock = product.stockQuantity();

        // Order count before the failing call
        int orderCountBefore = orderDao.findAll().size();

        // Try to order stock + 1 (should throw InsufficientStockException)
        OrderRecord order = new OrderRecord(
                null, null, 1, 2, LocalDateTime.now(), "PAID",
                product.price().multiply(new BigDecimal(originalStock + 1)),
                "Test Address", "Test order", null);

        List<OrderItemRecord> items = List.of(
                new OrderItemRecord(null, null, product.productId(), originalStock + 1, product.price()));

        PaymentRecord payment = new PaymentRecord(
                null, null, "CASH", order.totalAmount(), LocalDateTime.now(),
                order.totalAmount(), BigDecimal.ZERO, null, null);

        assertThrows(InsufficientStockException.class,
                () -> orderDao.createOrderWithTransaction(order, items, payment));

        // Stock unchanged
        ProductRecord updated = productDao.findById(product.productId()).orElseThrow();
        assertEquals(originalStock, updated.stockQuantity());

        // Order count unchanged
        int orderCountAfter = orderDao.findAll().size();
        assertEquals(orderCountBefore, orderCountAfter);
    }

    @Test
    void cancelRestoresStock() {
        // Find a product with stock >= 2 (product 1 has stock 15)
        ProductRecord product = productDao.findById(1).orElseThrow();
        int originalStock = product.stockQuantity();

        // Create a PAID order
        OrderRecord order = new OrderRecord(
                null, null, 1, 2, LocalDateTime.now(), "PAID",
                product.price().multiply(new BigDecimal("2")),
                "Test Address", "Test order", null);

        List<OrderItemRecord> items = List.of(
                new OrderItemRecord(null, null, product.productId(), 2, product.price()));

        PaymentRecord payment = new PaymentRecord(
                null, null, "CASH", order.totalAmount(), LocalDateTime.now(),
                order.totalAmount(), BigDecimal.ZERO, null, null);

        OrderRecord created = orderDao.createOrderWithTransaction(order, items, payment);

        // Cancel the order (should restore stock)
        boolean cancelled = orderDao.updateStatusWithStockRollback(created.orderId(), "CANCELLED", "customer changed mind");
        assertTrue(cancelled);

        // Stock restored
        ProductRecord restored = productDao.findById(product.productId()).orElseThrow();
        assertEquals(originalStock, restored.stockQuantity());

        // Second cancel attempt on CANCELLED order returns false (terminal state)
        boolean secondCancel = orderDao.updateStatusWithStockRollback(created.orderId(), "PAID", "try again");
        assertFalse(secondCancel);
    }

@Test
    void invalidTransitionOnShippedOrder() {
        // Find a SHIPPED order from seed (order 1 or 2)
        Optional<OrderRecord> shipped = orderDao.findAll().stream()
                .filter(o -> "SHIPPED".equals(o.status()))
                .findFirst();

        assertTrue(shipped.isPresent());

        // Try to cancel a SHIPPED order - should return false
        boolean result = orderDao.updateStatusWithStockRollback(shipped.get().orderId(), "CANCELLED", "x");
        assertFalse(result);
    }

    @Test
    void rollbackWhenPaymentInsertFailsLate() {
        // Product 1 has stock >= 2 (seed has stock 15)
        ProductRecord product = productDao.findById(1).orElseThrow();
        int originalStock = product.stockQuantity();
        int orderCountBefore = orderDao.findAll().size();
        int orderItemCountBefore = new PgOrderItemDao(testDb).findAll().size();

        // Build a valid order (status "PAID", total = 2 * price) with ONE item (product 1, quantity 2)
        BigDecimal total = product.price().multiply(new BigDecimal("2"));
        OrderRecord order = new OrderRecord(
                null, null, 1, 2, LocalDateTime.now(), "PAID",
                total, "Test Address", "Test order", null);

        List<OrderItemRecord> items = List.of(
                new OrderItemRecord(null, null, product.productId(), 2, product.price()));

        // INVALID cash payment: cashReceived = total - 1 (violates CHECK cash_received >= amount)
        PaymentRecord payment = new PaymentRecord(
                null, null, "CASH", total, LocalDateTime.now(),
                total.subtract(BigDecimal.ONE), BigDecimal.ZERO, null, null);

        // Payment insert (step 7) should fail after order, items, stock update already ran
        assertThrows(DataAccessException.class,
                () -> orderDao.createOrderWithTransaction(order, items, payment));

        // Product stock unchanged
        ProductRecord updated = productDao.findById(product.productId()).orElseThrow();
        assertEquals(originalStock, updated.stockQuantity());

        // Order count unchanged
        int orderCountAfter = orderDao.findAll().size();
        assertEquals(orderCountBefore, orderCountAfter);

        // Order item count unchanged
        int orderItemCountAfter = new PgOrderItemDao(testDb).findAll().size();
        assertEquals(orderItemCountBefore, orderItemCountAfter);
    }

    @Test
    void ordersFindAllPaginatedSortedDesc() {
        List<OrderSummaryRecord> page = orderDao.findAll(3, 0);
        assertTrue(page.size() <= 3);

        // Sorted by orderDate descending
        for (int i = 0; i < page.size() - 1; i++) {
            assertTrue(page.get(i).orderDate().isAfter(page.get(i + 1).orderDate())
                    || page.get(i).orderDate().isEqual(page.get(i + 1).orderDate()));
        }
    }

    @Test
    void ordersSearchByStatus() {
        List<OrderSummaryRecord> paid = orderDao.search("PAID", null, null, null);
        for (OrderSummaryRecord o : paid) {
            assertEquals("PAID", o.status());
        }
    }

    @Test
    void nextOrderCodeFormat() {
        String code = orderDao.nextOrderCode(LocalDate.of(2020, 1, 1));
        assertEquals("ORD-20200101-001", code);
    }

    @Test
    void reportsDailyRevenue() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(30);

        List<DailyRevenueRecord> revenue = reportDao.getDailyRevenue(start, today);
        assertNotNull(revenue);

        // Manually verify by summing PAID/SHIPPED orders in date range
        List<OrderSummaryRecord> allOrders = orderDao.findAll(1000, 0);
        BigDecimal manualSum = allOrders.stream()
                .filter(o -> ("PAID".equals(o.status()) || "SHIPPED".equals(o.status())))
                .filter(o -> !o.orderDate().toLocalDate().isBefore(start) && !o.orderDate().toLocalDate().isAfter(today))
                .map(OrderSummaryRecord::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal reportSum = revenue.stream()
                .map(DailyRevenueRecord::totalRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Allow small rounding differences
        assertEquals(0, manualSum.compareTo(reportSum),
                "Daily revenue sum should match manual calculation");
    }

    @Test
    void reportsTopSellingProducts() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(30);

        List<TopProductRecord> top = reportDao.getTopSellingProducts(start, today, 3);
        assertTrue(top.size() <= 3);

        // Sorted by quantitySold descending
        for (int i = 0; i < top.size() - 1; i++) {
            assertTrue(top.get(i).quantitySold() >= top.get(i + 1).quantitySold(),
                    "Top products should be sorted by quantitySold descending");
        }
    }
}