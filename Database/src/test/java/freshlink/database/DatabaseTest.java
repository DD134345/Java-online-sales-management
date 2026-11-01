package freshlink.database;

import freshlink.database.dao.CartDAO;
import freshlink.database.dao.OrderDAO;
import freshlink.database.dao.ProductDAO;
import freshlink.database.dao.RecipeDAO;
import freshlink.database.dao.ReviewDAO;
import freshlink.database.dao.UserDAO;
import freshlink.database.dao.VoucherDAO;
import freshlink.database.model.Address;
import freshlink.database.model.Order;
import freshlink.database.model.Product;
import freshlink.database.model.Review;
import freshlink.database.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests all DAOs on the test database (freshlink_test).
 * The database is reset with schema.sql + seed.sql before every test.
 */
class DatabaseTest {

    private final UserDAO userDAO = new UserDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final CartDAO cartDAO = new CartDAO();
    private final VoucherDAO voucherDAO = new VoucherDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final ReviewDAO reviewDAO = new ReviewDAO();
    private final RecipeDAO recipeDAO = new RecipeDAO();

    @BeforeEach
    void resetDatabase() throws Exception {
        DBConnection.useTestDatabase();
        DBSetup.run();
    }

    /** Checkout info for Tran Van Minh (user 3). */
    private Order checkoutInfo() {
        return new Order(0, 3, "Trần Văn Minh", "0987654321", "88 Phan Xích Long, Phú Nhuận, TP. Hồ Chí Minh",
                LocalDate.now().plusDays(1), "18:00-20:00", null, 0, 0, 0, 0,
                "BANK_CARD", null, null, null, null);
    }

    @Test
    void findUserForLogin() throws Exception {
        assertEquals("ADMIN", userDAO.getUserByUsername("Admin").role());
        assertNull(userDAO.getUserByUsername("nobody"));
    }

    @Test
    void registerUserAndAddresses() throws Exception {
        int userId = userDAO.addUser(new User(0, "an.pham", "hash", "salt", "Phạm Văn An",
                null, "0901112223", null, true, null));
        assertEquals("CUSTOMER", userDAO.getUserById(userId).role());

        int first = userDAO.addAddress(new Address(0, userId, "Phạm Văn An", "0901112223",
                "1 Lê Lợi", "Phường Bến Nghé", "Quận 1", "TP. Hồ Chí Minh", false));
        int second = userDAO.addAddress(new Address(0, userId, "Phạm Văn An", "0901112223",
                "2 Lê Lợi", "Phường Bến Nghé", "Quận 1", "TP. Hồ Chí Minh", false));
        assertTrue(userDAO.getAddresses(userId).get(0).isDefault());   // first address is the default
        assertEquals(first, userDAO.getAddresses(userId).get(0).addressId());

        userDAO.setDefaultAddress(userId, second);
        assertEquals(second, userDAO.getAddresses(userId).get(0).addressId());
        assertEquals(1, userDAO.getAddresses(userId).stream().filter(a -> a.isDefault()).count());
    }

    @Test
    void productsAndCategories() throws Exception {
        assertEquals(6, productDAO.getMainCategories().size());
        assertEquals(9, productDAO.getProductsByCategory(1).size()); // includes sub-categories
        assertEquals(2, productDAO.searchProducts("cà").size());      // Cà chua, Cà rốt
        Product best = productDAO.getSaleProducts().get(0);
        assertEquals("Táo Envy New Zealand", best.name());
        assertEquals(25, best.getDiscountPercent());
        assertFalse(productDAO.changeStock(33, -1));                  // stock cannot go below 0
    }

    @Test
    void cartAddsUpQuantity() throws Exception {
        cartDAO.addToCart(4, 1, 1);
        cartDAO.addToCart(4, 1, 2);
        assertEquals(3, cartDAO.getCart(4).get(0).quantity());
        assertEquals(45_900, cartDAO.getCart(4).get(0).price());     // sale price
    }

