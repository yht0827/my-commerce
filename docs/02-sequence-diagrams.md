# 시퀀스 다이어그램

> API별 요청/응답 흐름을 시각화한 시퀀스 다이어그램

## 목차

- [회원 (Member)](#회원-member)
- [포인트 (Point)](#포인트-point)
- [상품 (Product)](#상품-product)
- [브랜드 (Brand)](#브랜드-brand)
- [좋아요 (Like)](#좋아요-like)
- [주문 (Order)](#주문-order)

---

## 회원 (Member)

### 회원가입

`POST /api/v1/users`

```mermaid
sequenceDiagram
    autonumber
    actor User as 사용자
    participant Client as 클라이언트
    participant Server as API 서버
    participant DB as Database

    User->>Client: 회원정보 입력 (ID, 이메일, 생년월일, 성별)
    Client->>Client: 입력값 유효성 검증

    alt 유효성 검증 실패
        Client-->>User: 입력 오류 메시지 표시
    else 유효성 검증 성공
        Client->>Server: POST /api/v1/users
        Server->>Server: 요청 데이터 유효성 검증

        alt 유효성 검증 실패
            Server-->>Client: 400 Bad Request
            Client-->>User: 오류 메시지 표시
        else 유효성 검증 성공
            Server->>DB: ID 중복 확인

            alt ID 중복
                DB-->>Server: 중복 ID 존재
                Server-->>Client: 409 Conflict (ID 중복)
                Client-->>User: "이미 사용 중인 ID입니다"
            else ID 사용 가능
                Server->>DB: 이메일 중복 확인

                alt 이메일 중복
                    DB-->>Server: 중복 이메일 존재
                    Server-->>Client: 409 Conflict (이메일 중복)
                    Client-->>User: "이미 사용 중인 이메일입니다"
                else 이메일 사용 가능
                    Server->>DB: 회원 정보 저장
                    DB-->>Server: 저장 완료
                    Server-->>Client: 201 Created (회원 정보)
                    Client-->>User: 회원가입 완료, 로그인 페이지로 이동
                end
            end
        end
    end
```

---

### 내 정보 조회

`GET /api/v1/users/me`

```mermaid
sequenceDiagram
    autonumber
    actor User as 사용자
    participant Client as 클라이언트
    participant Server as API 서버
    participant DB as Database

    User->>Client: 마이페이지 접근
    Client->>Server: GET /api/v1/users/me<br/>[Header: X-USER-ID]

    Server->>Server: X-USER-ID 헤더 검증

    alt 헤더 없음
        Server-->>Client: 401 Unauthorized
        Client-->>User: 로그인 필요 안내
    else 헤더 존재
        Server->>DB: 사용자 조회 (userId)

        alt 사용자 미존재
            DB-->>Server: 조회 결과 없음
            Server-->>Client: 401 Unauthorized
            Client-->>User: 로그인 필요 안내
        else 사용자 존재
            DB-->>Server: 사용자 정보
            Server-->>Client: 200 OK (회원 정보)
            Client-->>User: 마이페이지 화면 표시
        end
    end
```

---

## 포인트 (Point)

### 포인트 충전

`POST /api/v1/points/charge`

```mermaid
sequenceDiagram
    autonumber
    actor User as 사용자
    participant Client as 클라이언트
    participant Server as API 서버
    participant DB as Database

    User->>Client: 충전 금액 입력
    Client->>Server: POST /api/v1/points/charge<br/>[Header: X-USER-ID]<br/>{amount: 10000}

    Server->>Server: X-USER-ID 헤더 검증

    alt 인증 실패
        Server-->>Client: 401 Unauthorized
        Client-->>User: 로그인 필요 안내
    else 인증 성공
        Server->>Server: 충전 금액 유효성 검증

        alt 금액 유효성 실패
            Server-->>Client: 400 Bad Request
            Client-->>User: "올바른 금액을 입력해주세요"
        else 금액 유효
            Server->>DB: 트랜잭션 시작
            Server->>DB: 포인트 잔액 증가
            Server->>DB: 충전 내역 저장
            Server->>DB: 트랜잭션 커밋
            DB-->>Server: 처리 완료
            Server-->>Client: 200 OK (갱신된 포인트 정보)
            Client-->>User: 충전 완료 표시
        end
    end
```

---

### 포인트 조회

`GET /api/v1/points`

```mermaid
sequenceDiagram
    autonumber
    actor User as 사용자
    participant Client as 클라이언트
    participant Server as API 서버
    participant DB as Database

    User->>Client: 포인트 내역 페이지 접근
    Client->>Server: GET /api/v1/points<br/>[Header: X-USER-ID]

    Server->>Server: X-USER-ID 헤더 검증

    alt 인증 실패
        Server-->>Client: 401 Unauthorized
        Client-->>User: 로그인 필요 안내
    else 인증 성공
        Server->>DB: 포인트 잔액 조회
        Server->>DB: 포인트 내역 조회 (최신순)
        DB-->>Server: 포인트 정보
        Server-->>Client: 200 OK (잔액, 충전/사용 내역)
        Client-->>User: 포인트 정보 화면 표시
    end
```

---

## 상품 (Product)

### 상품 목록 조회

`GET /api/v1/products`

```mermaid
sequenceDiagram
    autonumber
    actor User as 사용자
    participant Client as 클라이언트
    participant Server as API 서버
    participant DB as Database

    User->>Client: 상품 목록 페이지 접근
    Client->>Server: GET /api/v1/products<br/>?brandId=1&sort=latest&page=0&size=20

    Server->>Server: 쿼리 파라미터 검증

    alt 잘못된 정렬 기준
        Server-->>Client: 400 Bad Request
        Client-->>User: 오류 메시지 표시
    else 파라미터 유효
        Server->>DB: 상품 목록 조회<br/>(필터, 정렬, 페이징 적용)
        DB-->>Server: 상품 목록 + 페이징 정보
        Server-->>Client: 200 OK (상품 목록, 페이징 정보)
        Client-->>User: 상품 목록 화면 표시
    end
```

---

### 상품 상세 조회

`GET /api/v1/products/{productId}`

```mermaid
sequenceDiagram
    autonumber
    actor User as 사용자
    participant Client as 클라이언트
    participant Server as API 서버
    participant DB as Database

    User->>Client: 상품 클릭
    Client->>Server: GET /api/v1/products/{productId}

    Server->>DB: 상품 정보 조회

    alt 상품 미존재
        DB-->>Server: 조회 결과 없음
        Server-->>Client: 404 Not Found
        Client-->>User: "상품을 찾을 수 없습니다"
    else 상품 존재
        DB-->>Server: 상품 기본 정보
        Server->>DB: 상품 옵션 조회
        DB-->>Server: 옵션 정보
        Server->>DB: 재고 정보 조회
        DB-->>Server: 재고 정보
        Server->>DB: 좋아요 수 조회
        DB-->>Server: 좋아요 수
        Server-->>Client: 200 OK (상품 상세 정보)
        Client-->>User: 상품 상세 화면 표시
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
    participant Client as 클라이언트
    participant Server as API 서버
    participant DB as Database

    User->>Client: 브랜드 페이지 접근
    Client->>Server: GET /api/v1/brands/{brandId}

    Server->>DB: 브랜드 정보 조회

    alt 브랜드 미존재
        DB-->>Server: 조회 결과 없음
        Server-->>Client: 404 Not Found
        Client-->>User: "브랜드를 찾을 수 없습니다"
    else 브랜드 존재
        DB-->>Server: 브랜드 정보
        Server->>DB: 브랜드 상품 목록 조회
        DB-->>Server: 상품 목록
        Server-->>Client: 200 OK (브랜드 정보, 상품 목록)
        Client-->>User: 브랜드 상세 화면 표시
    end
```

---

## 좋아요 (Like)

### 좋아요 등록

`POST /api/v1/like/products/{productId}`

```mermaid
sequenceDiagram
    autonumber
    actor User as 사용자
    participant Client as 클라이언트
    participant Server as API 서버
    participant DB as Database

    User->>Client: 좋아요 버튼 클릭
    Client->>Server: POST /api/v1/like/products/{productId}<br/>[Header: X-USER-ID]

    Server->>Server: X-USER-ID 헤더 검증

    alt 인증 실패
        Server-->>Client: 401 Unauthorized
        Client-->>User: 로그인 필요 안내
    else 인증 성공
        Server->>DB: 상품 존재 여부 확인

        alt 상품 미존재
            DB-->>Server: 조회 결과 없음
            Server-->>Client: 404 Not Found
            Client-->>User: "상품을 찾을 수 없습니다"
        else 상품 존재
            Server->>DB: 좋아요 존재 여부 확인

            alt 이미 좋아요 함
                DB-->>Server: 좋아요 존재
                Server-->>Client: 200 OK (멱등성 보장)
                Client-->>User: 좋아요 상태 유지
            else 좋아요 없음
                Server->>DB: 좋아요 저장
                Server->>DB: 상품 좋아요 수 증가
                DB-->>Server: 처리 완료
                Server-->>Client: 200 OK
                Client-->>User: 좋아요 UI 업데이트
            end
        end
    end
```

---

### 좋아요 취소

`DELETE /api/v1/like/products/{productId}`

```mermaid
sequenceDiagram
    autonumber
    actor User as 사용자
    participant Client as 클라이언트
    participant Server as API 서버
    participant DB as Database

    User->>Client: 좋아요 버튼 클릭 (취소)
    Client->>Server: DELETE /api/v1/like/products/{productId}<br/>[Header: X-USER-ID]

    Server->>Server: X-USER-ID 헤더 검증

    alt 인증 실패
        Server-->>Client: 401 Unauthorized
        Client-->>User: 로그인 필요 안내
    else 인증 성공
        Server->>DB: 좋아요 존재 여부 확인

        alt 좋아요 없음
            DB-->>Server: 좋아요 미존재
            Server-->>Client: 200 OK (멱등성 보장)
            Client-->>User: 좋아요 해제 상태 유지
        else 좋아요 존재
            Server->>DB: 좋아요 삭제
            Server->>DB: 상품 좋아요 수 감소
            DB-->>Server: 처리 완료
            Server-->>Client: 200 OK
            Client-->>User: 좋아요 해제 UI 업데이트
        end
    end
```

---

### 좋아요 목록 조회

`GET /api/v1/like/products`

```mermaid
sequenceDiagram
    autonumber
    actor User as 사용자
    participant Client as 클라이언트
    participant Server as API 서버
    participant DB as Database

    User->>Client: 좋아요 목록 페이지 접근
    Client->>Server: GET /api/v1/like/products<br/>[Header: X-USER-ID]

    Server->>Server: X-USER-ID 헤더 검증

    alt 인증 실패
        Server-->>Client: 401 Unauthorized
        Client-->>User: 로그인 필요 안내
    else 인증 성공
        Server->>DB: 좋아요 상품 목록 조회 (최신순)
        DB-->>Server: 좋아요 상품 목록
        Server-->>Client: 200 OK (상품 목록)
        Client-->>User: 좋아요 목록 화면 표시
    end
```

---

## 주문 (Order)

### 주문 생성

`POST /api/v1/orders`

```mermaid
sequenceDiagram
    autonumber
    actor User as 사용자
    participant Client as 클라이언트
    participant Server as API 서버
    participant DB as Database
    participant External as 외부 시스템

    User->>Client: 주문하기 버튼 클릭
    Client->>Server: POST /api/v1/orders<br/>[Header: X-USER-ID]<br/>{items: [{productId, quantity}]}

    Server->>Server: X-USER-ID 헤더 검증

    alt 인증 실패
        Server-->>Client: 401 Unauthorized
        Client-->>User: 로그인 필요 안내
    else 인증 성공
        Server->>Server: 요청 데이터 검증<br/>(상품 개수 50개 이하)

        alt 검증 실패
            Server-->>Client: 400 Bad Request
            Client-->>User: 오류 메시지 표시
        else 검증 성공
            Server->>DB: 트랜잭션 시작
            Server->>DB: 회원 정보 조회

            loop 각 주문 상품
                Server->>DB: 상품 정보 조회

                alt 상품 미존재 or 판매중 아님
                    Server->>DB: 트랜잭션 롤백
                    Server-->>Client: 400 Bad Request
                    Client-->>User: 오류 메시지 표시
                else 상품 유효
                    Server->>DB: 재고 확인 및 차감 (Lock)

                    alt 재고 부족
                        Server->>DB: 트랜잭션 롤백
                        Server-->>Client: 400 Bad Request
                        Client-->>User: "재고가 부족합니다"
                    end
                end
            end

            Server->>DB: 총 금액 계산
            Server->>DB: 포인트 잔액 확인

            alt 포인트 부족
                Server->>DB: 트랜잭션 롤백
                Server-->>Client: 400 Bad Request
                Client-->>User: "포인트가 부족합니다"
            else 포인트 충분
                Server->>DB: 포인트 차감
                Server->>DB: 주문 정보 저장
                Server->>DB: 주문 상품 저장
                Server->>DB: 트랜잭션 커밋
                Server-->>External: 주문 정보 전송 (비동기)
                Server-->>Client: 201 Created (주문 정보)
                Client-->>User: 주문 완료 화면 표시
            end
        end
    end
```

---

### 주문 목록 조회

`GET /api/v1/orders`

```mermaid
sequenceDiagram
    autonumber
    actor User as 사용자
    participant Client as 클라이언트
    participant Server as API 서버
    participant DB as Database

    User->>Client: 주문 내역 페이지 접근
    Client->>Server: GET /api/v1/orders<br/>[Header: X-USER-ID]

    Server->>Server: X-USER-ID 헤더 검증

    alt 인증 실패
        Server-->>Client: 401 Unauthorized
        Client-->>User: 로그인 필요 안내
    else 인증 성공
        Server->>DB: 주문 목록 조회 (최신순)
        DB-->>Server: 주문 목록
        Server-->>Client: 200 OK (주문 목록)
        Client-->>User: 주문 내역 화면 표시
    end
```

---

### 주문 상세 조회

`GET /api/v1/orders/{orderId}`

```mermaid
sequenceDiagram
    autonumber
    actor User as 사용자
    participant Client as 클라이언트
    participant Server as API 서버
    participant DB as Database

    User->>Client: 주문 상세 클릭
    Client->>Server: GET /api/v1/orders/{orderId}<br/>[Header: X-USER-ID]

    Server->>Server: X-USER-ID 헤더 검증

    alt 인증 실패
        Server-->>Client: 401 Unauthorized
        Client-->>User: 로그인 필요 안내
    else 인증 성공
        Server->>DB: 주문 정보 조회

        alt 주문 미존재
            DB-->>Server: 조회 결과 없음
            Server-->>Client: 404 Not Found
            Client-->>User: "주문을 찾을 수 없습니다"
        else 주문 존재
            Server->>Server: 본인 주문 여부 확인

            alt 본인 주문 아님
                Server-->>Client: 403 Forbidden
                Client-->>User: "접근 권한이 없습니다"
            else 본인 주문
                Server->>DB: 주문 상품 목록 조회
                DB-->>Server: 주문 상품 목록
                Server->>DB: 결제 정보 조회
                DB-->>Server: 결제 정보
                Server-->>Client: 200 OK (주문 상세 정보)
                Client-->>User: 주문 상세 화면 표시
            end
        end
    end
```
