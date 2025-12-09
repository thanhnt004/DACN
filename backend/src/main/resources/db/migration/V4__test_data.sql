-- V4__test_data.sql
-- Comprehensive test data: 30+ Products, Brands, Categories, Orders

BEGIN;

-- ============================================================================
-- 1. BRANDS (10 Brands)
-- ============================================================================
INSERT INTO brands (id, name, slug, description, created_at, updated_at) VALUES
('11111111-1111-1111-1111-111111111111', 'Zara', 'zara', 'Fast fashion giant', now(), now()),
('22222222-2222-2222-2222-222222222222', 'H&M', 'hm', 'Fashion and quality at the best price', now(), now()),
('33333333-3333-3333-3333-333333333333', 'Uniqlo', 'uniqlo', 'LifeWear', now(), now()),
('44444444-4444-4444-4444-444444444444', 'Gucci', 'gucci', 'Luxury fashion', now(), now()),
('55555555-5555-5555-5555-555555555555', 'Louis Vuitton', 'louis-vuitton', 'French luxury fashion house', now(), now()),
('66666666-6666-6666-6666-666666666666', 'Chanel', 'chanel', 'High fashion', now(), now()),
('77777777-7777-7777-7777-777777777777', 'Levi''s', 'levis', 'Quality jeans', now(), now()),
('88888888-8888-8888-8888-888888888888', 'Calvin Klein', 'calvin-klein', 'Modern, minimal, bold', now(), now()),
('99999999-9999-9999-9999-999999999999', 'Ralph Lauren', 'ralph-lauren', 'American fashion', now(), now()),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Versace', 'versace', 'Italian luxury fashion', now(), now());

-- ============================================================================
-- 2. CATEGORIES (8 Categories)
-- ============================================================================
INSERT INTO categories (id, name, slug, parent_id, description, version, created_at, updated_at) VALUES
('c1111111-1111-1111-1111-111111111111', 'Quần Áo Nam', 'quan-ao-nam', NULL, 'Men''s Clothing', 1, now(), now()),
('c2222222-2222-2222-2222-222222222222', 'Quần Áo Nữ', 'quan-ao-nu', NULL, 'Women''s Clothing', 1, now(), now()),
('c3333333-3333-3333-3333-333333333333', 'Áo Thun', 'ao-thun', 'c1111111-1111-1111-1111-111111111111', 'T-Shirts', 1, now(), now()),
('c4444444-4444-4444-4444-444444444444', 'Áo Sơ Mi', 'ao-so-mi', 'c1111111-1111-1111-1111-111111111111', 'Shirts', 1, now(), now()),
('c5555555-5555-5555-5555-555555555555', 'Quần Jeans', 'quan-jeans', 'c1111111-1111-1111-1111-111111111111', 'Jeans', 1, now(), now()),
('c6666666-6666-6666-6666-666666666666', 'Váy Đầm', 'vay-dam', 'c2222222-2222-2222-2222-222222222222', 'Dresses', 1, now(), now()),
('c7777777-7777-7777-7777-777777777777', 'Áo Khoác', 'ao-khoac', NULL, 'Jackets & Coats', 1, now(), now()),
('c8888888-8888-8888-8888-888888888888', 'Đồ Thể Thao', 'do-the-thao', NULL, 'Sportswear', 1, now(), now());

-- ============================================================================
-- 3. COLORS & SIZES
-- ============================================================================
INSERT INTO colors (id, name, hex_code) VALUES
('10000000-0000-0000-0000-000000000001', 'Black', '#000000'),
('10000000-0000-0000-0000-000000000002', 'White', '#FFFFFF'),
('10000000-0000-0000-0000-000000000003', 'Red', '#FF0000'),
('10000000-0000-0000-0000-000000000004', 'Blue', '#0000FF'),
('10000000-0000-0000-0000-000000000005', 'Green', '#00FF00'),
('10000000-0000-0000-0000-000000000006', 'Grey', '#808080');

INSERT INTO sizes (id, code, name) VALUES
('51111111-1111-1111-1111-111111111111', 'XS', 'Extra Small'),
('52222222-2222-2222-2222-222222222222', 'S', 'Small'),
('53333333-3333-3333-3333-333333333333', 'M', 'Medium'),
('54444444-4444-4444-4444-444444444444', 'L', 'Large'),
('55555555-5555-5555-5555-555555555555', 'XL', 'Extra Large'),
('56666666-6666-6666-6666-666666666666', 'XXL', 'Double Extra Large');

