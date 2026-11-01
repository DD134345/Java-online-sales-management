# FreshLink – Online Grocery Store

Course project for **VGU – Object-Oriented Programming in Java**.

FreshLink is an online fresh-food market in the style of [Bách Hóa Xanh](https://www.bachhoaxanh.com/).
Customers browse products by category, add them to a cart, order with a voucher, a delivery date and
time slot, track the delivery, review what they bought and read recipes. Admins manage products,
stock, vouchers, orders and users. All money is in Vietnamese Dong (VND).

## Tech stack

| | |
|---|---|
| Language | Java 21 |
| UI | JavaFX or Swing (desktop) |
| Database | PostgreSQL 15+ with plain JDBC (`PreparedStatement`, no ORM) |
| Build | Maven, one project per tier |
| Architecture | MVC 3-tier: UI/UX → Controller → Database |

## Project structure and owners

```
Java-online-sales-management/
├── Database/     SQL tables, sample data, model classes, DAO classes      → Nguyễn Duy Đức Trọng
├── Controller/   business rules (login, cart, checkout, roles, …)         → Controller member
└── UI/UX/        screens                                                   → UI/UX member
```

Each tier only talks to the tier next to it: UI/UX calls Controller, Controller calls Database.

| Tier | Status | Guide |
|---|---|---|
| Database | Done: 12 tables, sample data, 7 DAOs, 10 tests | [`Database/README.md`](Database/README.md) |
| Controller | To do (starter `pom.xml` and `PasswordUtil` ready) | [`Controller/README.md`](Controller/README.md) |
| UI/UX | To do | [`UI/UX/README.md`](UI/UX/README.md) |

## Getting started

Needed: JDK 21, Maven 3.9+, PostgreSQL 15+ (and IntelliJ IDEA).

```bash
# 1. Create the databases
createdb -U postgres freshlink
createdb -U postgres freshlink_test

# 2. Database settings (db.properties is not committed, so your password stays private)
cp Database/src/main/resources/db.properties.example Database/src/main/resources/db.properties
#    → open db.properties and write your PostgreSQL password

# 3. Create the tables and sample data
mvn -f Database/pom.xml compile exec:java

# 4. Build the tiers in order (each one installs itself for the next)
mvn -f Database/pom.xml install
mvn -f Controller/pom.xml install
```

Demo accounts: `admin` / `Admin@123` (admin), `lan.nguyen` / `Fresh@123` (customer).

## Team workflow

1. `git pull` on `main` before you start.
2. Create a branch for your task: `database/...`, `controller/...` or `ui/...`.
3. Commit small steps with clear messages, push the branch, open a Pull Request.
4. Another member reviews, then merge into `main`. Nobody pushes straight to `main`.
5. Only change your own tier's folder. Need something from another tier? Open an issue or ask its owner.
