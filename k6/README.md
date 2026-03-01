# K6 Performance Testing

이 디렉토리는 Loopers Commerce API의 성능 테스트를 위한 k6 스크립트들을 포함합니다.

## 디렉토리 구조

```
k6/
├── config/
│   ├── base.js          # 기본 설정 (URL, 상수값)
│   └── thresholds.js    # 성능 임계값 설정
├── tests/
│   ├── load/            # 부하 테스트
│   ├── smoke/           # 스모크 테스트
│   └── stress/          # 스트레스 테스트
└── utils/
    ├── api.js           # API 유틸리티
    └── helpers.js       # 헬퍼 함수
```

## 테스트 유형

### 1. Smoke Test
```bash
k6 run k6/tests/smoke/products-smoke-test.js
```
- 기본적인 기능 검증
- 1-2명의 가상 사용자
- 짧은 실행 시간

### 2. Load Test
```bash
k6 run k6/tests/load/products-load-test.js
```
- 일반적인 부하 상황 테스트
- 최대 20명의 가상 사용자
- 중간 지속 시간

### 3. Stress Test
```bash
k6 run k6/tests/stress/products-stress-test.js
```
- 높은 부하 상황 테스트
- 최대 250명의 가상 사용자
- 시스템 한계점 확인

### 4. Comprehensive Stress Test
```bash
k6 run k6/tests/stress/comprehensive-stress-test.js
```
- 종합적인 스트레스 테스트
- 최대 500명의 가상 사용자
- 실제 사용자 패턴 모방
- 복합 시나리오 포함

### 5. Order Lock Benchmark
```bash
k6 run k6/tests/load/order-lock-low-contention.js
k6 run k6/tests/stress/order-lock-medium-contention.js
k6 run k6/tests/stress/order-lock-high-contention.js
```
- 주문 경합 시나리오 기반 락 전략 비교용
- 상세 가이드: `k6/ORDER_LOCK_BENCHMARK.md`

## 성능 임계값

### Load Test (PERFORMANCE_THRESHOLDS)
- p95 < 50ms
- p99 < 100ms
- p99.9 < 200ms
- 에러율 < 1%

### Stress Test (STRESS_THRESHOLDS)
- p90 < 300ms
- p95 < 500ms
- p99 < 1s
- p99.9 < 2s
- 에러율 < 10%

## 설정

### Base Configuration (config/base.js)
- `BASE_URL`: API 기본 URL
- `BRAND_ID_RANGE`: 테스트용 브랜드 ID 범위
- `SORT_OPTIONS`: 정렬 옵션들

### API Endpoints
- 상품 목록 조회: `GET /api/v1/products`
- 쿼리 파라미터:
  - `sort`: 정렬 방식 (createdAt, price_asc, likes_desc)
  - `brandId`: 브랜드 필터

## 실행 전 확인사항

1. API 서버가 실행 중인지 확인 (localhost:8080)
2. 테스트 데이터가 준비되어 있는지 확인
3. 브랜드 ID 범위가 실제 데이터와 일치하는지 확인

## 결과 분석

테스트 실행 후 다음 메트릭들을 확인하세요:

- **HTTP Request Duration**: 응답 시간 분포
- **HTTP Request Failed**: 실패율
- **Custom Response Time**: 커스텀 응답 시간 메트릭
- **Throughput**: 처리량 (requests/second)
- **Error Rate**: 에러율 (stress test only)
