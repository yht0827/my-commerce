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

CREATE TABLE IF NOT EXISTS stocks
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    product_id BIGINT      NOT NULL,
    quantity   BIGINT      NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_stocks_product_id UNIQUE (product_id),
    CONSTRAINT chk_stocks_quantity_non_negative CHECK (quantity >= 0),
    CONSTRAINT fk_stocks_product_id FOREIGN KEY (product_id) REFERENCES products (id)
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
