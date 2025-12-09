-- V6__test_orders_data.sql
-- Test data: 25 orders với dữ liệu hợp lệ cho báo cáo

BEGIN;

-- ============================================================================
-- 1. TẠO THÊM USERS TEST
-- ============================================================================
INSERT INTO users (id, email, email_verified_at, password_hash, full_name, phone, status, role, gender, created_at, updated_at) 
VALUES
('30000000-0000-0000-0000-000000000001', 'khach1@test.com', now(), '$2a$10$dummyHash1', 'Nguyễn Văn Khách 1', '0987654321', 'ACTIVE', 'CUSTOMER', 'M', now() - interval '60 days', now()),
('30000000-0000-0000-0000-000000000002', 'khach2@test.com', now(), '$2a$10$dummyHash2', 'Trần Thị Khách 2', '0987654322', 'ACTIVE', 'CUSTOMER', 'F', now() - interval '50 days', now()),
('30000000-0000-0000-0000-000000000003', 'khach3@test.com', now(), '$2a$10$dummyHash3', 'Lê Văn Khách 3', '0987654323', 'ACTIVE', 'CUSTOMER', 'M', now() - interval '40 days', now()),
('30000000-0000-0000-0000-000000000004', 'khach4@test.com', now(), '$2a$10$dummyHash4', 'Phạm Thị Khách 4', '0987654324', 'ACTIVE', 'CUSTOMER', 'F', now() - interval '30 days', now()),
('30000000-0000-0000-0000-000000000005', 'khach5@test.com', now(), '$2a$10$dummyHash5', 'Hoàng Văn Khách 5', '0987654325', 'ACTIVE', 'CUSTOMER', 'M', now() - interval '20 days', now())
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- 2. TẠO 25 ĐƠN HÀNG (THÁNG 10-11-12/2024)
-- ============================================================================

-- Orders 1-5: THÁNG 10/2024 (45-38 ngày trước)
INSERT INTO orders (id, order_number, user_id, status, previous_status, subtotal_amount, discount_amount, shipping_amount, total_amount, shipping_address, placed_at, paid_at, version, created_at, updated_at) VALUES
('40000001-0000-0000-0000-000000000001', 'ORD-2024-1001', '30000000-0000-0000-0000-000000000001', 'DELIVERED', 'SHIPPED', 750000, 0, 30000, 780000, 
 '{"fullName":"Nguyễn Văn Khách 1","phone":"0987654321","province":"TP Hồ Chí Minh","district":"Quận 1","ward":"Phường Bến Nghé","line1":"123 Đường Lê Lợi"}'::jsonb,
 now() - interval '45 days', now() - interval '45 days', 1, now() - interval '45 days', now() - interval '40 days'),
 
('40000002-0000-0000-0000-000000000002', 'ORD-2024-1002', '30000000-0000-0000-0000-000000000002', 'DELIVERED', 'SHIPPED', 1200000, 100000, 30000, 1130000,
 '{"fullName":"Trần Thị Khách 2","phone":"0987654322","province":"Hà Nội","district":"Quận Ba Đình","ward":"Phường Điện Biên","line1":"456 Đường Láng"}'::jsonb,
 now() - interval '43 days', now() - interval '43 days', 1, now() - interval '43 days', now() - interval '38 days'),

('40000003-0000-0000-0000-000000000003', 'ORD-2024-1003', '30000000-0000-0000-0000-000000000003', 'CANCELLED', 'PENDING', 500000, 0, 30000, 530000,
 '{"fullName":"Lê Văn Khách 3","phone":"0987654323","province":"Đà Nẵng","district":"Quận Hải Châu","ward":"Phường Hải Châu 1","line1":"789 Đường Trần Phú"}'::jsonb,
 now() - interval '42 days', NULL, 1, now() - interval '42 days', now() - interval '42 days'),

