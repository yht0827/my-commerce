# My Commerce

> 멀티모듈 아키텍처 기반의 E-Commerce 플랫폼

## 목차

- [프로젝트 소개](#프로젝트-소개)
- [기술 스택](#기술-스택)
- [시스템 아키텍처](#시스템-아키텍처)
- [모듈 구조](#모듈-구조)
- [시작하기](#시작하기)
- [학습 목표](#학습-목표)

---

## 프로젝트 소개

본 프로젝트는 **멀티모듈 아키텍처**로 구성된 현대적인 E-Commerce 플랫폼입니다.
도메인 주도 설계(DDD)와 클린 아키텍처 원칙을 적용하여 확장성과 유지보수성을 확보했습니다.

### 핵심 가치

| 가치 | 설명 |
|------|------|
| **확장성** | 마이크로서비스 아키텍처로 전환 가능한 구조 |
| **성능** | Redis 캐싱과 비동기 처리로 최적화 |
| **안정성** | 테스트 주도 개발과 Circuit Breaker 패턴 적용 |
| **관찰가능성** | 모니터링과 로깅 시스템 완비 |

---

## 기술 스택

| 구분 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 3.4.4 |
| ORM | JPA (Hibernate) |
| Database | MySQL |
| Cache | Redis |
| Message Queue | Kafka |
| Test | JUnit 5, AssertJ |
| Monitoring | Prometheus, Grafana |

---

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
        │ (Primary)  │  │ (Master/RO) │  │ (Events)│
        └────────────┘  └─────────────┘  └─────────┘
```

---

## 모듈 구조

본 프로젝트는 멀티 모듈로 구성되어 있으며, 각 모듈의 역할과 규칙은 다음과 같습니다.

| 계층 | 설명 |
|------|------|
| **apps** | 실행 가능한 Spring Boot Application |
| **modules** | 도메인에 독립적인 재사용 가능한 설정 모듈 |
| **supports** | 로깅, 모니터링 등 부가 기능 지원 모듈 |

```
Root
├── apps ( spring-applications )
│   ├── 📦 commerce-api        # REST API 서버
│   ├── 📦 commerce-batch      # 배치 처리
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

---

## 시작하기

### 사전 요구사항

- Java 21
- Docker & Docker Compose

### 인프라 실행

`local` 프로필 실행에 필요한 인프라(MySQL, Redis, Kafka)를 Docker Compose로 제공합니다.

```shell
docker-compose -f ./docker/infra-compose.yml up -d
```

### 모니터링 환경 (선택)

Prometheus와 Grafana를 통한 모니터링 환경을 제공합니다.

```shell
docker-compose -f ./docker/monitoring-compose.yml up -d
```

애플리케이션 실행 후 http://localhost:3000 에서 Grafana에 접속할 수 있습니다.
- 계정: `admin` / `admin`

---

## 학습 목표

- 테스트 자동화 및 TDD
- 동시성 제어 (Pessimistic/Optimistic Lock)
- 트랜잭션 관리 및 격리 수준
- 캐싱 전략 (Local/Global Cache)
- 이벤트 기반 비동기 처리 및 시스템 결합도 완화