# FreshLink – UI/UX tier (guide for the UI/UX member)

The UI is the top tier: **UI/UX → Controller → Database**.
It shows the screens and reacts to clicks. It only calls **Controller** classes
(`AuthController`, `ProductController`, …). It never calls DAOs and never writes SQL.

## Rules from the lecturer

- Desktop: **JavaFX** or **Swing**. (Web is also allowed, but then you need a web server in Java; a desktop app is much simpler for this project.)
- Spring Boot is allowed for the UI only, not needed for a desktop app.
- No business logic in the UI: no price calculation, no password checking. Ask the Controller.

**Recommendation:** JavaFX with FXML + Scene Builder (drag-and-drop screen design). Swing is fine if the team already knows it.

## Setup (once)

1. Set up the database: [`Database/README.md`](../../Database/README.md).
2. Install the lower tiers into your local Maven repository (repeat after every pull that changes them):
   ```
   mvn -f Database/pom.xml install
   mvn -f Controller/pom.xml install
   ```
3. Create `UI/UX/pom.xml`. Starting point for **JavaFX**:
   ```xml
   <project xmlns="http://maven.apache.org/POM/4.0.0"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
       <modelVersion>4.0.0</modelVersion>
       <groupId>freshlink</groupId>
       <artifactId>freshlink-ui</artifactId>
       <version>1.0-SNAPSHOT</version>
       <properties>
           <maven.compiler.release>21</maven.compiler.release>
           <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
       </properties>
       <dependencies>
           <dependency>
               <groupId>freshlink</groupId>
               <artifactId>freshlink-controller</artifactId>
               <version>1.0-SNAPSHOT</version>
           </dependency>
           <dependency>
               <groupId>org.openjfx</groupId>
               <artifactId>javafx-controls</artifactId>
               <version>21.0.5</version>
           </dependency>
           <dependency>
               <groupId>org.openjfx</groupId>
               <artifactId>javafx-fxml</artifactId>
               <version>21.0.5</version>
           </dependency>
       </dependencies>
       <build>
           <plugins>
               <plugin>
                   <groupId>org.openjfx</groupId>
                   <artifactId>javafx-maven-plugin</artifactId>
                   <version>0.0.8</version>
                   <configuration>
                       <mainClass>freshlink.ui.App</mainClass>
                   </configuration>
               </plugin>
           </plugins>
       </build>
   </project>
   ```
   Run with `mvn -f UI/UX/pom.xml javafx:run`.
   For **Swing**, remove the two `org.openjfx` dependencies and the plugin; Swing is built into Java.
4. Put your code in `UI/UX/src/main/java/freshlink/ui/` (start with `App.java`) and FXML files in `UI/UX/src/main/resources/`.

## Screens to build (like Bách Hóa Xanh)

### Customer

| # | Screen | Shows / does | Controller (suggested) |
|---|---|---|---|
| 1 | Login / Register | username + password, link to sign up | `AuthController` |
| 2 | Home | category menu on the left, search bar, **"Khuyến mãi hot"** product grid, cart icon with count | `ProductController`, `CartController` |
| 3 | Category / search results | product cards: image, name, unit ("Khay 300g"), price, sale badge "-12%", "MUA" button | `ProductController`, `CartController` |
| 4 | Product detail | big image, prices, origin, description, quantity +/-, stars and reviews, "Món ngon với sản phẩm này" | `ProductController`, `ReviewController`, `RecipeController` |
| 5 | Cart | lines with +/- and remove, subtotal, shipping fee, voucher box with "Áp dụng", total, "Đặt hàng" | `CartController` |
| 6 | Checkout | choose address, delivery date + time slot, payment method (COD / thẻ / MoMo / ZaloPay), note, confirm | `OrderController`, `AccountController` |
| 7 | Order success | order number, total, button to tracking | `OrderController` |
| 8 | My orders + tracking | list of orders; detail shows items and a timeline (Đặt hàng → Xác nhận → Soạn hàng → Đang giao → Đã giao), cancel button, "write review" after delivery | `OrderController`, `ReviewController` |
| 9 | Recipes | list + search; detail shows ingredients, steps, "Thêm nguyên liệu vào giỏ" | `RecipeController` |
| 10 | Account | profile, saved addresses (add, set default, delete), change password | `AccountController`, `AuthController` |

### Admin (only when `Session.isAdmin()`)

| # | Screen | Shows / does |
|---|---|---|
| 11 | Products | table of all products, add / edit, change stock, hide, low-stock warning |
| 12 | Categories | add main and sub-categories |
| 13 | Vouchers | list, add, turn on/off |
| 14 | Orders | filter by status, open order, move to next status or cancel |
| 15 | Users | list, lock / unlock |

Start with screens 1 → 2 → 3 → 5 → 6: that is the main shopping flow and enough for a first demo.

## Display tips

- Prices are `long` in VND. Show them like Bách Hóa Xanh: `45.900₫`
  ```java
  String text = String.format(java.util.Locale.GERMANY, "%,d₫", price);   // 45.900₫
  ```
- On sale: show `getFinalPrice()` big, the old `price()` crossed out, and the badge `"-" + getDiscountPercent() + "%"`.
- `stock() == 0`: grey out the card and show "Tạm hết hàng" instead of the MUA button.
- Show status codes in Vietnamese:

  | Code | Text |
  |---|---|
  | PENDING | Chờ xác nhận |
  | CONFIRMED | Đã xác nhận |
  | PACKING | Đang soạn hàng |
  | SHIPPING | Đang giao |
  | DELIVERED | Đã giao |
  | CANCELLED | Đã hủy |

- When a Controller method returns an error message, show it in a dialog (`Alert` in JavaFX, `JOptionPane` in Swing).
- Database calls can be slow: for big lists use `javafx.concurrent.Task` (or `SwingWorker`) so the window does not freeze.
- `products.image_url` is empty in the sample data: use a placeholder image, or ask the Database member to add image links.

## Task checklist

- [ ] `pom.xml` + `App.java` that opens a window
- [ ] Login / Register screen
- [ ] Home + category menu + hot deals
- [ ] Product list and product detail
- [ ] Cart and checkout
- [ ] My orders + tracking timeline
- [ ] Recipes
- [ ] Account and addresses
- [ ] Admin screens
- [ ] Same colors and fonts everywhere (FreshLink green, e.g. `#0A8F3C`)

Demo accounts: `admin` / `Admin@123` (admin), `lan.nguyen` / `Fresh@123` (customer).

## Working together (Git)

- Never push straight to `main`. Make a branch: `git checkout -b ui/login-screen`
- Commit small steps, push the branch, open a Pull Request, ask a teammate to review, then merge.
- Only edit files in `UI/UX/`. If you need a new Controller method, ask the Controller member.
