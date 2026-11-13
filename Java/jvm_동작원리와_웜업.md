# JVM 동작 원리와 웜업

> 자바는 컴파일이 **두 번** 일어난다. 빌드할 때 javac가 한 번, 실행 중에 JIT이 한 번.
> 이 두 번째 컴파일이 언제 어떻게 일어나는지가 곧 **웜업**의 정체다.

다이어그램 버전: https://claude.ai/code/artifact/549ad830-29f5-47c9-89e7-ab57b3ebb776

---

## 1. javac는 생각보다 하는 게 없다

### 하는 일

- **파싱** — 소스를 구문 트리로
- **타입 검사** — 제네릭, 상속 규칙, 검사 예외
- **디슈가링** — 문법 설탕 걷어내기
  - 제네릭 소거: `List<String>` → `List` + 캐스트 삽입
  - for-each → 이터레이터 루프
  - 람다 → `invokedynamic` 명령 하나 (실제 구현체는 런타임에 생성)
  - 문자열 `+` → JDK 9부터 `invokedynamic`

### 안 하는 일

**최적화를 거의 안 한다.** 인라이닝도, 루프 최적화도, 죽은 코드 제거도 없다.
`-O` 같은 최적화 플래그가 아예 없다.

일부러다. 최적화는 런타임 정보를 가진 JIT이 훨씬 잘하니까, 바이트코드는
**최적화되지 않은 중립적인 중간 표현**으로 남겨둔다.

### 바이트코드는 스택 머신 명령어

x86이 레지스터 머신이라면 JVM은 **스택 머신**이다.
값을 스택에 올리고, 연산자가 꺼내 쓰고, 결과를 다시 올린다.
레지스터 할당이 없으니 어떤 CPU에도 이식 가능하고, 대신 그대로는 못 돌린다.

```java
int calc(int a, int b) {
    int c = a + b;
    return c * 2;
}
```

`javap -c` 출력:

```
0: iload_1     // a를 스택에 push
1: iload_2     // b를 push
2: iadd        // 둘 pop, 합을 push
3: istore_3    // pop해서 c에 저장
4: iload_3     // c를 push
5: iconst_2    // 상수 2를 push
6: imul        // 둘 pop, 곱을 push
7: ireturn     // pop해서 반환
```

직접 보려면:

```bash
javap -c -p build/classes/java/main/com/example/Order.class
```

### .class 파일 구조

```
magic          CAFEBABE
version        메이저 · 마이너
constant_pool  문자열 · 클래스명 · 메소드 시그니처가 전부 여기
access_flags   public / final / abstract ...
this_class     자기 자신과 부모의 상수 풀 인덱스
super_class
interfaces
fields
methods        각 메소드의 Code 속성 = 실제 바이트코드
attributes     LineNumberTable, StackMapTable ...
```

- 첫 4바이트가 `0xCAFEBABE`
- 메이저 버전 = **JDK 버전 + 44**
  - JDK 8 = 52, 11 = 55, 17 = 61, 21 = 65, 25 = 69
  - `UnsupportedClassVersionError`에 찍히는 그 숫자
- **상수 풀**이 핵심. 클래스 안의 모든 이름과 참조가 여기 문자열로 있고,
  바이트코드는 인덱스로만 가리킨다.
  즉 이 시점의 참조는 전부 **심볼**이다 — `"java/util/ArrayList.add"`라는 글자일 뿐,
  실제 메모리 주소가 아니다. 이걸 주소로 바꾸는 게 다음 단계.

---

## 2. 클래스 로딩 · 링킹 · 초기화

JVM은 기동할 때 클래스를 전부 읽지 않는다. **필요해지는 순간에 하나씩** 읽는다.
이게 자바 기동이 느린 이유이자, CDS가 개입하는 지점.

### 로딩 — 부모에게 먼저 위임

