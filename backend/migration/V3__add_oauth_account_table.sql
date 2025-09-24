CREATE TABLE addresses
(
    id                  CHAR(36) DEFAULT gen_random_uuid() NOT NULL,
    user_id             CHAR(36),
    full_name           TEXT                               NOT NULL,
    phone               VARCHAR(30),
    line1               TEXT                               NOT NULL,
    line2               TEXT,
    ward                TEXT,
    district            TEXT,
    city                TEXT,
    province            TEXT,
    country_code        CHAR(2)  DEFAULT 'VN'              NOT NULL,
    is_default_shipping BOOLEAN  DEFAULT FALSE             NOT NULL,
    is_default_billing  BOOLEAN  DEFAULT FALSE             NOT NULL,
    created_at          TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()             NOT NULL,
    updated_at          TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()             NOT NULL,
    CONSTRAINT addresses_pkey PRIMARY KEY (id)
);

CREATE TABLE audit_logs
(
    id            CHAR(36) DEFAULT gen_random_uuid() NOT NULL,
    actor_user_id CHAR(36),
    action        TEXT                               NOT NULL,
    entity_type   TEXT                               NOT NULL,
    entity_id     CHAR(36),
    metadata      JSONB,
    trace_id      TEXT,
    created_at    TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()             NOT NULL,
    CONSTRAINT audit_logs_pkey PRIMARY KEY (id)
);

CREATE TABLE brands
(
    id          CHAR(36) DEFAULT gen_random_uuid() NOT NULL,
    name        TEXT                               NOT NULL,
    slug        TEXT                               NOT NULL,
    description TEXT,
    created_at  TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()             NOT NULL,
    updated_at  TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()             NOT NULL,
    deleted_at  TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT brands_pkey PRIMARY KEY (id)
);

CREATE TABLE cart_items
(
    id                CHAR(36) DEFAULT gen_random_uuid() NOT NULL,
    cart_id           CHAR(36)                           NOT NULL,
    variant_id        CHAR(36)                           NOT NULL,
    quantity          INTEGER                            NOT NULL,
    unit_price_amount BIGINT                             NOT NULL,
    currency          CHAR(3)                            NOT NULL,
    created_at        TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()             NOT NULL,
    updated_at        TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()             NOT NULL,
    CONSTRAINT cart_items_pkey PRIMARY KEY (id)
);

CREATE TABLE carts
(
    id         CHAR(36) DEFAULT gen_random_uuid() NOT NULL,
    user_id    CHAR(36),
    cart_token CHAR(36),
    status     TEXT     DEFAULT 'ACTIVE'          NOT NULL,
    currency   CHAR(3)  DEFAULT 'VND'             NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()             NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()             NOT NULL,
    CONSTRAINT carts_pkey PRIMARY KEY (id)
);

CREATE TABLE categories
(
    id          CHAR(36) DEFAULT gen_random_uuid() NOT NULL,
    name        TEXT                               NOT NULL,
    slug        TEXT                               NOT NULL,
    parent_id   CHAR(36),
    description TEXT,
    created_at  TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()             NOT NULL,
    updated_at  TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()             NOT NULL,
    deleted_at  TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT categories_pkey PRIMARY KEY (id)
);

CREATE TABLE colors
(
    id       CHAR(36) DEFAULT gen_random_uuid() NOT NULL,
    name     TEXT                               NOT NULL,
    hex_code CHAR(7),
    CONSTRAINT colors_pkey PRIMARY KEY (id)
);

CREATE TABLE discount_categories
(
    discount_id CHAR(36) NOT NULL,
    category_id CHAR(36) NOT NULL,
    CONSTRAINT discount_categories_pkey PRIMARY KEY (discount_id, category_id)
);

CREATE TABLE discount_products
(
    discount_id CHAR(36) NOT NULL,
    product_id  CHAR(36) NOT NULL,
    CONSTRAINT discount_products_pkey PRIMARY KEY (discount_id, product_id)
);

CREATE TABLE discount_redemptions
(
    id          CHAR(36) DEFAULT gen_random_uuid() NOT NULL,
    discount_id CHAR(36)                           NOT NULL,
    user_id     CHAR(36),
    order_id    CHAR(36),
    redeemed_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()             NOT NULL,
    CONSTRAINT discount_redemptions_pkey PRIMARY KEY (id)
);

