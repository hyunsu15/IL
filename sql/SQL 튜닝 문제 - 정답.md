# SQL 튜닝 문제 - 정답

> 문제: [SQL 튜닝 문제.md](./SQL%20튜닝%20문제.md)

---

## 문제 1. 인덱스가 있는데 풀 스캔이 발생한다 — 묵시적 형변환

`order_no`는 `VARCHAR`인데 숫자 리터럴 `20260715000001`과 비교했다. MySQL은 문자열과 숫자를 비교할 때 **문자열 쪽을 숫자로 변환**한다. 즉 조건이 사실상 `CAST(order_no AS DOUBLE) = 20260715000001`이 되어, 인덱스 컬럼에 함수를 씌운 것과 같아진다 → 인덱스 사용 불가, 전 행 변환 후 비교(풀 스캔).

**개선:** 컬럼 타입에 맞는 리터럴을 사용한다.

```sql
SELECT *
FROM orders
WHERE order_no = '20260715-000001';
```

추가로, 애플리케이션(MyBatis/JPA) 파라미터 타입이 컬럼 타입과 일치하는지도 확인해야 한다. 자바에서 `Long`으로 바인딩하면 쿼리를 고쳐도 같은 문제가 재발한다.

> 함정 포인트: 반대 방향(숫자 컬럼 = 문자열 리터럴)은 리터럴 쪽이 변환되므로 인덱스를 탈 수 있다. "형변환 = 무조건 풀 스캔"이 아니라 **어느 쪽이 변환되는가**가 핵심.

---

## 문제 2. 날짜 조건인데 인덱스를 못 탄다 — 인덱스 컬럼 가공

`DATE_FORMAT(created_at, ...)`처럼 **인덱스 컬럼을 함수로 가공하면** 인덱스의 정렬 순서를 활용할 수 없다. (이런 조건을 non-sargable 조건이라 부른다.)

**개선:** 컬럼은 그대로 두고 조건 값을 범위로 바꾼다.

```sql
SELECT *
FROM users
WHERE created_at >= '2026-07-01 00:00:00'
  AND created_at <  '2026-07-02 00:00:00';
```

`BETWEEN '2026-07-01 00:00:00' AND '2026-07-01 23:59:59'`도 되지만, DATETIME(3) 등 밀리초가 붙는 순간 `23:59:59.500` 데이터가 누락된다. **상한을 "미만(<) 다음날 0시"로 잡는 습관**이 안전하다.

같은 원리로 피해야 하는 패턴: `WHERE YEAR(created_at) = 2026`, `WHERE SUBSTR(order_no, 1, 8) = '20260715'`, `WHERE amount * 1.1 > 10000` (→ `amount > 10000 / 1.1`로 변형).

---

## 문제 3. LIKE 검색

**1. `LIKE '%@gmail.com'`이 레인지 스캔을 못 타는 이유**
B-Tree 인덱스는 왼쪽부터 정렬되어 있다. 앞부분이 와일드카드면 탐색 시작점을 정할 수 없어 레인지 스캔이 불가능하다. (인덱스 풀 스캔으로 풀릴 수는 있지만 전 엔트리를 훑는 건 마찬가지.)

**2. `LIKE 'kim%'`와의 차이**
전방 일치는 `email >= 'kim' AND email < 'kin'` 범위 조건과 동등하므로 레인지 스캔이 가능하다.

**3. 도메인 검색 설계 대안**
- **역순 컬럼**: `email_reversed` 컬럼(`moc.liamg@...`)을 추가하고 인덱스를 건 뒤 `LIKE REVERSE('%@gmail.com')` → 전방 일치로 변환. MySQL 8.0은 함수 기반 인덱스 `((REVERSE(email)))`로도 가능.
- **도메인 분리 저장**: `email_domain` 컬럼을 분리 저장(또는 generated column)하고 인덱스. 요구사항이 "도메인 검색"이면 이게 가장 정직한 설계.
- **전문 검색**: 임의 패턴 검색까지 필요하면 FULLTEXT 인덱스나 Elasticsearch 같은 검색엔진 위임.

---

## 문제 4. 페이징이 뒤로 갈수록 느려진다 — OFFSET의 비밀

**1. 느려지는 이유**
`LIMIT 20 OFFSET 2000000`은 20건을 읽는 게 아니라 **2,000,020건을 읽어서 앞의 200만 건을 버리는** 동작이다. OFFSET은 "건너뛰기"가 아니라 "읽고 버리기"다. 뒤 페이지일수록 버리는 양이 선형으로 늘어난다.

**2. 커서(No-Offset) 방식 — 더보기 UI**