| 클래스로더 | 담당 | 비고 |
|---|---|---|
| Bootstrap | `java.base` 등 핵심 모듈 | JVM에 네이티브로 내장 |
| Platform | 나머지 JDK 모듈 | JDK 9에서 Extension을 대체 |
| Application | 클래스패스의 내 코드와 라이브러리 | 기본 로더 |

요청이 오면 **먼저 부모에게 위임**하고, 부모가 못 찾을 때만 자기가 찾는다.

이유는 **보안**. 누가 `java.lang.String`을 직접 만들어 클래스패스에 넣어도
요청이 Bootstrap까지 올라가서 진짜 `String`이 먼저 로딩된다.

톰캣이나 스프링 부트는 이 규칙을 살짝 비트는 커스텀 로더를 쓴다.
→ `NoClassDefFoundError`가 이해 안 되는 방식으로 날 때 대개 이 부분.

### 링킹 3단계

1. **검증(Verification)** — 바이트코드가 안전한지 확인.
   스택 오버/언더플로우, 타입 안전성, 잘못된 점프.
   javac가 만든 정상 클래스라도 예외 없이 검사한다 (누가 바이트를 조작했을 수 있으니).
   **비용이 꽤 크다** ← CDS가 노리는 지점
2. **준비(Preparation)** — static 필드에 자리 잡고 기본값(`0`, `null`) 할당. 아직 진짜 값 아님
3. **해석(Resolution)** — 상수 풀의 심볼 참조를 실제 참조로 변환.
   지연 가능해서 해당 명령이 처음 실행될 때 풀리는 경우가 많다

### 초기화

static 초기화 블록과 static 필드 실제 값 대입이 실행된다.
javac가 이걸 모아서 `<clinit>`이라는 특수 메소드로 만들어 둔다.

JVM이 **클래스당 정확히 한 번, 스레드 안전하게** 호출해준다.
→ 홀더 패턴 싱글턴이 락 없이도 동작하는 이유.

---

## 3. 런타임 데이터 영역

### 모든 스레드가 공유

| 영역 | 내용 |
|---|---|
| **힙** | 모든 객체 인스턴스와 배열. GC가 관리하는 유일한 영역 |
| **메타스페이스** | 클래스 메타데이터 · 상수 풀 · 메소드. JDK 8부터 PermGen 대체, 네이티브 메모리. **CDS 아카이브가 여기 매핑됨** |
| **코드 캐시** | JIT이 만든 기계어가 사는 곳 |

### 스레드마다 하나씩

- **JVM 스택** — 프레임 하나가 메소드 호출 하나.
  프레임 안에 지역변수 배열 + 피연산자 스택 (위의 `iload_1`, `iadd`가 작동하는 무대).
  재귀가 깊어지면 `StackOverflowError`
- **PC 레지스터** — 현재 실행 중인 바이트코드 위치
- **네이티브 메소드 스택**

### 코드 캐시 주의

JIT 컴파일한 기계어가 전부 여기 들어간다.
기본 크기(`-XX:ReservedCodeCacheSize`, 보통 240MB)를 다 쓰면
**JIT이 컴파일을 중단하고 앱이 조용히 인터프리터로 되돌아간다.**

클래스가 아주 많은 대형 모놀리식 앱에서 "며칠 돌다가 갑자기 느려졌다"의 범인일 수 있음.
로그에 `CodeCache is full` 찍힘.

---

## 4. 실행 엔진 — 웜업이 태어나는 곳

바이트코드를 실제로 돌리는 수단은 둘.

- **인터프리터** — 바이트코드를 하나씩 읽어 실행. 준비 시간 0, 대신 대략 10~20배 느림
- **JIT 컴파일러** — 기계어로 번역해서 코드 캐시에 저장. 빠르지만 컴파일 자체가 시간과 CPU를 먹음

전부 컴파일하면 기동이 하염없이 느려지고, 전부 해석하면 처리량이 안 나온다.
그래서 **계층 컴파일(Tiered Compilation)** 로 절충한다.

### 계층 구조