CREATE TABLE discounts
(
    id               CHAR(36) DEFAULT gen_random_uuid() NOT NULL,
    code             TEXT                               NOT NULL,
    name             TEXT                               NOT NULL,
    description      TEXT,
    type             TEXT                               NOT NULL,
    value            INTEGER                            NOT NULL,
    currency         CHAR(3),
    starts_at        TIMESTAMP WITHOUT TIME ZONE,
    ends_at          TIMESTAMP WITHOUT TIME ZONE,
    max_redemptions  INTEGER,
    per_user_limit   INTEGER,
    min_order_amount BIGINT,
    active           BOOLEAN  DEFAULT TRUE              NOT NULL,
    created_at       TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()             NOT NULL,
    updated_at       TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()             NOT NULL,
    CONSTRAINT discounts_pkey PRIMARY KEY (id)
);

CREATE TABLE email_log
(
    id            CHAR(36) DEFAULT gen_random_uuid() NOT NULL,
    to_email      VARCHAR(255)                       NOT NULL,
    subject       TEXT                               NOT NULL,
    template_code TEXT,
    payload       JSONB,
    status        TEXT     DEFAULT 'PENDING'         NOT NULL,
    attempts      INTEGER  DEFAULT 0                 NOT NULL,
    last_error    TEXT,
    message_id    TEXT,
    sent_at       TIMESTAMP WITHOUT TIME ZONE,
    created_at    TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()             NOT NULL,
    updated_at    TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()             NOT NULL,
    CONSTRAINT email_log_pkey PRIMARY KEY (id)
);

CREATE TABLE flyway_schema_history
(
    installed_rank INTEGER       NOT NULL,
    version        VARCHAR(50),
    description    VARCHAR(200)  NOT NULL,
    type           VARCHAR(20)   NOT NULL,
    script         VARCHAR(1000) NOT NULL,
    checksum       INTEGER,
    installed_by   VARCHAR(100)  NOT NULL,
    installed_on   TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW() NOT NULL,
    execution_time INTEGER       NOT NULL,
    success        BOOLEAN       NOT NULL,
    CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank)
);

CREATE TABLE inventory
(
    variant_id        CHAR(36)          NOT NULL,
    quantity_on_hand  INTEGER DEFAULT 0 NOT NULL,
    quantity_reserved INTEGER DEFAULT 0 NOT NULL,
    reorder_level     INTEGER DEFAULT 0 NOT NULL,
    updated_at        TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW() NOT NULL,
    CONSTRAINT inventory_pkey PRIMARY KEY (variant_id)
);

CREATE TABLE inventory_reservations
(
    id          CHAR(36) DEFAULT gen_random_uuid() NOT NULL,
    variant_id  CHAR(36)                           NOT NULL,
    order_id    CHAR(36),
    quantity    INTEGER                            NOT NULL,
    reserved_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()             NOT NULL,
    released_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT inventory_reservations_pkey PRIMARY KEY (id)
);

CREATE TABLE order_items
(
    id                CHAR(36) DEFAULT gen_random_uuid() NOT NULL,
    order_id          CHAR(36)                           NOT NULL,
    product_id        CHAR(36),
    variant_id        CHAR(36),
    sku               TEXT,
    product_name      TEXT                               NOT NULL,
    variant_name      TEXT,
    quantity          INTEGER                            NOT NULL,
    unit_price_amount BIGINT                             NOT NULL,
    discount_amount   BIGINT   DEFAULT 0                 NOT NULL,
    tax_amount        BIGINT   DEFAULT 0                 NOT NULL,
    total_amount      BIGINT                             NOT NULL,
    currency          CHAR(3)                            NOT NULL,
    created_at        TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()             NOT NULL,
    CONSTRAINT order_items_pkey PRIMARY KEY (id)
);

CREATE TABLE orders
(
    id               CHAR(36) DEFAULT gen_random_uuid() NOT NULL,
    order_number     TEXT                               NOT NULL,
    user_id          CHAR(36),
    status           TEXT     DEFAULT 'PENDING'         NOT NULL,
    currency         CHAR(3)  DEFAULT 'VND'             NOT NULL,
    subtotal_amount  BIGINT   DEFAULT 0                 NOT NULL,
    discount_amount  BIGINT   DEFAULT 0                 NOT NULL,
    shipping_amount  BIGINT   DEFAULT 0                 NOT NULL,
    tax_amount       BIGINT   DEFAULT 0                 NOT NULL,
    total_amount     BIGINT   DEFAULT 0                 NOT NULL,
    shipping_address JSONB,
    billing_address  JSONB,
    notes            TEXT,
    placed_at        TIMESTAMP WITHOUT TIME ZONE,
    created_at       TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()             NOT NULL,
    updated_at       TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()             NOT NULL,
    version          INTEGER  DEFAULT 0                 NOT NULL,
    CONSTRAINT orders_pkey PRIMARY KEY (id)
);