```sql
-- 첫 요청
SELECT * FROM orders
ORDER BY ordered_at DESC, id DESC
LIMIT 20;

-- 다음 요청: 마지막으로 받은 행의 (ordered_at, id)를 커서로 전달
SELECT * FROM orders
WHERE (ordered_at, id) < ('2026-07-15 10:00:00', 49999123)
ORDER BY ordered_at DESC, id DESC
LIMIT 20;
```

- `ordered_at`만으로는 동일 시각 주문이 있을 때 누락/중복이 생기므로 **유니크한 `id`를 타이브레이커**로 붙인다.
- `(ordered_at, id)` 복합 인덱스가 있으면 어느 페이지든 20건만 읽는다.

**3. 페이지 번호 이동이 꼭 필요할 때의 절충안**
- **지연 조인(deferred join)**: 커버링 인덱스로 PK만 먼저 뽑고 나중에 조인한다. OFFSET 스캔 자체는 남지만, 버리는 행이 인덱스 엔트리(좁음)라서 훨씬 싸다.
  ```sql
  SELECT o.*
  FROM (SELECT id FROM orders ORDER BY ordered_at DESC, id DESC LIMIT 20 OFFSET 2000000) t
  JOIN orders o ON o.id = t.id;
  ```
- 페이지 상한 제한(구글도 수백 페이지 이상은 안 보여준다), 검색 조건 강제로 대상 축소 등 **정책적 해결**도 유효한 답이다.

---

## 문제 5. SELECT * 와 커버링 인덱스

**1. 인덱스를 타는데도 느린 이유**
InnoDB 세컨더리 인덱스에는 `(ordered_at, PK)`만 들어 있다. `SUM(amount)` 때문에 인덱스에 없는 `amount`를 읽으러 **건마다 클러스터드 인덱스(테이블)로 랜덤 접근**해야 한다. 2,000만 건이면 2,000만 번의 랜덤 I/O가 발생한다. 이 비용 때문에 옵티마이저가 아예 풀 스캔을 선택해버리는 경우도 많다.

**2. 커버링 인덱스 설계**

```sql
ALTER TABLE orders ADD INDEX idx_orders_ordered_at_amount (ordered_at, amount);
```

쿼리가 필요로 하는 모든 컬럼(`ordered_at`, `amount`)이 인덱스 안에 있으므로 테이블 접근 없이 인덱스만 스캔해서 끝난다. 실행 계획에서 **`Extra: Using index`** 가 확인되면 커버링으로 동작하는 것이다. (`Using index condition`과 혼동 주의 — 그건 ICP이지 커버링이 아니다.)

> 일반화: `SELECT *`는 커버링 인덱스 가능성을 원천 차단한다. 자주 도는 쿼리일수록 필요한 컬럼만 명시할 것.

---

## 문제 6. 복합 인덱스 컬럼 순서

**1. 선택: `(user_id, status, ordered_at)`**

- (a) 쿼리(트래픽 95%)에서 `user_id = ?`, `status = 'PAID'` 두 동등 조건으로 인덱스를 파고든 뒤, 남은 `ordered_at`이 인덱스 내에서 이미 정렬되어 있어 **정렬 작업 없이 최신 10건만 읽고 종료**한다. 최적.
- `(status, user_id, ordered_at)`도 (a)를 처리할 수는 있지만, 선두 컬럼 `status`는 카디널리티가 3인 데다 'PAID'가 대부분이라 선두에 둘 가치가 낮다. 동등 조건이면 어차피 둘 다 타긴 하므로 차이는 크지 않지만, `user_id` 선두가 범용성(예: `WHERE user_id = ?`만 있는 쿼리)이 더 좋다.
- `(status, ordered_at)`은 (b) 전용. 95% 트래픽인 (a)를 버리는 선택이라 탈락.

(b)는 이 인덱스로 커버되지 않지만(선두 `user_id` 조건 없음), 관리자 화면 5%이고 'CANCELLED'는 소수라서 감수 가능하다. 정말 문제 되면 그때 `(status, ordered_at)`을 추가로 검토한다.

**2. (a)의 ORDER BY가 filesort 없이 처리되는 이유**
복합 인덱스는 `user_id, status`가 같은 행들끼리는 `ordered_at` 순으로 정렬돼 있다. (a)는 앞 두 컬럼이 **모두 동등 조건으로 고정**되므로, 남은 인덱스 순서 = `ordered_at` 순서가 되어 역방향 스캔으로 LIMIT 10만 읽으면 된다. 만약 `status IN ('PAID','REFUNDED')`처럼 범위/IN이 되는 순간 정렬 보장이 깨져 filesort가 등장한다(정렬 컬럼 앞의 컬럼은 동등 조건이어야 한다는 규칙).

