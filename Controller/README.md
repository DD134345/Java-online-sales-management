# FreshLink – Controller tier (guide for the Controller member)

The Controller is the middle tier: **UI/UX → Controller → Database**.
It holds the **business rules** (login, roles, validation, shipping fee, …) and is the only
tier the UI talks to. It calls the Database tier's DAO classes and never writes SQL itself.

## Rules from the lecturer

- No Spring Boot (or any framework) for the Controller. Plain Java classes only.
- No SQL and no `java.sql` code here, except catching `SQLException`. All queries are in `Database/`.
  If you need a new query, ask the Database member to add a DAO method.
- The UI must only call Controller classes, never DAOs.

## Setup (once)

1. Set up the database first: follow [`Database/README.md`](../Database/README.md) (create `freshlink`, copy `db.properties`, run `mvn compile exec:java`).
2. Install the Database tier into your local Maven repository, so the Controller can use it:
   ```
   mvn -f Database/pom.xml install
   ```
   Run this again every time `Database/` changes (after you pull).
3. Build and test the Controller:
   ```
   mvn -f Controller/pom.xml test
   ```
   `PasswordUtilTest` should pass. Open the project in IntelliJ: **File → Open → Controller/pom.xml**.

## What is already here

| File | What it does |
|---|---|
| `pom.xml` | Maven project, already depends on `freshlink-database` |
| `PasswordUtil.java` | `newSalt()`, `hash(password, salt)`, `check(password, hash, salt)`. Matches the demo accounts. Do not change its settings. |

## Suggested classes to write

Package `freshlink.controller`. One controller per area, like the DAOs. Names are suggestions; keep them simple.

| Class | Job | Uses DAO methods |
|---|---|---|
| `Session` | Remembers who is logged in (`static User currentUser`), `isAdmin()`, `logout()` | – |
| `AuthController` | `login(username, password)`, `register(...)`, `changePassword(...)` | `UserDAO.getUserByUsername`, `addUser`, `changePassword` + `PasswordUtil` |
| `AccountController` | profile and saved addresses | `UserDAO.updateProfile`, `getAddresses`, `addAddress`, `setDefaultAddress`, `deleteAddress` |
| `ProductController` | menu, product lists, search, hot deals, product page | `ProductDAO.getMainCategories`, `getSubCategories`, `getProductsByCategory`, `searchProducts`, `getSaleProducts`, `getProductById` |
| `CartController` | add / change / remove, cart total, shipping fee preview, voucher preview | `CartDAO.*`, `VoucherDAO.getVoucherByCode`, `VoucherDAO.checkVoucher`, `VoucherDAO.calculateDiscount` |
| `OrderController` | checkout, order history, tracking, cancel | `OrderDAO.placeOrder`, `getOrdersByUser`, `getOrderItems`, `getTracking`, `updateStatus` |
| `ReviewController` | show reviews and average stars, write a review | `ReviewDAO.getReviews`, `getAverageRating`, `canReview`, `addReview` |
| `RecipeController` | recipe list/detail, "add all ingredients to cart" | `RecipeDAO.*`, then `CartDAO.addToCart` for each ingredient with a `productId` |
| `AdminController` | admin-only: products, stock, categories, vouchers, order status, lock users | `ProductDAO.addProduct/updateProduct/changeStock/getLowStockProducts/addCategory`, `VoucherDAO.addVoucher/setActive`, `OrderDAO.getAllOrders/getOrdersByStatus/updateStatus`, `UserDAO.getAllUsers/setActive` |

Every DAO method's comment explains what it returns. Read `Database/src/main/java/freshlink/database/dao/`.

## Business rules the Controller must handle

The database stores data and keeps it consistent. These decisions are **yours**:

1. **Login:** user exists, `active` is true, `PasswordUtil.check(...)` is true. Show one message for wrong username *or* password.
2. **Register:** username not taken, phone is 10 digits starting with 0 (`^0\d{9}$`), password at least 8 characters, new users are always `"CUSTOMER"`.
3. **Roles:** every admin method must first check `Session.isAdmin()`.
4. **Shipping fee:** free when the subtotal is 300.000đ or more, otherwise 15.000đ (the sample data uses this rule).
5. **Delivery:** date must be today or later (max. 7 days ahead). Slot is one of `08:00-10:00`, `10:00-12:00`, `14:00-16:00`, `16:00-18:00`, `18:00-20:00`.
6. **Payment:** `COD`, `BANK_CARD`, `MOMO`, `ZALOPAY`. (Online payment is only simulated: you can call `updatePaymentStatus(orderId, "PAID")` after a fake "payment success".)
7. **Customers may cancel** only their own order and only while it is `PENDING` or `CONFIRMED`. Admins move orders through the other steps.
8. **Errors:** DAO methods throw `SQLException` (database problem). `placeOrder` also throws `IllegalStateException` (out of stock, empty cart) and `IllegalArgumentException` (bad voucher) with a message that can be shown to the user. Catch them and return a clear message to the UI, don't let the UI crash.

## Example code

```java
public class AuthController {
    private final UserDAO userDAO = new UserDAO();

    /** Returns null if OK, otherwise an error message for the UI. */
    public String login(String username, String password) {
        try {
            User user = userDAO.getUserByUsername(username);
            if (user == null || !PasswordUtil.check(password, user.passwordHash(), user.passwordSalt())) {
                return "Wrong username or password";
            }
            if (!user.active()) {
                return "This account is locked";
            }
            Session.currentUser = user;
            return null;
        } catch (SQLException e) {
            return "Cannot connect to the database";
        }
    }
}
```

```java
// Checkout: the cart is already filled with CartDAO.addToCart(...)
long subtotal = cartDAO.getCart(userId).stream().mapToLong(CartItem::getLineTotal).sum();
long shippingFee = subtotal >= 300_000 ? 0 : 15_000;
Order info = new Order(0, userId, address.receiverName(), address.phone(), address.fullAddress(),
        deliveryDate, "08:00-10:00", null, 0, 0, shippingFee, 0,
        "COD", null, null, note, null);
try {
    Order order = orderDAO.placeOrder(info, voucherCode);   // voucherCode may be null
    // show "Đặt hàng thành công", order.orderId(), order.total()
} catch (IllegalStateException | IllegalArgumentException e) {
    // show e.getMessage() to the user (out of stock, voucher problem ...)
}
```

## Task checklist

- [ ] `Session` + `AuthController` (login, logout, register)
- [ ] `ProductController` (menu, category page, search, hot deals, product detail)
- [ ] `CartController` (cart, totals, shipping fee, voucher preview)
- [ ] `OrderController` (checkout, history, tracking, cancel)
- [ ] `ReviewController`, `RecipeController`
- [ ] `AccountController` (profile, addresses)
- [ ] `AdminController`
- [ ] A JUnit test for each controller (use the test database: call `DBConnection.useTestDatabase()` and `DBSetup.run()` in `@BeforeEach`, like `Database/src/test/.../DatabaseTest.java`)
- [ ] Tell the UI member the method names as soon as each controller is ready

## Working together (Git)

- Never push straight to `main`. Make a branch: `git checkout -b controller/auth`
- Commit small steps, push the branch, open a Pull Request, ask a teammate to review, then merge.
- Only edit files in `Controller/`. For changes in `Database/` or `UI/UX/`, ask the owner.
