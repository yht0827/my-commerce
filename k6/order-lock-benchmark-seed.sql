SET @seed_now := NOW(6);

INSERT INTO brands (id, name, description, logo_url, created_at, updated_at, deleted_at)
SELECT
    910000 + n,
    CONCAT('K6-BRAND-', LPAD(n, 2, '0')),
    'k6 benchmark seed brand',
    CONCAT('https://example.com/brands/', 910000 + n, '.png'),
    @seed_now,
    @seed_now,
    NULL
FROM (
    SELECT (tens.n * 10 + ones.n + 1) AS n
    FROM (
        SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) ones
    CROSS JOIN (
        SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) tens
    WHERE (tens.n * 10 + ones.n + 1) <= 30
) seq
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    logo_url = VALUES(logo_url),
    updated_at = VALUES(updated_at),
    deleted_at = NULL;

INSERT INTO users (user_id, email, birthday, gender, created_at, updated_at, deleted_at)
SELECT
    CONCAT('k6u', LPAD(n, 5, '0')),
    CONCAT('k6u', LPAD(n, 5, '0'), '@bench.local'),
    '1990-01-01',
    'MALE',
    @seed_now,
    @seed_now,
    NULL
FROM (
    SELECT
        (d4.n * 1000 + d3.n * 100 + d2.n * 10 + d1.n + 1) AS n
    FROM (
        SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) d1
    CROSS JOIN (
        SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) d2
    CROSS JOIN (
        SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) d3
    CROSS JOIN (
        SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) d4
    WHERE (d4.n * 1000 + d3.n * 100 + d2.n * 10 + d1.n + 1) <= 10000
) seq
ON DUPLICATE KEY UPDATE
    email = VALUES(email),
    birthday = VALUES(birthday),
    gender = VALUES(gender),
    updated_at = VALUES(updated_at),
    deleted_at = NULL;

INSERT INTO users (user_id, email, birthday, gender, created_at, updated_at, deleted_at)
VALUES ('k6hotuser', 'k6hotuser@bench.local', '1990-01-01', 'MALE', @seed_now, @seed_now, NULL)
ON DUPLICATE KEY UPDATE
    email = VALUES(email),
    birthday = VALUES(birthday),
    gender = VALUES(gender),
    updated_at = VALUES(updated_at),
    deleted_at = NULL;

INSERT INTO points (user_id, balance, created_at, updated_at, deleted_at)
SELECT
    u.user_id,
    100000000.00,
    @seed_now,
    @seed_now,
    NULL
FROM users u
WHERE u.user_id LIKE 'k6u%'
   OR u.user_id = 'k6hotuser'
ON DUPLICATE KEY UPDATE
    balance = VALUES(balance),
    updated_at = VALUES(updated_at),
    deleted_at = NULL;

INSERT INTO products (id, brand_id, name, price, quantity, status, version, created_at, updated_at, deleted_at)
SELECT
    920000 + n,
    910001 + MOD((n - 1), 30),
    CONCAT('K6-PRODUCT-', LPAD(n, 4, '0')),
    CASE
        WHEN n <= 700 THEN 10000 + MOD(n * 137, 30000)
        WHEN n <= 900 THEN 40000 + MOD(n * 271, 110000)
        WHEN n <= 980 THEN 150000 + MOD(n * 389, 350000)
        ELSE 500000 + MOD(n * 997, 1500000)
    END,
    100000,
    'ON_SALE',
    0,
    @seed_now,
    @seed_now,
    NULL
FROM (
    SELECT
        (d3.n * 100 + d2.n * 10 + d1.n + 1) AS n
    FROM (
        SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) d1
    CROSS JOIN (
        SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) d2
    CROSS JOIN (
        SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) d3
    WHERE (d3.n * 100 + d2.n * 10 + d1.n + 1) <= 1000
) seq
ON DUPLICATE KEY UPDATE
    brand_id = VALUES(brand_id),
    name = VALUES(name),
    price = VALUES(price),
    quantity = VALUES(quantity),
    status = VALUES(status),
    version = VALUES(version),
    updated_at = VALUES(updated_at),
    deleted_at = NULL;

INSERT INTO stocks (product_id, quantity, created_at, updated_at, deleted_at)
SELECT
    p.id,
    100000,
    @seed_now,
    @seed_now,
    NULL
FROM products p
WHERE p.id BETWEEN 920001 AND 921000
ON DUPLICATE KEY UPDATE
    quantity = VALUES(quantity),
    updated_at = VALUES(updated_at),
    deleted_at = NULL;

INSERT INTO product_aggregate (product_id, like_count, order_count, view_count, created_at, updated_at, deleted_at)
SELECT
    p.id,
    100,
    0,
    1000,
    @seed_now,
    @seed_now,
    NULL
FROM products p
WHERE p.id BETWEEN 920001 AND 921000
ON DUPLICATE KEY UPDATE
    like_count = VALUES(like_count),
    order_count = VALUES(order_count),
    view_count = VALUES(view_count),
    updated_at = VALUES(updated_at),
    deleted_at = NULL;

INSERT INTO coupons (
    id,
    user_id,
    product_id,
    brand_id,
    coupon_name,
    discount_value,
    max_discount_amount,
    coupon_type,
    issued_at,
    used_at,
    expired_at,
    coupon_status,
    version,
    created_at,
    updated_at,
    deleted_at
)
VALUES (
    930001,
    'k6hotuser',
    920001,
    910001,
    'K6-HOT-COUPON',
    1000,
    NULL,
    'FIXED_AMOUNT',
    DATE_SUB(@seed_now, INTERVAL 1 DAY),
    DATE_SUB(@seed_now, INTERVAL 1 DAY),
    DATE_ADD(@seed_now, INTERVAL 30 DAY),
    'ACTIVE',
    0,
    @seed_now,
    @seed_now,
    NULL
)
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id),
    product_id = VALUES(product_id),
    brand_id = VALUES(brand_id),
    coupon_name = VALUES(coupon_name),
    discount_value = VALUES(discount_value),
    max_discount_amount = VALUES(max_discount_amount),
    coupon_type = VALUES(coupon_type),
    issued_at = VALUES(issued_at),
    used_at = VALUES(used_at),
    expired_at = VALUES(expired_at),
    coupon_status = VALUES(coupon_status),
    version = VALUES(version),
    updated_at = VALUES(updated_at),
    deleted_at = NULL;

SELECT 'users_k6' AS metric, COUNT(*) AS count_value FROM users WHERE user_id LIKE 'k6u%'
UNION ALL
SELECT 'points_k6', COUNT(*) FROM points WHERE user_id LIKE 'k6u%'
UNION ALL
SELECT 'products_k6', COUNT(*) FROM products WHERE id BETWEEN 920001 AND 921000
UNION ALL
SELECT 'stocks_k6', COUNT(*) FROM stocks WHERE product_id BETWEEN 920001 AND 921000
UNION ALL
SELECT 'product_aggregate_k6', COUNT(*) FROM product_aggregate WHERE product_id BETWEEN 920001 AND 921000
UNION ALL
SELECT 'hot_coupon', COUNT(*) FROM coupons WHERE id = 930001;
