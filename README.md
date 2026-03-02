# My Commerce

> 멀티모듈 구조의 E-Commerce 연습 프로젝트

## 목차

- [프로젝트 소개](#프로젝트-소개)
- [기술 스택](#기술-스택)
- [핵심 구현 포인트](#핵심-구현-포인트)
- [시스템 아키텍처](#시스템-아키텍처)
- [모듈 구조](#모듈-구조)
- [설계 문서](#설계-문서)
- [시작하기](#시작하기)

## 프로젝트 소개

DDD와 클린 아키텍처를 연습하기 위해 만든 멀티모듈 E-Commerce 플랫폼입니다.

단순한 CRUD를 넘어 실제 서비스에서 마주치는 동시성, 트랜잭션 정합성, 성능 문제를 직접 설계하고 구현하는 데 초점을 맞췄습니다.

## 기술 스택

| 구분 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 3.4.4 |
| ORM | JPA (Hibernate) |
| Database | MySQL |
| Cache | Redis |
| Message Queue | Kafka |
| Test | JUnit 5, AssertJ, Testcontainers, Instancio |
| Load Test | k6 |
| Monitoring | Prometheus, Grafana |

## 핵심 구현 포인트

### Outbox 패턴 기반 이벤트 분리

주문 생성 트랜잭션 내에서 `PAYMENT_REQUEST`, `DATA_PLATFORM_DISPATCH` 이벤트를 Outbox 테이블에 원자적으로 적재합니다.
스케줄러가 주기적으로 Dispatcher를 호출해 이벤트를 처리하며, 실패 시 자동 재시도합니다.

```
주문 생성 트랜잭션
├── orders 저장
└── event_outbox 적재 (PAYMENT_REQUEST, DATA_PLATFORM_DISPATCH)

Outbox Dispatcher (스케줄러)
├── PAYMENT_REQUEST     → PG 호출
├── ORDER_STATUS_SYNC   → 주문 상태 CONFIRMED / CANCELLED
└── DATA_PLATFORM_DISPATCH → 데이터 플랫폼 전송
```

### Saga 패턴 기반 보상 트랜잭션

결제 실패(CANCELLED) 시 아래 순서로 독립 트랜잭션을 통해 보상을 수행합니다.

```
CANCELLED 감지
└── restoreOnCancelled()
    ├── 재고 복구 (Pessimistic Lock)
    ├── 포인트 환불
    └── 쿠폰 복원 (USED → ACTIVE)
```

### Redis Sorted Set 기반 실시간 랭킹

조회·좋아요·주문에 가중치를 부여하고 시간 감쇠를 적용해 실시간 점수를 계산합니다.

| 이벤트 | 가중치 |
|--------|--------|
| VIEW   | 0.1    |
| LIKE   | 0.2    |
| ORDER  | 0.6    |

- 시간 감쇠: `e^(-0.1 × 경과시간(h))`
- Redis 키: `ranking:daily:{yyyyMMdd}` (TTL 35일)
- Redis 장애 시 Snapshot으로 Fallback

### Spring Batch 기반 랭킹 집계

| Job | 설명 |
|-----|------|
| `dailyRankingJob` | 일별 랭킹 집계 |
| `weeklyRankingJob` | 주별 랭킹 집계 |
| `monthlyRankingJob` | 월별 랭킹 집계 |
| `dailyRankingRecoveryJob` | 장애 후 복구 집계 |

### 동시성 제어

- 재고 차감: Pessimistic Lock (SELECT FOR UPDATE)
- 쿠폰 사용: Optimistic Lock (`@Version`)
- 포인트 차감: Pessimistic Lock

## 시스템 아키텍처

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Client App    │────│  Commerce API   │────│   External PG   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                               │
                ┌──────────────┼──────────────┐
                │              │              │
        ┌───────▼────┐  ┌──────▼──────┐  ┌───▼─────┐
        │   MySQL    │  │    Redis    │  │  Kafka  │
        │ (Primary)  │  │ (Cache/     │  │ (Events)│
        └────────────┘  │  Ranking)   │  └─────────┘
                        └─────────────┘
```

## 모듈 구조

| 계층 | 설명 |
|------|------|
| **apps** | 실행 가능한 Spring Boot Application |
| **modules** | 도메인에 독립적인 재사용 가능한 설정 모듈 |
| **supports** | 로깅, 모니터링 등 부가 기능 지원 모듈 |

```
Root
├── apps ( spring-applications )
│   ├── 📦 commerce-api        # REST API 서버
│   ├── 📦 commerce-batch      # 배치 처리 (랭킹 집계)
│   ├── 📦 commerce-streamer   # 스트리밍 처리
│   └── 📦 pg-simulator        # PG 시뮬레이터
│
├── modules ( reusable-configurations )
│   ├── 📦 jpa                 # JPA 설정
│   ├── 📦 kafka               # Kafka 설정
│   └── 📦 redis               # Redis 설정
│
└── supports ( add-ons )
    ├── 📦 jackson             # JSON 직렬화
    ├── 📦 logging             # 로깅
    └── 📦 monitoring          # 모니터링
```

## 설계 문서

- [01 Requirements](docs/01-requirements.md)
- [02 Sequence Diagrams](docs/02-sequence-diagrams.md)
- [03 Class Diagrams](docs/03-class-diagrams.md)
- [04 ERD](docs/04-erd.md)

## 시작하기

### 사전 요구사항

- Java 21
- Docker & Docker Compose

### 인프라 실행

`local` 프로필 실행에 필요한 인프라(MySQL, Redis, Kafka)를 Make 명령으로 간단히 실행할 수 있습니다.

```shell
make infra-up
```

인프라 + 모니터링을 한 번에 실행하려면:

```shell
make up
```

### 모니터링 환경 (선택)

```shell
make monitor-up
```

애플리케이션 실행 후 아래 주소에서 접속할 수 있습니다.

- Grafana: http://localhost:3000 (`admin` / `admin`)
- Prometheus: http://localhost:9091

종료:

```shell
make down
```
