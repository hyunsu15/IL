# SQL 튜닝 문제

> 기준 DBMS: MySQL 8.0 (InnoDB)
> 각 문제의 쿼리에는 성능 문제가 숨어 있다. **무엇이 문제인지**, **어떻게 개선할지** 답해보자.
> 정답은 [SQL 튜닝 문제 - 정답.md](./SQL%20튜닝%20문제%20-%20정답.md) 참고.

---

## 공통 스키마

```sql
CREATE TABLE users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    email       VARCHAR(100) NOT NULL,
    name        VARCHAR(50)  NOT NULL,
    phone       VARCHAR(20),
    status      VARCHAR(10)  NOT NULL,  -- 'ACTIVE', 'DORMANT', 'WITHDRAWN'
    created_at  DATETIME     NOT NULL,
    UNIQUE KEY uk_users_email (email),
    KEY idx_users_created_at (created_at)
);
-- 약 1,000만 건. status 비율: ACTIVE 90%, DORMANT 9%, WITHDRAWN 1%

CREATE TABLE orders (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    order_no    VARCHAR(20)  NOT NULL,   -- 예: '20260715-000001'
    amount      DECIMAL(15, 2) NOT NULL,
    status      VARCHAR(10)  NOT NULL,   -- 'PAID', 'CANCELLED', 'REFUNDED'
    ordered_at  DATETIME     NOT NULL,
    KEY idx_orders_user_id (user_id),
    KEY idx_orders_ordered_at (ordered_at)
);
-- 약 5,000만 건

CREATE TABLE order_items (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id    BIGINT      NOT NULL,
    product_id  BIGINT      NOT NULL,
    quantity    INT         NOT NULL,
    price       DECIMAL(15, 2) NOT NULL,
    KEY idx_order_items_order_id (order_id)
);
-- 약 1억 5,000만 건
```

---

## 문제 1. 인덱스가 있는데 풀 스캔이 발생한다

`order_no`에 인덱스를 추가했는데도(`KEY idx_orders_order_no (order_no)`) 아래 쿼리가 풀 테이블 스캔을 탄다.

```sql
SELECT *
FROM orders
WHERE order_no = 20260715000001;
```

왜 인덱스를 타지 못하는가? 어떻게 고쳐야 하는가?

= 으로하면 이게 숫자나 문자열로 바뀜. 그래서 인덱스가 안되는걸로 기억함.
이것을 DateTime 타입으로 바꿔주면 되는걸로알고있음.



---

## 문제 2. 날짜 조건인데 인덱스를 못 탄다

2026년 7월 1일에 가입한 유저를 조회한다. `created_at`에 인덱스가 있는데도 풀 스캔이 발생한다.

```sql
SELECT *
FROM users
WHERE DATE_FORMAT(created_at, '%Y-%m-%d') = '2026-07-01';
```

문제점과 개선 쿼리를 작성하라.
앞은 데이트타입인데, 뒤에는 문자열임. 그러다보니 안됨. 이것도
```sql
SELECT *
FROM users
WHERE DATE_FORMAT(created_at, '%Y-%m-%d') = DATE_FORMAT('2026-07-01','%Y-%m-%d');
```


---

## 문제 3. LIKE 검색

이메일 도메인이 gmail인 유저를 찾는다. `uk_users_email` 인덱스가 있다.

```sql
SELECT id, email, name
FROM users
WHERE email LIKE '%@gmail.com';
```

1. 이 쿼리가 인덱스 레인지 스캔을 못 타는 이유는?
앞에 %이기때문이다. 이렇게 하면 인덱스를 탈수없다. 그나마 문자열%이런식은 가능한걸로 알고 있따.

2. "이메일이 `kim`으로 시작하는 유저 검색"(`LIKE 'kim%'`)과의 차이는?
차이가 있다. 왜냐하면 저건 인덱스를 탈수 있기 때문이다.

3. 도메인 검색 요구사항이 자주 있다면 어떤 설계 대안이 있는가? (2가지 이상)
검색이 잦다면, 검색을 전문으로하는 사용할것같다.
- ex) 엘라스틱 서치 


---

## 문제 4. 페이징이 뒤로 갈수록 느려진다

주문 목록 화면의 페이징 쿼리다. 첫 페이지는 빠른데, 10만 페이지쯤 가면 수 초씩 걸린다.

```sql
SELECT *
FROM orders
ORDER BY ordered_at DESC
LIMIT 20 OFFSET 2000000;
```

1. 왜 뒤 페이지로 갈수록 느려지는가?
2. "더보기(무한스크롤)" 방식 UI라고 가정하고 개선 쿼리를 작성하라.
3. 페이지 번호로 임의 이동이 꼭 필요하다면 어떤 절충안이 있는가?

---

## 문제 5. SELECT * 와 커버링 인덱스

특정 기간의 주문 건수와 금액 합계를 구하는 배치 쿼리다. 실행 계획을 보니 `idx_orders_ordered_at` 인덱스를 타긴 하는데 여전히 느리다.

```sql
SELECT COUNT(*), SUM(amount)
FROM orders
WHERE ordered_at >= '2026-01-01'
  AND ordered_at <  '2026-07-01';
-- 대상: 약 2,000만 건
```

