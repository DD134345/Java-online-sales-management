# FreshLink – Database tier

Owner: Nguyễn Duy Đức Trọng. PostgreSQL + JDBC (no ORM). All SQL of the project lives here.

## Folder structure

```
Database/
├── pom.xml
└── src/
    ├── main/java/freshlink/database/
    │   ├── DBConnection.java      opens a connection (settings in db.properties)
    │   ├── DBSetup.java           creates the tables and sample data
    │   ├── model/                 one class per table (User, Product, Order, ...)
    │   └── dao/                   the SQL queries, one class per topic
    │       ├── UserDAO.java       users + addresses
    │       ├── ProductDAO.java    categories + products
    │       ├── CartDAO.java       shopping cart
    │       ├── VoucherDAO.java    discount codes
    │       ├── OrderDAO.java      checkout, orders, delivery tracking
    │       ├── ReviewDAO.java     product reviews
    │       └── RecipeDAO.java     recipes + ingredients
    ├── main/resources/
    │   ├── schema.sql             CREATE TABLE statements
    │   ├── seed.sql               sample data (Vietnamese)
    │   └── db.properties.example
    └── test/java/freshlink/database/DatabaseTest.java
```

## How to run

1. Create two databases in PostgreSQL: `freshlink` and `freshlink_test`.
2. Copy `db.properties.example` to `db.properties` (same folder) and enter your PostgreSQL password.
3. Create the tables and sample data:
   ```
   cd Database
   mvn compile exec:java
   ```
4. Run the tests (uses `freshlink_test`): `mvn test`

Demo accounts: `admin` / `Admin@123` (admin), `lan.nguyen` / `Fresh@123` (customer).

## Tables

| Table | Content |
|---|---|
| users | customers and admins (`role` = CUSTOMER / ADMIN) |
| addresses | saved delivery addresses, one is the default |
| categories | main categories and sub-categories (`parent_id`) |
| products | name, unit (e.g. "Khay 300g"), price, sale price, stock |
| cart_items | products in each customer's cart |
| vouchers | discount codes: percent or fixed amount, min. order, end date, usage limit |
| orders | receiver, address, delivery date + time slot, totals, payment, status |
| order_items | products in each order (name and price saved at checkout) |
| order_tracking | delivery timeline, one row per status change |
| reviews | 1–5 stars, only for products the customer received |
| recipes, recipe_ingredients | recipes, ingredients linked to products |

**Order status:** `PENDING → CONFIRMED → PACKING → SHIPPING → DELIVERED`, or `CANCELLED` before shipping.
**Payment:** `COD`, `BANK_CARD`, `MOMO`, `ZALOPAY`.

## Notes for the Controller tier

- All DAO methods throw `SQLException`. `OrderDAO.placeOrder` also throws `IllegalStateException`
  (out of stock, empty cart) or `IllegalArgumentException` (bad voucher) with a message you can show to the user.
- `placeOrder` uses the customer's cart, reads prices from the database, applies the voucher and saves
  everything in one transaction. If something fails, nothing is saved.
- The Controller decides the shipping fee (sample data: free from 300.000đ, otherwise 15.000đ).
- Passwords: the Controller hashes them with `Controller/src/main/java/freshlink/controller/PasswordUtil.java`
  (same settings as the sample data).

```java
CartDAO cartDAO = new CartDAO();
cartDAO.addToCart(userId, productId, 2);

Order info = new Order(0, userId, "Nguyễn Thị Lan", "0912345678", address.fullAddress(),
    LocalDate.now().plusDays(1), "08:00-10:00", null, 0, 0, 15000, 0,
    "COD", null, null, "Gọi trước khi giao", null);
Order order = new OrderDAO().placeOrder(info, "FRESH50K");
```
