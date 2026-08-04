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

## Agent 사용자 컨텍스트 전달 보장

회원가입과 온보딩 관심 분류 선택 변경 시 이벤트를 발행한다. `BEFORE_COMMIT` 리스너가 같은
트랜잭션에 `service.agent_context_outbox` 행을 적재하고, `AFTER_COMMIT` 리스너가 Agent API로
즉시 전달을 시도한다. Agent 장애·응답 유실·프로세스 중단 시에는 스케줄러가 만료된 lease를
다시 claim해 같은 `context_version`과 payload를 재전송한다.

- 전달 의미: at-least-once
- 중복 안전성: Agent의 `context_version` 검증과 `STALE_CONTEXT_VERSION` 성공 처리
- 재시도: 5초부터 2배 지수 backoff, 기본 최대 1시간
- 다중 인스턴스: PostgreSQL `FOR UPDATE SKIP LOCKED` + claim token
- 설정 prefix: `app.agent.context-outbox` / 환경변수 `AGENT_CONTEXT_OUTBOX_*`

## API (P0 세로 슬라이스)

| Method | Path | 인증 | 설명 |
|---|---|---|---|
| GET | `/api/health` | 공개 | 헬스체크 `{status:UP}` |
| GET | `/api/version` | 공개 | 버전/color |
| POST | `/api/auth/signup` | 공개 | 회원가입(USER) |
| POST | `/api/auth/login` | 공개 | 로그인 → 토큰 |
| GET | `/api/auth/me` | 필요 | 현재 사용자 |
| GET·PUT | `/api/onboarding/interests` | 필요 | 온보딩 Category·Topic 안정 ID 선택 조회·전체 교체 |
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