CREATE TABLE payments
(
    id             CHAR(36) DEFAULT gen_random_uuid() NOT NULL,
    order_id       CHAR(36)                           NOT NULL,
    provider       TEXT                               NOT NULL,
    status         TEXT                               NOT NULL,
    amount         BIGINT                             NOT NULL,
    currency       CHAR(3)                            NOT NULL,
    transaction_id TEXT,
    raw_response   JSONB,
    error_code     TEXT,
    error_message  TEXT,
    created_at     TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()             NOT NULL,
    updated_at     TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()             NOT NULL,
    CONSTRAINT payments_pkey PRIMARY KEY (id)
);

CREATE TABLE product_categories
(
    product_id  CHAR(36) NOT NULL,
    category_id CHAR(36) NOT NULL,
    CONSTRAINT product_categories_pkey PRIMARY KEY (product_id, category_id)
);

CREATE TABLE product_images
(
    id         CHAR(36) DEFAULT gen_random_uuid() NOT NULL,
    product_id CHAR(36)                           NOT NULL,
    image_url  TEXT                               NOT NULL,
    alt        TEXT,
    position   INTEGER  DEFAULT 0                 NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()             NOT NULL,
    CONSTRAINT product_images_pkey PRIMARY KEY (id)
);

CREATE TABLE product_variants
(
    id                CHAR(36) DEFAULT gen_random_uuid() NOT NULL,
    product_id        CHAR(36)                           NOT NULL,
    sku               TEXT                               NOT NULL,
    barcode           TEXT,
    size_id           CHAR(36),
    color_id          CHAR(36),
    price_amount      BIGINT                             NOT NULL,
    compare_at_amount BIGINT,
    currency          CHAR(3)  DEFAULT 'VND'             NOT NULL,
    weight_grams      INTEGER,
    status            TEXT     DEFAULT 'ACTIVE'          NOT NULL,
    created_at        TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()             NOT NULL,
    updated_at        TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()             NOT NULL,
    deleted_at        TIMESTAMP WITHOUT TIME ZONE,
    version           INTEGER  DEFAULT 0                 NOT NULL,
    CONSTRAINT product_variants_pkey PRIMARY KEY (id)
);

CREATE TABLE products
(
    id              CHAR(36) DEFAULT gen_random_uuid() NOT NULL,
    brand_id        CHAR(36),
    name            TEXT                               NOT NULL,
    slug            TEXT                               NOT NULL,
    description     TEXT,
    material        TEXT,
    gender          TEXT,
    status          TEXT     DEFAULT 'ACTIVE'          NOT NULL,
    seo_title       TEXT,
    seo_description TEXT,
    created_at      TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()             NOT NULL,
    updated_at      TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()             NOT NULL,
    deleted_at      TIMESTAMP WITHOUT TIME ZONE,
    version         INTEGER  DEFAULT 0                 NOT NULL,
    CONSTRAINT products_pkey PRIMARY KEY (id)
);

CREATE TABLE refresh_tokens
(
    id             CHAR(36) DEFAULT gen_random_uuid() NOT NULL,
    token_hash     VARCHAR(255)                       NOT NULL,
    expires_at     TIMESTAMP WITHOUT TIME ZONE                           NOT NULL,
    last_used_at   TIMESTAMP WITHOUT TIME ZONE,
    created_at     TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()             NOT NULL,
    is_revoked     BOOLEAN  DEFAULT FALSE             NOT NULL,
    revoked_at     TIMESTAMP WITHOUT TIME ZONE,
    revoked_reason VARCHAR(100),
    version        BIGINT   DEFAULT 0                 NOT NULL,
    user_id        CHAR(36)                           NOT NULL,
    CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id)
);

CREATE TABLE roles
(
    id   CHAR(36) DEFAULT gen_random_uuid() NOT NULL,
    code TEXT                               NOT NULL,
    name TEXT                               NOT NULL,
    CONSTRAINT roles_pkey PRIMARY KEY (id)
);

CREATE TABLE shipment_items
(
    id            CHAR(36) DEFAULT gen_random_uuid() NOT NULL,
    shipment_id   CHAR(36)                           NOT NULL,
    order_item_id CHAR(36)                           NOT NULL,
    quantity      INTEGER                            NOT NULL,
    CONSTRAINT shipment_items_pkey PRIMARY KEY (id)
);

