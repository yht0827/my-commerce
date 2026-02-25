# 클래스 다이어그램

> `apps/commerce-api` 현재 코드 기준 상세 클래스 구조

## 목차

- [전체 레이어 구조](#전체-레이어-구조)
- [회원/포인트](#회원포인트)
- [상품/좋아요/집계](#상품좋아요집계)
- [주문/아웃박스](#주문아웃박스)
- [결제/콜백](#결제콜백)
- [랭킹](#랭킹)

---

## 전체 레이어 구조

```mermaid
classDiagram
    direction TB

    class Interfaces
    class Application
    class Domain
    class Infrastructure

    Interfaces --> Application : invokes
    Application --> Domain : orchestrates
    Domain --> Infrastructure : repository impl
```

---

## 회원/포인트

```mermaid
classDiagram
    direction LR

    class UserV1Controller
    class UserApplicationService
    class UserService
    class UserRepository
    class User

    UserV1Controller --> UserApplicationService
    UserApplicationService --> UserService
    UserService --> UserRepository
    UserRepository --> User

    class PointV1Controller
    class PointApplicationService {
        +chargePoint(command) PointResult
        +getPoint(query) PointResult
    }
    class PointService {
        +charge(userId,amount) Point
        +use(userId,amount) Point
        +findByUserId(userId) Point
    }
    class PointRepository
    class PointHistoryRepository
    class Point
    class PointHistory

    PointV1Controller --> PointApplicationService
    PointApplicationService --> PointService
    PointService --> PointRepository
    PointService --> PointHistoryRepository
    PointRepository --> Point
    PointHistoryRepository --> PointHistory
```

---

## 상품/좋아요/집계

```mermaid
classDiagram
    direction TB

    class ProductV1Controller
    class ProductApplicationService {
        +getProductList(query) ProductListResult
        +getProductDetail(query) ProductDetailResult
    }
    class ProductQueryService
    class ProductRepository
    class ProductAggregateRepository
    class Product
    class ProductAggregate

    ProductV1Controller --> ProductApplicationService
    ProductApplicationService --> ProductQueryService
    ProductQueryService --> ProductRepository
    ProductRepository --> Product
    ProductAggregateRepository --> ProductAggregate

    class LikeV1Controller
    class LikeApplicationService
    class LikeService
    class LikeRepository
    class Like

    LikeV1Controller --> LikeApplicationService
    LikeApplicationService --> LikeService
    LikeService --> LikeRepository
    LikeRepository --> Like

    class LikeEventHandler {
        +handleProductLiked(event)
        +handleProductUnliked(event)
    }
    class ProductCounterEventHandler {
        +handleProductOrdered(event)
        +handleProductViewed(event)
    }
    class ProductCounterEventHistoryService {
        +generateDedupeKey(source) String
        +claim(dedupeKey, productId, type) boolean
        +markProcessing(dedupeKey)
        +complete(dedupeKey)
        +fail(dedupeKey, reason)
    }
    class ProductAggregateService
    class ProductCounterReconciliationService {
        +reconcileAllAggregates()
        +retryFailedCounterEvents(limit)
        +reconcileProduct(productId)
    }
    class ProductCounterReconciliationScheduler

    LikeEventHandler --> ProductCounterEventHistoryService
    LikeEventHandler --> ProductAggregateService
    ProductCounterEventHandler --> ProductCounterEventHistoryService
    ProductCounterEventHandler --> ProductAggregateService
    ProductCounterEventHandler --> RankingRealtimeUpdateService
    ProductCounterReconciliationService --> ProductAggregateRepository
    ProductCounterReconciliationService --> ProductCounterEventHistoryRepository
    ProductCounterReconciliationService --> LikeRepository
    ProductCounterReconciliationService --> OrderItemRepository
    ProductCounterReconciliationScheduler --> ProductCounterReconciliationService
```

---

## 주문/아웃박스

```mermaid
classDiagram
    direction TB

    class OrderV1Controller
    class OrderApplicationService {
        +createOrder(command) OrderResult
        +getOrders(query) List~OrderResult~
        +getOrder(query) OrderResult
    }
    class OrderProcessor {
        +process(command) OrderProcessResult
    }
    class OrderService {
        +createOrder(data,items,total,discount) OrderInfo
        +updateOrderStatus(orderId,status)
        +getOrder(data) OrderInfo
        +getOrders(data) List~OrderInfo~
    }
    class OrderHistoryRepository {
        +createIfNotExists(userId,key) boolean
        +findByUserIdAndIdempotencyKey(userId,key)
    }
    class OutboxService
    class OutboxDispatcherService
    class OutboxEventScheduler

    OrderV1Controller --> OrderApplicationService
    OrderApplicationService --> OrderHistoryRepository
    OrderApplicationService --> OrderProcessor
    OrderApplicationService --> OrderService
    OrderApplicationService --> OutboxService

    OrderProcessor --> ProductStockService
    OrderProcessor --> CouponService
    OrderProcessor --> PointService
    OrderProcessor --> OrderService

    OutboxEventScheduler --> OutboxDispatcherService
    OutboxDispatcherService --> OutboxService
    OutboxDispatcherService --> PaymentProcessor
    OutboxDispatcherService --> PaymentResultOutboxService
    OutboxDispatcherService --> OrderService
    OutboxDispatcherService --> DataPlatformApplicationService
```

---

## 결제/콜백

```mermaid
classDiagram
    direction TB

    class PaymentV1Controller {
        +handleCallback(callbackHeaders, rawBody)
    }

    class PaymentApplicationService {
        +acceptCallback(command) boolean
        +processCallback(command) PaymentResult
        +processOrderPayment(orderCreatedEvent) PaymentResult
    }

    class PaymentProcessor {
        +process(createPaymentCommand) PaymentInfo
    }

    class PaymentService {
        +createPayment(request) Payment
        +updatePaymentStatus(payment, pgTransaction) PaymentInfo
        +processCallback(callback) PaymentInfo
    }

    class PaymentGatewayService {
        +requestPaymentGateway(request) Transaction
        +processCallbackWithVerification(payment, callback) PaymentInfo
        +verifyPendingPayments() List~PaymentInfo~
    }

    class PaymentCallbackSignatureVerifier
    class PaymentCallbackHistoryService
    class PaymentCallbackAsyncProcessor
    class PaymentResultOutboxService
    class PaymentEventHandler
    class PaymentVerificationScheduler

    PaymentV1Controller --> PaymentApplicationService
    PaymentApplicationService --> PaymentCallbackSignatureVerifier
    PaymentApplicationService --> PaymentCallbackHistoryService
    PaymentApplicationService --> PaymentCallbackAsyncProcessor
    PaymentApplicationService --> PaymentProcessor
    PaymentApplicationService --> PaymentService

    PaymentCallbackAsyncProcessor --> PaymentService
    PaymentCallbackAsyncProcessor --> PaymentCallbackHistoryService
    PaymentCallbackAsyncProcessor --> PaymentResultOutboxService

    PaymentProcessor --> PaymentService
    PaymentProcessor --> PaymentGatewayService
    PaymentService --> PaymentGatewayService
    PaymentGatewayService --> PaymentStatusSynchronizer

    PaymentEventHandler --> PaymentApplicationService
    PaymentVerificationScheduler --> PaymentGatewayService
    PaymentVerificationScheduler --> PaymentResultOutboxService
```

---

## 랭킹

```mermaid
classDiagram
    direction LR

    class RankingV1Controller
    class RankingApplicationService
    class RankingQueryService
    class RedisRankingRepository
    class RankingSnapshotRepository
    class RankingRealtimeUpdateService
    class RankingPeriod

    RankingV1Controller --> RankingApplicationService
    RankingApplicationService --> RankingQueryService
    RankingQueryService --> RedisRankingRepository
    RankingQueryService --> RankingSnapshotRepository
    RankingRealtimeUpdateService --> RedisRankingRepository
    RankingQueryService --> RankingPeriod
```

---

## 동시성/멱등성 제어 포인트

- 주문 경로 행락: `products`, `coupons`, `points`는 `PESSIMISTIC_WRITE` 기반 접근
- 엔티티 버전: `Product`, `Coupon`, `Like`, `Payment`
- 주문 멱등성: `order_history(user_id, idempotency_key)` 유니크 + claim 패턴
- 콜백 멱등성: `payment_callback_history.dedupe_key` 유니크
- 카운터 멱등성: `product_counter_event_history.dedupe_key` 유니크
- 아웃박스 멱등성: `event_outbox.dedupe_key` 유니크
