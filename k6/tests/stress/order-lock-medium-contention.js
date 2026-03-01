import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

import { BASE_URL, SUMMARY_TREND_STATS } from '../../config/base.js';
import {
    benchmarkProductId,
    benchmarkUserId,
    floatEnv,
    intEnv,
    isExpectedConflictStatus,
    makeIdempotencyKey,
    orderRequestBody,
    randomIntInclusive,
} from '../../utils/order-benchmark.js';

const orderDuration = new Trend('order_create_duration', true);
const orderSuccessRate = new Rate('order_success_rate');
const orderConflictRate = new Rate('order_conflict_rate');
const orderServerErrorRate = new Rate('order_server_error_rate');

const userCount = intEnv('USER_COUNT', 10000);
const productCount = intEnv('PRODUCT_COUNT', 1000);
const productIdStart = intEnv('PRODUCT_ID_START', 920001);
const hotProductCount = intEnv('HOT_PRODUCT_COUNT', 20);
const hotTrafficRatio = floatEnv('HOT_TRAFFIC_RATIO', 0.8);
const boundedHotProductCount = Math.min(Math.max(hotProductCount, 1), productCount);
const normalizedHotTrafficRatio = Math.min(Math.max(hotTrafficRatio, 0), 1);
const sleepSeconds = floatEnv('SLEEP_SECONDS', 0.02);
const lockMode = __ENV.LOCK_MODE || 'pessimistic';
const cardType = __ENV.CARD_TYPE || 'KB';
const cardNo = __ENV.CARD_NO || '1111-2222-3333-4444';
const callbackUrl = __ENV.CALLBACK_URL || 'https://callback.local/k6';
const orderApiUrl = `${BASE_URL}/orders`;

export const options = {
    stages: [
        { duration: '30s', target: 20 },
        { duration: '2m', target: 80 },
        { duration: '2m', target: 80 },
        { duration: '30s', target: 0 },
    ],
    summaryTrendStats: SUMMARY_TREND_STATS,
    thresholds: {
        http_req_failed: ['rate<0.05'],
        http_req_duration: ['p(95)<1500', 'p(99)<2500'],
        order_success_rate: ['rate>0.9'],
        order_conflict_rate: ['rate<0.1'],
        order_server_error_rate: ['rate<0.02'],
    },
};

function pickProductId() {
    const applyHotPath = Math.random() < normalizedHotTrafficRatio;

    if (applyHotPath) {
        return benchmarkProductId(randomIntInclusive(1, boundedHotProductCount), productIdStart);
    }

    if (boundedHotProductCount >= productCount) {
        return benchmarkProductId(randomIntInclusive(1, productCount), productIdStart);
    }

    return benchmarkProductId(randomIntInclusive(boundedHotProductCount + 1, productCount), productIdStart);
}

export default function () {
    const userId = benchmarkUserId(randomIntInclusive(1, userCount));
    const productId = pickProductId();
    const idempotencyKey = makeIdempotencyKey('order-medium');

    const response = http.post(
        orderApiUrl,
        orderRequestBody(productId, null, cardType, cardNo, callbackUrl),
        {
            headers: {
                'Content-Type': 'application/json',
                'X-USER-ID': userId,
                'X-IDEMPOTENCY-KEY': idempotencyKey,
            },
            tags: {
                scenario: 'medium_contention',
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
