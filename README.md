# Java Online Sales Management

A desktop app that shop staff use to run a retail shop: look up customers, check stock, build orders, take payments and track sales. All money is in Vietnamese Dong (VND).

Course project for **VGU 61CSE215 – Object-Oriented Programming in Java**.

## Tech stack

| | |
|---|---|
| Language | Java 21 |
| UI | Java Swing |
| Database | PostgreSQL 15+ via plain JDBC (`PreparedStatement`, no ORM) |
| Build | Maven, one project per tier |
| Architecture | MVC 3-tier: UI/UX → Controller → Database |

## Features

Two roles log in to the app. Customers are records in the system, not users.

| Area | Administrator (shop owner) | Sales staff |
|---|---|---|
| Staff accounts | Create, update, reset passwords | Log in and out |
| Categories and products | Full control, including prices and stock | Search and view stock (read-only) |
| Customers | Create, search, edit, view order history | Same |
| Orders and payments | Cart, checkout, cash or card payment | Same |
| Order status | `PENDING` → `PAID` → `SHIPPED`, or `CANCELLED` | Same |
| Reports | Daily revenue, top products, low-stock alerts | Low-stock alerts only |

Checkout is atomic: the order, its items, the payment and the stock deduction are saved in one database transaction. If stock runs out, nothing is saved.

## Project structure

Each folder is its own Maven project. Dependencies go one way only, so there are no cycles.

```
Java-online-sales-management/
├── Database/      sales-database   · schema.sql, seed.sql, XxxRecord records, XxxDao interfaces, PgXxxDao JDBC classes
├── Controller/    sales-controller · domain classes, business rules, XxxService interfaces, ControllerFactory
└── UI/UX/         sales-ui         · Swing screens, App launcher
```

- **Database** returns its own records and knows nothing about the other tiers. All SQL lives here.
- **Controller** turns records into domain objects (`Customer`, `Product`, `Order`, …) and holds the business rules.
- **UI/UX** talks only to the Controller, through service interfaces.

Each tier has a `TierBoundaryTest` that fails the build if it imports from a tier it must not.

Design docs live next to the code: `Controller/docs/requirements.md`, `Database/docs/database.md`, `UI/UX/docs/screens.md`.

## Getting started

### Prerequisites

- JDK 21
- Maven 3.9+
- PostgreSQL 15 or newer

### 1. Create the databases

```bash
createdb -U postgres online_sales
createdb -U postgres online_sales_test   # optional, for integration tests
```

### 2. Configure the connection

Copy the example file and fill in your PostgreSQL password:

```bash
cp Database/src/main/resources/db.properties.example Database/src/main/resources/db.properties
```

`db.properties` is gitignored, so your password never gets committed.

### 3. Create the tables and sample data

```bash
mvn -q -f Database/pom.xml compile exec:java                      # main database
mvn -q -f Database/pom.xml compile exec:java -Dexec.args="--test" # test database
```

The seed data includes two demo accounts: `admin` / `Admin@123` and `sales1` / `Sales@123`.

### 4. Build and run

Build the tiers in order: Database → Controller → UI/UX.

```bash
mvn -q -f Database/pom.xml install
mvn -q -f Controller/pom.xml install
mvn -q -f UI/UX/pom.xml exec:java
```

The integration tests in `Database/` run against `online_sales_test` and are skipped when `db.test.url` is not set.

## Project status

| Tier | Status |
|---|---|
| Database | Done: schema, seed data, DAOs, transactions, reports, tests |
| Controller | In progress |
| UI/UX | In progress |

The work is split into slices: staff login and roles, categories and products, customers, order creation, order list and status, and sales statistics.

## Team

| Tier | Member |
|---|---|
| Database | Nguyen Duy Duc Trong |
| Controller | _TBD_ |
| UI/UX | _TBD_ |
