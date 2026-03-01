import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

import { BASE_URL, SUMMARY_TREND_STATS } from '../../config/base.js';
import {
    boolEnv,
    floatEnv,
    intEnv,
    isExpectedConflictStatus,
    makeIdempotencyKey,
    orderRequestBody,
} from '../../utils/order-benchmark.js';

const orderDuration = new Trend('order_create_duration', true);
const orderSuccessRate = new Rate('order_success_rate');
const orderConflictRate = new Rate('order_conflict_rate');
const orderServerErrorRate = new Rate('order_server_error_rate');

const lockMode = __ENV.LOCK_MODE || 'pessimistic';
const hotUserId = __ENV.HOT_USER_ID || 'k6hotuser';
const hotProductId = intEnv('HOT_PRODUCT_ID', 920001);
const useCoupon = boolEnv('USE_COUPON', false);
const hotCouponId = intEnv('HOT_COUPON_ID', 930001);
const sleepSeconds = floatEnv('SLEEP_SECONDS', 0);
const cardType = __ENV.CARD_TYPE || 'KB';
const cardNo = __ENV.CARD_NO || '1111-2222-3333-4444';
const callbackUrl = __ENV.CALLBACK_URL || 'https://callback.local/k6';
const orderApiUrl = `${BASE_URL}/orders`;

export const options = {
    stages: [
        { duration: '20s', target: 30 },
        { duration: '40s', target: 120 },
        { duration: '90s', target: 120 },
        { duration: '20s', target: 0 },
    ],
    summaryTrendStats: SUMMARY_TREND_STATS,
    thresholds: {
        http_req_failed: ['rate<0.15'],
        http_req_duration: ['p(95)<2500', 'p(99)<4000'],
        order_success_rate: ['rate>0.75'],
        order_conflict_rate: ['rate<0.25'],
        order_server_error_rate: ['rate<0.05'],
    },
};

export default function () {
    const idempotencyKey = makeIdempotencyKey('order-high');
    const couponId = useCoupon ? hotCouponId : null;

    const response = http.post(
        orderApiUrl,
        orderRequestBody(hotProductId, couponId, cardType, cardNo, callbackUrl),
        {
            headers: {
                'Content-Type': 'application/json',
                'X-USER-ID': hotUserId,
                'X-IDEMPOTENCY-KEY': idempotencyKey,
            },
            tags: {
                scenario: useCoupon ? 'high_contention_coupon' : 'high_contention_stock_point',
                lock_mode: lockMode,
            },
        }
    );

    orderDuration.add(response.timings.duration);

    const success = response.status === 200;
    const conflict = isExpectedConflictStatus(response.status);
    const serverError = response.status >= 500;

    orderSuccessRate.add(success);
    orderConflictRate.add(conflict);
    orderServerErrorRate.add(serverError);

    check(response, {
        'status is expected': (r) => r.status === 200 || isExpectedConflictStatus(r.status),
    });

    sleep(sleepSeconds);
}
