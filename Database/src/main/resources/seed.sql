-- FreshLink sample data. Run after schema.sql.
-- Demo accounts: admin / Admin@123, lan.nguyen / Fresh@123, minh.tran / Fresh@123, hoa.le / Fresh@123

-- Users (ids 1-4)
INSERT INTO users (username, password_hash, password_salt, full_name, email, phone, role) VALUES
('admin',      'e6a2db3bc2db60eea0ccb1c6917e84cc28ec9c899c6b81997f0f45ffd8c083c1', 'a1f3c9e27b4d8e60f2a5b7c913d4e6f8', 'Quản trị FreshLink', 'admin@freshlink.vn',  '0900000001', 'ADMIN'),
('lan.nguyen', '6ad702fe84d80da8538d7139309e4a0312587c73057d554f625e5baf124994e5', '5c2e8a1b9d7f3e4a6b0c2d8e1f9a3b7c', 'Nguyễn Thị Lan',     'lan.nguyen@gmail.com', '0912345678', 'CUSTOMER'),
('minh.tran',  '0f491764618c36274d6668381847163a8be4af851ab00f7e4c99f48a2f042406', '9e7d5c3b1a2f4e6d8c0b9a7e5d3c1b2f', 'Trần Văn Minh',      'minh.tran@gmail.com',  '0987654321', 'CUSTOMER'),
('hoa.le',     '384c96f6fb395d5d325a2b101ac49f04193642a1daffe1110ab70da367dd3cc9', '2b4d6f8a0c1e3a5b7d9f0e2c4a6b8d0f', 'Lê Thị Hoa',         NULL,                   '0933221100', 'CUSTOMER');

INSERT INTO addresses (user_id, receiver_name, phone, street, ward, district, city, is_default) VALUES
(2, 'Nguyễn Thị Lan', '0912345678', '25 Nguyễn Thị Thập',       'Phường Tân Phú',       'Quận 7',         'TP. Hồ Chí Minh', true),
(2, 'Nguyễn Văn Hùng', '0912000111', '12 Lê Văn Việt',          'Phường Hiệp Phú',      'TP. Thủ Đức',    'TP. Hồ Chí Minh', false),
(3, 'Trần Văn Minh',  '0987654321', '88 Phan Xích Long',        'Phường 2',             'Quận Phú Nhuận', 'TP. Hồ Chí Minh', true),
(4, 'Lê Thị Hoa',     '0933221100', 'Lô 3, KDC Mỹ Phước',       'Phường Mỹ Phước',      'TX. Bến Cát',    'Bình Dương',      true);

-- Categories: parents 1-6, children 7-22
INSERT INTO categories (parent_id, name) VALUES
(NULL, 'Thịt, cá, trứng, hải sản'),
(NULL, 'Rau, củ, nấm, trái cây'),
(NULL, 'Gạo, mì, đồ khô'),
(NULL, 'Dầu ăn, nước chấm, gia vị'),
(NULL, 'Sữa, đồ uống'),
(NULL, 'Bánh kẹo, ăn vặt');

INSERT INTO categories (parent_id, name) VALUES
(1, 'Thịt heo'),
(1, 'Thịt bò'),
(1, 'Cá, hải sản'),
(1, 'Trứng'),
(2, 'Rau lá'),
(2, 'Củ, quả'),
(2, 'Nấm'),
(2, 'Trái cây'),
(3, 'Gạo'),
(3, 'Mì, bún, phở'),
(4, 'Dầu ăn'),
(4, 'Nước mắm, nước tương'),
(4, 'Gia vị'),
(5, 'Sữa tươi'),
(5, 'Nước ngọt, nước suối'),
(6, 'Bánh quy, snack');