| 티어 | 누가 | 특징 |
|---|---|---|
| 0 | 인터프리터 | 즉시 실행, 제일 느림 |
| 1~3 | C1 | ms 단위로 빨리 컴파일, 성능 중간 |
| 4 | C2 / Graal | 컴파일이 오래 걸림, 코드는 최고 |

일반 경로는 `0 → 3 → 4`.

```
인터프리터  --~200회-->  C1(티어3, 프로파일 수집)  --~5,000회-->  C2(티어4)
     ^                                                            |
     +---------------- 역최적화 (uncommon trap) ------------------+
```

**티어 3은 일부러 티어 1보다 느리다.** 프로파일을 모으려고 카운터를 잔뜩 심기 때문.
그 데이터가 있어야 티어 4에서 공격적 최적화가 가능하다.

### 중요 — 임계치는 고정이 아니다

**컴파일 큐 길이에 따라 임계치가 동적으로 올라간다.**
차가운 서버에 실트래픽을 한 번에 꽂으면 큐가 밀리고,
그러면 JVM이 승격 임계치를 더 올려서 **웜업이 오히려 길어진다.**

→ 워밍업 스크립트로 미리 천천히 데우라는 조언의 근거.

### OSR (On-Stack Replacement)

승격 기준이 호출 횟수뿐이라면, `main`에서 한 번 호출되고 안에서 백만 번 도는 루프는
영원히 티어 0에 남는다.

그래서 **백엣지 카운터**(루프가 뒤로 점프한 횟수)도 센다.
임계치를 넘으면 **루프가 도는 도중에** 프레임을 컴파일된 버전으로 갈아끼운다.

---

## 5. 왜 처음부터 다 컴파일하지 않나

JIT은 정적 컴파일러가 **원리적으로** 못 하는 일을 한다.

```java
int sum(List<Integer> list) {
    int s = 0;
    for (int v : list) s += v;
    return s;
}
```

정적으로 보면 최악이다. `List` 구현체는 수십 개, `iterator()`/`hasNext()`/`next()`가
전부 가상 호출, `Integer` 언박싱, 이터레이터 객체 힙 할당까지.

JIT은 실행을 지켜본 뒤 이렇게 푼다:

1. **관측** — "이 호출 지점엔 지금까지 `ArrayList`만 들어왔다"
2. **탈가상화 + 인라이닝** — 가상 호출을 직접 호출로 바꾸고 이터레이터 코드를 펼침
3. **탈출 분석** — 펼쳐놓고 보니 이터레이터 객체가 메소드 밖으로 안 나감
4. **스칼라 치환** — 안 나가니 힙에 만들 필요 없음. 할당 자체가 소멸
5. 결국 **생 배열 인덱스 루프**로 내려앉음

1번을 정적 컴파일러는 못 한다. 자바는 클래스를 런타임에 로딩하니
**컴파일 시점엔 클래스 계층이 확정조차 안 된다.**

### JIT의 진짜 무기 — 역최적화라는 탈출구

JIT은 "이 참조는 null이 아니다", "이 클래스는 상속된 적 없다" 같은 걸
**증명 없이 가정하고** 컴파일한다.

가정이 깨지면 그 지점의 *uncommon trap* 이 발동 → 인터프리터로 롤백 → 프레임 복원 → 재컴파일.

**정적 컴파일러엔 이 탈출구가 없어서 보수적으로 갈 수밖에 없다.**
자바가 "느린 언어" 인식을 벗어난 게 대부분 이 메커니즘 덕분.

> 결론: 자바는 **웜업이라는 비용을 내고 피크 성능을 샀다.**

---

## 6. 실제 실행 순서는 순차적이지 않다

"다 로딩하고 → 다 링킹하고 → 그다음 실행"이 **아니다.**
**실행이 로딩을 불러온다.**

```java
public static void main(String[] args) {
    Order o = new Order();
    o.process();
}
```

실제 순서:

