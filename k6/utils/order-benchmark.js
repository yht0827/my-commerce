import exec from 'k6/execution';

export function intEnv(name, fallbackValue) {
    const raw = __ENV[name];
    if (!raw) {
        return fallbackValue;
    }

    const parsed = Number.parseInt(raw, 10);
    return Number.isFinite(parsed) ? parsed : fallbackValue;
}

export function floatEnv(name, fallbackValue) {
    const raw = __ENV[name];
    if (!raw) {
        return fallbackValue;
    }

    const parsed = Number.parseFloat(raw);
    return Number.isFinite(parsed) ? parsed : fallbackValue;
}

export function boolEnv(name, fallbackValue = false) {
    const raw = __ENV[name];
    if (!raw) {
        return fallbackValue;
    }

    return raw === '1' || raw.toLowerCase() === 'true' || raw.toLowerCase() === 'yes';
}

export function randomIntInclusive(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

export function benchmarkUserId(index) {
    return `k6u${String(index).padStart(5, '0')}`;
}

export function benchmarkProductId(index, productIdStart) {
    return productIdStart + index - 1;
}

export function makeIdempotencyKey(prefix) {
    return `${prefix}-${exec.vu.idInTest}-${exec.scenario.iterationInTest}-${Date.now()}`;
}

export function orderRequestBody(productId, couponId, cardType, cardNo, callbackUrl) {
    return JSON.stringify({
        items: [{ productId, quantity: 1 }],
        couponId,
        cardType,
        cardNo,
        callbackUrl,
    });
}

export function isExpectedConflictStatus(status) {
    return status === 409 || status === 429;
}