-- Products (ids 1-33)
INSERT INTO products (category_id, name, unit, price, sale_price, stock, origin, voucher_eligible, description) VALUES
( 7, 'Ba rọi heo',                     'Khay 300g',     52000,  45900, 80, 'Việt Nam',    true,  'Thịt ba rọi tươi, nạc mỡ xen kẽ, thích hợp kho, luộc, nướng.'),
( 7, 'Sườn non heo',                   'Khay 300g',     69000,   NULL, 50, 'Việt Nam',    true,  'Sườn non chặt miếng vừa ăn.'),
( 7, 'Thịt heo xay',                   'Khay 300g',     39000,  35000, 60, 'Việt Nam',    true,  'Thịt nạc vai xay.'),
( 8, 'Thăn bò Úc',                     'Khay 250g',    119000,   NULL, 30, 'Úc',          true,  'Thăn bò nhập khẩu, mềm, ít mỡ.'),
( 8, 'Bắp bò',                         'Khay 300g',     98000,  89000, 35, 'Việt Nam',    true,  'Bắp bò có gân, hợp luộc, hầm.'),
( 9, 'Cá basa phi lê',                 'Khay 500g',     45000,   NULL, 40, 'Việt Nam',    true,  'Phi lê cá basa đồng bằng sông Cửu Long.'),
( 9, 'Tôm thẻ tươi',                   'Khay 300g',     79000,  69000, 25, 'Việt Nam',    true,  'Tôm thẻ size 40-50 con/kg.'),
(10, 'Trứng gà ta',                    'Hộp 10 quả',    38000,   NULL, 100, 'Việt Nam',   true,  NULL),
(10, 'Trứng vịt',                      'Hộp 10 quả',    35000,   NULL, 90, 'Việt Nam',    true,  NULL),
(11, 'Rau muống',                      'Bó 500g',       12000,   NULL, 70, 'Đà Lạt',      true,  NULL),
(11, 'Cải ngọt',                       'Túi 500g',      15000,  12500, 60, 'Đà Lạt',      true,  NULL),
(11, 'Hành lá',                        'Túi 100g',       6000,   NULL, 120, 'Việt Nam',   true,  NULL),
(12, 'Cà chua',                        'kg',            28000,   NULL, 75, 'Đà Lạt',      true,  NULL),
(12, 'Khoai tây',                      'kg',            32000,   NULL, 65, 'Đà Lạt',      true,  NULL),
(12, 'Cà rốt',                         'kg',            22000,   NULL, 70, 'Đà Lạt',      true,  NULL),
(12, 'Hành tím',                       'Túi 200g',      14000,   NULL, 90, 'Sóc Trăng',   true,  NULL),
(13, 'Nấm kim châm',                   'Gói 150g',       9000,   7500, 110, 'Hàn Quốc',   true,  NULL),
(13, 'Nấm rơm',                        'Hộp 200g',      24000,   NULL, 40, 'Việt Nam',    true,  NULL),
(14, 'Chuối già Nam Mỹ',               'kg',            25000,   NULL, 55, 'Việt Nam',    true,  NULL),
(14, 'Táo Envy New Zealand',           'kg',           159000, 119000, 20, 'New Zealand', false, 'Giá sốc xả kho, không áp dụng phiếu mua hàng.'),
(15, 'Gạo ST25',                       'Túi 5kg',      185000,   NULL, 45, 'Sóc Trăng',   true,  'Gạo thơm đặc sản Sóc Trăng.'),
(16, 'Mì tôm chua cay',                'Gói 75g',        4500,   NULL, 500, 'Việt Nam',   true,  NULL),
(16, 'Bún tươi',                       'Túi 500g',      12000,   NULL, 50, 'Việt Nam',    true,  NULL),
(17, 'Dầu ăn đậu nành',                'Chai 1 lít',    52000,   NULL, 80, 'Việt Nam',    true,  NULL),
(18, 'Nước mắm cá cơm 40 độ đạm',      'Chai 500ml',    45000,   NULL, 70, 'Phú Quốc',    true,  NULL),
(18, 'Nước tương đậu nành',            'Chai 500ml',    22000,   NULL, 85, 'Việt Nam',    true,  NULL),
(19, 'Đường cát trắng',                'Túi 1kg',       27000,   NULL, 95, 'Việt Nam',    true,  NULL),
(19, 'Muối i-ốt',                      'Gói 500g',       6000,   NULL, 150, 'Việt Nam',   true,  NULL),
(20, 'Sữa tươi tiệt trùng ít đường',   'Hộp 1 lít',     36000,  32000, 120, 'Việt Nam',   true,  NULL),
(21, 'Nước suối',                      'Chai 500ml',     5000,   NULL, 300, 'Việt Nam',   true,  NULL),
(21, 'Nước ngọt có ga',                'Lon 330ml',     10000,   NULL, 240, 'Việt Nam',   true,  NULL),
(22, 'Bánh quy bơ',                    'Hộp 300g',      65000,   NULL, 35, 'Đan Mạch',    true,  NULL),
(22, 'Snack khoai tây vị tảo biển',    'Gói 50g',       12000,   NULL,  0, 'Việt Nam',    true,  'Tạm hết hàng.');