```
1. JVM 프로세스 시작, 힙·메타스페이스 확보
2. Bootstrap 로더가 Object, String 등 핵심 클래스 로딩
3. App 클래스 로딩 → 검증 → 준비 → <clinit>
4. main() 실행 시작            ← 인터프리터로
5.   new Order() 명령을 만남
       → 상수 풀의 "Order"가 아직 문자열 심볼임
       → 이제서야 Order 로딩 → 검증 → 링킹 → <clinit>
       → 그다음 인스턴스 생성
6.   o.process() 호출
       → 그 안에서 새 클래스를 만나면 또 5번 반복
```

**5번이 몇만 번 반복되는 게 곧 기동 시간.**
스프링 부트가 5초씩 걸리는 이유 — 컴포넌트 스캔하면서 클래스를 계속 새로 만나고,
만날 때마다 파일 읽고 파싱하고 검증한다.

### JIT도 "단계"가 아니라 앱 수명 내내 도는 백그라운드 활동

메소드가 임계치를 넘었다고 앱이 멈춰서 컴파일하는 게 아니다.
**별도 컴파일러 스레드**(`C2 CompilerThread0`)가 백그라운드에서 컴파일하고,
그동안 원래 메소드는 인터프리터로 계속 돈다. 완료되면 **다음 호출부터** 갈아탄다.

- 어떤 메소드는 티어 0, 어떤 건 티어 3, 어떤 건 티어 4 — **동시에 섞여 있다**
- 새 코드 경로를 타면 그때부터 새로 웜업 시작
- 역최적화로 굴러떨어졌다가 다시 올라가기도 함

**앱이 죽을 때까지 끝나지 않는다.**
"웜업이 끝났다"는 건 "지금 트래픽이 밟는 경로가 다 데워졌다"는 뜻이지
JIT이 일을 멈췄다는 게 아니다.

| | 언제 | 몇 번 |
|---|---|---|
| javac | 빌드 타임, 다른 기계 | 한 번 |
| 로딩·링킹·초기화 | 런타임, 실행 도중 | **클래스마다** 각각 한 번 |
| 인터프리터 / JIT | 런타임, 앱 수명 내내 | **메소드마다** 계속 |

---

## 7. 웜업 ≠ 기동 — 다른 문제다

이 둘을 "웜업"이라는 한 단어로 뭉뚱그리면 대책이 안 나온다.

### JVM 층 (진짜 웜업)

Spring을 안 써도, 순수 자바 서버라도 겪는다.

- JIT 계층 상승 (위의 4~5장)
- 프로파일 기반 투기적 최적화와 deopt
- 클래스 로딩/링킹/검증
- 컴파일러 스레드가 CPU를 먹음
- GC/힙 적응 (TLAB 크기, `AlwaysPreTouch` 안 켜면 페이지 폴트)

### 스프링/라이브러리 층 (지연 초기화 — 웜업처럼 보이지만 일회성)

- Hibernate: 엔티티별 첫 쿼리에서 프록시 생성, 쿼리 플랜/스테이트먼트 캐시가 빔
- Jackson: 타입별 시리얼라이저를 첫 사용 시점에 빌드
- HikariCP: 커넥션이 안 채워져 있으면 첫 요청이 TCP+인증 핸드셰이크 부담. DB 쪽 첫 실행계획 하드파싱
- TLS/JSSE 컨텍스트 초기화, 첫 핸드셰이크, DNS 조회
- Tomcat 스레드풀 램프업

컴포넌트 스캔·빈 생성은 **startup**이지 warmup이 아니다. 컨텍스트 ready에 끝난다.

### 스프링은 JVM 웜업을 *증폭*시킨다

요청 하나가 MVC → Security 필터체인 → AOP 프록시 → 트랜잭션 인터셉터 → Hibernate를
타면서 **고유 메소드 수천 개**를 밟는다.
프록시·다중 구현 인터페이스 때문에 호출 지점이 megamorphic이라 인라이닝이 잘 안 붙고,
리플렉션 경로는 인라인 캐시가 안정될 때까지 최적화가 안 된다.

