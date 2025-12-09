-- V5__more_test_data.sql
-- Additional test data: Banners, Discounts, More Users, and Historical Orders

BEGIN;

-- ============================================================================
-- 1. BANNERS
-- ============================================================================
INSERT INTO banners (id, title, image_url, link_url, description, display_order, is_active, start_date, end_date, created_at, updated_at) VALUES
(gen_random_uuid(), 'Summer Sale 2025', 'https://res.cloudinary.com/dyeqnh68q/image/upload/v1765288942/banners/misc/kwgpozculdw34bkqrxrm.jpg', '/products?category=giay-the-thao', 'Up to 50% off on selected items', 4, true, now(), now() + interval '30 days', now(), now()),
(gen_random_uuid(), 'New Collection Arrival', 'https://res.cloudinary.com/dyeqnh68q/image/upload/v1765288964/banners/misc/qvenvga4oxqjgs1itxxo.webp', '/products?sort=newest', 'Check out the latest trends', 5, true, now(), now() + interval '60 days', now(), now()),
(gen_random_uuid(), 'Free Shipping', 'https://res.cloudinary.com/dyeqnh68q/image/upload/v1765288976/banners/misc/ytramrd8wuwnvuyj951w.jpg', '/policy/shipping', 'Free shipping on orders over 1.000.000đ', 6, true, now(), now() + interval '90 days', now(), now());

-- ============================================================================
-- 2. DISCOUNTS
-- ============================================================================
INSERT INTO discounts (id, code, name, description, type, value, starts_at, ends_at, max_redemptions, per_user_limit, min_order_amount, active, created_at, updated_at) VALUES
(gen_random_uuid(), 'WELCOME10', 'Welcome Discount', '10% off for new customers', 'PERCENTAGE', 10, now(), now() + interval '1 year', 1000, 1, 0, true, now(), now()),
(gen_random_uuid(), 'SUMMER25', 'Summer Sale', '25% off summer collection', 'PERCENTAGE', 25, now(), now() + interval '3 months', 500, 2, 500000, true, now(), now()),
(gen_random_uuid(), 'SAVE50K', 'Save 50k', '50.000đ off on orders over 1M', 'FIXED_AMOUNT', 50000, now(), now() + interval '6 months', 200, 5, 1000000, true, now(), now()),
(gen_random_uuid(), 'FREESHIP', 'Free Shipping', 'Free shipping voucher', 'FIXED_AMOUNT', 30000, now(), now() + interval '1 month', 100, 1, 300000, true, now(), now());

-- ============================================================================
-- 3. MORE USERS
-- ============================================================================
INSERT INTO users (id, email, email_verified_at, password_hash, full_name, phone, status, role, gender, created_at, updated_at) VALUES
('20000000-0000-0000-0000-000000000004', 'customer4@example.com', now(), '$2a$10$JWRhvnnTHO1yPZnO0YDKiOELcORAZwXr.dAcqcHrFsOHBP4RihQXu', 'Phạm Văn D', '0934567890', 'ACTIVE', 'CUSTOMER', 'M', now(), now()),
('20000000-0000-0000-0000-000000000005', 'customer5@example.com', now(), '$2a$10$JWRhvnnTHO1yPZnO0YDKiOELcORAZwXr.dAcqcHrFsOHBP4RihQXu', 'Hoàng Thị E', '0945678901', 'ACTIVE', 'CUSTOMER', 'F', now(), now()),
('20000000-0000-0000-0000-000000000006', 'customer6@example.com', now(), '$2a$10$JWRhvnnTHO1yPZnO0YDKiOELcORAZwXr.dAcqcHrFsOHBP4RihQXu', 'Vũ Văn F', '0956789012', 'ACTIVE', 'CUSTOMER', 'M', now(), now()),
('20000000-0000-0000-0000-000000000007', 'customer7@example.com', now(), '$2a$10$JWRhvnnTHO1yPZnO0YDKiOELcORAZwXr.dAcqcHrFsOHBP4RihQXu', 'Đặng Thị G', '0967890123', 'ACTIVE', 'CUSTOMER', 'F', now(), now()),
('20000000-0000-0000-0000-000000000008', 'customer8@example.com', now(), '$2a$10$JWRhvnnTHO1yPZnO0YDKiOELcORAZwXr.dAcqcHrFsOHBP4RihQXu', 'Bùi Văn H', '0978901234', 'ACTIVE', 'CUSTOMER', 'M', now(), now());