-- Vouchers (ids 1-5)
INSERT INTO vouchers (code, description, discount_type, discount_value, min_order_value, max_discount, start_date, end_date, usage_limit, used_count, active) VALUES
('FRESH50K',   'Giảm 50.000đ cho đơn từ 300.000đ',             'FIXED',   50000, 300000,  NULL, '2026-01-01 00:00', '2027-12-31 23:59', 1000, 1, true),
('TUOINGON10', 'Giảm 10% tối đa 30.000đ cho đơn từ 150.000đ',  'PERCENT',    10, 150000, 30000, '2026-01-01 00:00', '2027-12-31 23:59', NULL, 1, true),
('GIAM15K',    'Giảm 15.000đ cho đơn từ 100.000đ',             'FIXED',   15000, 100000,  NULL, '2026-06-01 00:00', '2027-12-31 23:59',  500, 0, true),
('TET2026',    'Tết 2026: giảm 15% tối đa 50.000đ (đã hết hạn)', 'PERCENT', 15,      0, 50000, '2026-01-20 00:00', '2026-02-20 23:59', NULL, 0, true),
('VIP100K',    'Giảm 100.000đ cho đơn từ 500.000đ (đã hết lượt)', 'FIXED', 100000, 500000, NULL, '2026-01-01 00:00', '2027-12-31 23:59',    5, 5, true);

-- Orders (ids 1-5). total = subtotal - discount + shipping_fee
-- shipping is free from 300.000đ, otherwise 15.000đ
INSERT INTO orders (user_id, receiver_name, receiver_phone, delivery_address, delivery_date, delivery_slot, voucher_id,
                    subtotal, discount, shipping_fee, total, payment_method, payment_status, status, note, created_at) VALUES
(2, 'Nguyễn Thị Lan', '0912345678', '25 Nguyễn Thị Thập, Phường Tân Phú, Quận 7, TP. Hồ Chí Minh', '2026-10-20', '08:00-10:00', 1,
    349800, 50000,     0, 299800, 'MOMO',      'PAID',     'DELIVERED', 'Gọi trước khi giao', '2026-10-19 20:15'),
(3, 'Trần Văn Minh',  '0987654321', '88 Phan Xích Long, Phường 2, Quận Phú Nhuận, TP. Hồ Chí Minh', '2026-10-25', '18:00-20:00', 2,
    158000, 15800, 15000, 157200, 'COD',       'PAID',     'DELIVERED', NULL, '2026-10-25 09:40'),
(2, 'Nguyễn Thị Lan', '0912345678', '25 Nguyễn Thị Thập, Phường Tân Phú, Quận 7, TP. Hồ Chí Minh', '2026-11-01', '16:00-18:00', NULL,
    173000,     0, 15000, 188000, 'BANK_CARD', 'PAID',     'SHIPPING',  NULL, '2026-11-01 10:05'),
