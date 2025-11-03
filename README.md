# 프로젝트 개요

백엔드 핵심 역량 강화를 위한 이커머스 시스템 구축

## 주요 학습 목표

- 테스트 자동화 및 TDD
- 동시성 제어 (Pessimistic/Optimistic Lock)
- 트랜잭션 관리 및 격리 수준
- 캐싱 전략 (Local/Global Cache)
- 이벤트 기반의 비동기 처리 및 시스템 결합도 완화

## 개발환경

- Language: Java 21
- Framework: Spring Boot 3.4.4
- DB ORM: JPA
- Test: JUnit 5 + AssertJ

## About Multi-Module Project

본 프로젝트는 멀티 모듈 프로젝트로 구성되어 있습니다. 각 모듈의 위계 및 역할을 분명히 하고, 아래와 같은 규칙을 적용합니다.

- apps : 각 모듈은 실행가능한 **SpringBootApplication** 을 의미합니다.
- modules : 특정 구현이나 도메인에 의존적이지 않고, reusable 한 configuration 을 원칙으로 합니다.
- supports : logging, monitoring 과 같이 부가적인 기능을 지원하는 add-on 모듈입니다.

```
Root
├── apps ( spring-applications )
│   └── 📦 commerce-api
│   └── 📦 commerce-streamer
│   └── 📦 pg-simulator
├── modules ( reusable-configurations )
│   └── 📦 batch
│   └── 📦 cache
│   └── 📦 feign
│   └── 📦 jpa
│   └── 📦 kafka
│   └── 📦 redis
│   └── 📦 resilience
└── supports ( add-ons )
    ├── 📦 jackson
    ├── 📦 logging
    └── 📦 monitoring
```
