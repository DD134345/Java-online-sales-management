-- ============================================================================
-- Online Sales Management System: Database Schema Definition
-- Target DBMS: PostgreSQL 16+
-- Architecture: 3-Tier MVC, Pure JDBC with PreparedStatement (No ORM)
-- Author: Nguyen Duy Duc Trong (Member A — Database Tier)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. DROP EXISTING TABLES (Reverse Dependency Order)
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS payments CASCADE;
DROP TABLE IF EXISTS order_items CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS products CASCADE;
DROP TABLE IF EXISTS customers CASCADE;
DROP TABLE IF EXISTS categories CASCADE;
DROP TABLE IF EXISTS staff CASCADE;

-- ----------------------------------------------------------------------------
-- 2. CREATE TABLES (Dependency Order)
-- ----------------------------------------------------------------------------

-- Table: staff
CREATE TABLE staff (
    staff_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(64) NOT NULL,
    password_salt VARCHAR(32) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'SALES_STAFF')),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

-- Table: categories
CREATE TABLE categories (
    category_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT true
);

-- Table: customers
CREATE TABLE customers (
    customer_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    phone CHAR(10) NOT NULL UNIQUE CHECK (phone ~ '^0[0-9]{9}$'),
    email VARCHAR(100),
    address VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

-- Table: products
CREATE TABLE products (
    product_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    category_id INTEGER NOT NULL REFERENCES categories(category_id),
    name VARCHAR(150) NOT NULL,
    description TEXT,
    price NUMERIC(14,2) NOT NULL CHECK (price >= 0),
    stock_quantity INTEGER NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_products_category_name UNIQUE (category_id, name)
);

-- Table: orders
CREATE TABLE orders (
    order_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_code VARCHAR(20) NOT NULL UNIQUE,
    customer_id INTEGER NOT NULL REFERENCES customers(customer_id),
    staff_id INTEGER NOT NULL REFERENCES staff(staff_id),
    order_date TIMESTAMP NOT NULL DEFAULT now(),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'PAID', 'SHIPPED', 'CANCELLED')),
    total_amount NUMERIC(14,2) NOT NULL CHECK (total_amount >= 0),
    shipping_address VARCHAR(255) NOT NULL,
    notes TEXT,
    cancel_reason VARCHAR(255)
);

-- Table: order_items
CREATE TABLE order_items (
    order_item_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id INTEGER NOT NULL REFERENCES orders(order_id) ON DELETE CASCADE,
    product_id INTEGER NOT NULL REFERENCES products(product_id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(14,2) NOT NULL CHECK (unit_price >= 0),
    CONSTRAINT uq_order_items_order_product UNIQUE (order_id, product_id)
);

-- Table: payments
CREATE TABLE payments (
    payment_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id INTEGER NOT NULL UNIQUE REFERENCES orders(order_id),
    method VARCHAR(10) NOT NULL CHECK (method IN ('CASH', 'CARD')),
    amount NUMERIC(14,2) NOT NULL CHECK (amount >= 0),
    paid_at TIMESTAMP NOT NULL DEFAULT now(),
    cash_received NUMERIC(14,2) CHECK (cash_received >= 0),
    change_amount NUMERIC(14,2) CHECK (change_amount >= 0),
    bank_reference VARCHAR(100),
    cardholder_name VARCHAR(100),
    CONSTRAINT chk_payments_cash CHECK (
        method != 'CASH' OR (
            cash_received IS NOT NULL
            AND change_amount IS NOT NULL
            AND cash_received >= amount
            AND change_amount = cash_received - amount
            AND bank_reference IS NULL
            AND cardholder_name IS NULL
        )
    ),
    CONSTRAINT chk_payments_card CHECK (
        method != 'CARD' OR (
            bank_reference IS NOT NULL
            AND cardholder_name IS NOT NULL
            AND cash_received IS NULL
            AND change_amount IS NULL
        )
    )
);

-- ----------------------------------------------------------------------------
-- 3. CREATE PERFORMANCE & SEARCH INDEXES
-- ----------------------------------------------------------------------------
CREATE UNIQUE INDEX idx_staff_lower_username ON staff (lower(username));
CREATE UNIQUE INDEX idx_categories_lower_name ON categories (lower(name));
CREATE INDEX idx_products_category_id ON products (category_id);
CREATE INDEX idx_products_lower_name ON products (lower(name));
CREATE INDEX idx_customers_lower_full_name ON customers (lower(full_name));
CREATE INDEX idx_orders_customer_id ON orders (customer_id);
CREATE INDEX idx_orders_order_date ON orders (order_date);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_order_items_product_id ON order_items (product_id);