('40000004-0000-0000-0000-000000000004', 'ORD-2024-1004', '30000000-0000-0000-0000-000000000004', 'DELIVERED', 'SHIPPED', 2000000, 200000, 30000, 1830000,
 '{"fullName":"Phạm Thị Khách 4","phone":"0987654324","province":"TP Hồ Chí Minh","district":"Quận 3","ward":"Phường 1","line1":"111 Đường Võ Văn Tần"}'::jsonb,
 now() - interval '40 days', now() - interval '40 days', 1, now() - interval '40 days', now() - interval '35 days'),

('40000005-0000-0000-0000-000000000005', 'ORD-2024-1005', '30000000-0000-0000-0000-000000000005', 'DELIVERED', 'SHIPPED', 450000, 50000, 30000, 430000,
 '{"fullName":"Hoàng Văn Khách 5","phone":"0987654325","province":"Cần Thơ","district":"Quận Ninh Kiều","ward":"Phường Tân An","line1":"222 Đường 30/4"}'::jsonb,
 now() - interval '38 days', now() - interval '38 days', 1, now() - interval '38 days', now() - interval '33 days');

-- Orders 6-10: THÁNG 11/2024 (35-28 ngày trước)
INSERT INTO orders (id, order_number, user_id, status, previous_status, subtotal_amount, discount_amount, shipping_amount, total_amount, shipping_address, placed_at, paid_at, version, created_at, updated_at) VALUES
('40000006-0000-0000-0000-000000000006', 'ORD-2024-1006', '30000000-0000-0000-0000-000000000001', 'DELIVERED', 'SHIPPED', 900000, 0, 35000, 935000,
 '{"fullName":"Nguyễn Văn Khách 1","phone":"0987654321","province":"TP Hồ Chí Minh","district":"Quận 1","ward":"Phường Bến Nghé","line1":"123 Đường Lê Lợi"}'::jsonb,
 now() - interval '35 days', now() - interval '35 days', 1, now() - interval '35 days', now() - interval '30 days'),

('40000007-0000-0000-0000-000000000007', 'ORD-2024-1007', '30000000-0000-0000-0000-000000000002', 'DELIVERED', 'SHIPPED', 1500000, 150000, 30000, 1380000,
 '{"fullName":"Trần Thị Khách 2","phone":"0987654322","province":"Hà Nội","district":"Quận Ba Đình","ward":"Phường Điện Biên","line1":"456 Đường Láng"}'::jsonb,
 now() - interval '33 days', now() - interval '33 days', 1, now() - interval '33 days', now() - interval '28 days'),

('40000008-0000-0000-0000-000000000008', 'ORD-2024-1008', '30000000-0000-0000-0000-000000000003', 'RETURNED', 'DELIVERED', 800000, 0, 30000, 830000,
 '{"fullName":"Lê Văn Khách 3","phone":"0987654323","province":"Đà Nẵng","district":"Quận Hải Châu","ward":"Phường Hải Châu 1","line1":"789 Đường Trần Phú"}'::jsonb,
 now() - interval '32 days', now() - interval '32 days', 1, now() - interval '32 days', now() - interval '27 days'),

('40000009-0000-0000-0000-000000000009', 'ORD-2024-1009', '30000000-0000-0000-0000-000000000004', 'DELIVERED', 'SHIPPED', 2500000, 250000, 35000, 2285000,
 '{"fullName":"Phạm Thị Khách 4","phone":"0987654324","province":"TP Hồ Chí Minh","district":"Quận 3","ward":"Phường 1","line1":"111 Đường Võ Văn Tần"}'::jsonb,
 now() - interval '30 days', now() - interval '30 days', 1, now() - interval '30 days', now() - interval '25 days'),

('40000010-0000-0000-0000-000000000010', 'ORD-2024-1010', '30000000-0000-0000-0000-000000000005', 'DELIVERED', 'SHIPPED', 1200000, 100000, 30000, 1130000,
 '{"fullName":"Hoàng Văn Khách 5","phone":"0987654325","province":"Cần Thơ","district":"Quận Ninh Kiều","ward":"Phường Tân An","line1":"222 Đường 30/4"}'::jsonb,
 now() - interval '28 days', now() - interval '28 days', 1, now() - interval '28 days', now() - interval '23 days');