### 구분하는 법

| 관측 | 원인 |
|---|---|
| **첫 몇 건**만 극단적으로 느리고 바로 정상화 | 지연 초기화 (스프링/라이브러리) |
| 수백~수천 건 동안 완만하게 좋아짐 | JIT 웜업 |
| 잘 돌다가 특정 API 처음 호출 때만 튐 | 그 경로의 지연 초기화 + 클래스 로딩 |

측정:

```bash
java -XX:StartFlightRecording=duration=120s,filename=warmup.jfr,settings=profile -jar app.jar
```

`jdk.Compilation`, `jdk.ClassLoad` 이벤트가 시간축 어디에 몰려 있는지 본다.
컴파일 이벤트가 계속 깔려 있으면 JIT, 초반에만 클래스 로딩이 몰려 있으면 초기화 쪽.

---

## 8. 대책 — 우선순위 순

### 1순위. 워밍업 스크립트 + LB 등록 지연

readiness probe 통과 전에 주요 엔드포인트를 자기 자신에게 N번 호출.

**유일하게 JIT 웜업과 라이브러리 지연 초기화를 동시에 해소한다.**
원인 분리 안 해도 둘 다 커버되니 ROI가 제일 좋다.

### 2순위. JDK 24/25 AOT 캐시 (Project Leyden)

- JEP 483 (JDK 24): Ahead-of-Time Class Loading & Linking
- JEP 514 (JDK 25): 커맨드라인 한 단계로 축약
- JEP 515 (JDK 25): **AOT Method Profiling** ← 이게 진짜 웜업 대책

트레이닝 실행 중에 JIT이 런타임에 힘들게 모으던 실행 프로파일을 미리 기록.
운영에서는 C2가 만 번 기다릴 필요 없이 처음부터 좋은 프로파일로 컴파일에 들어간다.

```bash
java -XX:AOTCacheOutput=app.aot -jar app.jar
```

```bash
java -XX:AOTCache=app.aot -jar app.jar
```

JDK 24는 두 단계로 나뉜다.

```bash
java -XX:AOTMode=record -XX:AOTConfiguration=app.aotconf -jar app.jar
```

```bash
java -XX:AOTMode=create -XX:AOTConfiguration=app.aotconf -XX:AOTCache=app.aot -jar app.jar
```

- 기동 **-30~40%** (잘 측정됨)
- 웜업 개선은 **트레이닝이 요청 경로를 밟았을 때만** 나옴
- 프로파일을 저장하는 거지 기계어를 저장하는 건 아님. 램프가 왼쪽으로 당겨질 뿐 사라지진 않음
- 트레이닝 트래픽 패턴이 운영과 다르면 잘못된 추측 → deopt → 재컴파일로 **역효과** 가능

### 3순위. AppCDS (JDK 21 이하에 묶여 있다면)

```bash
java -XX:+AutoCreateSharedArchive -XX:SharedArchiveFile=app.jsa -jar app.jar
```

Spring Boot 공식 플로우:

```bash
java -Djarmode=tools -jar my-app.jar extract --destination application
```

```bash
java -XX:ArchiveClassesAtExit=application.jsa -Dspring.context.exit=onRefresh -jar my-app.jar
```

```bash
java -XX:SharedArchiveFile=application.jsa -jar my-app.jar
```

- 기동 **-20~25%**
- **웜업은 거의 그대로.** 컴파일된 코드도 프로파일도 없으니까
- `-Dspring.context.exit=onRefresh`로 트레이닝하면 기동 경로만 담긴다 →
  컨트롤러/Jackson/Hibernate 실행 경로는 아카이브에 없음

**JDK 24+면 AOT 캐시가 CDS를 포함한다.** 둘을 같이 못 쓴다
(`-XX:AOTCache`와 `-XX:SharedArchiveFile`은 배타적).

### GraalVM — 두 가지를 구분할 것