CREATE TABLE shipments
(
    id              CHAR(36) DEFAULT gen_random_uuid() NOT NULL,
    order_id        CHAR(36)                           NOT NULL,
    carrier         TEXT,
    service_level   TEXT,
    tracking_number TEXT,
    status          TEXT                               NOT NULL,
    shipped_at      TIMESTAMP WITHOUT TIME ZONE,
    delivered_at    TIMESTAMP WITHOUT TIME ZONE,
    created_at      TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()             NOT NULL,
    updated_at      TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()             NOT NULL,
    CONSTRAINT shipments_pkey PRIMARY KEY (id)
);

CREATE TABLE sizes
(
    id   CHAR(36) DEFAULT gen_random_uuid() NOT NULL,
    code TEXT                               NOT NULL,
    name TEXT                               NOT NULL,
    CONSTRAINT sizes_pkey PRIMARY KEY (id)
);

CREATE TABLE user_roles
(
    user_id CHAR(36) NOT NULL,
    role_id CHAR(36) NOT NULL,
    CONSTRAINT user_roles_pkey PRIMARY KEY (user_id, role_id)
);

CREATE TABLE users
(
    id                  CHAR(36) DEFAULT gen_random_uuid() NOT NULL,
    email               VARCHAR(255)                       NOT NULL,
    email_verified_at   TIMESTAMP WITHOUT TIME ZONE,
    password_hash       TEXT,
    full_name           VARCHAR(255),
    phone               VARCHAR(30),
    avatar_url          TEXT,
    status              TEXT     DEFAULT 'ACTIVE'          NOT NULL,
    token_version       INTEGER  DEFAULT 0                 NOT NULL,
    password_changed_at TIMESTAMP WITHOUT TIME ZONE,
    last_login_at       TIMESTAMP WITHOUT TIME ZONE,
    created_at          TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()             NOT NULL,
    updated_at          TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()             NOT NULL,
    version             INTEGER  DEFAULT 0                 NOT NULL,
    CONSTRAINT users_pkey PRIMARY KEY (id)
);

ALTER TABLE brands
    ADD CONSTRAINT brands_slug_key UNIQUE (slug);

ALTER TABLE cart_items
    ADD CONSTRAINT cart_items_cart_id_variant_id_key UNIQUE (cart_id, variant_id);

ALTER TABLE carts
    ADD CONSTRAINT carts_cart_token_key UNIQUE (cart_token);

ALTER TABLE categories
    ADD CONSTRAINT categories_slug_key UNIQUE (slug);

ALTER TABLE discount_redemptions
    ADD CONSTRAINT discount_redemptions_order_id_key UNIQUE (order_id);

ALTER TABLE discounts
    ADD CONSTRAINT discounts_code_key UNIQUE (code);

ALTER TABLE orders
    ADD CONSTRAINT orders_order_number_key UNIQUE (order_number);

ALTER TABLE product_variants
    ADD CONSTRAINT product_variants_sku_key UNIQUE (sku);

ALTER TABLE products
    ADD CONSTRAINT products_slug_key UNIQUE (slug);

ALTER TABLE refresh_tokens
    ADD CONSTRAINT refresh_tokens_token_hash_key UNIQUE (token_hash);

ALTER TABLE roles
    ADD CONSTRAINT roles_code_key UNIQUE (code);

ALTER TABLE sizes
    ADD CONSTRAINT sizes_code_key UNIQUE (code);

CREATE INDEX flyway_schema_history_s_idx ON flyway_schema_history (success);

CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);

CREATE INDEX ix_orders_status ON orders (status);

CREATE INDEX ix_product_variants_size_color ON product_variants (product_id, size_id, color_id);

CREATE INDEX ix_products_status ON products (status);

ALTER TABLE addresses
    ADD CONSTRAINT addresses_user_id_fkey FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

CREATE INDEX ix_addresses_user ON addresses (user_id);

ALTER TABLE audit_logs
    ADD CONSTRAINT audit_logs_actor_user_id_fkey FOREIGN KEY (actor_user_id) REFERENCES users (id) ON DELETE SET NULL;

ALTER TABLE cart_items
    ADD CONSTRAINT cart_items_cart_id_fkey FOREIGN KEY (cart_id) REFERENCES carts (id) ON DELETE CASCADE;

ALTER TABLE cart_items
    ADD CONSTRAINT cart_items_variant_id_fkey FOREIGN KEY (variant_id) REFERENCES product_variants (id) ON DELETE NO ACTION;

ALTER TABLE carts
    ADD CONSTRAINT carts_user_id_fkey FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL;

CREATE INDEX ix_carts_user ON carts (user_id);

ALTER TABLE categories
    ADD CONSTRAINT categories_parent_id_fkey FOREIGN KEY (parent_id) REFERENCES categories (id) ON DELETE SET NULL;

CREATE INDEX ix_categories_parent ON categories (parent_id);