-- Orders 11-25: THÁNG 12/2024 (25 ngày - hôm nay)
INSERT INTO orders (id, order_number, user_id, status, previous_status, subtotal_amount, discount_amount, shipping_amount, total_amount, shipping_address, placed_at, paid_at, version, created_at, updated_at) VALUES
('40000011-0000-0000-0000-000000000011', 'ORD-2024-1011', '30000000-0000-0000-0000-000000000001', 'DELIVERED', 'SHIPPED', 3000000, 300000, 40000, 2740000,
 '{"fullName":"Nguyễn Văn Khách 1","phone":"0987654321","province":"TP Hồ Chí Minh","district":"Quận 1","ward":"Phường Bến Nghé","line1":"123 Đường Lê Lợi"}'::jsonb,
 now() - interval '25 days', now() - interval '25 days', 1, now() - interval '25 days', now() - interval '20 days'),

('40000012-0000-0000-0000-000000000012', 'ORD-2024-1012', '30000000-0000-0000-0000-000000000002', 'DELIVERED', 'SHIPPED', 650000, 50000, 30000, 630000,
 '{"fullName":"Trần Thị Khách 2","phone":"0987654322","province":"Hà Nội","district":"Quận Ba Đình","ward":"Phường Điện Biên","line1":"456 Đường Láng"}'::jsonb,
 now() - interval '23 days', now() - interval '23 days', 1, now() - interval '23 days', now() - interval '18 days'),

('40000013-0000-0000-0000-000000000013', 'ORD-2024-1013', '30000000-0000-0000-0000-000000000003', 'DELIVERED', 'SHIPPED', 1800000, 180000, 35000, 1655000,
 '{"fullName":"Lê Văn Khách 3","phone":"0987654323","province":"Đà Nẵng","district":"Quận Hải Châu","ward":"Phường Hải Châu 1","line1":"789 Đường Trần Phú"}'::jsonb,
 now() - interval '20 days', now() - interval '20 days', 1, now() - interval '20 days', now() - interval '15 days'),

('40000014-0000-0000-0000-000000000014', 'ORD-2024-1014', '30000000-0000-0000-0000-000000000004', 'DELIVERED', 'SHIPPED', 4000000, 400000, 40000, 3640000,
 '{"fullName":"Phạm Thị Khách 4","phone":"0987654324","province":"TP Hồ Chí Minh","district":"Quận 3","ward":"Phường 1","line1":"111 Đường Võ Văn Tần"}'::jsonb,
 now() - interval '18 days', now() - interval '18 days', 1, now() - interval '18 days', now() - interval '13 days'),

('40000015-0000-0000-0000-000000000015', 'ORD-2024-1015', '30000000-0000-0000-0000-000000000005', 'DELIVERED', 'SHIPPED', 550000, 0, 30000, 580000,
 '{"fullName":"Hoàng Văn Khách 5","phone":"0987654325","province":"Cần Thơ","district":"Quận Ninh Kiều","ward":"Phường Tân An","line1":"222 Đường 30/4"}'::jsonb,
 now() - interval '15 days', now() - interval '15 days', 1, now() - interval '15 days', now() - interval '10 days'),

('40000016-0000-0000-0000-000000000016', 'ORD-2024-1016', '30000000-0000-0000-0000-000000000001', 'DELIVERED', 'SHIPPED', 2200000, 200000, 35000, 2035000,
 '{"fullName":"Nguyễn Văn Khách 1","phone":"0987654321","province":"TP Hồ Chí Minh","district":"Quận 1","ward":"Phường Bến Nghé","line1":"123 Đường Lê Lợi"}'::jsonb,
 now() - interval '13 days', now() - interval '13 days', 1, now() - interval '13 days', now() - interval '8 days'),

('40000017-0000-0000-0000-000000000017', 'ORD-2024-1017', '30000000-0000-0000-0000-000000000002', 'SHIPPED', 'PROCESSING', 1300000, 100000, 30000, 1230000,
 '{"fullName":"Trần Thị Khách 2","phone":"0987654322","province":"Hà Nội","district":"Quận Ba Đình","ward":"Phường Điện Biên","line1":"456 Đường Láng"}'::jsonb,
 now() - interval '10 days', now() - interval '10 days', 1, now() - interval '10 days', now() - interval '8 days'),

