-- ============================================================================
-- Online Sales Management System: Database Seed Data Script
-- Target DBMS: PostgreSQL 16+
-- Author: Nguyen Duy Duc Trong (Member A — Database Tier)
-- ============================================================================

-- Clean up any existing data in reverse dependency order
TRUNCATE TABLE payments, order_items, orders, products, customers, categories, staff CASCADE;

-- ----------------------------------------------------------------------------
-- 1. SEED: staff (2 rows)
-- Passwords:
--   admin  -> Admin@123 (salt 'a1b2c3d4e5f60718293a4b5c6d7e8f90', hash '8c8746be97b5fda756b26fb956ac8f714ffe6b9781563d9c65320acc221a8e7a')
--   sales1 -> Sales@123 (salt '0f1e2d3c4b5a69788796a5b4c3d2e1f0', hash 'bce595ecde332a2029476d655e58d6f9425062c5e80e95a957ae5686710cf6c2')
-- ----------------------------------------------------------------------------
INSERT INTO staff (staff_id, username, password_hash, password_salt, full_name, email, phone, role, active, created_at)
OVERRIDING SYSTEM VALUE VALUES
(1, 'admin', '8c8746be97b5fda756b26fb956ac8f714ffe6b9781563d9c65320acc221a8e7a', 'a1b2c3d4e5f60718293a4b5c6d7e8f90', 'System Administrator', 'admin@sales.vgu.vn', '0901234567', 'ADMIN', true, now()),
(2, 'sales1', 'bce595ecde332a2029476d655e58d6f9425062c5e80e95a957ae5686710cf6c2', '0f1e2d3c4b5a69788796a5b4c3d2e1f0', 'Nguyen Van Ban', 'sales1@sales.vgu.vn', '0902345678', 'SALES_STAFF', true, now());

-- ----------------------------------------------------------------------------
-- 2. SEED: categories (5 rows)
-- ----------------------------------------------------------------------------
INSERT INTO categories (category_id, name, description, active)
OVERRIDING SYSTEM VALUE VALUES
(1, 'Smartphones & Tablets', 'Mobile phones, flagship tablets, styluses and mobile accessories', true),
(2, 'Laptops & Computers', 'Ultrabooks, high-performance workstations, monitors and PC peripherals', true),
(3, 'Audio & Wearables', 'Over-ear headphones, TWS earbuds, Bluetooth speakers and smart watches', true),
(4, 'Smart Home & IoT', 'Smart home appliances, robot vacuum cleaners, environmental sensors and security cameras', true),
(5, 'Computer Accessories', 'Mechanical keyboards, ergonomic mice, GaN chargers, adapters and high-speed cables', true);