1. 인덱스를 타는데도 느린 이유는? (InnoDB의 데이터 접근 방식 관점에서)
2. 이 쿼리를 위한 인덱스 설계를 제시하고, 실행 계획에서 무엇을 확인하면 개선 여부를 알 수 있는지 말하라.

---

## 문제 6. 복합 인덱스 컬럼 순서

아래 두 쿼리가 자주 실행된다. 인덱스는 하나만 추가할 수 있다고 하자.

```sql
-- (a) 특정 유저의 특정 상태 주문 조회 (전체 트래픽의 95%)
SELECT * FROM orders
WHERE user_id = ? AND status = 'PAID'
ORDER BY ordered_at DESC
LIMIT 10;

-- (b) 특정 상태의 최근 주문 전체 조회 (관리자 화면, 5%)
SELECT * FROM orders
WHERE status = 'CANCELLED'
ORDER BY ordered_at DESC
LIMIT 100;
```

1. `(user_id, status, ordered_at)`, `(status, user_id, ordered_at)`, `(status, ordered_at)` 세 후보 중 무엇을 선택할 것인가? 근거를 실행 계획 관점에서 설명하라.
2. 선택한 인덱스에서 (a) 쿼리의 `ORDER BY`가 filesort 없이 처리되는 이유(혹은 안 되는 이유)를 설명하라.

---

## 문제 7. IN 서브쿼리

탈퇴한 유저의 주문을 찾는 쿼리다. 매우 느리다.

```sql
SELECT *
FROM orders
WHERE user_id IN (
    SELECT id FROM users WHERE status = 'WITHDRAWN'
);
```

1. 이 쿼리를 JOIN으로 다시 작성하라.
2. MySQL 8.0 옵티마이저는 이런 서브쿼리를 자동으로 최적화(세미 조인)하기도 한다. 그럼에도 실행 계획을 확인해야 하는 이유는?
3. 반대로 "주문이 하나라도 있는 탈퇴 유저 목록"을 구할 때 `IN`, `EXISTS`, `JOIN + DISTINCT` 중 무엇이 적절한지 논하라.

---

## 문제 8. OR 조건과 인덱스

이름 또는 전화번호로 유저를 검색한다. `name`, `phone` 각각에 인덱스가 있다.

```sql
SELECT *
FROM users
WHERE name = '김현수' OR phone = '010-1234-5678';
```

1. 이 쿼리에서 옵티마이저가 선택할 수 있는 실행 계획 후보를 말하고, 풀 스캔으로 풀릴 위험이 있는 이유를 설명하라.
2. `UNION ALL`을 이용해 다시 작성하라. 이때 중복 행 문제는 어떻게 처리할 것인가?

---

## 문제 9. 통계 쿼리와 조인 순서

월별 매출 통계 화면이 느리다.

```sql
SELECT u.name, COUNT(o.id) AS order_count, SUM(oi.quantity * oi.price) AS total
FROM users u
JOIN orders o       ON o.user_id = u.id
JOIN order_items oi ON oi.order_id = o.id
WHERE o.ordered_at >= '2026-07-01'
  AND o.ordered_at <  '2026-08-01'
  AND u.status = 'ACTIVE'
GROUP BY u.id, u.name
ORDER BY total DESC
LIMIT 20;
```

1. 드라이빙 테이블은 어느 테이블이 되어야 유리한가? 이유는?
2. `u.status = 'ACTIVE'` 조건(전체의 90%)은 어느 시점에 걸리는 게 좋은가? 이 조건에 인덱스를 추가하는 것이 의미가 있는가?
3. GROUP BY 대상 건수가 많아 임시 테이블 + filesort가 발생한다. 화면 요구사항을 만족하면서 부하를 줄이는 방법을 1가지 이상 제시하라.

---

## 문제 10. 실행 계획 읽기

아래는 어떤 쿼리의 `EXPLAIN` 결과다.

| id | select_type | table | type  | possible_keys      | key  | rows      | Extra                                        |
|----|-------------|-------|-------|--------------------|------|-----------|----------------------------------------------|
| 1  | SIMPLE      | o     | ALL   | idx_orders_user_id | NULL | 48,213,55 | Using where; Using temporary; Using filesort |
| 1  | SIMPLE      | u     | eq_ref| PRIMARY            | PRIMARY | 1      | NULL                                         |

1. 이 실행 계획에서 발견되는 위험 신호 3가지를 짚어라.
2. `type` 컬럼의 값들을 성능이 좋은 순서대로 나열하라. (`ALL`, `index`, `range`, `ref`, `eq_ref`, `const`)
3. `Using temporary; Using filesort`가 뜨는 대표적인 쿼리 패턴은 무엇이며, 항상 제거해야 하는가?

---

## 보너스 문제 11. 통계가 낡으면 생기는 일

운영 중 갑자기 특정 쿼리가 인덱스를 안 타고 풀 스캔으로 돌기 시작했다. 쿼리도, 스키마도, 데이터 분포도 바뀐 게 없다고 한다(실제로는 대량 배치 INSERT 직후였다).

1. 원인으로 의심할 수 있는 것은?
2. 즉시 조치와 근본 조치를 각각 말하라.
