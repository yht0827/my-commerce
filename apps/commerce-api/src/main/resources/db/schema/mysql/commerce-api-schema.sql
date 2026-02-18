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
    price      BIGINT       NOT NULL,
    quantity   BIGINT       NOT NULL,
    status     VARCHAR(20)  NOT NULL DEFAULT 'ON_SALE',
    version    BIGINT       NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    deleted_at DATETIME(6)  NULL,
    PRIMARY KEY (id),
    INDEX idx_products_brand_id (brand_id),
    INDEX idx_products_status (status),
    CONSTRAINT fk_products_brand_id FOREIGN KEY (brand_id) REFERENCES brands (id)
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
    CONSTRAINT fk_stocks_product_id FOREIGN KEY (product_id) REFERENCES products (id)
) DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