('40000018-0000-0000-0000-000000000018', 'ORD-2024-1018', '30000000-0000-0000-0000-000000000003', 'PROCESSING', 'CONFIRMED', 750000, 50000, 30000, 730000,
 '{"fullName":"Lê Văn Khách 3","phone":"0987654323","province":"Đà Nẵng","district":"Quận Hải Châu","ward":"Phường Hải Châu 1","line1":"789 Đường Trần Phú"}'::jsonb,
 now() - interval '8 days', now() - interval '8 days', 1, now() - interval '8 days', now() - interval '7 days'),

('40000019-0000-0000-0000-000000000019', 'ORD-2024-1019', '30000000-0000-0000-0000-000000000004', 'DELIVERED', 'SHIPPED', 3500000, 350000, 40000, 3190000,
 '{"fullName":"Phạm Thị Khách 4","phone":"0987654324","province":"TP Hồ Chí Minh","district":"Quận 3","ward":"Phường 1","line1":"111 Đường Võ Văn Tần"}'::jsonb,
 now() - interval '7 days', now() - interval '7 days', 1, now() - interval '7 days', now() - interval '3 days'),

('40000020-0000-0000-0000-000000000020', 'ORD-2024-1020', '30000000-0000-0000-0000-000000000005', 'DELIVERED', 'SHIPPED', 980000, 80000, 30000, 930000,
 '{"fullName":"Hoàng Văn Khách 5","phone":"0987654325","province":"Cần Thơ","district":"Quận Ninh Kiều","ward":"Phường Tân An","line1":"222 Đường 30/4"}'::jsonb,
 now() - interval '5 days', now() - interval '5 days', 1, now() - interval '5 days', now() - interval '2 days'),

('40000021-0000-0000-0000-000000000021', 'ORD-2024-1021', '30000000-0000-0000-0000-000000000001', 'CONFIRMED', 'PENDING', 1500000, 150000, 35000, 1385000,
 '{"fullName":"Nguyễn Văn Khách 1","phone":"0987654321","province":"TP Hồ Chí Minh","district":"Quận 1","ward":"Phường Bến Nghé","line1":"123 Đường Lê Lợi"}'::jsonb,
 now() - interval '4 days', now() - interval '4 days', 1, now() - interval '4 days', now() - interval '3 days'),

('40000022-0000-0000-0000-000000000022', 'ORD-2024-1022', '30000000-0000-0000-0000-000000000002', 'DELIVERED', 'SHIPPED', 2800000, 280000, 40000, 2560000,
 '{"fullName":"Trần Thị Khách 2","phone":"0987654322","province":"Hà Nội","district":"Quận Ba Đình","ward":"Phường Điện Biên","line1":"456 Đường Láng"}'::jsonb,
 now() - interval '3 days', now() - interval '3 days', 1, now() - interval '3 days', now() - interval '1 day'),

('40000023-0000-0000-0000-000000000023', 'ORD-2024-1023', '30000000-0000-0000-0000-000000000003', 'PENDING', 'PENDING', 450000, 0, 30000, 480000,
 '{"fullName":"Lê Văn Khách 3","phone":"0987654323","province":"Đà Nẵng","district":"Quận Hải Châu","ward":"Phường Hải Châu 1","line1":"789 Đường Trần Phú"}'::jsonb,
 now() - interval '2 days', NULL, 1, now() - interval '2 days', now() - interval '2 days'),

('40000024-0000-0000-0000-000000000024', 'ORD-2024-1024', '30000000-0000-0000-0000-000000000004', 'PENDING', 'PENDING', 1200000, 100000, 30000, 1130000,
 '{"fullName":"Phạm Thị Khách 4","phone":"0987654324","province":"TP Hồ Chí Minh","district":"Quận 3","ward":"Phường 1","line1":"111 Đường Võ Văn Tần"}'::jsonb,
 now() - interval '1 day', NULL, 1, now() - interval '1 day', now() - interval '1 day'),

