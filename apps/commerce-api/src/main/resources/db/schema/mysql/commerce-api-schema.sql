CREATE TABLE IF NOT EXISTS users
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    VARCHAR(20)  NOT NULL,
    email      VARCHAR(255) NOT NULL,
    birthday   VARCHAR(10)  NOT NULL,
    gender     VARCHAR(20)  NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    deleted_at DATETIME(6)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_user_id UNIQUE (user_id),
    CONSTRAINT uk_users_email UNIQUE (email)
) DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS points
(
    id         BIGINT         NOT NULL AUTO_INCREMENT,
    user_id    VARCHAR(20)    NOT NULL,
    balance    DECIMAL(19, 2) NOT NULL,
    created_at DATETIME(6)    NOT NULL,
    updated_at DATETIME(6)    NOT NULL,
    deleted_at DATETIME(6)    NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_points_user_id UNIQUE (user_id),
    CONSTRAINT fk_points_user_id FOREIGN KEY (user_id) REFERENCES users (user_id)
) DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS point_histories
(
    id          BIGINT         NOT NULL AUTO_INCREMENT,
    user_id     VARCHAR(20)    NOT NULL,
    amount      DECIMAL(19, 2) NOT NULL,
    type        VARCHAR(20)    NOT NULL,
    description VARCHAR(200)   NULL,
    created_at  DATETIME(6)    NOT NULL,
    updated_at  DATETIME(6)    NOT NULL,
    deleted_at  DATETIME(6)    NULL,
    PRIMARY KEY (id),
    INDEX idx_point_histories_user_id_created_at (user_id, created_at DESC),
    CONSTRAINT fk_point_histories_user_id FOREIGN KEY (user_id) REFERENCES users (user_id)
) DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS brands
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    logo_url    VARCHAR(500) NULL,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    deleted_at  DATETIME(6)  NULL,
    PRIMARY KEY (id),
    INDEX idx_brands_name (name)
) DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS products
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    brand_id   BIGINT       NOT NULL,
    name       VARCHAR(255) NOT NULL,
    price      BIGINT       NOT NULL COMMENT 'KRW integer amount (won)',
    quantity   BIGINT       NOT NULL,
    status     VARCHAR(20)  NOT NULL DEFAULT 'ON_SALE',
    version    BIGINT       NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    deleted_at DATETIME(6)  NULL,
    PRIMARY KEY (id),
    INDEX idx_products_brand_id (brand_id),
    INDEX idx_products_status (status),
    CONSTRAINT chk_products_price_non_negative CHECK (price >= 0),
    CONSTRAINT chk_products_quantity_non_negative CHECK (quantity >= 0),
    CONSTRAINT fk_products_brand_id FOREIGN KEY (brand_id) REFERENCES brands (id)
) DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS product_aggregate
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    product_id BIGINT      NOT NULL,
    like_count BIGINT      NOT NULL DEFAULT 0,
    order_count BIGINT     NOT NULL DEFAULT 0,
    view_count BIGINT      NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_product_aggregate_product_id UNIQUE (product_id),
    CONSTRAINT chk_product_aggregate_like_count_non_negative CHECK (like_count >= 0),
    CONSTRAINT chk_product_aggregate_order_count_non_negative CHECK (order_count >= 0),
    CONSTRAINT chk_product_aggregate_view_count_non_negative CHECK (view_count >= 0),
    CONSTRAINT fk_product_aggregate_product_id FOREIGN KEY (product_id) REFERENCES products (id)
) DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS product_counter_event_history
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    dedupe_key    VARCHAR(128) NOT NULL,
    product_id    BIGINT       NOT NULL,
    counter_type  VARCHAR(20)  NOT NULL,
    process_status VARCHAR(20) NOT NULL,
    failed_reason VARCHAR(255) NULL,
    processed_at  DATETIME(6)  NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    deleted_at    DATETIME(6)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_product_counter_event_history_dedupe_key UNIQUE (dedupe_key),
    INDEX idx_product_counter_event_history_product_id (product_id),
    INDEX idx_product_counter_event_history_counter_type (counter_type),
    INDEX idx_product_counter_event_history_process_status (process_status)
) DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS product_ranking
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    product_id    BIGINT       NOT NULL,
    rank_type     VARCHAR(20)  NOT NULL,
    rank_position BIGINT       NOT NULL,
    score         DOUBLE       NOT NULL,
    rank_date     DATE         NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    deleted_at    DATETIME(6)  NULL,
    PRIMARY KEY (id),
    INDEX idx_product_ranking_type_date (rank_type, rank_date),
    INDEX idx_product_ranking_product_id (product_id),
    CONSTRAINT fk_product_ranking_product_id FOREIGN KEY (product_id) REFERENCES products (id)
) DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS likes
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    VARCHAR(20) NOT NULL,
    product_id BIGINT      NOT NULL,
    version    BIGINT      NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_likes_user_product UNIQUE (user_id, product_id),
    INDEX idx_likes_user_id_created_at (user_id, created_at DESC),
    INDEX idx_likes_product_id (product_id),
    CONSTRAINT fk_likes_user_id FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_likes_product_id FOREIGN KEY (product_id) REFERENCES products (id)
) DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS coupons
(
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    user_id             VARCHAR(20)  NOT NULL,
    product_id          BIGINT       NOT NULL,
    brand_id            BIGINT       NOT NULL,
    coupon_name         VARCHAR(255) NOT NULL,
    discount_value      BIGINT       NOT NULL,
    max_discount_amount BIGINT       NULL,
    coupon_type         VARCHAR(20)  NOT NULL,
    issued_at           DATETIME(6)  NOT NULL,
    used_at             DATETIME(6)  NOT NULL,
    expired_at          DATETIME(6)  NOT NULL,
    coupon_status       VARCHAR(20)  NOT NULL,
    version             BIGINT       NOT NULL DEFAULT 0,
    created_at          DATETIME(6)  NOT NULL,
    updated_at          DATETIME(6)  NOT NULL,
    deleted_at          DATETIME(6)  NULL,
    PRIMARY KEY (id),
    INDEX idx_coupons_user_id (user_id),
    INDEX idx_coupons_product_id (product_id),
    INDEX idx_coupons_brand_id (brand_id),
    INDEX idx_coupons_status (coupon_status),
    INDEX idx_coupons_expired_at (expired_at),
    CONSTRAINT chk_coupons_discount_value_non_negative CHECK (discount_value >= 0),
    CONSTRAINT chk_coupons_max_discount_amount_non_negative CHECK (max_discount_amount IS NULL OR max_discount_amount >= 0),
    CONSTRAINT chk_coupons_percentage_discount_range CHECK (
        coupon_type <> 'PERCENTAGE' OR (discount_value >= 0 AND discount_value <= 100)
    ),
    CONSTRAINT fk_coupons_user_id FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_coupons_product_id FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_coupons_brand_id FOREIGN KEY (brand_id) REFERENCES brands (id)
) DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS orders
(
    id                     BIGINT      NOT NULL AUTO_INCREMENT,
    user_id                VARCHAR(20) NOT NULL,
    total_price            BIGINT      NOT NULL,
    coupon_discount_amount BIGINT      NOT NULL,
    final_payment_amount   BIGINT      NOT NULL,
    coupon_id              BIGINT      NULL,
    order_number           VARCHAR(50) NOT NULL,
    status                 VARCHAR(20) NOT NULL,
    created_at             DATETIME(6) NOT NULL,
    updated_at             DATETIME(6) NOT NULL,
    deleted_at             DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_orders_order_number UNIQUE (order_number),
    INDEX idx_orders_user_id_created_at (user_id, created_at DESC),
    INDEX idx_orders_status (status),
    CONSTRAINT chk_orders_total_price_non_negative CHECK (total_price >= 0),
    CONSTRAINT chk_orders_coupon_discount_amount_non_negative CHECK (coupon_discount_amount >= 0),
    CONSTRAINT chk_orders_final_payment_amount_non_negative CHECK (final_payment_amount >= 0),
    CONSTRAINT fk_orders_user_id FOREIGN KEY (user_id) REFERENCES users (user_id)
) DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS order_items
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    order_id   VARCHAR(50) NOT NULL,
    product_id BIGINT      NOT NULL,
    quantity   BIGINT      NOT NULL,
    price      BIGINT      NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    INDEX idx_order_items_order_id (order_id),
    INDEX idx_order_items_product_id (product_id),
    CONSTRAINT chk_order_items_quantity_non_negative CHECK (quantity >= 0),
    CONSTRAINT chk_order_items_price_non_negative CHECK (price >= 0),
    CONSTRAINT fk_order_items_order_id FOREIGN KEY (order_id) REFERENCES orders (order_number),
    CONSTRAINT fk_order_items_product_id FOREIGN KEY (product_id) REFERENCES products (id)
) DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS order_history
(
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    user_id         VARCHAR(20)  NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    order_id        VARCHAR(50)  NULL,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    deleted_at      DATETIME(6)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_order_history_user_idempotency_key UNIQUE (user_id, idempotency_key),
    INDEX idx_order_history_order_id (order_id),
    CONSTRAINT fk_order_history_user_id FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_order_history_order_id FOREIGN KEY (order_id) REFERENCES orders (order_number)
) DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS event_outbox
(
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    event_type    VARCHAR(50)   NOT NULL,
    aggregate_id  VARCHAR(100)  NOT NULL,
    dedupe_key    VARCHAR(150)  NOT NULL,
    payload       LONGTEXT      NOT NULL,
    status        VARCHAR(20)   NOT NULL,
    retry_count   INT           NOT NULL DEFAULT 0,
    next_retry_at DATETIME(6)   NOT NULL,
    last_error    VARCHAR(500)  NULL,
    created_at    DATETIME(6)   NOT NULL,
    updated_at    DATETIME(6)   NOT NULL,
    deleted_at    DATETIME(6)   NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_event_outbox_dedupe_key UNIQUE (dedupe_key),
    INDEX idx_event_outbox_status_next_retry (status, next_retry_at),
    INDEX idx_event_outbox_event_type_created_at (event_type, created_at)
) DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS payment_callback_history
(
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    dedupe_key      VARCHAR(128) NOT NULL,
    transaction_key VARCHAR(255) NULL,
    order_id        VARCHAR(50)  NULL,
    callback_status VARCHAR(20)  NULL,
    process_status  VARCHAR(20)  NOT NULL,
    failed_reason   VARCHAR(255) NULL,
    processed_at    DATETIME(6)  NULL,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    deleted_at      DATETIME(6)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_payment_callback_history_dedupe_key UNIQUE (dedupe_key),
    INDEX idx_payment_callback_history_transaction_key (transaction_key),
    INDEX idx_payment_callback_history_order_id (order_id),
    INDEX idx_payment_callback_history_process_status (process_status)
) DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS payments
(
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    order_id        VARCHAR(50)  NOT NULL,
    user_id         VARCHAR(20)  NOT NULL,
    card_type       VARCHAR(20)  NOT NULL,
    card_no         VARCHAR(19)  NOT NULL,
    price           BIGINT       NOT NULL,
    callback_url    VARCHAR(500) NOT NULL,
    transaction_key VARCHAR(255) NULL,
    status          VARCHAR(20)  NOT NULL,
    reason          VARCHAR(255) NOT NULL,
    version         BIGINT       NOT NULL DEFAULT 0,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    deleted_at      DATETIME(6)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_payments_transaction_key UNIQUE (transaction_key),
    INDEX idx_payments_order_id (order_id),
    INDEX idx_payments_status (status),
    CONSTRAINT chk_payments_price_non_negative CHECK (price >= 0),
    CONSTRAINT fk_payments_order_id FOREIGN KEY (order_id) REFERENCES orders (order_number),
    CONSTRAINT fk_payments_user_id FOREIGN KEY (user_id) REFERENCES users (user_id)
) DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
