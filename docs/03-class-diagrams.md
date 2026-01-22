# 클래스 다이어그램

> 도메인별 클래스 구조 및 관계

## 목차

- [전체 도메인 구조](#전체-도메인-구조)
- [회원 (Member)](#회원-member)
- [포인트 (Point)](#포인트-point)
- [상품 (Product)](#상품-product)
- [브랜드 (Brand)](#브랜드-brand)
- [좋아요 (Like)](#좋아요-like)
- [주문 (Order)](#주문-order)

---

## 전체 도메인 구조

```mermaid
classDiagram
    direction TB

    Member "1" --> "*" Point : has
    Member "1" --> "*" PointHistory : has
    Member "1" --> "*" Like : has
    Member "1" --> "*" Order : places

    Brand "1" --> "*" Product : owns
    Product "1" --> "*" Like : receives
    Product "1" --> "1" Stock : has
    Product "1" --> "*" OrderItem : included in

    Order "1" --> "*" OrderItem : contains
    Order "1" --> "1" Payment : has
```

---

## 회원 (Member)

```mermaid
classDiagram
    class Member {
        -Long id
        -String memberId
        -String email
        -Gender gender
        -LocalDate birthDate
        -LocalDateTime createdAt
        -LocalDateTime updatedAt
        +register(command) Member
        +getInfo() MemberInfo
    }

    class Gender {
        <<enumeration>>
        MALE
        FEMALE
        OTHER
    }

    class MemberInfo {
        <<DTO>>
        +String memberId
        +String email
        +Gender gender
        +LocalDate birthDate
        +LocalDateTime createdAt
    }

    class MemberRepository {
        <<interface>>
        +save(member) Member
        +findByMemberId(memberId) Optional~Member~
        +findByEmail(email) Optional~Member~
        +existsByMemberId(memberId) boolean
        +existsByEmail(email) boolean
    }

    class MemberService {
        -MemberRepository memberRepository
        +register(command) MemberInfo
        +getMemberInfo(memberId) MemberInfo
    }

    Member --> Gender
    Member ..> MemberInfo : creates
    MemberService --> MemberRepository
    MemberService --> Member
```

---

## 포인트 (Point)

```mermaid
classDiagram
    class Point {
        -Long id
        -Long memberId
        -Long balance
        -LocalDateTime updatedAt
        +charge(amount) void
        +use(amount) void
        +getBalance() Long
    }

    class PointHistory {
        -Long id
        -Long memberId
        -Long amount
        -PointType type
        -String description
        -LocalDateTime createdAt
    }

    class PointType {
        <<enumeration>>
        CHARGE
        USE
        REFUND
    }

    class PointInfo {
        <<DTO>>
        +Long balance
        +List~PointHistoryInfo~ histories
    }

    class PointHistoryInfo {
        <<DTO>>
        +Long amount
        +PointType type
        +String description
        +LocalDateTime createdAt
    }

    class PointRepository {
        <<interface>>
        +save(point) Point
        +findByMemberId(memberId) Optional~Point~
        +findByMemberIdWithLock(memberId) Optional~Point~
    }

    class PointHistoryRepository {
        <<interface>>
        +save(history) PointHistory
        +findByMemberIdOrderByCreatedAtDesc(memberId) List~PointHistory~
    }

    class PointService {
        -PointRepository pointRepository
        -PointHistoryRepository historyRepository
        +charge(memberId, amount) PointInfo
        +use(memberId, amount) void
        +getPointInfo(memberId) PointInfo
    }

    Point --> PointHistory : records
    PointHistory --> PointType
    PointService --> PointRepository
    PointService --> PointHistoryRepository
```

---

## 상품 (Product)

```mermaid
classDiagram
    class Product {
        -Long id
        -Long brandId
        -String name
        -String description
        -Long price
        -ProductStatus status
        -Long likeCount
        -LocalDateTime createdAt
        -LocalDateTime updatedAt
        +isAvailable() boolean
        +increaseLikeCount() void
        +decreaseLikeCount() void
    }

    class ProductStatus {
        <<enumeration>>
        ON_SALE
        SOLD_OUT
        DISCONTINUED
    }

    class Stock {
        -Long id
        -Long productId
        -Integer quantity
        -LocalDateTime updatedAt
        +decrease(quantity) void
        +increase(quantity) void
        +hasEnough(quantity) boolean
    }

    class ProductInfo {
        <<DTO>>
        +Long id
        +String name
        +String description
        +Long price
        +ProductStatus status
        +Long likeCount
        +Integer stockQuantity
        +BrandInfo brand
    }

    class ProductListInfo {
        <<DTO>>
        +List~ProductInfo~ products
        +PageInfo pageInfo
    }

    class ProductRepository {
        <<interface>>
        +save(product) Product
        +findById(id) Optional~Product~
        +findAll(condition, pageable) Page~Product~
        +findByBrandId(brandId, pageable) Page~Product~
    }

    class StockRepository {
        <<interface>>
        +save(stock) Stock
        +findByProductId(productId) Optional~Stock~
        +findByProductIdWithLock(productId) Optional~Stock~
    }

    class ProductService {
        -ProductRepository productRepository
        -StockRepository stockRepository
        -BrandRepository brandRepository
        +getProducts(condition, pageable) ProductListInfo
        +getProductDetail(productId) ProductInfo
    }

    Product --> ProductStatus
    Product "1" --> "1" Stock
    ProductService --> ProductRepository
    ProductService --> StockRepository
```

---

## 브랜드 (Brand)

```mermaid
classDiagram
    class Brand {
        -Long id
        -String name
        -String description
        -String logoUrl
        -LocalDateTime createdAt
        -LocalDateTime updatedAt
    }

    class BrandInfo {
        <<DTO>>
        +Long id
        +String name
        +String description
        +String logoUrl
    }

    class BrandDetailInfo {
        <<DTO>>
        +BrandInfo brand
        +List~ProductInfo~ products
    }

    class BrandRepository {
        <<interface>>
        +save(brand) Brand
        +findById(id) Optional~Brand~
        +findAll() List~Brand~
    }

    class BrandService {
        -BrandRepository brandRepository
        -ProductRepository productRepository
        +getBrandDetail(brandId) BrandDetailInfo
    }

    Brand ..> BrandInfo : maps to
    BrandService --> BrandRepository
    BrandService --> ProductRepository
```

---

## 좋아요 (Like)

```mermaid
classDiagram
    class Like {
        -Long id
        -Long memberId
        -Long productId
        -LocalDateTime createdAt
    }

    class LikeInfo {
        <<DTO>>
        +Long productId
        +String productName
        +Long price
        +LocalDateTime likedAt
    }

    class LikeRepository {
        <<interface>>
        +save(like) Like
        +delete(like) void
        +findByMemberIdAndProductId(memberId, productId) Optional~Like~
        +findByMemberIdOrderByCreatedAtDesc(memberId) List~Like~
        +existsByMemberIdAndProductId(memberId, productId) boolean
    }

    class LikeService {
        -LikeRepository likeRepository
        -ProductRepository productRepository
        -MemberRepository memberRepository
        +addLike(memberId, productId) void
        +removeLike(memberId, productId) void
        +getLikedProducts(memberId) List~LikeInfo~
    }

    Like ..> LikeInfo : maps to
    LikeService --> LikeRepository
    LikeService --> ProductRepository
```

---

## 주문 (Order)

```mermaid
classDiagram
    class Order {
        -Long id
        -Long memberId
        -String orderNumber
        -OrderStatus status
        -Long totalAmount
        -LocalDateTime orderedAt
        -LocalDateTime updatedAt
        +create(member, items) Order
        +cancel() void
        +complete() void
    }

    class OrderStatus {
        <<enumeration>>
        PENDING
        PAID
        COMPLETED
        CANCELLED
    }

    class OrderItem {
        -Long id
        -Long orderId
        -Long productId
        -String productName
        -Long price
        -Integer quantity
        -Long subtotal
    }

    class Payment {
        -Long id
        -Long orderId
        -Long amount
        -PaymentMethod method
        -PaymentStatus status
        -LocalDateTime paidAt
    }

    class PaymentMethod {
        <<enumeration>>
        POINT
    }

    class PaymentStatus {
        <<enumeration>>
        PENDING
        COMPLETED
        FAILED
        REFUNDED
    }

    class OrderCreateCommand {
        <<DTO>>
        +String memberId
        +List~OrderItemCommand~ items
    }

    class OrderItemCommand {
        <<DTO>>
        +Long productId
        +Integer quantity
    }

    class OrderInfo {
        <<DTO>>
        +Long id
        +String orderNumber
        +OrderStatus status
        +Long totalAmount
        +List~OrderItemInfo~ items
        +PaymentInfo payment
        +LocalDateTime orderedAt
    }

    class OrderRepository {
        <<interface>>
        +save(order) Order
        +findById(id) Optional~Order~
        +findByMemberIdOrderByOrderedAtDesc(memberId) List~Order~
    }

    class OrderItemRepository {
        <<interface>>
        +saveAll(items) List~OrderItem~
        +findByOrderId(orderId) List~OrderItem~
    }

    class PaymentRepository {
        <<interface>>
        +save(payment) Payment
        +findByOrderId(orderId) Optional~Payment~
    }

    class OrderService {
        -OrderRepository orderRepository
        -OrderItemRepository orderItemRepository
        -PaymentRepository paymentRepository
        -ProductService productService
        -StockService stockService
        -PointService pointService
        +createOrder(command) OrderInfo
        +getOrders(memberId) List~OrderInfo~
        +getOrderDetail(memberId, orderId) OrderInfo
    }

    Order --> OrderStatus
    Order "1" --> "*" OrderItem : contains
    Order "1" --> "1" Payment : has
    Payment --> PaymentMethod
    Payment --> PaymentStatus
    OrderService --> OrderRepository
    OrderService --> OrderItemRepository
    OrderService --> PaymentRepository
```

---

## 레이어드 아키텍처

```mermaid
classDiagram
    direction TB

    class Controller {
        <<interface>>
        +handle request
        +return response
    }

    class Service {
        <<interface>>
        +business logic
        +transaction management
    }

    class Repository {
        <<interface>>
        +data access
        +CRUD operations
    }

    class Entity {
        <<interface>>
        +domain model
        +business rules
    }

    Controller --> Service : uses
    Service --> Repository : uses
    Repository --> Entity : manages

    class MemberController
    class ProductController
    class OrderController

    class MemberService
    class ProductService
    class OrderService

    class MemberRepository
    class ProductRepository
    class OrderRepository

    class Member
    class Product
    class Order

    MemberController ..|> Controller
    ProductController ..|> Controller
    OrderController ..|> Controller

    MemberService ..|> Service
    ProductService ..|> Service
    OrderService ..|> Service

    MemberRepository ..|> Repository
    ProductRepository ..|> Repository
    OrderRepository ..|> Repository

    Member ..|> Entity
    Product ..|> Entity
    Order ..|> Entity
```