ALTER TABLE discount_categories
    ADD CONSTRAINT discount_categories_category_id_fkey FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE CASCADE;

ALTER TABLE discount_categories
    ADD CONSTRAINT discount_categories_discount_id_fkey FOREIGN KEY (discount_id) REFERENCES discounts (id) ON DELETE CASCADE;

ALTER TABLE discount_products
    ADD CONSTRAINT discount_products_discount_id_fkey FOREIGN KEY (discount_id) REFERENCES discounts (id) ON DELETE CASCADE;

ALTER TABLE discount_products
    ADD CONSTRAINT discount_products_product_id_fkey FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE;

ALTER TABLE discount_redemptions
    ADD CONSTRAINT discount_redemptions_discount_id_fkey FOREIGN KEY (discount_id) REFERENCES discounts (id) ON DELETE CASCADE;

ALTER TABLE discount_redemptions
    ADD CONSTRAINT discount_redemptions_order_id_fkey FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE SET NULL;

ALTER TABLE discount_redemptions
    ADD CONSTRAINT discount_redemptions_user_id_fkey FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL;

CREATE INDEX ix_discount_redemptions_user ON discount_redemptions (user_id);

ALTER TABLE inventory_reservations
    ADD CONSTRAINT inventory_reservations_order_id_fkey FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE;

ALTER TABLE inventory_reservations
    ADD CONSTRAINT inventory_reservations_variant_id_fkey FOREIGN KEY (variant_id) REFERENCES product_variants (id) ON DELETE CASCADE;

CREATE INDEX ix_inventory_reservations_variant ON inventory_reservations (variant_id);

ALTER TABLE inventory
    ADD CONSTRAINT inventory_variant_id_fkey FOREIGN KEY (variant_id) REFERENCES product_variants (id) ON DELETE CASCADE;

ALTER TABLE order_items
    ADD CONSTRAINT order_items_order_id_fkey FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE;

CREATE INDEX ix_order_items_order ON order_items (order_id);

ALTER TABLE order_items
    ADD CONSTRAINT order_items_product_id_fkey FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE SET NULL;

ALTER TABLE order_items
    ADD CONSTRAINT order_items_variant_id_fkey FOREIGN KEY (variant_id) REFERENCES product_variants (id) ON DELETE SET NULL;

ALTER TABLE orders
    ADD CONSTRAINT orders_user_id_fkey FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL;

CREATE INDEX ix_orders_user ON orders (user_id);

ALTER TABLE payments
    ADD CONSTRAINT payments_order_id_fkey FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE;

CREATE INDEX ix_payments_order ON payments (order_id);

ALTER TABLE product_categories
    ADD CONSTRAINT product_categories_category_id_fkey FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE CASCADE;

ALTER TABLE product_categories
    ADD CONSTRAINT product_categories_product_id_fkey FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE;

ALTER TABLE product_images
    ADD CONSTRAINT product_images_product_id_fkey FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE;

ALTER TABLE product_variants
    ADD CONSTRAINT product_variants_color_id_fkey FOREIGN KEY (color_id) REFERENCES colors (id) ON DELETE NO ACTION;

ALTER TABLE product_variants
    ADD CONSTRAINT product_variants_product_id_fkey FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE;

CREATE INDEX ix_product_variants_product ON product_variants (product_id);

ALTER TABLE product_variants
    ADD CONSTRAINT product_variants_size_id_fkey FOREIGN KEY (size_id) REFERENCES sizes (id) ON DELETE NO ACTION;

ALTER TABLE products
    ADD CONSTRAINT products_brand_id_fkey FOREIGN KEY (brand_id) REFERENCES brands (id) ON DELETE SET NULL;

CREATE INDEX ix_products_brand ON products (brand_id);

ALTER TABLE refresh_tokens
    ADD CONSTRAINT refresh_tokens_user_id_fkey FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);

ALTER TABLE shipment_items
    ADD CONSTRAINT shipment_items_order_item_id_fkey FOREIGN KEY (order_item_id) REFERENCES order_items (id) ON DELETE CASCADE;

ALTER TABLE shipment_items
    ADD CONSTRAINT shipment_items_shipment_id_fkey FOREIGN KEY (shipment_id) REFERENCES shipments (id) ON DELETE CASCADE;

ALTER TABLE shipments
    ADD CONSTRAINT shipments_order_id_fkey FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE;

CREATE INDEX ix_shipments_order ON shipments (order_id);

ALTER TABLE user_roles
    ADD CONSTRAINT user_roles_role_id_fkey FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE;

ALTER TABLE user_roles
    ADD CONSTRAINT user_roles_user_id_fkey FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;