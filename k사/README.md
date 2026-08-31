# 이력서 — K사 경력 · 프로젝트

---

## K사 — 백엔드 개발

**C / Pro\*C 레거시를 Java · Spring · MyBatis 로 이관하는 차세대 전환 프로젝트.**
이관 개발과 함께, 이관에 필요한 검증 도구를 만들어 팀에 제공했습니다.

### 성능 개선

| 대상 | 조치 | 결과 |
|---|---|---|
| [결과 조회 API](<ai를 써서 20초짜리 응답개선하기.md>) | 대형 CLOB 본문을 전 행 fetch → **lazy 로딩** | **20s → 400ms** |
| [결과 비교 화면](<20초짜리 페이지 렌더링개선하기.md>) | diff 라이브러리에 강제 주입한 CSS 제거 → 기본 테마 사용 | **20s → 2s** (평균 40ms) |

추정으로 고치지 않았습니다. 목 데이터로 운영 규모를 재현하고 **프로파일링으로 병목을 확인한 뒤** 조치했습니다.

### [레거시 호환 데이터 계층 설계](<spring,C레거시 호환 데이터계층 구성/readMe.md>)

C 개발자 위주 조직에서 계층마다 try-catch로 분기하는 코드가 굳어져 있어, **"C처럼 읽히되 Java답게 관리되는"** 구조를 설계했습니다.

- 실패를 [예외가 아닌 결과 객체](<spring,C레거시 호환 데이터계층 구성/예외보단객체로 내야하는 이유.md>)(`DataResultWrapper<T>`)로 반환 → 중간 계층 try-catch 제거
- 매퍼별 **파사드 빈 자동 등록** → 래핑·변환 일원화 ([예제 코드](https://github.com/hyunsu15/spring-c-layer))
- Spring 추상화를 벗어난 MyBatis `TooManyResultsException` [표준 예외로 번역](<spring,C레거시 호환 데이터계층 구성/마이 바티스 예외표준화.md>)

### 제안으로 한 것들

- **[`@Fix타입` 어노테이션제작](<아이디어로 편의성 개선한 경험.md>)** — "확장이 어렵다"는 회신을 받았지만 `@RequestMapping` → `@PostMapping` 구조를 근거로 `@AliasFor` 초안을 만들어 제안, **사내 라이브러리에 반영**
- **[Git 브랜치 전략](<git 브랜치 전략.md>)** — 환경별 3브랜치 → `main` + `dev` + **동일 아티팩트 승격 배포** 제안
- **[빌드 실패 알림](<빌드실패시 메터모스트알림가는기능.md>)** — ArgoCD · GitLab CI 실패를 Mattermost로 자동 통보, 수동 확인 제거
- **[BigDecimal 수식 가독성](<자바스프링에서 BigDecimal 쉽게쓰기.md>)** — Kotlin 연산자 오버로딩으로 도메인 수식 분리 제안

---

## 프로젝트

### PB5 검증 도구(비공개)

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

### 환경·날짜별 소스 Diff 비교 도구 (date-code-differ)

**환경·날짜별로 쌓인 소스 스냅샷을 좌우로 골라 비교하는 diff 도구.**
차세대 전환 기간에 환경마다 배포 시점이 달라, "이번 배포에 뭐가 바뀌었나"와 "지금 운영과 테스트가 얼마나 벌어져 있나"를 원격 서버를 직접 읽어 답합니다.

`React 18` `TypeScript` `Vite 6` `Prism` `Node.js` `OpenSSH` `node:test`

[github.com/hyunsu15/date-code-differ](https://github.com/hyunsu15/date-code-differ) · [사용설명서](https://github.com/hyunsu15/date-code-differ/blob/master/docs/사용설명서.md)

- **느린 원인을 diff가 아닌 SSH 접속 횟수로 특정** — 병목은 계산도 전송량도 아닌 `ssh` 프로세스 기동(TCP·키교환·인증) 횟수였습니다. Windows OpenSSH는 접속 재사용(`ControlMaster`)을 지원하지 않아, 재사용 대신 **명령 수 자체를 줄이는** 쪽으로 소스 계층을 배치형(`readFiles`·`listFilesIn`)으로 바꿨습니다 — 좌우 비교(파일 500개) **1,000회 → 7회**, 날짜 목록(폴더 30개) **32회 → 2회**
- **성능 개선이 결과를 바꾸지 않음을 테스트로 고정** — `cksum`으로 좌우 동일 파일을 걸러 한쪽만 받도록 바꾸면서, 체크섬이 있을 때와 없을 때의 비교 결과가 `deepEqual`로 완전히 같고 **읽기 횟수만** 줄어드는 것을 픽스처로 못박았습니다. 요약 숫자가 달라지면 그건 개선이 아니라 버그라고 봤습니다
- **비밀번호를 다루지 않는 인증** — 받지도·보관하지도·대신 답하지도 않고 `execFile('ssh', ...)`로 OpenSSH에 위임(개인키·ssh-agent). 원격에서는 읽기 명령만 돌리고 파일을 내려받지 않습니다(`scp` 대신 `cat` 표준출력)
- **레거시 환경 그대로 읽기** — EUC-KR로 저장된 Pro\*C 소스를 UTF-8 우선 시도 후 자동 판별(올바른 UTF-8은 항상 UTF-8로 읽혀 멀쩡한 파일이 망가지지 않음), GNU 전용 옵션(`find -printf`·`stat -c`)을 배제해 AIX·Solaris에서도 동작
- **환경 선택을 필터가 아니라 경로 치환으로** — 좌우가 각각 (환경, 날짜)를 고르게 해서 기존 "한 환경의 두 날짜"가 그 특수 케이스가 되도록 했습니다. 모드 전환이 없어지고 서버에는 환경이라는 개념 자체가 남지 않아 캐시도 저절로 갈립니다
- 테스트 240개 · 소스 53개 파일 · 스크린샷은 실제 요소 좌표를 읽어 자동 생성(`npm run shots`)