---

## 문제 7. IN 서브쿼리

**1. JOIN 재작성**

```sql
SELECT o.*
FROM orders o
JOIN users u ON u.id = o.user_id
WHERE u.status = 'WITHDRAWN';
```

WITHDRAWN 유저는 전체의 1%(약 10만 명)뿐이므로, `users`를 먼저 걸러서 드라이빙하고 `idx_orders_user_id`로 `orders`를 찾아가는 것이 유리하다. `users.status` 인덱스(또는 `(status, id)`)가 있으면 더 좋다.

**2. 그럼에도 실행 계획을 확인해야 하는 이유**
MySQL 8.0은 IN 서브쿼리를 세미 조인(materialization, firstmatch 등)으로 자동 변환하는 경우가 많지만, **항상 되는 게 아니다.** `NOT IN`, 상관 서브쿼리, `LIMIT`이 낀 서브쿼리 등은 변환이 제한되고, 통계에 따라 비효율적인 전략이 선택될 수도 있다. "옵티마이저가 알아서 해주겠지"가 아니라 `EXPLAIN`으로 세미 조인 적용 여부와 조인 순서를 확인하는 습관이 필요하다.

**3. "주문이 하나라도 있는 탈퇴 유저 목록"**

```sql
SELECT u.*
FROM users u
WHERE u.status = 'WITHDRAWN'
  AND EXISTS (SELECT 1 FROM orders o WHERE o.user_id = u.id);
```

- **EXISTS가 적합**: 유저당 주문이 수십 건이어도 **한 건 찾으면 즉시 중단**한다. 의도도 명확하다.
- `JOIN + DISTINCT`: 유저당 주문 수만큼 부풀린 뒤 중복 제거하는 구조라 낭비. 다만 옵티마이저가 세미 조인으로 같게 풀 수도 있다.
- `IN`도 8.0에선 대부분 세미 조인으로 EXISTS와 동등하게 풀린다. 결론: **셋 다 실행 계획이 같아질 수 있으나, 의미상 EXISTS가 의도를 가장 잘 드러내고 변환 실패 시에도 안전하다.**

---

## 문제 8. OR 조건과 인덱스

**1. 실행 계획 후보와 위험**
- `index_merge (union)`: `name` 인덱스와 `phone` 인덱스를 각각 스캔해서 PK 기준으로 합집합. 이상적인 경우.
- 풀 테이블 스캔: 옵티마이저가 index_merge 비용을 높게 보거나, 한쪽 조건이 인덱스를 못 쓰는 상태(형변환·가공 등)면 OR 전체가 풀 스캔으로 풀린다. **OR는 한쪽만 무너져도 전체가 무너진다**는 게 위험 포인트. index_merge는 통계에 민감해서 선택이 불안정하기도 하다.

**2. UNION ALL 재작성**

```sql
SELECT * FROM users WHERE name = '김현수'
UNION ALL
SELECT * FROM users WHERE phone = '010-1234-5678'
                      AND name <> '김현수';
```

- 각 SELECT가 자기 인덱스를 확실히 타도록 분리한다.
- `UNION`(중복 제거)은 정렬/해시 비용이 들므로, **두 번째 쿼리에서 첫 조건을 배제하는 방식**으로 중복을 원천 차단하고 `UNION ALL`을 쓴다. (name이 NULL 허용이면 `name <> '김현수'`가 NULL 행을 걸러버리므로 `(name <> '김현수' OR name IS NULL)`로 보정 — 여기선 NOT NULL이라 불필요.)

---

## 문제 9. 통계 쿼리와 조인 순서

**1. 드라이빙 테이블: `orders`**
가장 강한 필터는 `o.ordered_at`의 1개월 범위 조건이다. `idx_orders_ordered_at`으로 7월 주문만 걸러낸 뒤 → `users`(PK로 eq_ref) → `order_items`(`idx_order_items_order_id`) 순으로 조인하는 것이 접근 행 수를 최소화한다. `users`(1,000만) 또는 `order_items`(1.5억)에서 시작하면 걸러지지 않은 대량 행이 조인으로 흘러간다.

**2. `u.status = 'ACTIVE'` 조건**
- 조인 시점에 함께 평가되면 충분하다(주문에서 유저로 PK 조인하면서 status 확인).
- **인덱스는 의미 없다.** 전체의 90%를 골라내는 조건은 선택도가 최악이라, 인덱스로 걸러봐야 얻는 게 없다. 인덱스는 "적게 골라낼 때" 가치가 있다. (오히려 WITHDRAWN 1%를 찾는 문제 7과 대비되는 지점.)