INSERT INTO addresses (id, user_id, full_name, phone, province, district, ward, line1, is_default_shipping, created_at, updated_at) VALUES
(gen_random_uuid(), '20000000-0000-0000-0000-000000000004', 'Phạm Văn D', '0934567890', 'Điện Biên', 'Huyện Điện Biên', 'Xã Phu Luông', '123 Xuân Thủy', true, now(), now()),
(gen_random_uuid(), '20000000-0000-0000-0000-000000000005', 'Hoàng Thị E', '0945678901', 'Điện Biên', 'Huyện Điện Biên', 'Xã Phu Luông', '456 Điện Biên Phủ', true, now(), now());

-- ============================================================================
-- 4. HISTORICAL ORDERS (Generate ~30 orders)
-- ============================================================================
DO $$
DECLARE
    user_ids UUID[] := ARRAY[
        '20000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', 
        '20000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000004', 
        '20000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000006'
    ];
    order_statuses TEXT[] := ARRAY['DELIVERED', 'DELIVERED', 'DELIVERED', 'SHIPPED', 'PROCESSING', 'CONFIRMED', 'CANCELLED'];
    payment_methods TEXT[] := ARRAY['VNPAY', 'COD'];
    
    selected_user UUID;
    selected_status TEXT;
    selected_payment TEXT;
    
    order_id UUID;
    order_date TIMESTAMP;
    random_address TEXT;
    
    prod_rec RECORD;
    variant_rec RECORD;
    variant_id UUID;
    
    subtotal BIGINT;
    shipping_fee BIGINT;
    total BIGINT;
    
    i INT;
    j INT;
    num_items INT;