| | 웜업 | 피크 |
|---|---|---|
| **Graal JIT (JVM 모드)** | 비슷하거나 약간 느림 | 워크로드 따라 0~20% 향상 (스트림/람다/Kotlin에 유리) |
| **Native Image (AOT)** | 사실상 소멸 | **-20~30%** (PGO+G1이면 -5~15%까지 좁혀짐) |

- Graal JIT을 웜업 목적으로 켜는 건 헛수고
- Native Image는 웜업을 사서 처리량을 파는 거래.
  오래 떠 있는 API 서버엔 불리, 짧게 뜨고 죽는 배치/람다/CLI엔 유리
- 비용: 리플렉션·동적 프록시·JNI 메타데이터 등록, 빌드 시간, 라이브러리 호환성, JFR/APM 제약
- RSS는 1/3~1/2로 감소
- GraalVM **CE에는 PGO와 G1이 없다.** CE로 벤치 돌리고 "네이티브 느리네" 하면 오판.
  Oracle GraalVM은 GFTC 하에 프로덕션 무료

### 안 되는 것 / 주의

- `-XX:TieredStopAtLevel=1` — 시작만 빠르고 피크가 죽는다. 단명 프로세스 외엔 금지
- CRaC — 시작+웜업 거의 0이지만 CRIU 커널 권한, 열린 커넥션/시크릿 복원 처리 필요.
  금융권에선 승인이 번거로움

### 적용 확인 (중요)

CDS/AOT 캐시는 **안 맞아도 조용히 무시하고 그냥 뜬다.**

```bash
java -Xshare:on -XX:SharedArchiveFile=application.jsa -jar my-app.jar
```

기본값 `-Xshare:auto`는 경고만 남기고 넘어가지만
`-Xshare:on`은 **기동 자체를 실패시킨다.** 배포 스모크 테스트에 넣을 것.

실제 몇 개나 공유됐는지 — `cl.log`에서 `shared objects file`로 끝나는 줄이 아카이브에서 온 클래스다.

```bash
java -Xlog:class+load:file=cl.log -XX:SharedArchiveFile=application.jsa -jar my-app.jar
```

---

## 9. HikariCP — 커넥션 풀 웜업

### 기동 시점에 커넥션을 딱 1개만 만든다

`HikariPool` 생성자의 `checkFailFast()`가 "DB에 붙긴 붙는지" 확인용으로 한 개만 만들고 리턴.
나머지는 HouseKeeper 스레드가 비동기로, **단일 스레드 executor에서 하나씩 순차적으로** 채운다.

원격 DB에 TLS+인증까지 개당 50~200ms면 풀 20개 채우는 데 수 초.
그동안 들어온 요청은 `getConnection()`에서 블로킹된다.

참고: `minimum-idle`은 기본값이 `maximumPoolSize`라 명시 안 해도 고정 크기 풀이다.
**설정 문제가 아니라 채우는 시점 문제.**

### 강제 프리필

```java
@Component
@RequiredArgsConstructor
public class DataSourceWarmUp {

    private final DataSource dataSource;

    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() throws SQLException {
        int size = dataSource.unwrap(HikariDataSource.class).getMaximumPoolSize();
        List<Connection> held = new ArrayList<>(size);
        try {
            for (int i = 0; i < size; i++) {
                Connection c = dataSource.getConnection();
                held.add(c);
                try (Statement st = c.createStatement()) {
                    st.execute("SELECT 1 FROM DUAL");
                }
            }
        } finally {
            for (Connection c : held) {
                try { c.close(); } catch (SQLException ignore) {}
            }
        }
    }
}
```

**전부 동시에 들고 있다가 마지막에 반납하는 게 포인트.**
try-with-resources로 하나씩 열고 닫으면 같은 커넥션 하나만 재사용해서 풀이 안 채워진다.
가장 흔한 실수.

`ApplicationReadyEvent`에 거는 이유는 readiness probe보다 앞서기 위해서.

### 같이 봐야 할 설정