-- ----------------------------------------------------------------------------
-- 3. SEED: products (15 rows)
-- VND Prices range: 95,000 to 34,990,000 VND
-- Low stock condition: exactly 3 items with stock <= 5 (IDs: 3, 5, 7) and 1 item with stock = 0 (ID: 9)
-- ----------------------------------------------------------------------------
INSERT INTO products (product_id, category_id, name, description, price, stock_quantity, active, created_at)
OVERRIDING SYSTEM VALUE VALUES
(1, 1, 'iPhone 15 Pro Max 256GB', 'Titanium build, A17 Pro 3nm chip, 5x telephoto optical zoom camera', 31990000.00, 15, true, now()),
(2, 1, 'Samsung Galaxy S24 Ultra 512GB', 'Snapdragon 8 Gen 3 for Galaxy, Quad Tele AI Camera, integrated S-Pen', 33490000.00, 8, true, now()),
(3, 1, 'iPad Air 11-inch M2 128GB', 'Liquid Retina display with P3 wide color, Apple M2 chip, Wi-Fi 6E', 16990000.00, 4, true, now()), -- stock <= 5 (#1)
(4, 2, 'MacBook Pro 14 M3 Pro', '18GB unified memory, 512GB SSD, Liquid Retina XDR display, Space Black', 34990000.00, 6, true, now()),
(5, 2, 'Dell XPS 13 9340', 'Intel Core Ultra 7 155H, 16GB LPDDR5x, 512GB SSD, FHD+ InfinityEdge', 29500000.00, 3, true, now()), -- stock <= 5 (#2)
(6, 2, 'LG UltraGear 27GR93U Monitor', '27-inch 4K UHD 3840x2160, 144Hz, 1ms IPS, VESA DisplayHDR 400', 12500000.00, 12, true, now()),
(7, 3, 'Sony WH-1000XM5 Headphones', 'Industry-leading noise canceling with dual processors, 30h battery life', 8490000.00, 2, true, now()), -- stock <= 5 (#3)
(8, 3, 'Apple AirPods Pro 2 USB-C', 'H2 chip, Adaptive Audio, Active Noise Cancellation, MagSafe Charging Case', 5990000.00, 25, true, now()),
(9, 3, 'Marshall Emberton II Speaker', 'Iconic portable Bluetooth speaker, True Stereophonic 360 sound, IP67', 3990000.00, 0, true, now()), -- stock == 0 (#4)
(10, 4, 'Xiaomi Robot Vacuum X10+', 'All-in-one smart cleaning station, 4000Pa suction, dual spinning mop pads', 11990000.00, 10, true, now()),
(11, 4, 'Philips Hue Starter Kit E27', 'Smart LED ambiance lighting, 3 white and color bulbs + Hue Bridge hub', 2490000.00, 18, true, now()),
(12, 5, 'Logitech MX Master 3S Mouse', 'Ergonomic wireless mouse, 8K DPI sensor, Quiet Clicks, MagSpeed wheel', 2290000.00, 30, true, now()),
(13, 5, 'Keychron Q1 Pro Mechanical Keyboard', '75% layout, wireless QMK/VIA custom keyboard, CNC aluminum body', 4500000.00, 7, true, now()),
(14, 5, 'Anker 737 GaNPrime 140W Charger', 'Fast charging 3-port wall adapter with PowerIQ 4.0 and ActiveShield 2.0', 1890000.00, 45, true, now()),
(15, 5, 'Ugreen USB-C to USB-C Cable 100W', 'Nylon braided high-speed PD fast charging cable, aluminum alloy shell, 1m', 95000.00, 150, true, now());

-- ----------------------------------------------------------------------------
-- 4. SEED: customers (8 rows)
-- Phone numbers: CHAR(10) starting with '0'
-- ----------------------------------------------------------------------------
INSERT INTO customers (customer_id, full_name, phone, email, address, created_at)
OVERRIDING SYSTEM VALUE VALUES
(1, 'Tran Thi Mai', '0912345678', 'mai.tran@gmail.com', '12 Le Loi, District 1, Ho Chi Minh City', now()),
(2, 'Le Van Hung', '0923456789', 'hung.le@outlook.com', '45 Nguyen Hue, District 1, Ho Chi Minh City', now()),
(3, 'Pham Minh Tuan', '0934567890', 'tuan.pham@yahoo.com', '78 Hai Ba Trung, District 3, Ho Chi Minh City', now()),
(4, 'Hoang Thu Thao', '0945678901', 'thao.hoang@gmail.com', '102 Tran Phu, Ward 4, District 5, Ho Chi Minh City', now()),
(5, 'Doan Van Binh', '0956789012', NULL, '15 Vo Van Ngan, Thu Duc City, Ho Chi Minh City', now()),
(6, 'Vu Thi Lan', '0967890123', 'lan.vu@gmail.com', '88 Quang Trung, Go Vap District, Ho Chi Minh City', now()),
(7, 'Nguyen Duc Nam', '0978901234', 'nam.nguyen@protonmail.com', '210 Dien Bien Phu, Binh Thanh District, Ho Chi Minh City', now()),
(8, 'Bui Thanh Huong', '0989012345', NULL, '34 Hoang Dieu 2, Linh Trung, Thu Duc City, Ho Chi Minh City', now());

-- ----------------------------------------------------------------------------
-- 5. SEED: orders (6 rows over last 14 days)
-- Covering all 4 statuses: PENDING, PAID, SHIPPED, CANCELLED
-- ----------------------------------------------------------------------------
INSERT INTO orders (order_id, order_code, customer_id, staff_id, order_date, status, total_amount, shipping_address, notes, cancel_reason)
OVERRIDING SYSTEM VALUE VALUES
(1, 'ORD-20261006-001', 1, 2, CURRENT_DATE - INTERVAL '14 days' + TIME '09:30:00', 'SHIPPED', 8280000.00, '12 Le Loi, District 1, Ho Chi Minh City', 'Deliver during standard business hours', NULL),
(2, 'ORD-20261010-001', 2, 2, CURRENT_DATE - INTERVAL '10 days' + TIME '14:15:00', 'SHIPPED', 3970000.00, '45 Nguyen Hue, District 1, Ho Chi Minh City', NULL, NULL),
(3, 'ORD-20261013-001', 3, 2, CURRENT_DATE - INTERVAL '7 days' + TIME '11:00:00', 'PAID', 31990000.00, '78 Hai Ba Trung, District 3, Ho Chi Minh City', 'Please phone recipient 15 minutes prior to arrival', NULL),
(4, 'ORD-20261016-001', 4, 2, CURRENT_DATE - INTERVAL '4 days' + TIME '16:45:00', 'PAID', 14480000.00, '102 Tran Phu, Ward 4, District 5, Ho Chi Minh City', 'Fragile electronic equipment - gift packaging requested', NULL),
(5, 'ORD-20261018-001', 6, 2, CURRENT_DATE - INTERVAL '2 days' + TIME '10:20:00', 'CANCELLED', 12500000.00, '88 Quang Trung, Go Vap District, Ho Chi Minh City', 'Urgent express delivery', 'Customer requested cancellation due to change of mind'),
(6, 'ORD-20261020-001', 7, 2, CURRENT_DATE + TIME '08:45:00', 'PENDING', 4500000.00, '210 Dien Bien Phu, Binh Thanh District, Ho Chi Minh City', 'Store counter pickup scheduled for this afternoon', NULL);

-- ----------------------------------------------------------------------------
-- 6. SEED: order_items (9 rows)
-- unit_price snapshots the historical catalog price; total_amount = SUM(quantity * unit_price)
-- ----------------------------------------------------------------------------
INSERT INTO order_items (order_item_id, order_id, product_id, quantity, unit_price)
OVERRIDING SYSTEM VALUE VALUES
-- Order 1 Items (Total: 5,990,000 + 2,290,000 = 8,280,000.00)
(1, 1, 8, 1, 5990000.00),
(2, 1, 12, 1, 2290000.00),

-- Order 2 Items (Total: 2 * 1,890,000 + 2 * 95,000 = 3,780,000 + 190,000 = 3,970,000.00)
(3, 2, 14, 2, 1890000.00),
(4, 2, 15, 2, 95000.00),

-- Order 3 Items (Total: 1 * 31,990,000 = 31,990,000.00)
(5, 3, 1, 1, 31990000.00),

-- Order 4 Items (Total: 1 * 11,990,000 + 1 * 2,490,000 = 14,480,000.00)
(6, 4, 10, 1, 11990000.00),
(7, 4, 11, 1, 2490000.00),

-- Order 5 Items (Total: 1 * 12,500,000 = 12,500,000.00)
(8, 5, 6, 1, 12500000.00),

-- Order 6 Items (Total: 1 * 4,500,000 = 4,500,000.00)
(9, 6, 13, 1, 4500000.00);

-- ----------------------------------------------------------------------------
-- 7. SEED: payments (6 rows)
-- Covering both CASH and CARD methods
-- amount = order.total_amount
-- ----------------------------------------------------------------------------
INSERT INTO payments (payment_id, order_id, method, amount, paid_at, cash_received, change_amount, bank_reference, cardholder_name)
OVERRIDING SYSTEM VALUE VALUES
(1, 1, 'CARD', 8280000.00, CURRENT_DATE - INTERVAL '14 days' + TIME '09:32:00', NULL, NULL, 'VNPAY-20261006-88192', 'TRAN THI MAI'),
(2, 2, 'CASH', 3970000.00, CURRENT_DATE - INTERVAL '10 days' + TIME '14:20:00', 4000000.00, 30000.00, NULL, NULL),
(3, 3, 'CARD', 31990000.00, CURRENT_DATE - INTERVAL '7 days' + TIME '11:05:00', NULL, NULL, 'MB-20261013-91283', 'PHAM MINH TUAN'),
(4, 4, 'CASH', 14480000.00, CURRENT_DATE - INTERVAL '4 days' + TIME '16:50:00', 14500000.00, 20000.00, NULL, NULL),
(5, 5, 'CARD', 12500000.00, CURRENT_DATE - INTERVAL '2 days' + TIME '10:22:00', NULL, NULL, 'TCB-20261018-44512', 'VU THI LAN'),
(6, 6, 'CASH', 4500000.00, CURRENT_DATE + TIME '08:48:00', 4500000.00, 0.00, NULL, NULL);

-- ----------------------------------------------------------------------------
-- 8. SYNCHRONIZE IDENTITY SEQUENCES
-- ----------------------------------------------------------------------------
SELECT setval(pg_get_serial_sequence('staff', 'staff_id'), (SELECT MAX(staff_id) FROM staff));
SELECT setval(pg_get_serial_sequence('categories', 'category_id'), (SELECT MAX(category_id) FROM categories));
SELECT setval(pg_get_serial_sequence('customers', 'customer_id'), (SELECT MAX(customer_id) FROM customers));
SELECT setval(pg_get_serial_sequence('products', 'product_id'), (SELECT MAX(product_id) FROM products));
SELECT setval(pg_get_serial_sequence('orders', 'order_id'), (SELECT MAX(order_id) FROM orders));
SELECT setval(pg_get_serial_sequence('order_items', 'order_item_id'), (SELECT MAX(order_item_id) FROM order_items));
SELECT setval(pg_get_serial_sequence('payments', 'payment_id'), (SELECT MAX(payment_id) FROM payments));
