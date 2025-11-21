INSERT INTO users (id, email, password_hash, full_name, status, role, token_version, created_at, updated_at, email_verified_at)
VALUES (
    '49033c4e-a674-44a4-9dc2-742536045636',
    'admin@example.com',
    '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRkgVduVkyv9fthc.pTgJ/a.uoC',
    'Admin User',
    'ACTIVE',
    'ADMIN',
    0,
    NOW(),
    NOW(),
    NOW()
);