```yaml
spring:
  datasource:
    hikari:
      pool-name: main-pool
      maximum-pool-size: 20
      connection-timeout: 3000
      keepalive-time: 120000      # 유휴 커넥션 주기적 검사
      max-lifetime: 1740000       # DB/방화벽 타임아웃보다 짧게
```

- **`keepalive-time`** (Hikari 4.0+) — 없으면 야간처럼 트래픽 없는 구간에 방화벽/DB가
  유휴 커넥션을 끊는다. 아침 첫 요청이 죽은 커넥션을 잡고 실패하거나 재연결.
  **"새벽 배포도 안 했는데 아침 첫 요청만 느리다"의 범인**
- **`max-lifetime`** — DB `wait_timeout`이나 L4/방화벽 idle timeout보다 반드시 몇 초 짧게
- **`connection-test-query`** — 최신 JDBC4 드라이버면 설정하지 말 것. `isValid()`가 더 싸다

### 한계

풀 프리필은 **TCP + 인증 + TLS까지만** 데운다. 아래는 그대로 남는다.

- DB 실행계획 하드파싱
- Hibernate 쿼리 플랜 캐시, 엔티티 메타데이터, 프록시 생성
- 서버측 prepared statement 캐시

→ `SELECT 1` 말고 **실제 대표 쿼리**를 태우거나, 애초에 엔드포인트 워밍업 스크립트를 쓰는 게 낫다.

### 확인

```bash
curl -s localhost:8080/actuator/metrics/hikaricp.connections.idle
```

또는 `logging.level.com.zaxxer.hikari=DEBUG` → 30초마다:

```
main-pool - Pool stats (total=20, active=0, idle=20, waiting=0)
```

기동 직후 `total`이 1이 아니라 20이면 프리필 성공.
`waiting`이 0보다 크면 풀 부족이거나 아직 채워지는 중.

---

## 10. Docker / k8s

### 도커가 AOT 캐시에 제일 잘 맞는 환경

무효화 조건(JVM 버전 + 클래스패스 일치)을 이미지가 공짜로 보장해준다.
VM에 jar만 갈아끼우는 배포에선 사람이 지켜야 해서 조용히 깨진다.

```dockerfile
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY . .
RUN ./gradlew bootJar

FROM eclipse-temurin:25-jdk AS train
WORKDIR /app
COPY --from=build /app/build/libs/app.jar app.jar
RUN java -XX:AOTCacheOutput=app.aot -Dspring.context.exit=onRefresh -jar app.jar

FROM eclipse-temurin:25-jdk
WORKDIR /app
COPY --from=build /app/build/libs/app.jar app.jar
COPY --from=train  /app/app.aot            app.aot
ENTRYPOINT ["java", "-XX:AOTCache=app.aot", "-jar", "app.jar"]
```

**반드시 지킬 것:**

- **경로가 완전히 같아야 한다.** 트레이닝 `/app/app.jar`, 운영 `/opt/app/app.jar`면
  클래스패스 문자열이 달라져서 캐시 거부. `WORKDIR`도 파일명도 동일하게
- **베이스 이미지 동일하게.** `25-jdk`로 트레이닝하고 `25-jre`로 운영하는 건 위험.
  크기를 줄이려면 jlink 커스텀 런타임을 양쪽에 똑같이 사용

### 트레이닝 실행을 어떻게 끝내나 — `-Dspring.context.exit=onRefresh`

- **JVM 플래그가 아니라 Spring Boot 프로퍼티다.**
  `--spring.context.exit=onRefresh`나 `SPRING_CONTEXT_EXIT=onRefresh`도 동일
- ApplicationContext refresh 직후 정상 종료 (종료 코드 0)
- 왜 필요한가:
  1. 웹 앱은 스스로 안 죽어서 `docker build`가 멈춤
  2. **아카이브는 "정상 종료 시점"에 써진다.** `kill -9`면 파일이 아예 안 생김

담기는 것 / 안 담기는 것:

