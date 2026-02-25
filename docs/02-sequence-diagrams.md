# 시퀀스 다이어그램

> `apps/commerce-api` 현재 코드 기준 상세 흐름

## 목차

- [회원 (User)](#회원-user)
- [포인트 (Point)](#포인트-point)
- [상품 (Product)](#상품-product)
- [브랜드 (Brand)](#브랜드-brand)
- [좋아요 (Like)](#좋아요-like)
- [주문/쿠폰 (Order/Coupon)](#주문쿠폰-ordercoupon)
- [결제 (Payment)](#결제-payment)
- [랭킹 (Ranking)](#랭킹-ranking)

---

## 회원 (User)

### 회원가입

`POST /api/v1/users`

```mermaid
sequenceDiagram
    autonumber
    actor User as 사용자
    participant Client as 클라이언트
    participant API as UserV1Controller
    participant App as UserApplicationService
    participant Domain as UserService
    participant DB as Database

    User->>Client: 회원정보 입력
    Client->>API: POST /api/v1/users
    API->>App: register(command)
    App->>Domain: register(command)
    Domain->>DB: userId/email 중복 검증
    Domain->>DB: users INSERT
    Domain-->>App: UserResult
    App-->>API: UserResult
    API-->>Client: 201 Created
```

### 내 정보 조회

`GET /api/v1/users/me`

```mermaid
sequenceDiagram
    autonumber
    actor User as 사용자
    participant Client as 클라이언트
    participant API as UserV1Controller
    participant Resolver as CurrentUserIdArgumentResolver
    participant App as UserApplicationService
    participant Domain as UserService
    participant DB as Database

    User->>Client: 마이페이지 접근
    Client->>API: GET /api/v1/users/me (X-USER-ID)
    API->>Resolver: @CurrentUserId resolve
    Resolver-->>API: userId
    API->>App: getUser(query)
    App->>Domain: findByUserId
    Domain->>DB: users SELECT
    API-->>Client: 200 OK
```

---

## 포인트 (Point)

### 포인트 충전

`POST /api/v1/points/charge`

```mermaid
sequenceDiagram
    autonumber
    actor User as 사용자
    participant API as PointV1Controller
    participant App as PointApplicationService
    participant Domain as PointService
    participant Hist as PointHistoryRepository
    participant DB as Database

    User->>API: 충전 요청 (X-USER-ID, balance)
    API->>App: chargePoint(command)
    App->>Domain: charge(userId, amount)
    Domain->>DB: points SELECT
    Domain->>DB: points UPDATE
    Domain->>Hist: point_histories INSERT(CHARGE)
    API-->>User: 200 OK
```

### 포인트 조회

`GET /api/v1/points`

```mermaid
sequenceDiagram
    autonumber
    actor User as 사용자
    participant API as PointV1Controller
    participant App as PointApplicationService
    participant Domain as PointService
    participant DB as Database

    User->>API: 포인트 조회 (X-USER-ID)
    API->>App: getPoint(query)
    App->>Domain: findByUserId
    Domain->>DB: points SELECT
    API-->>User: 200 OK (잔액)
```

---

## 상품 (Product)

### 상품 목록 조회

`GET /api/v1/products`

```mermaid
sequenceDiagram
    autonumber
    actor User as 사용자
    participant API as ProductV1Controller
    participant App as ProductApplicationService
    participant Query as ProductQueryService
    participant Repo as ProductRepositoryImpl
    participant DB as Database

    User->>API: 목록 조회 (brandId, sort, page, size)
    API->>App: getProductList(query)
    App->>Query: getProductList(data)
    Query->>Repo: getProductList(brandId, pageable)
    Repo->>DB: products + brands + product_aggregate 조회
    Note over Repo: sort가 잘못되어도 latest fallback
    Repo-->>Query: Page<ProductInfo>
    API-->>User: 200 OK
```

### 상품 상세 조회 + 조회수 이벤트

`GET /api/v1/products/{productId}`

```mermaid
sequenceDiagram
    autonumber
    actor User as 사용자
    participant API as ProductV1Controller
    participant App as ProductApplicationService
    participant Query as ProductQueryService
    participant Rank as RankingQueryService
    participant Event as EventPublisher
    participant Counter as ProductCounterEventHandler
    participant Hist as ProductCounterEventHistoryService
    participant Agg as ProductAggregateService
    participant Cache as ProductCacheInvalidationService
    participant DB as Database

    User->>API: 상세 조회
    API->>App: getProductDetail(productId)
    App->>Query: getProductDetail(productId)
    Query->>DB: products + brands + product_aggregate SELECT
    App->>Rank: getProductRanking(productId)
    App->>Event: publish(ProductViewedEvent)
    API-->>User: 200 OK (like/order/view 포함)

    Event-->>Counter: ProductViewedEvent (AFTER_COMMIT, Async)
    Counter->>Hist: claim(dedupeKey)
    alt duplicate
        Counter-->>Counter: skip
    else new event
        Counter->>Hist: markProcessing
        Counter->>Agg: incrementViewCount
        Counter->>Cache: evictProductCache
        Counter->>Hist: complete
    end
```

---

## 브랜드 (Brand)

### 브랜드 상세 조회

`GET /api/v1/brands/{brandId}`

```mermaid
sequenceDiagram
    autonumber
    actor User as 사용자
    participant API as BrandV1Controller
    participant App as BrandApplicationService
    participant Domain as BrandService
    participant DB as Database

    User->>API: 브랜드 상세 조회
    API->>App: getBrandById(query)
    App->>Domain: getBrandById(data)
    Domain->>DB: brands SELECT
    API-->>User: 200 OK
```

---

## 좋아요 (Like)

### 좋아요 등록

`POST /api/v1/like/products/{productId}`

```mermaid
sequenceDiagram
    autonumber
    actor User as 사용자
    participant API as LikeV1Controller
    participant App as LikeApplicationService
    participant Domain as LikeService
    participant Repo as LikeRepository
    participant Event as EventPublisher
    participant Handler as LikeEventHandler
    participant DB as Database

    User->>API: 좋아요 등록 (header: userId)
    API->>App: likeProduct(command)
    App->>Domain: likeProduct(data)
    Domain->>Repo: findByUserIdAndProductId
    alt already exists
        Domain-->>API: 400 BAD_REQUEST
    else not exists
        Domain->>Repo: save(Like)
        App->>Event: publish(ProductLikedEvent)
        API-->>User: 200 OK
    end

    Event-->>Handler: ProductLikedEvent
    Handler->>DB: product_counter_event_history claim/process/complete
    Handler->>DB: product_aggregate.like_count +1
```

### 좋아요 취소

`DELETE /api/v1/like/products/{productId}`

```mermaid
sequenceDiagram
    autonumber
    actor User as 사용자
    participant API as LikeV1Controller
    participant App as LikeApplicationService
    participant Domain as LikeService
    participant Repo as LikeRepository
    participant Event as EventPublisher
    participant Handler as LikeEventHandler

    User->>API: 좋아요 취소 (header: userId)
    API->>App: unlikeProduct(command)
    App->>Domain: unlikeProduct(data)
    Domain->>Repo: findByUserIdAndProductId
    alt not found
        Domain-->>API: 404 NOT_FOUND
    else found
        Domain->>Repo: delete
        App->>Event: publish(ProductUnLikedEvent)
        API-->>User: 200 OK
    end

    Event-->>Handler: ProductUnLikedEvent
    Handler->>Handler: dedupe claim
    Handler->>Handler: like_count decrement
```

---

## 주문/쿠폰 (Order/Coupon)

### 주문 생성 (동기 처리 + 멱등성 + outbox)

`POST /api/v1/orders`

```mermaid
sequenceDiagram
    autonumber
    actor User as 사용자
    participant API as OrderV1Controller
    participant App as OrderApplicationService
    participant Hist as OrderHistoryRepository
    participant Proc as OrderProcessor
    participant Stock as ProductStockService
    participant Coupon as CouponService
    participant Point as PointService
    participant Order as OrderService
    participant Outbox as OutboxService
    participant DB as Database

    User->>API: 주문 요청 (X-USER-ID, X-IDEMPOTENCY-KEY optional)
    API->>App: createOrder(command)

    alt idempotency key exists
        App->>Hist: createIfNotExists(userId,key)
        alt claim fail
            App->>Hist: findByUserIdAndIdempotencyKey
            alt completed
                App->>Order: getOrder(userId, orderId)
                App-->>API: 기존 주문 반환
            else in progress
                App-->>API: 409 CONFLICT
            end
        else claim success
            App->>Proc: process(command)
        end
    else no key
        App->>Proc: process(command)
    end

    Proc->>Stock: deductStock (product row lock)
    Proc->>Coupon: applyDiscount (coupon row lock, optional)
    Proc->>Point: use (point row lock)
    Proc->>Order: createOrder + orderItems 저장

    App->>Outbox: enqueue(PAYMENT_REQUEST)
    App->>Outbox: enqueue(DATA_PLATFORM_DISPATCH: ORDER_CREATED)
    API-->>User: 200 OK
```

### Outbox 디스패치

```mermaid
sequenceDiagram
    autonumber
    participant Sch as OutboxEventScheduler
    participant Disp as OutboxDispatcherService
    participant O as OutboxService
    participant PayProc as PaymentProcessor
    participant PayOut as PaymentResultOutboxService
    participant Ord as OrderService
    participant Platform as DataPlatformApplicationService

    Sch->>Disp: dispatchPendingEvents()
    Disp->>O: findPendingEventIds(batch)
    loop each id
        Disp->>O: claim(id)
        alt PAYMENT_REQUEST
            Disp->>PayProc: process()
            Disp->>PayOut: enqueueByPaymentStatus()
        else ORDER_STATUS_SYNC
            Disp->>Ord: updateOrderStatus()
        else DATA_PLATFORM_DISPATCH
            Disp->>Platform: sendEventWithFailure()
        end
        alt success
            Disp->>O: complete(id)
        else fail
            Disp->>O: retry(id,error)
        end
    end
```

---

## 결제 (Payment)

### 주문 생성 이벤트 기반 결제 요청

```mermaid
sequenceDiagram
    autonumber
    participant Event as EventPublisher
    participant PEH as PaymentEventHandler
    participant App as PaymentApplicationService
    participant Proc as PaymentProcessor
    participant PaySvc as PaymentService
    participant Gateway as PaymentGatewayService

    Event-->>PEH: OrderCreatedEvent
    PEH->>App: processOrderPayment(orderEvent)
    App->>Proc: process(createPaymentCommand)
    Proc->>PaySvc: createPayment
    Proc->>Gateway: requestPaymentGateway
    Proc->>PaySvc: updatePaymentStatus

    alt SUCCESS
        PEH->>Event: publish(PaymentCompletedEvent)
    else FAILED
        PEH->>Event: publish(PaymentFailedEvent)
    end
```

### 결제 콜백 접수/비동기 처리

`POST /api/v1/payments/callback`

```mermaid
sequenceDiagram
    autonumber
    actor PG as PG
    participant API as PaymentV1Controller
    participant App as PaymentApplicationService
    participant Sig as PaymentCallbackSignatureVerifier
    participant Hist as PaymentCallbackHistoryService
    participant Async as PaymentCallbackAsyncProcessor
    participant PaySvc as PaymentService
    participant Gate as PaymentGatewayService
    participant Out as PaymentResultOutboxService

    PG->>API: callback(rawBody + X-CALLBACK-*)
    API->>App: acceptCallback(command)
    App->>Sig: verify(command)
    App->>Hist: claim(dedupeKey)

    alt duplicate
        App-->>API: accepted=false
        API-->>PG: 200 OK (중복 메시지)
    else accepted
        App->>Async: process(dedupeKey, command)
        API-->>PG: 200 OK (접수 완료)

        Async->>Hist: markProcessing
        Async->>PaySvc: processCallback
        PaySvc->>Gate: processCallbackWithVerification
        Async->>Out: enqueueByPaymentStatus
        Async->>Hist: complete/fail
    end
```

### 결제 상태 재검증 스케줄러

```mermaid
sequenceDiagram
    autonumber
    participant Sch as PaymentVerificationScheduler
    participant Gate as PaymentGatewayService
    participant Out as PaymentResultOutboxService

    Sch->>Gate: verifyPendingPayments()
    Gate-->>Sch: terminal transitions
    loop each payment
        Sch->>Out: enqueueByPaymentStatus(orderId,status)
    end
```

---

## 랭킹 (Ranking)

### 랭킹 조회

`GET /api/v1/rankings`

```mermaid
sequenceDiagram
    autonumber
    actor User as 사용자
    participant API as RankingV1Controller
    participant App as RankingApplicationService
    participant Domain as RankingQueryService
    participant Redis as RedisRankingRepository
    participant DB as RankingSnapshotRepository

    User->>API: 랭킹 조회 (X-USER-ID, date/type/page/size)
    API->>App: getRanking(query)
    App->>Domain: getRankingPage(data)
    alt redis available
        Domain->>Redis: read ranking slice
    else snapshot fallback
        Domain->>DB: read ranking snapshot
    end
    API-->>User: 200 OK
```
