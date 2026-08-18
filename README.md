> **📌 이 저장소는 포트폴리오용 포크입니다.**
> 원본: [hk-toss-final-project/bambi-service-api](https://github.com/hk-toss-final-project/bambi-service-api) · 프로젝트 개요는 [bambi](https://github.com/flathfk/bambi)
> 아래는 **임소라(flathfk)가 이 레포에서 맡은 부분**이고, 원본 README는 이어서 나옵니다.

# 내가 한 일 — service ↔ agent 연동 경계 · 관리자 API

**53커밋 / +8,245줄** (팀 전체 233커밋 중) · **PR 14건** · 마이그레이션 4개(V6·V23·V24·V26) · 테스트 클래스 15개

사용자가 쓰는 **service**와 AI가 도는 **agent** 사이의 연동 전부를 맡았습니다.

🔗 **배포** — [hktoss.elixirevo.com](https://hktoss.elixirevo.com/) (`/api` 가 이 레포) · [전체 아키텍처 다이어그램](https://github.com/flathfk/bambi#전체-시스템-아키텍처)

---

# 트러블슈팅

## 2-1. 성공(200)으로 위장된 실패

> **한 줄** — 실패를 성공으로 기록하고 있었습니다.

**증상** AI팀에서 "사용자가 관심사를 골랐는데 우리 쪽엔 비어 있다"는 제보. **우리 로그는 전부 200.**

**원인 — 두 겹**

|  | 무엇 | 왜 안 보였나 |
| --- | --- | --- |
| 첫 겹 | 관심사 **저장 API**와 AI **동기화 API**가 분리. 화면이 동기화를 따로 불러야 반영 | 그 호출을 빠뜨리면 AI엔 0개. 저장은 성공했으니 에러가 없음 |
| **둘째 겹 (진짜)** | 컨텍스트 **버전을 service와 AI가 각자 관리**. AI 쪽이 앞서면 `409 STALE`로 거절 | **우리 코드가 409를 "이미 최신"으로 해석해 삼켰다** |

**조치**

- 관심사가 바뀌면 **서버가 재전송**하도록 이벤트에 붙임 → 화면 호출과 무관해짐 (#46)
- AI가 거절할 때 **자기 버전을 함께 반환**하도록 계약 변경(AI팀 협의) → 그 값으로 정합 후 **1회 재전송** (#47·#48)

**판단** 첫 겹만 고쳤으면 증상은 그대로였습니다. **보내는 쪽을 자동화해도 받는 쪽이 거절하면 소용없습니다.**
이후 원칙 — **성공으로 처리할 실패와, 드러낼 실패를 코드에서 구분한다.**

📁 `AgentContextSyncService` · `AgentContextVersionAllocator` · `StaleContextVersionException` · `V6__agent_context_version.sql`

---

## 2-2. 아침 브리핑 주제 계약을 다시 설계

> **한 줄** — 잘못된 값을 넣으면 결과 전체가 틀어지는 자리였습니다.

**증상** 아침 브리핑 주제가 엉뚱했습니다. 어떤 계정은 `서울`, 어떤 계정은 개발 도구 이름(`DBeaver`).

**원인 — 이 필드의 성격을 잘못 봤습니다**

계약상 이 값은 **화면 라벨이 아니라 AI가 실제로 쓰는 검색 주제**입니다. 그런데 처음엔 `"오늘의 관심사 뉴스"` 같은 **고정 문구**를 넣었고, 그 다음엔 위키 태그의 **점수 상위**를 기계적으로 넣었습니다 — 폭염 기사 한 건 때문에 상위가 `서울`·`온열질환`으로 채워진 계정이 그대로 통과했습니다.

**즉 주제를 고르는 판단이 필요한 자리인데, 정렬 결과를 그대로 흘려보내고 있었습니다.**

**조치 — 책임을 옮기고, 계약을 설계했습니다 (#82)**

주제 선정 자체는 **AI 쪽 책임으로 넘겼습니다.** 제가 만든 건 그걸 안전하게 받아 쓰는 구조입니다.

| 내가 설계한 것 | 왜 |
| --- | --- |
| **폴백 2단** — 선정 결과가 비면 등록 관심사 최근 3개 | 폴백이 없으면 **아침 브리핑이 전면 중단.** 위키 없는 신규 사용자는 1단계가 항상 빔 |
| **호출 실패에 예외를 올리지 않음** | 스케줄러는 전체 사용자를 도는 배치. 예외를 던지면 **AI가 흔들리는 날 아침이 통째로 날아감** |
| **선정 근거를 로그에 남김** | 결과만 보면 왜 그 주제인지 알 수 없음. 신고가 들어왔을 때 **유일한 단서** |
| **타임아웃 분리 (3초 → 15초)** | 공통 3초로 끊으면 선정이 **항상 실패해 폴백만 탐.** 폴백은 잘 도는데 원래 기능이 죽는 상태였음 |
| **주제 상한 3개 · 날짜 고정** | 워커가 늦게 처리해 자정을 넘겨도 "예정 날짜" 기준으로 일관 |

**판단** **외부 시스템의 판단을 대신하려 하지 않고, 실패했을 때의 경로를 내가 책임진다**로 정리했습니다.
폴백 설계 기준은 "정확도"가 아니라 **"이게 없으면 무엇이 멈추는가"** 였습니다.

📁 `BriefingTopicService` · `MorningBriefingGenerationService` · `AgentWikiClient` · `BriefingTopicServiceTest`

---

## 2-3. 폴백이 도는데 결과가 0건

리허설에서 카드 5장 중 1장만 `taxonomy_topic_ids`가 채워졌습니다.
→ 수정이 agent 쪽에 있어 상세는 **[bambi-agent-api](https://github.com/flathfk/bambi-agent-api#2-3-폴백이-도는데-결과가-0건이던-문제)** 에 있습니다.

---

## 2-4. 계약을 코드로 검증해 불일치 2건

> **한 줄** — 문서를 믿지 않고 대조했더니 값과 실제가 달랐습니다.

**상황** 카드 본문이 두 가지 형식으로 늘어났습니다. 프론트가 이를 구분해야 하는데, **본문 문자열을 파싱해 추측하게 하면 안 됩니다** — 문구가 조금 바뀌면 조용히 깨집니다.

그래서 형식을 알려주는 **boolean 하나를 계약에 넣고**, 저장·응답까지 배선했습니다 (#92, V26).

```
발행 payload → reports.change_history_enabled (V26) → ReportResponse
```

**검증 결과 — 계약과 구현이 어긋난 곳 2건**

| # | 문제 | 백엔드 영향 |
| --- | --- | --- |
| 1 | 값이 **요청 값을 그대로 되돌려주고 있었다** | 차단 스위치나 실패로 옛 형식이 나가도 값은 `true`. **값과 본문이 불일치** |
| 2 | 다주제 카드의 **구조가 결과에 따라 달라졌다** | 프론트가 **요청 시점에 구조를 예측할 수 없음** |

둘 다 지적해 수정됐습니다.

**내 쪽 설계 — 이 필드만 정책이 반대입니다**

| 필드 | 값이 안 올 때 | 왜 |
| --- | --- | --- |
| `report_type` | **기존 값 유지** | 구버전 재발행이 저장된 유형을 지우면 안 됨 |
| `change_history_enabled` | **항상 덮어씀** | 함께 온 본문을 설명하는 값. 짝이 어긋나면 화면이 깨짐 |

같은 파일에서 정책이 갈리므로 **왜 다른지 주석에 남기고 테스트로 고정**했습니다.

**추가로 — 값을 두 개로 분리**

| 값 | 의미 |
| --- | --- |
| `users.change_history_enabled` (V22) | **앞으로** 생성할 때 쓸지 (계정 설정) |
| `reports.change_history_enabled` (V26) | **이미 저장된 이 본문이** 무엇인지 |

계정 설정으로 화면을 그리면 **사용자가 설정을 끄는 순간 과거 보고서가 전부 깨집니다.**

**⚠️ 내가 먼저 틀린 부분도 있었습니다** 처음 지적할 때 **제 로컬이 10커밋 뒤처져 있었습니다.** 이미 반영된 필드를 "없다"고 잘못 짚었습니다. 확인 비용은 `git fetch` 한 번이었습니다. 이후로는 지적 전에 원격을 먼저 맞춥니다.

📁 `PublishItem` · `PublishProcessingService` · `Report` · `V26__report_change_history.sql`

---

## 2-5. 마이그레이션 번호 충돌

> **한 줄** — git이 clean이라고 말해주는 종류의 사고입니다.

**사건** 제 마이그레이션 번호를 **V19 → V21 → V22로 두 번** 옮겼습니다. 1차는 다른 PR이 먼저 머지되며 선점, 2차는 **합의에 없던 PR**이 들어와 선점.

**왜 위험한가** 파일명이 다르니 **git은 충돌로 잡지 않고 mergeable도 clean**입니다. 그대로 머지되면 배포에서 Flyway가 같은 버전 2개를 거부해 **애플리케이션이 기동에 실패**합니다.

**판단**

- **"우리는 V21, 저쪽은 V22" 같은 합의로는 못 막습니다.** 합의 밖 PR이 언제든 번호를 가져갑니다.
- 기준을 **PR 생성 시점이 아니라 머지 시점**으로 바꿨습니다 — 머지 직전에 main과 **모든 오픈 PR**의 번호를 다시 대조.
- V26을 넣을 때 실제로 이 절차를 밟았고(원격 브랜치 전체 확인) 충돌 없이 들어갔습니다.
- 엔티티 타입·기본값도 함께 대조합니다. 컬럼 타입이 어긋나면 `ddl-auto=validate`가 기동을 막습니다.

---

# 설계 판단

## 실패를 어디서 드러낼지 정해뒀다

같은 호출 실패인데 처리가 다릅니다. **의도적으로** 다릅니다.

| 상황 | 처리 | 이유 |
| --- | --- | --- |
| 가입 시 컨텍스트 전달 | 삼킴 | AI가 죽어도 **가입은 성공해야** 함 |
| 저장 자료 중계 | 삼킴 | 중계 실패로 **사용자 저장을 되돌리지 않음** |
| 아침 주제 선정 | 폴백 | 예외를 올리면 **그날 아침이 통째로** 날아감 |
| 관리자 재동기화 | **실패 전파** | 관리자는 결과를 보려고 누름. 조용히 실패하면 버튼이 무의미 |
| 모르는 필터 값 | **거절** | 오타가 "전체"로 보이면 "실패가 없구나"로 오해 |

**기준** — 사용자 행동을 막지 않아야 하는 곳은 삼키고, **사람이 결과를 확인하려는 곳은 드러낸다.**

## 중복 저장을 세 겹으로 막았다

| 겹 | 수단 |
| --- | --- |
| 요청 | 멱등키 `{날짜}-{userId}-{종류}` → 같은 요청은 Job 1개 |
| 저장 | 유니크 인덱스 `(user_id, external_content_id)` |
| 갱신 | **수신 버전이 더 클 때만** 갱신, 같거나 작으면 skip |

요청은 접수(202)만 받고 끊고 결과는 나중에 따로 받기 때문에, **"외부 호출은 됐는데 DB 저장 전에 죽는" 구간이 아예 없습니다.** `ack` 전에 죽어서 같은 카드를 다시 받아도 안전합니다.

## 트랜잭션 경계를 커밋 뒤로 뺐다

후속 AI 호출을 `@TransactionalEventListener(AFTER_COMMIT)`으로 분리했습니다.

- AI 왕복 시간만큼 **DB 커넥션을 붙잡지 않습니다**
- AI 실패가 **가입 트랜잭션을 롤백시키지 않습니다**

## 발행 파이프라인이 절대 멈추지 않게

카드 발행은 파이프라인 끝단이라, 여기서 예외가 나면 **완성된 카드가 사용자에게 도달하지 못합니다.**

- 단계적으로 추가되는 필드는 **안 오면 null·빈 값으로 관용 처리**
- **항목별 독립 트랜잭션** — 카드 한 장이 실패해도 나머지는 저장
- 동시 워커가 유니크 제약에 걸리면 예외 대신 **"이미 발행됨"으로 멱등 처리**

## 경계를 만들어 둔 덕분에 로직이 살았다

주제 결정·폴백·요청 조립을 `MorningBriefingGenerationService` 한 곳에 모아뒀습니다. 08-11 밤 팀에서 생성 스케줄을 **Outbox 방식으로 전면 전환**했는데(제 작업 아님), 스케줄러가 통째로 교체되는 동안에도 **그 경계 안의 로직은 보존**되고 호출 위치만 옮겨졌습니다.

---

# 아쉬운 것

- **설계 근거를 코드 주석에만 뒀다** — `GenerationScheduler`의 멱등키 규칙·대상 선정 근거를 주석으로만 남겼는데, Outbox 전환에서 파일이 교체되며 전부 사라졌습니다(로직은 보존). **근거가 코드에만 있으면 그 코드가 재작성될 때 함께 사라집니다.**
- **아무도 읽지 않는 데이터를 계속 저장 중** — `briefing-topics` API가 살아 있고 데이터도 쌓이는데 생성 경로가 그 값을 읽지 않습니다. 계약 문서에 남긴다고 적어뒀지만 **API 표면에 아무 신호가 없었던 게** 문제. 뒤늦게 컨트롤러 주석으로 명시(#104).
- **알고 감수한 트레이드오프** — 대시보드가 매번 AI 로그 전량을 훑습니다(집계 쿼리·캐시 없음). 규칙 이중 구현을 피한 대가이고, 적재량이 늘면 먼저 손봐야 할 곳입니다.
- **아직 못 닫은 것** — AI가 아예 응답하지 않으면 생성 상태가 `PUBLISHING`에 영구히 남을 수 있습니다. 지금은 관리자 화면으로 확인해 손으로 복구하고, 시간 기반 자동 정리는 없습니다.

---
---

# Bambi Service API

밤새비서 **Service Layer** (Spring Boot). source of truth · 인증/인가 · Agent Gateway.

## 실행

### 전체 스택 (권장) — build 레포에서
```bash
cd ../bambi-build
cp .env.example .env      # 필요 시 값 수정 (JWT_SECRET, ADMIN_* 등)
docker compose up --build # nginx + backend + postgres
```
- 확인: `curl http://localhost/api/health` → `{"status":"UP"}`
- postgres 는 Flyway(V1/V2)로 자동 마이그레이션된다. (`ddl-auto=validate`)

### 단독 실행 (로컬 개발)
postgres 가 `localhost:5432/bambi` 에 떠 있어야 한다(계정 `bambi`/`bambi`).
```bash
./gradlew bootRun        # ⚠️ gradlew wrapper 커밋 후 사용 가능 (아래 참고)
```

> **CI/빌드 주의:** 아직 `gradlew` wrapper 가 없어서 `./gradlew build` (CI 포함)가 실패한다.
> gradle 설치 환경에서 `gradle wrapper --gradle-version 8.10` 실행 후 wrapper 파일을 커밋해야 CI 가 green.
> Docker 빌드는 `gradle:8.10-jdk21` 이미지를 쓰므로 wrapper 없이도 동작한다.

## 공통 규약

- 응답 포맷: `{ success, data, error }` — 성공 `error:null`, 실패 `data:null` + `error:{code,message}`
- 에러 코드: HTTP status + 내부 code (`VALIDATION_ERROR`/`AUTH_INVALID_TOKEN`/`FORBIDDEN`/`NOT_FOUND`/`DUPLICATE_RESOURCE`/`INTERNAL_ERROR`)
- 인증: JWT access token (`Authorization: Bearer <token>`). P0 는 프론트 localStorage 저장 전제.

## API (P0 세로 슬라이스)

| Method | Path | 인증 | 설명 |
|---|---|---|---|
| GET | `/api/health` | 공개 | 헬스체크 `{status:UP}` |
| GET | `/api/version` | 공개 | 버전/color |
| POST | `/api/auth/signup` | 공개 | 회원가입(USER) |
| POST | `/api/auth/login` | 공개 | 로그인 → 토큰 |
| GET | `/api/auth/me` | 필요 | 현재 사용자 |
| GET·POST·PUT·DELETE | `/api/notes[/{id}]` | 필요 | reference CRUD 템플릿 |
| `/api/admin/**` | | ADMIN | 관리자 전용(예약) |

### 예시

```bash
# 회원가입
curl -XPOST http://localhost/api/auth/signup -H 'Content-Type: application/json' \
  -d '{"email":"a@b.com","password":"password1","displayName":"철수"}'
# → 201 { "success":true, "data":{ "id":1, "email":"a@b.com", "roles":["USER"] }, "error":null }

# 로그인
curl -XPOST http://localhost/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"a@b.com","password":"password1"}'
# → { "success":true, "data":{ "accessToken":"eyJ...", "tokenType":"Bearer", "user":{...} } }

# 인증 필요 요청
curl http://localhost/api/notes -H 'Authorization: Bearer eyJ...'
```

## 코드 지도 (팀원 복붙 시작점)

```
common/response  ApiResponse, ErrorResponse         # 공통 응답 포맷
common/error     ErrorCode, ApiException, GlobalExceptionHandler
config           SecurityConfig, AdminSeeder        # 인증 설정 + 관리자 seed
auth             AuthController/Service, JwtTokenProvider, JwtAuthenticationFilter, AuthPrincipal
user             User, Role, *Repository            # 엔티티(V1 스키마 매핑)
note             Note CRUD 1세트  ← 도메인 만들 때 이 구조를 복붙
```

> **도메인 개발자(영현) 안내:** `note/` 패키지가 Controller→Service→Repository→DTO + 공통응답/예외 + 소유자 권한(userId) + soft delete 를 갖춘 최소 템플릿이다. Bookmark/Card 등은 이 구조를 복사해 시작할 것.
