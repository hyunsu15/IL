# 이력서 — K사 경력 · 프로젝트

---

## K사 — 백엔드 개발

**C / Pro\*C 레거시를 Java · Spring · MyBatis 로 이관하는 차세대 전환 프로젝트.**
이관 개발과 함께, 이관에 필요한 검증 도구를 직접 만들어 팀에 제공했습니다.

### 성능 개선

| 대상 | 조치 | 결과 |
|---|---|---|
| [결과 조회 API](<ai를 써서 20초짜리 응답개선하기.md>) | 대형 CLOB 본문을 전 행 fetch → **lazy 로딩** | **20s → 400ms** |
| [결과 비교 화면](<20초짜리 페이지 렌더링개선하기.md>) | diff 라이브러리에 강제 주입한 CSS 제거 → 기본 테마 사용 | **20s → 2s** (평균 40ms) |

추정으로 고치지 않았습니다. 목 데이터로 운영 규모를 재현하고 **프로파일링으로 병목을 확인한 뒤** 조치했습니다.
AI가 권한 인덱스 추가는 **의도적으로 제외** — 조회보다 저장이 잦은 서비스라 쓰기 비용이 더 크고, 응답 크기는 그대로였습니다.

### [레거시 호환 데이터 계층 설계](<spring,C레거시 호환 데이터계층 구성/readMe.md>)

C 개발자 위주 조직에서 계층마다 try-catch로 분기하는 코드가 굳어져 있어, **"C처럼 읽히되 Java답게 관리되는"** 구조를 설계했습니다.

- 실패를 [예외가 아닌 결과 객체](<spring,C레거시 호환 데이터계층 구성/예외보단객체로 내야하는 이유.md>)(`DataResultWrapper<T>`)로 반환 → 중간 계층 try-catch 제거
- 매퍼별 **파사드 빈 자동 등록** → 래핑·변환 일원화 ([예제 코드](https://github.com/hyunsu15/spring-c-layer))
- Spring 추상화를 벗어난 MyBatis `TooManyResultsException` [표준 예외로 번역](<spring,C레거시 호환 데이터계층 구성/마이 바티스 예외표준화.md>)

### 제안으로 해결한 것

- **[타입별 `@Fix` 어노테이션](<아이디어로 편의성 개선한 경험.md>)** — "확장이 어렵다"는 회신을 받았지만 `@RequestMapping` → `@PostMapping` 구조를 근거로 `@AliasFor` 초안을 만들어 재제안, **사내 라이브러리에 반영**
- **[Git 브랜치 전략](<git 브랜치 전략.md>)** — 환경별 3브랜치 → `main` + `dev` + **동일 아티팩트 승격 배포** 제안
- **[빌드 실패 알림](<빌드실패시 메터모스트알림가는기능.md>)** — ArgoCD · GitLab CI 실패를 Mattermost로 자동 통보, 수동 확인 제거
- **[BigDecimal 수식 가독성](<자바스프링에서 BigDecimal 쉽게쓰기.md>)** — Kotlin 연산자 오버로딩으로 도메인 수식 분리 제안

---

## 프로젝트

### PB5 검증 도구

**기존(PB)과 신규(PB5) 서버에 같은 전문을 보내고 응답 차이를 찾아내는 회귀 테스트 도구.**
차세대 전환 기간에 두 시스템이 정말 같은 응답을 주는지 전문 단위로 검증합니다.

`React 19` `TypeScript` `Tailwind` `Express 5` `WebSocket` `Oracle` `Vitest`

[github.com/hyunsu15/compare-test](https://github.com/hyunsu15/compare-test) · [사용설명서](https://github.com/hyunsu15/compare-test/blob/main/docs/사용설명서.md)

- **깨진 전문의 폴백 제거** — 헤더가 잘리면 빈 헤더끼리 '일치'로 집계되던 문제 발견. 테스트 도구에서 가장 위험한 건 실패가 성공으로 보이는 것이라 판단해 예외로 승격
- **수만 건 실행 전제 설계** — 스텝에 `caseId` 참조만 전송(413 회피), 저장 버퍼를 JSON 텍스트로 유지(약 3.8배 경량)
- **끊겨도 유실 없는 진행 스트리밍** — `seq` 기반 버퍼로 재연결 시 replay
- **무인 실행** — 실패 시 exit `1` 로 종료해 CI·스케줄러 연동
- 커밋 284개 · 소스 215개 파일

### 소스 비교검증 도구 (comparsion)

**PB(C) 소스와 PB5(Java) 소스를 좌우로 놓고 줄 단위로 의견을 주고받는 이관 정합성 리뷰 도구.**
이관 담당자와 원 업무 담당자가 같은 화면을 보게 만드는 것이 목적입니다.

`React 19` `TypeScript` `Tailwind` `Shiki` `NestJS 11` `Oracle`

[github.com/hyunsu15/comparsion-code](https://github.com/hyunsu15/comparsion-code) · [사용 설명서](https://github.com/hyunsu15/comparsion-code/blob/main/사용%20설명서.md)

- **줄 단위 토론** — 마지막 작성자에 따라 "다음 확인 차례"가 자동으로 넘어가 리뷰 누락 방지
- **SQL 대응 보기** — 매퍼 호출 줄 클릭 시 PB 인라인 `EXEC SQL` ↔ MyBatis XML 나란히 비교
- **메서드 대응 점프 · 가상 스크롤 · EUC-KR 자동 복원** — 대용량 레거시 소스 대응
- **체크리스트 매트릭스** — 프로그램 × 점검 문항으로 진행률 조망
- 소스 119개 파일 · 캡처 18장 사용 설명서
