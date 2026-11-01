-- FreshLink database schema (PostgreSQL)
-- All prices are in VND (whole numbers), stored as BIGINT.

DROP TABLE IF EXISTS recipe_ingredients, recipes, reviews, order_tracking, order_items, orders,
    vouchers, cart_items, products, categories, addresses, users CASCADE;

-- Customers and admins
CREATE TABLE users (
    user_id       SERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(64)  NOT NULL,
    password_salt VARCHAR(32)  NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(100),
    phone         VARCHAR(10)  NOT NULL UNIQUE,
    role          VARCHAR(10)  NOT NULL DEFAULT 'CUSTOMER' CHECK (role IN ('CUSTOMER', 'ADMIN')),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Saved delivery addresses of a customer
CREATE TABLE addresses (
    address_id    SERIAL PRIMARY KEY,
    user_id       INT          NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    receiver_name VARCHAR(100) NOT NULL,
    phone         VARCHAR(10)  NOT NULL,
    street        VARCHAR(200) NOT NULL,
    ward          VARCHAR(100) NOT NULL,
    district      VARCHAR(100) NOT NULL,
    city          VARCHAR(100) NOT NULL,
    is_default    BOOLEAN      NOT NULL DEFAULT FALSE
);

-- Categories: parent_id is NULL for main categories
CREATE TABLE categories (
    category_id SERIAL PRIMARY KEY,
    parent_id   INT REFERENCES categories(category_id),
    name        VARCHAR(100) NOT NULL
);

-- Products. unit = how it is sold, e.g. 'Khay 300g', 'kg'.
-- sale_price is NULL when the product is not on sale.
-- voucher_eligible = FALSE for clearance products (vouchers do not apply).
CREATE TABLE products (
    product_id       SERIAL PRIMARY KEY,
    category_id      INT          NOT NULL REFERENCES categories(category_id),
    name             VARCHAR(150) NOT NULL,
    unit             VARCHAR(50)  NOT NULL,
    price            BIGINT       NOT NULL CHECK (price > 0),
    sale_price       BIGINT       CHECK (sale_price > 0 AND sale_price < price),
    stock            INT          NOT NULL DEFAULT 0 CHECK (stock >= 0),
    origin           VARCHAR(100),
    description      TEXT,
    image_url        VARCHAR(255),
    voucher_eligible BOOLEAN      NOT NULL DEFAULT TRUE,
    active           BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Shopping cart: one row per product per customer
CREATE TABLE cart_items (
    user_id    INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    product_id INT NOT NULL REFERENCES products(product_id),
    quantity   INT NOT NULL CHECK (quantity > 0),
    PRIMARY KEY (user_id, product_id)
);

-- Discount codes. PERCENT: value is 1-100. FIXED: value is VND.
CREATE TABLE vouchers (
    voucher_id      SERIAL PRIMARY KEY,
    code            VARCHAR(30)  NOT NULL UNIQUE,
    description     VARCHAR(255) NOT NULL,
    discount_type   VARCHAR(10)  NOT NULL CHECK (discount_type IN ('PERCENT', 'FIXED')),
    discount_value  BIGINT       NOT NULL CHECK (discount_value > 0),
    min_order_value BIGINT       NOT NULL DEFAULT 0,
    max_discount    BIGINT,
    start_date      TIMESTAMP    NOT NULL,
    end_date        TIMESTAMP    NOT NULL,
    usage_limit     INT,
    used_count      INT          NOT NULL DEFAULT 0,
    active          BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Orders. Receiver and address are copied here so old orders do not change.
CREATE TABLE orders (
    order_id         SERIAL PRIMARY KEY,
    user_id          INT          NOT NULL REFERENCES users(user_id),
    receiver_name    VARCHAR(100) NOT NULL,
    receiver_phone   VARCHAR(10)  NOT NULL,
    delivery_address VARCHAR(500) NOT NULL,
    delivery_date    DATE         NOT NULL,
    delivery_slot    VARCHAR(20)  NOT NULL,   -- e.g. '08:00-10:00'
    voucher_id       INT REFERENCES vouchers(voucher_id),
    subtotal         BIGINT       NOT NULL,
    discount         BIGINT       NOT NULL DEFAULT 0,
    shipping_fee     BIGINT       NOT NULL DEFAULT 0,
    total            BIGINT       NOT NULL,
    payment_method   VARCHAR(15)  NOT NULL CHECK (payment_method IN ('COD', 'BANK_CARD', 'MOMO', 'ZALOPAY')),
    payment_status   VARCHAR(10)  NOT NULL DEFAULT 'UNPAID' CHECK (payment_status IN ('UNPAID', 'PAID', 'REFUNDED')),
    status           VARCHAR(15)  NOT NULL DEFAULT 'PENDING'
                     CHECK (status IN ('PENDING', 'CONFIRMED', 'PACKING', 'SHIPPING', 'DELIVERED', 'CANCELLED')),
    note             VARCHAR(255),
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Products in an order. Name and price are copied at checkout.
CREATE TABLE order_items (
    order_item_id SERIAL PRIMARY KEY,
    order_id      INT          NOT NULL REFERENCES orders(order_id) ON DELETE CASCADE,
    product_id    INT          NOT NULL REFERENCES products(product_id),
    product_name  VARCHAR(150) NOT NULL,
    unit_price    BIGINT       NOT NULL,
    quantity      INT          NOT NULL CHECK (quantity > 0)
);

-- Delivery tracking: one row each time the order status changes
CREATE TABLE order_tracking (
    tracking_id SERIAL PRIMARY KEY,
    order_id    INT         NOT NULL REFERENCES orders(order_id) ON DELETE CASCADE,
    status      VARCHAR(15) NOT NULL,
    note        VARCHAR(255),
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- Product reviews: one review per customer per product
CREATE TABLE reviews (
    review_id  SERIAL PRIMARY KEY,
    product_id INT       NOT NULL REFERENCES products(product_id) ON DELETE CASCADE,
    user_id    INT       NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    rating     INT       NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment    VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (product_id, user_id)
);

-- Cooking recipes
CREATE TABLE recipes (
    recipe_id    SERIAL PRIMARY KEY,
    title        VARCHAR(150) NOT NULL,
    description  VARCHAR(500),
    instructions TEXT         NOT NULL,
    cook_time    INT          NOT NULL,   -- minutes
    servings     INT          NOT NULL
);

-- Ingredients of a recipe. product_id is NULL if the shop does not sell it.
CREATE TABLE recipe_ingredients (
    ingredient_id SERIAL PRIMARY KEY,
    recipe_id     INT          NOT NULL REFERENCES recipes(recipe_id) ON DELETE CASCADE,
    name          VARCHAR(100) NOT NULL,
    amount        VARCHAR(50)  NOT NULL,
    product_id    INT REFERENCES products(product_id) ON DELETE SET NULL
);