| 담김 | 안 담김 |
|---|---|
| 컴포넌트 스캔, 빈 생성 | 컨트롤러 핸들러 실행 경로 |
| 자동 설정 클래스 | Jackson 시리얼라이저 |
| JPA 엔티티 메타데이터 | Hibernate 쿼리 실행 경로 |
| 웹 서버 초기화 | 필터체인 실제 통과 |

→ 웜업까지 노린다면 프로퍼티 빼고 트래픽을 태운 뒤 `SIGTERM`:

```bash
java -XX:ArchiveClassesAtExit=application.jsa -jar my-app.jar &
PID=$!
./warmup.sh
kill -TERM $PID
wait $PID
```

`kill -9`는 절대 금지 (아카이브가 안 써짐), `wait`도 빼면 안 된다 (쓰기 완료 전에 다음 스텝으로 넘어감).

### fat jar 대신 추출된 형태

중첩 jar 구조는 CDS/AOT가 잘 못 다룬다.

```bash
java -Djarmode=tools -jar app.jar extract --destination application
```

의존성이 평평하게 풀려서 아카이브 효율 + 도커 레이어 캐싱이 같이 좋아진다.

### CPU 제한이 웜업을 죽인다

cgroup 쿼터에서 `ActiveProcessorCount`가 결정되는데, `--cpus=1`이나 낮은 k8s CPU limit이면
JIT 컴파일러 스레드가 요청 처리 스레드와 같은 한 코어를 두고 싸운다.

**같은 앱이 물리 서버에선 30초 만에 데워지는데 CPU 1코어 컨테이너에선 몇 분씩 걸린다.**

대책:

- startupProbe를 넉넉하게 잡고, 통과 전엔 LB에 안 붙이기
- CPU limit을 안 걸고 request만 걸기 (JIT이 초반에 여유분을 쓰게)
- 워밍업 스크립트를 startupProbe 안쪽에서 실행

### 이미지 크기

Spring Boot 앱의 AOT 캐시는 보통 수백 MB.
매 빌드마다 바뀌니 별도 레이어로 맨 뒤에 두고,
**pull 시간 증가 vs 웜업 단축**을 저울질할 것. 스케일아웃이 잦으면 손해일 수도.

---

## 11. 최종 정리

| 기법 | 기동 시간 | 웜업 | 피크 성능 | 도입 비용 |
|---|---|---|---|---|
| AppCDS | -20~25% | 거의 없음 | 변화 없음 | 빌드 한 단계 |
| AOT 캐시 | -30~40% | 개선 (트레이닝에 트래픽 필요) | 변화 없음 | 빌드 한 단계 + JDK 24↑ |
| 워밍업 스크립트 | 오히려 증가 | **가장 확실** | 변화 없음 | 스크립트 + readiness 연동 |
| Native Image | -95%↑ | 소멸 | **-5~30%** | 메타데이터·호환성·빌드 시간 |

### 목적별 선택

- **"배포 후 ready까지가 길다"** → CDS 또는 AOT 캐시
- **"배포 직후 p99가 튄다"** → 워밍업 스크립트가 먼저
- 둘은 다른 문제고 다른 약을 쓴다

### 지표는 하나로

> **배포 후 정상 p99에 도달하기까지 걸린 요청 수**

기동 시간만 보면 개선된 것처럼 보이는데 정작 원했던 게 아닐 수 있다.

---

## 직접 확인해보는 명령어

바이트코드 들여다보기:

```bash
javap -c -p Foo.class
```

JIT 끄고 인터프리터로만 실행 (얼마나 느린지 체감):

```bash
java -Xint -jar app.jar
```

어떤 메소드가 몇 번째 티어로 언제 올라가는지:

```bash
java -XX:+PrintCompilation -jar app.jar
```

클래스가 언제 어디서 로딩되는지:

```bash
java -Xlog:class+load -jar app.jar
```

웜업 구간 전체를 타임라인으로:

```bash
java -XX:StartFlightRecording=settings=profile,filename=warmup.jfr -jar app.jar
```