-- ============================================================================
-- 4. USERS
-- ============================================================================
INSERT INTO users (id, email, email_verified_at, password_hash, full_name, phone, status, role, gender, created_at, updated_at) VALUES
('20000000-0000-0000-0000-000000000000', 'admin@example.com', now(), '$2a$10$JWRhvnnTHO1yPZnO0YDKiOELcORAZwXr.dAcqcHrFsOHBP4RihQXu', 'Administrator', '0326725877', 'ACTIVE', 'ADMIN', 'M', now(), now()),
('20000000-0000-0000-0000-000000000001', 'customer1@example.com', now(), '$2a$10$JWRhvnnTHO1yPZnO0YDKiOELcORAZwXr.dAcqcHrFsOHBP4RihQXu', 'Nguyễn Văn A', '0901234567', 'ACTIVE', 'CUSTOMER', 'M', now(), now()),
('20000000-0000-0000-0000-000000000002', 'customer2@example.com', now(), '$2a$10$JWRhvnnTHO1yPZnO0YDKiOELcORAZwXr.dAcqcHrFsOHBP4RihQXu', 'Trần Thị B', '0912345678', 'ACTIVE', 'CUSTOMER', 'F', now(), now()),
('20000000-0000-0000-0000-000000000003', 'customer3@example.com', now(), '$2a$10$JWRhvnnTHO1yPZnO0YDKiOELcORAZwXr.dAcqcHrFsOHBP4RihQXu', 'Lê Văn C', '0923456789', 'ACTIVE', 'CUSTOMER', 'M', now(), now());

INSERT INTO addresses (id, user_id, full_name, phone, province, district, ward, line1, is_default_shipping, created_at, updated_at) VALUES
(gen_random_uuid(), '20000000-0000-0000-0000-000000000001', 'Nguyễn Văn A', '0901234567', 'Hồ Chí Minh', 'Quận 1', 'Phường Bến Nghé', '123 Đường Lê Lợi', true, now(), now());

-- ============================================================================
-- 5. PRODUCTS (30 Products)
-- ============================================================================
-- Helper: 
-- Brands: 1=Nike, 2=Adidas, 3=Puma, 4=NB, 5=Reebok, 6=Converse, 7=Vans, 8=UA, 9=Asics, a=Fila
-- Cats: c2=Sport, c3=Run, c4=Life, c5=Basket, c6=Gym, c7=Tennis, c8=Sandal