(4, 'Lê Thị Hoa',     '0933221100', 'Lô 3, KDC Mỹ Phước, Phường Mỹ Phước, TX. Bến Cát, Bình Dương', '2026-11-02', '08:00-10:00', NULL,
     92000,     0, 15000, 107000, 'COD',       'UNPAID',   'PENDING',   'Để hàng ở bảo vệ', '2026-11-01 17:30'),
(3, 'Trần Văn Minh',  '0987654321', '88 Phan Xích Long, Phường 2, Quận Phú Nhuận, TP. Hồ Chí Minh', '2026-10-28', '10:00-12:00', NULL,
     65000,     0, 15000,  80000, 'ZALOPAY',   'REFUNDED', 'CANCELLED', NULL, '2026-10-27 21:00');

INSERT INTO order_items (order_id, product_id, product_name, unit_price, quantity) VALUES
(1,  1, 'Ba rọi heo',   45900, 2),
(1, 13, 'Cà chua',          28000, 1),
(1, 25, 'Nước mắm cá cơm 40 độ đạm',  45000, 1),
(1, 21, 'Gạo ST25',    185000, 1),
(2,  7, 'Tôm thẻ tươi',   69000, 1),
(2, 11, 'Cải ngọt',    12500, 2),
(2, 29, 'Sữa tươi tiệt trùng ít đường',   32000, 2),
(3,  4, 'Thăn bò Úc',  119000, 1),
(3, 14, 'Khoai tây',          32000, 1),
(3, 15, 'Cà rốt',          22000, 1),
(4,  8, 'Trứng gà ta',  38000, 1),
(4, 10, 'Rau muống',     12000, 2),
(4, 30, 'Nước suối',   5000, 6),
(5, 32, 'Bánh quy bơ',    65000, 1);

INSERT INTO order_tracking (order_id, status, note, created_at) VALUES
(1, 'PENDING',   'Đơn hàng đã được đặt',              '2026-10-19 20:15'),
(1, 'CONFIRMED', 'Cửa hàng đã xác nhận',              '2026-10-19 20:30'),
(1, 'PACKING',   'Đang soạn hàng',                    '2026-10-20 07:10'),
(1, 'SHIPPING',  'Nhân viên giao hàng đang trên đường', '2026-10-20 08:05'),
(1, 'DELIVERED', 'Giao hàng thành công',              '2026-10-20 08:50'),
(2, 'PENDING',   'Đơn hàng đã được đặt',              '2026-10-25 09:40'),
(2, 'CONFIRMED', 'Cửa hàng đã xác nhận',              '2026-10-25 10:00'),
(2, 'PACKING',   'Đang soạn hàng',                    '2026-10-25 17:20'),
(2, 'SHIPPING',  'Nhân viên giao hàng đang trên đường', '2026-10-25 18:05'),
(2, 'DELIVERED', 'Giao hàng thành công, đã thu tiền',  '2026-10-25 18:45'),
(3, 'PENDING',   'Đơn hàng đã được đặt',              '2026-11-01 10:05'),
(3, 'CONFIRMED', 'Cửa hàng đã xác nhận',              '2026-11-01 10:20'),
(3, 'PACKING',   'Đang soạn hàng',                    '2026-11-01 15:10'),
(3, 'SHIPPING',  'Nhân viên giao hàng đang trên đường', '2026-11-01 16:02'),
(4, 'PENDING',   'Đơn hàng đã được đặt',              '2026-11-01 17:30'),
(5, 'PENDING',   'Đơn hàng đã được đặt',              '2026-10-27 21:00'),
(5, 'CANCELLED', 'Khách hủy đơn, đã hoàn tiền ZaloPay', '2026-10-28 07:45');