('40000025-0000-0000-0000-000000000025', 'ORD-2024-1025', '30000000-0000-0000-0000-000000000005', 'PENDING', 'PENDING', 850000, 50000, 30000, 830000,
 '{"fullName":"Hoàng Văn Khách 5","phone":"0987654325","province":"Cần Thơ","district":"Quận Ninh Kiều","ward":"Phường Tân An","line1":"222 Đường 30/4"}'::jsonb,
 now(), NULL, 1, now(), now());

-- ============================================================================
-- 3. TẠO ORDER ITEMS
-- ============================================================================
DO $$
DECLARE
    v_product_1 UUID;
    v_variant_1 UUID;
BEGIN
    -- Lấy product và variant đầu tiên
    SELECT id INTO v_product_1 FROM products LIMIT 1;
    SELECT id INTO v_variant_1 FROM product_variants WHERE product_id = v_product_1 LIMIT 1;

    -- Tạo order items cho 25 đơn hàng
    INSERT INTO order_items (id, order_id, product_id, variant_id, sku, product_name, variant_name, quantity, unit_price_amount, history_cost, total_amount, created_at)
    SELECT 
        gen_random_uuid(),
        o.id,
        v_product_1,
        v_variant_1,
        'SKU-' || o.order_number,
        'Sản phẩm test',
        'Black / M',
        CASE 
            WHEN o.subtotal_amount < 1000000 THEN 2
            WHEN o.subtotal_amount < 2000000 THEN 3
            ELSE 5
        END,
        o.subtotal_amount / CASE 
            WHEN o.subtotal_amount < 1000000 THEN 2
            WHEN o.subtotal_amount < 2000000 THEN 3
            ELSE 5
        END,
        (o.subtotal_amount / CASE 
            WHEN o.subtotal_amount < 1000000 THEN 2
            WHEN o.subtotal_amount < 2000000 THEN 3
            ELSE 5
        END) * 0.6,
        o.subtotal_amount,
        o.created_at
    FROM orders o
    WHERE o.order_number LIKE 'ORD-2024-10%';
END $$;

-- ============================================================================
-- 4. TẠO PAYMENTS
-- ============================================================================
INSERT INTO payments (id, order_id, provider, status, amount, transaction_id, raw_response, created_at, updated_at)
SELECT 
    gen_random_uuid(),
    id,
    CASE 
        WHEN random() < 0.5 THEN 'VNPAY'
        WHEN random() < 0.8 THEN 'MOMO'
        ELSE 'COD'
    END,
    CASE 
        WHEN status IN ('DELIVERED', 'SHIPPED', 'PROCESSING', 'CONFIRMED', 'RETURNED') THEN 'CAPTURED'
        WHEN status = 'PENDING' THEN 'PENDING'
        ELSE 'FAILED'
    END,
    total_amount,
    'TXN-' || order_number,
    '{}'::jsonb,
    created_at,
    updated_at
FROM orders
WHERE order_number LIKE 'ORD-2024-10%';

-- ============================================================================
-- 5. TẠO SHIPMENTS
-- ============================================================================
INSERT INTO shipments (id, order_id, carrier, tracking_number, status, shipped_at, delivered_at, is_active, created_at, updated_at)
SELECT 
    gen_random_uuid(),
    id,
    CASE 
        WHEN random() < 0.6 THEN 'GHN'
        WHEN random() < 0.9 THEN 'GHTK'
        ELSE 'VNPost'
    END,
    'TRACK-' || order_number,
    CASE 
        WHEN status = 'DELIVERED' THEN 'DELIVERED'
        WHEN status = 'SHIPPED' THEN 'IN_TRANSIT'
        ELSE 'PENDING'
    END,
    CASE WHEN status IN ('SHIPPED', 'DELIVERED', 'RETURNED') THEN created_at + interval '1 day' ELSE NULL END,
    CASE WHEN status = 'DELIVERED' THEN updated_at ELSE NULL END,
    true,
    created_at,
    updated_at
FROM orders
WHERE status IN ('SHIPPED', 'DELIVERED', 'RETURNED')
AND order_number LIKE 'ORD-2024-10%';

COMMIT;