INSERT INTO products (id, brand_id, name, slug, description, material, gender, status, price, primary_image_url, sold_count, total_stock, is_in_stock, version, created_at, updated_at) VALUES
-- Zara
('00000001-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'Áo Thun Basic Zara', 'ao-thun-basic-zara', 'Áo thun cotton đơn giản.', 'Cotton', 'men', 'ACTIVE', 250000, 'https://res.cloudinary.com/dyeqnh68q/image/upload/v1765288424/products/misc/images/n4q1no1ab5og2v44oj5w.jpg', 45, 80, true, 1, now(), now()),
('00000002-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'Quần Jeans Slim Fit Zara', 'quan-jeans-slim-fit-zara', 'Quần jeans dáng ôm.', 'Denim', 'men', 'ACTIVE', 750000, 'https://res.cloudinary.com/dyeqnh68q/image/upload/v1765288449/products/misc/images/dhfhfs2s9n1xi8myba2w.jpg', 30, 50, true, 1, now(), now()),
('00000003-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', 'Áo Khoác Bomber Zara', 'ao-khoac-bomber-zara', 'Áo khoác bomber sành điệu.', 'Polyester', 'men', 'ACTIVE', 1200000, 'https://res.cloudinary.com/dyeqnh68q/image/upload/v1765288474/products/misc/images/xlx0nfsercrkekaw47iz.webp', 20, 30, true, 1, now(), now()),

-- H&M
('00000004-0000-0000-0000-000000000004', '22222222-2222-2222-2222-222222222222', 'Váy Hoa H&M', 'vay-hoa-hm', 'Váy hoa mùa hè.', 'Cotton', 'women', 'ACTIVE', 450000, 'https://res.cloudinary.com/dyeqnh68q/image/upload/v1765288495/products/misc/images/h7hcnk7d7gbyvfikrr1n.webp', 60, 100, true, 1, now(), now()),
('00000005-0000-0000-0000-000000000005', '22222222-2222-2222-2222-222222222222', 'Áo Sơ Mi Trắng H&M', 'ao-so-mi-trang-hm', 'Áo sơ mi công sở.', 'Cotton', 'women', 'ACTIVE', 350000, 'https://res.cloudinary.com/dyeqnh68q/image/upload/v1765288424/products/misc/images/n4q1no1ab5og2v44oj5w.jpg', 50, 80, true, 1, now(), now()),
('00000006-0000-0000-0000-000000000006', '22222222-2222-2222-2222-222222222222', 'Quần Short H&M', 'quan-short-hm', 'Quần short năng động.', 'Kaki', 'women', 'ACTIVE', 250000, 'https://res.cloudinary.com/dyeqnh68q/image/upload/v1765288449/products/misc/images/dhfhfs2s9n1xi8myba2w.jpg', 40, 60, true, 1, now(), now()),

-- Uniqlo
('00000007-0000-0000-0000-000000000007', '33333333-3333-3333-3333-333333333333', 'Áo Thun UT Uniqlo', 'ao-thun-ut-uniqlo', 'Áo thun in hình.', 'Cotton', 'unisex', 'ACTIVE', 399000, 'https://res.cloudinary.com/dyeqnh68q/image/upload/v1765288474/products/misc/images/xlx0nfsercrkekaw47iz.webp', 100, 200, true, 1, now(), now()),
('00000008-0000-0000-0000-000000000008', '33333333-3333-3333-3333-333333333333', 'Áo Khoác Chống Nắng Uniqlo', 'ao-khoac-chong-nang-uniqlo', 'Chống tia UV.', 'Polyester', 'women', 'ACTIVE', 499000, 'https://res.cloudinary.com/dyeqnh68q/image/upload/v1765288495/products/misc/images/h7hcnk7d7gbyvfikrr1n.webp', 150, 300, true, 1, now(), now()),
('00000009-0000-0000-0000-000000000009', '33333333-3333-3333-3333-333333333333', 'Quần Tây EZY Uniqlo', 'quan-tay-ezy-uniqlo', 'Thoải mái như quần thun.', 'Cotton/Spandex', 'men', 'ACTIVE', 799000, 'https://res.cloudinary.com/dyeqnh68q/image/upload/v1765288424/products/misc/images/n4q1no1ab5og2v44oj5w.jpg', 80, 120, true, 1, now(), now()),

-- Gucci
('00000010-0000-0000-0000-000000000010', '44444444-4444-4444-4444-444444444444', 'Áo Polo Gucci', 'ao-polo-gucci', 'Sang trọng và đẳng cấp.', 'Cotton', 'men', 'ACTIVE', 15000000, 'https://res.cloudinary.com/dyeqnh68q/image/upload/v1765288449/products/misc/images/dhfhfs2s9n1xi8myba2w.jpg', 5, 10, true, 1, now(), now()),
('00000011-0000-0000-0000-000000000011', '44444444-4444-4444-4444-444444444444', 'Váy Dạ Hội Gucci', 'vay-da-hoi-gucci', 'Lộng lẫy.', 'Silk', 'women', 'ACTIVE', 50000000, 'https://res.cloudinary.com/dyeqnh68q/image/upload/v1765288474/products/misc/images/xlx0nfsercrkekaw47iz.webp', 2, 5, true, 1, now(), now()),
('00000012-0000-0000-0000-000000000012', '44444444-4444-4444-4444-444444444444', 'Áo Khoác Da Gucci', 'ao-khoac-da-gucci', 'Da thật cao cấp.', 'Leather', 'men', 'ACTIVE', 80000000, 'https://res.cloudinary.com/dyeqnh68q/image/upload/v1765288495/products/misc/images/h7hcnk7d7gbyvfikrr1n.webp', 1, 3, true, 1, now(), now()),

-- Louis Vuitton
('00000013-0000-0000-0000-000000000013', '55555555-5555-5555-5555-555555555555', 'Áo Sơ Mi LV Monogram', 'ao-so-mi-lv-monogram', 'Họa tiết đặc trưng.', 'Silk', 'men', 'ACTIVE', 25000000, 'https://res.cloudinary.com/dyeqnh68q/image/upload/v1765288424/products/misc/images/n4q1no1ab5og2v44oj5w.jpg', 3, 5, true, 1, now(), now()),
('00000014-0000-0000-0000-000000000014', '55555555-5555-5555-5555-555555555555', 'Đầm LV', 'dam-lv', 'Thanh lịch.', 'Wool', 'women', 'ACTIVE', 40000000, 'https://res.cloudinary.com/dyeqnh68q/image/upload/v1765288449/products/misc/images/dhfhfs2s9n1xi8myba2w.jpg', 2, 4, true, 1, now(), now()),
('00000015-0000-0000-0000-000000000015', '55555555-5555-5555-5555-555555555555', 'Áo Khoác LV', 'ao-khoac-lv', 'Ấm áp và thời trang.', 'Cashmere', 'unisex', 'ACTIVE', 60000000, 'https://res.cloudinary.com/dyeqnh68q/image/upload/v1765288474/products/misc/images/xlx0nfsercrkekaw47iz.webp', 1, 2, true, 1, now(), now()),

-- Chanel
('00000016-0000-0000-0000-000000000016', '66666666-6666-6666-6666-666666666666', 'Áo Tweed Chanel', 'ao-tweed-chanel', 'Biểu tượng của Chanel.', 'Tweed', 'women', 'ACTIVE', 90000000, 'https://res.cloudinary.com/dyeqnh68q/image/upload/v1765288495/products/misc/images/h7hcnk7d7gbyvfikrr1n.webp', 2, 3, true, 1, now(), now()),
('00000017-0000-0000-0000-000000000017', '66666666-6666-6666-6666-666666666666', 'Váy Đen Chanel', 'vay-den-chanel', 'Little Black Dress.', 'Silk', 'women', 'ACTIVE', 50000000, 'https://res.cloudinary.com/dyeqnh68q/image/upload/v1765288424/products/misc/images/n4q1no1ab5og2v44oj5w.jpg', 5, 8, true, 1, now(), now()),
('00000018-0000-0000-0000-000000000018', '66666666-6666-6666-6666-666666666666', 'Áo Len Chanel', 'ao-len-chanel', 'Mềm mại.', 'Wool', 'women', 'ACTIVE', 30000000, 'https://res.cloudinary.com/dyeqnh68q/image/upload/v1765288449/products/misc/images/dhfhfs2s9n1xi8myba2w.jpg', 4, 6, true, 1, now(), now()),

-- Levi's
('00000019-0000-0000-0000-000000000019', '77777777-7777-7777-7777-777777777777', 'Quần Jeans 501 Levi''s', 'quan-jeans-501-levis', 'Huyền thoại.', 'Denim', 'men', 'ACTIVE', 1500000, 'https://res.cloudinary.com/dyeqnh68q/image/upload/v1765288474/products/misc/images/xlx0nfsercrkekaw47iz.webp', 100, 200, true, 1, now(), now()),
('00000020-0000-0000-0000-000000000020', '77777777-7777-7777-7777-777777777777', 'Áo Khoác Jeans Levi''s', 'ao-khoac-jeans-levis', 'Bụi bặm.', 'Denim', 'unisex', 'ACTIVE', 2000000, 'https://res.cloudinary.com/dyeqnh68q/image/upload/v1765288495/products/misc/images/h7hcnk7d7gbyvfikrr1n.webp', 50, 80, true, 1, now(), now()),
('00000021-0000-0000-0000-000000000021', '77777777-7777-7777-7777-777777777777', 'Áo Thun Logo Levi''s', 'ao-thun-logo-levis', 'Đơn giản.', 'Cotton', 'unisex', 'ACTIVE', 500000, 'https://res.cloudinary.com/dyeqnh68q/image/upload/v1765288424/products/misc/images/n4q1no1ab5og2v44oj5w.jpg', 150, 300, true, 1, now(), now()),

-- Calvin Klein
('00000022-0000-0000-0000-000000000022', '88888888-8888-8888-8888-888888888888', 'Áo Thun CK', 'ao-thun-ck', 'Minimalist.', 'Cotton', 'men', 'ACTIVE', 900000, 'https://res.cloudinary.com/dyeqnh68q/image/upload/v1765288449/products/misc/images/dhfhfs2s9n1xi8myba2w.jpg', 60, 100, true, 1, now(), now()),
('00000023-0000-0000-0000-000000000023', '88888888-8888-8888-8888-888888888888', 'Quần Jeans CK', 'quan-jeans-ck', 'Dáng đẹp.', 'Denim', 'men', 'ACTIVE', 2500000, 'https://res.cloudinary.com/dyeqnh68q/image/upload/v1765288474/products/misc/images/xlx0nfsercrkekaw47iz.webp', 40, 60, true, 1, now(), now()),
('00000024-0000-0000-0000-000000000024', '88888888-8888-8888-8888-888888888888', 'Áo Lót CK', 'ao-lot-ck', 'Thoải mái.', 'Cotton', 'women', 'ACTIVE', 800000, 'https://res.cloudinary.com/dyeqnh68q/image/upload/v1765288495/products/misc/images/h7hcnk7d7gbyvfikrr1n.webp', 200, 400, true, 1, now(), now()),

-- Ralph Lauren
('00000025-0000-0000-0000-000000000025', '99999999-9999-9999-9999-999999999999', 'Áo Polo Ralph Lauren', 'ao-polo-ralph-lauren', 'Kinh điển.', 'Cotton', 'men', 'ACTIVE', 2500000, 'https://res.cloudinary.com/dyeqnh68q/image/upload/v1765288424/products/misc/images/n4q1no1ab5og2v44oj5w.jpg', 80, 150, true, 1, now(), now()),
('00000026-0000-0000-0000-000000000026', '99999999-9999-9999-9999-999999999999', 'Áo Sơ Mi Oxford', 'ao-so-mi-oxford', 'Lịch lãm.', 'Cotton', 'men', 'ACTIVE', 3000000, 'https://res.cloudinary.com/dyeqnh68q/image/upload/v1765288449/products/misc/images/dhfhfs2s9n1xi8myba2w.jpg', 40, 70, true, 1, now(), now()),
('00000027-0000-0000-0000-000000000027', '99999999-9999-9999-9999-999999999999', 'Áo Len Ralph Lauren', 'ao-len-ralph-lauren', 'Ấm áp.', 'Wool', 'men', 'ACTIVE', 4000000, 'https://res.cloudinary.com/dyeqnh68q/image/upload/v1765288474/products/misc/images/xlx0nfsercrkekaw47iz.webp', 20, 40, true, 1, now(), now()),

-- Versace
('00000028-0000-0000-0000-000000000028', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Áo Sơ Mi Lụa Versace', 'ao-so-mi-lua-versace', 'Họa tiết Baroque.', 'Silk', 'men', 'ACTIVE', 20000000, 'https://res.cloudinary.com/dyeqnh68q/image/upload/v1765288495/products/misc/images/h7hcnk7d7gbyvfikrr1n.webp', 5, 10, true, 1, now(), now()),
('00000029-0000-0000-0000-000000000029', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Váy Versace', 'vay-versace', 'Quyến rũ.', 'Viscose', 'women', 'ACTIVE', 30000000, 'https://res.cloudinary.com/dyeqnh68q/image/upload/v1765288424/products/misc/images/n4q1no1ab5og2v44oj5w.jpg', 3, 5, true, 1, now(), now()),
('00000030-0000-0000-0000-000000000030', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Áo Thun Versace', 'ao-thun-versace', 'Logo Medusa.', 'Cotton', 'unisex', 'ACTIVE', 10000000, 'https://res.cloudinary.com/dyeqnh68q/image/upload/v1765288449/products/misc/images/dhfhfs2s9n1xi8myba2w.jpg', 10, 20, true, 1, now(), now());

-- ============================================================================
-- 6. PRODUCT_CATEGORIES
-- ============================================================================
INSERT INTO product_categories (product_id, category_id) VALUES
('00000001-0000-0000-0000-000000000001', 'c3333333-3333-3333-3333-333333333333'), -- Zara T-Shirt -> Ao Thun
('00000002-0000-0000-0000-000000000002', 'c5555555-5555-5555-5555-555555555555'), -- Zara Jeans -> Quan Jeans
('00000003-0000-0000-0000-000000000003', 'c7777777-7777-7777-7777-777777777777'), -- Zara Jacket -> Ao Khoac
('00000004-0000-0000-0000-000000000004', 'c6666666-6666-6666-6666-666666666666'), -- HM Dress -> Vay Dam
('00000005-0000-0000-0000-000000000005', 'c4444444-4444-4444-4444-444444444444'), -- HM Shirt -> Ao So Mi
('00000006-0000-0000-0000-000000000006', 'c2222222-2222-2222-2222-222222222222'), -- HM Shorts -> Quan Ao Nu
('00000007-0000-0000-0000-000000000007', 'c3333333-3333-3333-3333-333333333333'), -- Uniqlo T-Shirt -> Ao Thun
('00000008-0000-0000-0000-000000000008', 'c7777777-7777-7777-7777-777777777777'), -- Uniqlo Jacket -> Ao Khoac
('00000009-0000-0000-0000-000000000009', 'c1111111-1111-1111-1111-111111111111'), -- Uniqlo Pants -> Quan Ao Nam
('00000010-0000-0000-0000-000000000010', 'c3333333-3333-3333-3333-333333333333'), -- Gucci Polo -> Ao Thun
('00000011-0000-0000-0000-000000000011', 'c6666666-6666-6666-6666-666666666666'), -- Gucci Dress -> Vay Dam
('00000012-0000-0000-0000-000000000012', 'c7777777-7777-7777-7777-777777777777'), -- Gucci Jacket -> Ao Khoac
('00000013-0000-0000-0000-000000000013', 'c4444444-4444-4444-4444-444444444444'), -- LV Shirt -> Ao So Mi
('00000014-0000-0000-0000-000000000014', 'c6666666-6666-6666-6666-666666666666'), -- LV Dress -> Vay Dam
('00000015-0000-0000-0000-000000000015', 'c7777777-7777-7777-7777-777777777777'), -- LV Jacket -> Ao Khoac
('00000016-0000-0000-0000-000000000016', 'c7777777-7777-7777-7777-777777777777'), -- Chanel Tweed -> Ao Khoac
('00000017-0000-0000-0000-000000000017', 'c6666666-6666-6666-6666-666666666666'), -- Chanel Dress -> Vay Dam
('00000018-0000-0000-0000-000000000018', 'c2222222-2222-2222-2222-222222222222'), -- Chanel Sweater -> Quan Ao Nu
('00000019-0000-0000-0000-000000000019', 'c5555555-5555-5555-5555-555555555555'), -- Levis Jeans -> Quan Jeans
('00000020-0000-0000-0000-000000000020', 'c7777777-7777-7777-7777-777777777777'), -- Levis Jacket -> Ao Khoac
('00000021-0000-0000-0000-000000000021', 'c3333333-3333-3333-3333-333333333333'), -- Levis T-Shirt -> Ao Thun
('00000022-0000-0000-0000-000000000022', 'c3333333-3333-3333-3333-333333333333'), -- CK T-Shirt -> Ao Thun
('00000023-0000-0000-0000-000000000023', 'c5555555-5555-5555-5555-555555555555'), -- CK Jeans -> Quan Jeans
('00000024-0000-0000-0000-000000000024', 'c2222222-2222-2222-2222-222222222222'), -- CK Underwear -> Quan Ao Nu
('00000025-0000-0000-0000-000000000025', 'c3333333-3333-3333-3333-333333333333'), -- RL Polo -> Ao Thun
('00000026-0000-0000-0000-000000000026', 'c4444444-4444-4444-4444-444444444444'), -- RL Shirt -> Ao So Mi
('00000027-0000-0000-0000-000000000027', 'c1111111-1111-1111-1111-111111111111'), -- RL Sweater -> Quan Ao Nam
('00000028-0000-0000-0000-000000000028', 'c4444444-4444-4444-4444-444444444444'), -- Versace Shirt -> Ao So Mi
('00000029-0000-0000-0000-000000000029', 'c6666666-6666-6666-6666-666666666666'), -- Versace Dress -> Vay Dam
('00000030-0000-0000-0000-000000000030', 'c3333333-3333-3333-3333-333333333333'); -- Versace T-Shirt -> Ao Thun

-- ============================================================================
-- 7. PRODUCT_VARIANTS & INVENTORY
-- ============================================================================
DO $$
DECLARE
    prod_rec RECORD;
    var_id UUID;
    size_id UUID;
    color_id UUID;
    sku_base TEXT;
    v_image_urls text[] := ARRAY[
        'https://res.cloudinary.com/dyeqnh68q/image/upload/v1765288424/products/misc/images/n4q1no1ab5og2v44oj5w.jpg',
        'https://res.cloudinary.com/dyeqnh68q/image/upload/v1765288449/products/misc/images/dhfhfs2s9n1xi8myba2w.jpg',
        'https://res.cloudinary.com/dyeqnh68q/image/upload/v1765288474/products/misc/images/xlx0nfsercrkekaw47iz.webp',
        'https://res.cloudinary.com/dyeqnh68q/image/upload/v1765288495/products/misc/images/h7hcnk7d7gbyvfikrr1n.webp'
    ];
BEGIN
    FOR prod_rec IN SELECT id, slug, price FROM products LOOP
        -- Variant 1: Size M, Color Black
        var_id := gen_random_uuid();
        size_id := '53333333-3333-3333-3333-333333333333';
        color_id := '10000000-0000-0000-0000-000000000001';
        sku_base := upper(replace(prod_rec.slug, '-', '')) || '-BLK-M';
        
        INSERT INTO product_variants (id, product_id, sku, size_id, color_id, price_amount, compare_at_amount, weight_grams, status, sold_count, is_in_stock, history_cost, version, created_at, updated_at)
        VALUES (var_id, prod_rec.id, sku_base, size_id, color_id, prod_rec.price, prod_rec.price + 50000, 300, 'ACTIVE', 10, true, prod_rec.price * 0.6, 1, now(), now());
        
        INSERT INTO inventory (variant_id, quantity_on_hand, quantity_reserved, reorder_level, updated_at)
        VALUES (var_id, 50, 0, 10, now());

        INSERT INTO product_images (id, product_id, variant_id, color_id, image_url, public_id, is_default, position, created_at)
        VALUES (gen_random_uuid(), prod_rec.id, var_id, color_id, v_image_urls[1], 'img-1-' || prod_rec.slug, true, 1, now());

        -- Variant 2: Size L, Color White
        var_id := gen_random_uuid();
        size_id := '54444444-4444-4444-4444-444444444444';
        color_id := '10000000-0000-0000-0000-000000000002';
        sku_base := upper(replace(prod_rec.slug, '-', '')) || '-WHT-L';
        
        INSERT INTO product_variants (id, product_id, sku, size_id, color_id, price_amount, compare_at_amount, weight_grams, status, sold_count, is_in_stock, history_cost, version, created_at, updated_at)
        VALUES (var_id, prod_rec.id, sku_base, size_id, color_id, prod_rec.price, prod_rec.price + 50000, 300, 'ACTIVE', 15, true, prod_rec.price * 0.6, 1, now(), now());
        
        INSERT INTO inventory (variant_id, quantity_on_hand, quantity_reserved, reorder_level, updated_at)
        VALUES (var_id, 50, 0, 10, now());

        INSERT INTO product_images (id, product_id, variant_id, color_id, image_url, public_id, is_default, position, created_at)
        VALUES (gen_random_uuid(), prod_rec.id, var_id, color_id, v_image_urls[2], 'img-2-' || prod_rec.slug, false, 2, now());

        -- Variant 3: Size S, Color Red
        var_id := gen_random_uuid();
        size_id := '52222222-2222-2222-2222-222222222222';
        color_id := '10000000-0000-0000-0000-000000000003';
        sku_base := upper(replace(prod_rec.slug, '-', '')) || '-RED-S';
        
        INSERT INTO product_variants (id, product_id, sku, size_id, color_id, price_amount, compare_at_amount, weight_grams, status, sold_count, is_in_stock, history_cost, version, created_at, updated_at)
        VALUES (var_id, prod_rec.id, sku_base, size_id, color_id, prod_rec.price, prod_rec.price + 50000, 300, 'ACTIVE', 5, true, prod_rec.price * 0.6, 1, now(), now());
        
        INSERT INTO inventory (variant_id, quantity_on_hand, quantity_reserved, reorder_level, updated_at)
        VALUES (var_id, 50, 0, 10, now());

        INSERT INTO product_images (id, product_id, variant_id, color_id, image_url, public_id, is_default, position, created_at)
        VALUES (gen_random_uuid(), prod_rec.id, var_id, color_id, v_image_urls[3], 'img-3-' || prod_rec.slug, false, 3, now());

        -- Variant 4: Size XL, Color Blue
        var_id := gen_random_uuid();
        size_id := '55555555-5555-5555-5555-555555555555';
        color_id := '10000000-0000-0000-0000-000000000004';
        sku_base := upper(replace(prod_rec.slug, '-', '')) || '-BLU-XL';
        
        INSERT INTO product_variants (id, product_id, sku, size_id, color_id, price_amount, compare_at_amount, weight_grams, status, sold_count, is_in_stock, history_cost, version, created_at, updated_at)
        VALUES (var_id, prod_rec.id, sku_base, size_id, color_id, prod_rec.price, prod_rec.price + 50000, 300, 'ACTIVE', 8, true, prod_rec.price * 0.6, 1, now(), now());
        
        INSERT INTO inventory (variant_id, quantity_on_hand, quantity_reserved, reorder_level, updated_at)
        VALUES (var_id, 50, 0, 10, now());

        INSERT INTO product_images (id, product_id, variant_id, color_id, image_url, public_id, is_default, position, created_at)
        VALUES (gen_random_uuid(), prod_rec.id, var_id, color_id, v_image_urls[4], 'img-4-' || prod_rec.slug, false, 4, now());

    END LOOP;
END $$;

-- ============================================================================
-- 8. ORDERS
-- ============================================================================
DO $$
DECLARE
    u1 UUID := '20000000-0000-0000-0000-000000000001';
    u2 UUID := '20000000-0000-0000-0000-000000000002';
    p1 UUID := '00000001-0000-0000-0000-000000000001';
    v1 UUID;
    o1 UUID := gen_random_uuid();
    o2 UUID := gen_random_uuid();
BEGIN
    -- Get a variant for P1 (Zara T-Shirt)
    SELECT id INTO v1 FROM product_variants WHERE product_id = p1 LIMIT 1;

    -- Order 1: DELIVERED
    INSERT INTO orders (id, order_number, user_id, status, previous_status, subtotal_amount, discount_amount, shipping_amount, total_amount, shipping_address, placed_at, paid_at, version, created_at, updated_at) VALUES
    (o1, 'ORD-2024-0001', u1, 'DELIVERED', 'SHIPPED', 250000, 0, 30000, 280000, 
     '{"fullName":"Nguyen Van A","phone":"0901234567","province":"TP Hồ Chí Minh","district":"Quận 1","ward":"Phường Bến Nghé","line1":"123 Nguyễn Huệ"}'::jsonb,
     now() - interval '7 days', now() - interval '7 days', 1, now() - interval '7 days', now());
    
    INSERT INTO order_items (id, order_id, product_id, variant_id, sku, product_name, variant_name, quantity, unit_price_amount, total_amount, created_at) VALUES
    (gen_random_uuid(), o1, p1, v1, 'AOTHUNBASICZARA-BLK-M', 'Áo Thun Basic Zara', 'Black / M', 1, 250000, 250000, now() - interval '7 days');
    
    INSERT INTO payments (id, order_id, provider, status, amount, transaction_id, raw_response, created_at) VALUES
    (gen_random_uuid(), o1, 'VNPAY', 'CAPTURED', 280000, 'TRANS001', '{}', now() - interval '7 days');
    
    INSERT INTO shipments (id, order_id, carrier, tracking_number, status, shipped_at, delivered_at, is_active, created_at) VALUES
    (gen_random_uuid(), o1, 'GHN', 'GHN001', 'DELIVERED', now() - interval '6 days', now() - interval '2 days', true, now() - interval '7 days');

    -- Order 2: PROCESSING (changed from PENDING to make it shippable for testing)
    INSERT INTO orders (id, order_number, user_id, status, previous_status, subtotal_amount, discount_amount, shipping_amount, total_amount, shipping_address, placed_at, paid_at, version, created_at, updated_at) VALUES
    (o2, 'ORD-2024-0002', u2, 'PROCESSING', 'CONFIRMED', 250000, 0, 30000, 280000, 
     '{"fullName":"Tran Thi B","phone":"0901234568","province":"Hà Nội","district":"Quận Ba Đình","ward":"Phường Điện Biên","line1":"456 Hoàng Diệu"}'::jsonb,
     now() - interval '1 day', now() - interval '1 day', 1, now() - interval '1 day', now());
    
    INSERT INTO order_items (id, order_id, product_id, variant_id, sku, product_name, variant_name, quantity, unit_price_amount, total_amount, created_at) VALUES
    (gen_random_uuid(), o2, p1, v1, 'AOTHUNBASICZARA-BLK-M', 'Áo Thun Basic Zara', 'Black / M', 1, 250000, 250000, now());
    
    INSERT INTO payments (id, order_id, provider, status, amount, transaction_id, raw_response, created_at) VALUES
    (gen_random_uuid(), o2, 'COD', 'CAPTURED', 280000, 'COD-002', '{}', now() - interval '1 day');

END $$;

-- ============================================================================
-- 9. BANNERS
-- ============================================================================
INSERT INTO banners (title, image_url, description, display_order, is_active) VALUES
('Banner 1', 'https://res.cloudinary.com/dyeqnh68q/image/upload/v1765288942/banners/misc/kwgpozculdw34bkqrxrm.jpg', 'Banner 1 Description', 1, true),
('Banner 2', 'https://res.cloudinary.com/dyeqnh68q/image/upload/v1765288964/banners/misc/qvenvga4oxqjgs1itxxo.webp', 'Banner 2 Description', 2, true),
('Banner 3', 'https://res.cloudinary.com/dyeqnh68q/image/upload/v1765288976/banners/misc/ytramrd8wuwnvuyj951w.jpg', 'Banner 3 Description', 3, true);

COMMIT;