BEGIN
    -- Loop to create 30 orders
    FOR i IN 1..30 LOOP
        -- Random selection
        selected_user := user_ids[1 + floor(random() * array_length(user_ids, 1))::int];
        selected_status := order_statuses[1 + floor(random() * array_length(order_statuses, 1))::int];
        selected_payment := payment_methods[1 + floor(random() * array_length(payment_methods, 1))::int];
        
        -- Random date in last 30 days
        order_date := now() - (floor(random() * 30) || ' days')::interval - (floor(random() * 24) || ' hours')::interval;
        
        order_id := gen_random_uuid();
        shipping_fee := 30000;
        subtotal := 0;
        
        -- Generate random but valid Vietnamese address (all Điện Biên - Huyện Điện Biên - Xã Phu Luông)
        random_address := CASE floor(random() * 5)::int
            WHEN 0 THEN '{"fullName":"Nguyễn Văn Tự Động ' || i || '","phone":"090' || LPAD(floor(random()*10000000)::text, 7, '0') || '","province":"Điện Biên","district":"Huyện Điện Biên","ward":"Xã Phu Luông","line1":"' || floor(random()*500)::text || ' Nguyễn Huệ"}'
            WHEN 1 THEN '{"fullName":"Trần Thị Tự Động ' || i || '","phone":"091' || LPAD(floor(random()*10000000)::text, 7, '0') || '","province":"Điện Biên","district":"Huyện Điện Biên","ward":"Xã Phu Luông","line1":"' || floor(random()*300)::text || ' Hoàng Diệu"}'
            WHEN 2 THEN '{"fullName":"Lê Văn Tự Động ' || i || '","phone":"092' || LPAD(floor(random()*10000000)::text, 7, '0') || '","province":"Điện Biên","district":"Huyện Điện Biên","ward":"Xã Phu Luông","line1":"' || floor(random()*200)::text || ' Trần Phú"}'
            WHEN 3 THEN '{"fullName":"Phạm Thị Tự Động ' || i || '","phone":"093' || LPAD(floor(random()*10000000)::text, 7, '0') || '","province":"Điện Biên","district":"Huyện Điện Biên","ward":"Xã Phu Luông","line1":"' || floor(random()*400)::text || ' Đường 30/4"}'
            ELSE '{"fullName":"Hoàng Văn Tự Động ' || i || '","phone":"094' || LPAD(floor(random()*10000000)::text, 7, '0') || '","province":"Điện Biên","district":"Huyện Điện Biên","ward":"Xã Phu Luông","line1":"' || floor(random()*250)::text || ' Võ Văn Tần"}'
        END;
        
        -- Create Order Shell first (amounts updated later)
        INSERT INTO orders (id, order_number, user_id, status, previous_status, subtotal_amount, discount_amount, shipping_amount, total_amount, shipping_address, placed_at, paid_at, version, created_at, updated_at)
        VALUES (
            order_id, 
            'ORD-AUTO-' || i || '-' || floor(random()*1000)::text, 
            selected_user, 
            selected_status, 
            CASE WHEN selected_status = 'DELIVERED' THEN 'SHIPPED' ELSE 'PENDING' END,
            0, 0, shipping_fee, 0, 
            random_address::jsonb, 
            order_date, 
            CASE WHEN selected_status IN ('DELIVERED', 'SHIPPED', 'PROCESSING', 'CONFIRMED') AND selected_payment = 'VNPAY' THEN order_date + interval '5 minutes' ELSE NULL END,
            1, order_date, order_date
        );
        
        -- Add 1-3 items per order
        num_items := 1 + floor(random() * 3)::int;
        
        FOR j IN 1..num_items LOOP
            -- Pick a random product (p1 to p30)
            -- We know IDs are p0000001... to p0000030...
            -- Let's just pick a random one from the products table to be safe
            SELECT id, name, price INTO prod_rec FROM products ORDER BY random() LIMIT 1;
            
            -- Get a variant for this product with history_cost
            SELECT id, history_cost INTO variant_rec FROM product_variants WHERE product_id = prod_rec.id LIMIT 1;
            
            IF variant_rec.id IS NOT NULL THEN
                INSERT INTO order_items (id, order_id, product_id, variant_id, sku, product_name, variant_name, quantity, unit_price_amount, total_amount, history_cost, created_at)
                VALUES (
                    gen_random_uuid(), 
                    order_id, 
                    prod_rec.id, 
                    variant_rec.id, 
                    'SKU-' || floor(random()*10000), 
                    prod_rec.name, 
                    'Default Variant', 
                    1, 
                    prod_rec.price, 
                    prod_rec.price,
                    COALESCE(variant_rec.history_cost, prod_rec.price * 0.6), -- Use variant cost or default to 60% of price
                    order_date
                );
                
                subtotal := subtotal + prod_rec.price;
            END IF;
        END LOOP;
        
        total := subtotal + shipping_fee;
        
        -- Update Order Totals
        UPDATE orders SET subtotal_amount = subtotal, total_amount = total WHERE id = order_id;
        
        -- Create Payment
        INSERT INTO payments (id, order_id, provider, status, amount, transaction_id, raw_response, created_at)
        VALUES (
            gen_random_uuid(), 
            order_id, 
            selected_payment, 
            CASE WHEN selected_status IN ('DELIVERED', 'SHIPPED', 'PROCESSING', 'CONFIRMED') AND selected_payment = 'VNPAY' THEN 'CAPTURED' ELSE 'PENDING' END,
            total, 
            'TRANS-' || floor(random()*100000), 
            '{}', 
            order_date
        );
        
        -- Create Shipment if applicable
        IF selected_status IN ('DELIVERED', 'SHIPPED') THEN
            INSERT INTO shipments (id, order_id, carrier, tracking_number, status, shipped_at, delivered_at, is_active, created_at)
            VALUES (
                gen_random_uuid(), 
                order_id, 
                'GHN', 
                'GHN-' || floor(random()*100000), 
                selected_status, 
                order_date + interval '1 day', 
                CASE WHEN selected_status = 'DELIVERED' THEN order_date + interval '3 days' ELSE NULL END, 
                true, 
                order_date
            );
        END IF;
        
    END LOOP;
END $$;

COMMIT;