    @Test
    void placeOrderWithVoucher() throws Exception {
        cartDAO.addToCart(3, 4, 2);   // Thăn bò 2 x 119.000
        cartDAO.addToCart(3, 20, 1);  // Táo Envy 119.000 (clearance: voucher does not apply)

        Order order = orderDAO.placeOrder(checkoutInfo(), "tuoingon10");

        assertEquals(357_000, order.subtotal());
        assertEquals(23_800, order.discount());                       // 10% of 238.000 only
        assertEquals(333_200, order.total());
        assertEquals("PENDING", order.status());
        assertEquals(2, orderDAO.getOrderItems(order.orderId()).size());
        assertEquals(28, productDAO.getProductById(4).stock());
        assertEquals(2, voucherDAO.getVoucherByCode("TUOINGON10").usedCount());
        assertTrue(cartDAO.getCart(3).isEmpty());
    }

    @Test
    void outOfStockCancelsWholeCheckout() throws Exception {
        cartDAO.addToCart(3, 21, 2);
        cartDAO.addToCart(3, 33, 1);  // out of stock
        assertThrows(IllegalStateException.class, () -> orderDAO.placeOrder(checkoutInfo(), null));
        assertEquals(45, productDAO.getProductById(21).stock());      // nothing was taken
        assertEquals(2, cartDAO.getCart(3).size());                   // cart is still there
    }

    @Test
    void invalidVouchersAreRejected() throws Exception {
        cartDAO.addToCart(3, 21, 3);
        assertThrows(IllegalArgumentException.class, () -> orderDAO.placeOrder(checkoutInfo(), "TET2026")); // expired
        assertThrows(IllegalArgumentException.class, () -> orderDAO.placeOrder(checkoutInfo(), "VIP100K")); // used up
        assertThrows(IllegalArgumentException.class, () -> orderDAO.placeOrder(checkoutInfo(), "NOPE"));    // unknown
    }

    @Test
    void cancelOrderGivesStockBack() throws Exception {
        cartDAO.addToCart(3, 21, 2);
        Order order = orderDAO.placeOrder(checkoutInfo(), "FRESH50K");
        orderDAO.updatePaymentStatus(order.orderId(), "PAID");

        assertTrue(orderDAO.updateStatus(order.orderId(), "CANCELLED", "Khách đổi ý"));

        assertEquals(45, productDAO.getProductById(21).stock());
        assertEquals(1, voucherDAO.getVoucherByCode("FRESH50K").usedCount());
        assertEquals("REFUNDED", orderDAO.getOrderById(order.orderId()).paymentStatus());
    }

    @Test
    void deliveryStepsThenReview() throws Exception {
        int orderId = 4; // Lê Thị Hoa, PENDING, cash on delivery, contains Trứng gà ta (product 8)
        assertFalse(orderDAO.updateStatus(orderId, "DELIVERED", null)); // cannot skip steps
        assertFalse(reviewDAO.addReview(new Review(0, 8, 4, null, 5, "Ngon", null)));

        orderDAO.updateStatus(orderId, "CONFIRMED", null);
        orderDAO.updateStatus(orderId, "PACKING", null);
        orderDAO.updateStatus(orderId, "SHIPPING", null);
        orderDAO.updateStatus(orderId, "DELIVERED", null);

        assertEquals("PAID", orderDAO.getOrderById(orderId).paymentStatus());
        assertEquals(5, orderDAO.getTracking(orderId).size());
        assertTrue(reviewDAO.addReview(new Review(0, 8, 4, null, 4, "Trứng tươi", null)));
        assertTrue(reviewDAO.addReview(new Review(0, 8, 4, null, 5, "Rất tươi", null))); // replaces old review
        assertEquals(1, reviewDAO.getReviews(8).size());
        assertEquals(5.0, reviewDAO.getAverageRating(8));
    }

    @Test
    void recipes() throws Exception {
        assertEquals(4, recipeDAO.getAllRecipes().size());
        assertEquals(3, recipeDAO.getRecipesByProduct(25).size()); // recipes using Nước mắm
        assertEquals(6, recipeDAO.getIngredients(1).size());
        List<String> titles = recipeDAO.searchRecipes("canh").stream().map(r -> r.title()).toList();
        assertEquals(List.of("Canh chua cá basa"), titles);
    }
}
