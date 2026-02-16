CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    email VARCHAR(255) NOT NULL,
    birthday VARCHAR(10) NOT NULL,
    gender VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_user_id UNIQUE (user_id),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS points (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    balance DECIMAL(19, 2) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_points_user_id UNIQUE (user_id),
    CONSTRAINT fk_points_user_id FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS point_histories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    type VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    INDEX idx_point_histories_user_id_created_at (user_id, created_at DESC),
    CONSTRAINT fk_point_histories_user_id FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;