**3. GROUP BY 부하 축소 방안**
- **집계 먼저, 조인 나중**: `orders`+`order_items`를 `user_id` 기준으로 먼저 집계해 20명으로 줄인 뒤 `users`와 조인하면, `users` 조인과 `u.name` 운반이 집계 대상에서 빠진다.
  ```sql
  SELECT u.name, t.order_count, t.total
  FROM (
      SELECT o.user_id, COUNT(DISTINCT o.id) AS order_count,
             SUM(oi.quantity * oi.price) AS total
      FROM orders o
      JOIN order_items oi ON oi.order_id = o.id
      WHERE o.ordered_at >= '2026-07-01' AND o.ordered_at < '2026-08-01'
      GROUP BY o.user_id
      ORDER BY total DESC
      LIMIT 20
  ) t
  JOIN users u ON u.id = t.user_id AND u.status = 'ACTIVE';
  ```
  (엄밀히는 LIMIT 후 ACTIVE 필터라 20명 미만이 될 수 있음 — 요구사항에 따라 서브쿼리 안으로 옮길지 결정.)
- **집계 테이블/배치 사전 계산**: 월별 매출은 실시간일 필요가 없는 대표적 데이터. 일 단위 집계 테이블을 만들어 화면은 그걸 읽게 한다. 통계 화면 튜닝의 최종 병기.

---

## 문제 10. 실행 계획 읽기

**1. 위험 신호 3가지**
- `type: ALL` — `orders`(5,000만 건) 풀 테이블 스캔.
- `possible_keys`에 인덱스가 있는데 `key: NULL` — 쓸 수 있는 인덱스를 옵티마이저가 **버렸다**. 조건 가공/형변환이나 통계 문제를 의심.
- `Using temporary; Using filesort` — 대량 행을 임시 테이블에 부어 정렬까지 한다. 풀 스캔 결과 전체가 정렬 대상이면 메모리 초과 시 디스크 정렬로 번진다.

**2. type 성능 순서 (좋은 것부터)**

```
const > eq_ref > ref > range > index > ALL
```

- `const`: PK/유니크 동등 조건, 최대 1건. `eq_ref`: 조인에서 PK/유니크로 1건 매칭. `ref`: 논유니크 인덱스 동등. `range`: 인덱스 범위. `index`: 인덱스 **풀** 스캔(오해 주의 — 좋은 게 아니다). `ALL`: 테이블 풀 스캔.

**3. Using temporary; Using filesort 대표 패턴과 판단 기준**
- 대표 패턴: `GROUP BY`와 `ORDER BY`의 기준이 다른 쿼리, 조인 결과를 드라이빙 테이블 인덱스로 커버 못 하는 정렬, `DISTINCT` + 정렬 조합 등.
- **항상 제거해야 하는 건 아니다.** 몇십~몇백 건을 메모리에서 정렬하는 filesort는 무해하다. 문제는 "대량 행 × 임시 테이블 × 디스크 정렬"의 조합이다. rows가 작으면 그대로 두는 게 맞고, 크면 인덱스 정렬 활용(문제 6)이나 대상 축소(문제 9)로 접근한다.

---

## 보너스 문제 11. 통계가 낡으면 생기는 일

**1. 의심 원인: 낡은 인덱스 통계(스테일 통계)**
옵티마이저는 실제 데이터가 아니라 **통계 정보(카디널리티 추정)** 로 실행 계획을 세운다. 대량 배치 INSERT 직후 통계가 실제 분포를 반영하지 못하면, 인덱스 스캔 비용을 과대평가해서 풀 스캔을 선택할 수 있다. "쿼리도 스키마도 안 바뀌었는데 계획이 바뀌었다"의 단골 범인. (그 외 후보: 파라미터 값에 따른 선택도 변화, 인덱스 단편화.)

**2. 조치**
- **즉시 조치**: `ANALYZE TABLE orders;`로 통계 재수집. 급하면 해당 쿼리에 힌트(`FORCE INDEX`, 8.0의 옵티마이저 힌트)로 응급 처치하되, 힌트는 데이터 분포가 변하면 족쇄가 되므로 원인 해소 후 제거를 검토한다.
- **근본 조치**: 대량 배치 후 `ANALYZE TABLE`을 배치 마지막 단계에 포함. InnoDB 자동 통계 갱신(`innodb_stats_auto_recalc`, 기본은 전체의 10% 변경 시)과 `innodb_stats_persistent_sample_pages`(샘플링 정확도)를 점검하고, 실행 계획 변동을 감지할 수 있게 슬로우 쿼리 모니터링을 걸어둔다.