-- Reviews (only for products in DELIVERED orders)
INSERT INTO reviews (product_id, user_id, rating, comment, created_at) VALUES
( 1, 2, 5, 'Thịt tươi, nạc mỡ đều, kho rất ngon.',       '2026-10-21 12:00'),
(13, 2, 4, 'Cà chua chín đều, vài quả hơi mềm.',          '2026-10-21 12:05'),
(21, 2, 5, 'Gạo thơm, dẻo, sẽ mua lại.',                  '2026-10-21 12:10'),
( 7, 3, 4, 'Tôm còn tươi, size đúng mô tả.',              '2026-10-26 08:30'),
(29, 3, 5, 'Sữa ngon, giao nhanh.',                       '2026-10-26 08:35');

-- Recipes (ids 1-4) and ingredients
INSERT INTO recipes (title, description, instructions, cook_time, servings) VALUES
('Thịt kho trứng', 'Món kho truyền thống ngày Tết, đậm đà, ăn với cơm trắng.',
'1. Thịt ba rọi rửa sạch, cắt miếng vuông, ướp nước mắm, đường, hành tím 30 phút.
2. Trứng vịt luộc chín, bóc vỏ.
3. Thắng đường thành nước màu, cho thịt vào xào săn.
4. Đổ nước dừa ngập thịt, thêm trứng, kho lửa nhỏ khoảng 1 giờ đến khi thịt mềm.', 90, 4),
('Canh chua cá basa', 'Canh chua miền Tây chua ngọt, thanh mát.',
'1. Cá basa cắt khúc, ướp chút nước mắm.
2. Nấu nước sôi, cho me vào dầm lấy nước chua.
3. Cho cà chua, cá vào nấu chín, nêm nước mắm, đường cho vừa.
4. Tắt bếp, thêm bạc hà, hành lá.', 30, 4),
('Bò xào nấm kim châm', 'Món xào nhanh, giàu đạm, hợp bữa tối.',
'1. Thăn bò thái mỏng, ướp nước tương và chút dầu ăn 15 phút.
2. Nấm kim châm cắt gốc, tách sợi.
3. Phi thơm dầu, xào bò lửa lớn đến khi tái thì vớt ra.
4. Xào nấm chín tới, cho bò vào đảo đều, thêm hành lá.', 20, 2),
('Rau muống xào tỏi', 'Món rau quen thuộc, giòn xanh.',
'1. Rau muống nhặt, rửa sạch, để ráo.
2. Phi thơm tỏi với dầu ăn.
3. Cho rau vào xào lửa lớn, nêm nước mắm, đảo nhanh rồi tắt bếp.', 10, 3);

INSERT INTO recipe_ingredients (recipe_id, name, amount, product_id) VALUES
(1, 'Ba rọi heo',      '500g',          1),
(1, 'Trứng vịt',       '6 quả',         9),
(1, 'Nước mắm',        '3 muỗng canh', 25),
(1, 'Đường',           '2 muỗng canh', 27),
(1, 'Hành tím',        '3 củ',         16),
(1, 'Nước dừa tươi',   '400ml',      NULL),
(2, 'Cá basa phi lê',  '500g',          6),
(2, 'Cà chua',         '2 quả',        13),
(2, 'Hành lá',         '2 nhánh',      12),
(2, 'Nước mắm',        '2 muỗng canh', 25),
(2, 'Đường',           '1 muỗng canh', 27),
(2, 'Me chua',         '30g',        NULL),
(2, 'Bạc hà',          '1 cây',      NULL),
(3, 'Thăn bò Úc',      '250g',          4),
(3, 'Nấm kim châm',    '2 gói',        17),
(3, 'Hành lá',         '2 nhánh',      12),
(3, 'Dầu ăn',          '2 muỗng canh', 24),
(3, 'Nước tương',      '1 muỗng canh', 26),
(4, 'Rau muống',       '1 bó',         10),
(4, 'Tỏi',             '5 tép',      NULL),
(4, 'Dầu ăn',          '2 muỗng canh', 24),
(4, 'Nước mắm',        '1 muỗng canh', 25);
