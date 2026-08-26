> **동기화 사본.** 정본은 `gilbert-sentbiz/ARC_Onboarding` main `docs/SERVER-STANDARD.md`. 규범 변경은 정본을 고치고 이 파일에 반영한다.

# ARC 온보딩 플랫폼 — 서버

SentBe(센트비)의 B2B 고객 온보딩 플랫폼 서버. 고객이 설문에 답하고 서류를 올리면, 내부 3역할(영업 → 운영 → 컴플라이언스 → 운영)이 순서대로 심사해서 계정을 개설한다. MVP는 **송금 전용** — 한국 법인(CORP)과 한국 개인사업자(INDIV)만 온보딩한다.

## 스택, 로컬 환경

**회사 백엔드 표준(BizPlatform / Tech-SentBiz-Backend)을 그대로 따른다.** 아래 "회사 백엔드 표준" 섹션이 아키텍처·스택·컨벤션의 상위 규범이고 어긋나면 안 된다.

- **backend: Kotlin 2.3.20 + Spring Boot 4.1.0**(Spring MVC, 동기 + 코루틴 병행), **JDK 25**, Gradle 9.2.1(Kotlin DSL, wrapper). 영속성은 **Spring Data JDBC (JPA 아님)**, 마이그레이션은 **Liquibase**.
- **frontend: React** — 기존 `prototype-next`(Next.js) 재사용, localStorage → API 전환. 접점 `prototype-next/services/`. **프론트 전용 회사 표준 문서는 없음** → 백엔드의 API 계약·4프로필(local/dev/stg/prd)·인증(OTP/SSO)에 맞춘다.
- **db: PostgreSQL** · **cache: Redis**(OTP 코드 등 단기 TTL) · **storage: AWS S3(SDK v2)** — 로컬은 MinIO(S3 호환).
- 로컬은 **도커 compose**로 db/redis/storage/backend/frontend 기동 → 검증 후 회사 환경 이관. 규격 `LOCAL_DEV.md`. 로컬↔회사 차이는 **환경변수로만** 흡수(엔드포인트 하드코딩 금지).

> ⚠️ 현재 `server/`의 코드(arc-dev 참조 구현, PI-128~132)는 이 표준 확정 이전에 작성돼 **비준수**다(Spring Boot 3.4 / JPA / Flyway / 평면 레이어 / Kotlin 1.9 / JDK 21). 동작하는 **참조 구현**으로만 쓰고, 실제 백엔드는 아래 표준 위에서 재구성(re-scaffold)한다 — PI-133.

## 회사 백엔드 표준 (상위 규범, BINDING)

출처: [백엔드 테크 스펙 (BizPlatform 기준)](https://sentbe-product.atlassian.net/wiki/spaces/S2/pages/4173660292). 아키텍처·컨벤션·버전은 이 문서가 최종이다. ARC는 *무엇을 만드나*(도메인)를 제공하고, *어떻게 만드나*는 전부 여기 따른다.

### 아키텍처 — 헥사고날 (Ports & Adapters)

ARC는 **bizplatform 안의 모듈로 편입**한다. 패키지 루트 `com.sentbe.bizplatform.arc.{도메인}` (모듈명 `arc`는 **잠정** — 백엔드팀 컨벤션 확정 시 리네임, 패키지 경로 찾아바꾸기라 기계적 작업). 도메인마다 동일 계층:

```
com.sentbe.bizplatform.arc.{도메인}/
├── adapter/
│   ├── in/          # REST 컨트롤러, 요청·응답 DTO
│   └── out/         # JDBC 리포지토리, S3·외부 API 클라이언트
└── application/
    ├── domain/      # 도메인 모델
    ├── port/in/     # 유스케이스 인터페이스 (컨트롤러가 호출)
    ├── port/out/    # 외부 의존 인터페이스 (서비스가 호출)
    ├── service/     # 비즈니스 로직
    ├── event/       # 도메인 이벤트
    └── exception/   # 도메인 예외
```

비즈니스 로직(`application`)은 `port` 인터페이스에만 의존하고, DB·S3 등 구체 기술은 `adapter`에 격리한다.

### ARC 도메인 분할

| 도메인 | 테이블 | 핵심 유스케이스 |
| --- | --- | --- |
| `case` | onboarding_case, case_event | 케이스 생성, 4단계 상태 전이, 타임라인 |
| `intake` | intake_response | 1·2차 응답 저장·제출 |
| `document` | document, document_file, revision_request | 서류 생성·업로드·보완 루프·승인 |
| `rule` | segment, question, doc_template | 룰 시드 조회, 분류·질문·서류 결정 |
| `customer` | customer | 고객 계정, 이메일 OTP 인증 |
| `staff` | staff | 내부 계정, 역할 인가(SSO) |
| `global` | — | 공통(예외, 설정, 이벤트 인프라) |

케이스 생성/1·2차 제출은 `case` 도메인 서비스가 `rule`·`document`·`intake`의 `port/out`을 호출하는 오케스트레이션이다(도메인 간 직접 DB 접근 금지).

### 영속성 — Spring Data JDBC (JPA 금지)

- `@Entity`·지연 로딩 등 JPA 스타일 금지. 애그리거트 루트 단위 리포지토리.
- `jsonb`(pinned_question_ids, answers, segment_meta, payload, options 등)와 `text[]`(services, sectors)는 **커스텀 컨버터**로 매핑(Jackson jsonb, Postgres array).
- 스키마는 `schema.sql` 내용을 **Liquibase changelog**로 옮긴다(포맷만 변경, 내용 동일). `question` 불변 트리거·partial unique index 등 DB 레벨 제약은 ORM 무관하게 그대로 유지.

### 컨벤션·품질 게이트

- **ktlint 1.8.0 강제** — 위반 시 컴파일 실패. 커밋 전 `./gradlew ktlintFormat`.
- 컴파일러 null 안정성 엄격(`-Xjsr305=strict`).
- 직렬화 kotlinx-serialization + Jackson 병행, 로깅 Log4j2(Logback 제외).
- API 문서 Springdoc OpenAPI(Swagger UI) + Spring REST Docs.

### 테스트

- **Kotest 6.x(BDD 스타일)** + JUnit 5. DB 테스트는 **Testcontainers(PostgreSQL)**, 스키마는 Liquibase. mockito-kotlin, 외부 HTTP는 MockWebServer.

### 인프라·빌드

- Docker 멀티스테이지(temurin 25, alpine, 비루트 실행), 사내 Nexus 저장소 — 빌드에 `NEXUS_USERNAME`/`NEXUS_PASSWORD` 필요(없으면 빌드 실패).
- AWS SDK v2: Secrets Manager, S3, STS/SSO. 환경 프로필 4개(local/dev/stg/prd).

### AI 코딩 주의 (회사 문서 6장)

- **Spring Boot 4.x·Kotlin 2.3·JDK 25는 최신 메이저** — AI가 3.x/구버전 API를 제안하기 쉬우니 버전을 항상 컨텍스트에 넣는다.
- JPA 스타일 코드가 나오면 동작 안 함(Spring Data JDBC임)을 프롬프트에 명시.
- 온보딩은 금액 계산이 없는 대신 **상태 전이·불변식**이 위험 지점 — AI 생성 코드라도 반드시 리뷰·테스트.

## 스펙 원천 (충돌 시 이 순서)

> **아키텍처·스택·컨벤션은 위 "회사 백엔드 표준"이 최우선.** 아래는 *무엇을 만드나*(도메인)의 원천이다.

1. [테이블 정의서](https://sentbe-product.atlassian.net/wiki/spaces/NSBS/pages/4158980234) — 스키마 (`schema.sql`의 원본)
2. [PRD](https://sentbe-product.atlassian.net/wiki/spaces/NSBS/pages/4134994324) — 스콥, 화면, 워크플로우 (섹션별 MVP vs Full 표 — MVP 열만 구현)
3. [ERD](https://sentbe-product.atlassian.net/wiki/spaces/NSBS/pages/4148920321) — 설계 원칙과 이유
4. 프로토타입 `prototype-next/` — 화면·플로우의 살아있는 스펙. 서버 로직의 참조 구현은 `prototype-next/services/`

스펙 변경은 문서 → 코드 순서. 코드에서 먼저 바꾸지 않는다.

## 절대 어기면 안 되는 불변식

1. **question 행은 불변.** UPDATE는 `deactivated_at` 세팅만 허용 (schema.sql에 트리거로 강제됨). 질문 수정 = 기존 행 비활성 + 새 행 insert (`replaces_question_id`로 계보 연결).
2. **케이스는 룰을 "고정"해서 참조한다.** 케이스 생성 시 1차 질문 id 목록, 1차 제출 시 2차 질문 id 목록을 `onboarding_case.pinned_question_ids`에 저장. 진행 중 케이스의 화면은 항상 이 목록으로 렌더 — 룰 변경이 기존 케이스에 소급되면 버그다.
3. **서류 목록은 판정 시점에 doc_template에서 document 행으로 복사**된다 (합집합 + type dedup). 이후 템플릿 변경은 기존 케이스에 영향 없어야 한다.
4. **세그먼트 분류는 1차 제출 시 1회 평가 후 결과 저장** (`entity_code`, `services`). 재평가 없음.
5. **case_event는 append-only.** 수정, 삭제 금지. 타임라인 화면 = 이 테이블 하나.
6. **1계정 1활성 케이스** — partial unique index로 강제됨. COMPLETED/CLOSED 이후에만 새 케이스 가능.
7. **룰 테이블(segment, question, doc_template) 삭제는 소프트 삭제**(`deactivated_at`) — 과거 케이스가 참조하므로 행은 보존.

## 케이스 상태와 전이

상태 (PRD 3.1 — 액션 기준 명명, 담당 역할은 별도 매핑):

| 코드 | 내부 라벨 | 담당 |
| --- | --- | --- |
| `INQUIRY_RECEIVED` | 케이스 생성 (1차, 2차 정보 입력 중) | 고객 |
| `DOCUMENT_SUBMISSION_REQUIRED` | 서류 제출 대기 | 고객 |
| `INITIAL_SCREENING` | 1차 스크리닝 | 영업 (SALES) |
| `DOCUMENT_SCREENING_REQUIRED` | 서류 스크리닝 | 운영 (OPS) |
| `APPROVAL_REVIEW_REQUIRED` | 심사, 승인 | 컴플라이언스 (COMPLIANCE) |
| `ACCOUNT_SETUP_REQUIRED` | 계정 개설 | 운영 (OPS) |
| `REVISION_REQUESTED` | 보완 요청 (고객 대기) | 고객 |
| `COMPLETED` | 완료 | — |
| `CLOSED` | 종료 (`close_reason`: DROPPED=내부 중단 / EXITED=고객 이탈) | — |

정상 흐름:

| from | to | 주체 | 트리거 |
| --- | --- | --- | --- |
| INQUIRY_RECEIVED | DOCUMENT_SUBMISSION_REQUIRED | 고객 | 2차 설문 제출 (서류 목록 생성) |
| DOCUMENT_SUBMISSION_REQUIRED | INITIAL_SCREENING | 고객 | 필수 서류 전부 업로드 후 제출 |
| INITIAL_SCREENING | DOCUMENT_SCREENING_REQUIRED | 영업 | 1차 스크리닝 통과 |
| DOCUMENT_SCREENING_REQUIRED | APPROVAL_REVIEW_REQUIRED | 운영 | 서류 스크리닝 통과 |
| APPROVAL_REVIEW_REQUIRED | ACCOUNT_SETUP_REQUIRED | 컴플라이언스 | 심사 승인 (서류 전건 APPROVED) |
| ACCOUNT_SETUP_REQUIRED | COMPLETED | 운영 | 계정 개설 완료 |

보완 루프 (검토 3단계 공통):

- INITIAL_SCREENING / DOCUMENT_SCREENING_REQUIRED / APPROVAL_REVIEW_REQUIRED → **REVISION_REQUESTED** (보완요청 — 서류별 사유 입력 필수, `revision_requested_from`에 요청 단계 기록)
- REVISION_REQUESTED → **`revision_requested_from`에 기록된 단계로 복귀** (고객 재제출 시)
- 미해결 revision_request 행은 항상 같은 단계 출신이다 (케이스는 한 시점에 한 단계) — `revision_requested_from`은 파생 캐시, 원천은 revision_request 테이블

종료: 위 모든 상태에서 내부가 사유 입력 후 CLOSED(DROPPED) 가능. 자동 이탈(EXITED 배치)은 Full Spec — MVP는 수동만.

서류 상태(문서당): `NOT_REQUESTED → REQUESTED → SUBMITTED → APPROVED 또는 REVISION_REQUIRED → SUBMITTED …` 승인은 컴플라이언스만, 개별 승인만(일괄 없음).

구현 참조: `prototype-next/services/stateMachine.ts` (구현체와 위 표가 다르면 PRD 3.1이 맞음 — PM에게 확인).

## API 후보 목록

프로토타입 서비스 레이어(`prototype-next/services/`)의 함수가 곧 필요한 엔드포인트다. 초안:

| 프로토타입 함수 | 제안 REST | 권한 |
| --- | --- | --- |
| createCase | POST /cases (1차 응답 포함) | 고객 |
| getIntakeResponse | GET /cases/{id}/intake/{phase} | 고객, 내부 |
| confirmSecondIntake | POST /cases/{id}/intake/second (제출 → 분류 확정 + 서류 생성) | 고객 |
| getDocuments | GET /cases/{id}/documents | 고객, 내부 |
| uploadFile | POST /documents/{id}/files | 고객 |
| approveDocument | POST /documents/{id}/approve | 컴플라이언스 |
| requestRevision | POST /documents/{id}/revision-requests (사유 필수) | 영업, 운영, 컴플라이언스 |
| resubmitRevision | POST /cases/{id}/resubmit (→ 요청 단계로 복귀) | 고객 |
| transitionStatus | POST /cases/{id}/transitions (가드 = 위 전이표) | 역할별 |
| changeOwner | PATCH /cases/{id}/assignee | 내부 |
| (caseEventStore) | GET /cases/{id}/events (타임라인) | 고객(일부), 내부 |
| (ruleStore) | GET /rules/active (1차 질문, 세그먼트, 핀 대상 조회용) | 고객, 내부 |

인증: 고객 = **email + 이메일 OTP**(2026-08-07 확정 — 비밀번호 없음, `password_hash`는 항상 null. OTP 코드 저장은 별도 단기 저장소로 개발팀 선택), 내부 = 구글 SSO(백오피스 계정) + staff 테이블 role 인가. 내부 API는 VDI/백오피스 망, 고객 API는 인터넷망 — 망 분리는 API 계층에서, DB는 공유.

파일 업로드(2026-08-07 확정): 허용 형식 pdf, png, jpg / 상한 10MB / **MVP는 서류당 1파일, Full은 멀티업로드** / 바이러스 스캔은 Full. 형식, 용량 검증은 API에서.

데이터 파기(2026-08-07 확정): **MVP는 수동 파기만**. **Full은 케이스 종료 1개월 후 파기 배치** — intake_response·document·document_file·revision_request 삭제, customer(company_name, contact_name)·onboarding_case(entity_code, services, status)만 잔존. company_name/contact_name을 customer에 복사해두는 이유가 이 잔존 때문. ⚠️ 1개월 기준·담당자명 보관 근거는 컴플라이언스 사인오프 대상.

## MVP에서 만들지 않는 것

수금(Collection)과 FI 세그먼트 전체, 룰 관리 화면(룰은 시드로만 — 시드 변경은 마이그레이션), 댓글, 알림, 임시저장(draft), 일괄 승인, 즉석 서류 추가(ad-hoc), 사업자번호 자동 중복 판단, 자동 이탈 배치, 계정/권한 관리 화면. 전부 Full Spec — 스키마는 추가/완화만으로 확장되도록 이미 설계돼 있으니 미리 만들지 말 것.

## 데이터 주의

실고객 데이터, 운영 크리덴셜을 AI 도구 입력에 넣지 않는다. 시드와 테스트 데이터는 전부 가짜(schema.sql 하단 예시). 개인정보 컬럼(사업자번호, 연락처, BO 정보)은 로그에 남기지 않는다.

---

# English Version

> **Synced copy.** The source of truth is `gilbert-sentbiz/ARC_Onboarding` main `docs/SERVER-STANDARD.md`. Change the standard in the source of truth, then reflect it here.

# ARC Onboarding Platform — Server

The B2B customer onboarding platform server for SentBe. A customer answers a survey and uploads documents; then three internal roles (Sales → Ops → Compliance → Ops) review it in sequence to open an account. The MVP is **remittance-only** — it onboards only Korean corporations (CORP) and Korean sole proprietors (INDIV).

## Stack, Local Environment

**We follow the company backend standard (BizPlatform / Tech-SentBiz-Backend) exactly.** The "Company Backend Standard" section below is the higher-level standard for architecture, stack, and conventions, and must not be violated.

- **backend: Kotlin 2.3.20 + Spring Boot 4.1.0** (Spring MVC, synchronous + coroutines in parallel), **JDK 25**, Gradle 9.2.1 (Kotlin DSL, wrapper). Persistence is **Spring Data JDBC (not JPA)**, migrations are **Liquibase**.
- **frontend: React** — reuse the existing `prototype-next` (Next.js), switching localStorage → API. Touch point `prototype-next/services/`. **There is no company standard document dedicated to the frontend** → align it with the backend's API contract, the 4 profiles (local/dev/stg/prd), and authentication (OTP/SSO).
- **db: PostgreSQL** · **cache: Redis** (short TTL for OTP codes, etc.) · **storage: AWS S3 (SDK v2)** — locally, MinIO (S3-compatible).
- Locally, bring up db/redis/storage/backend/frontend with **Docker compose** → after verification, migrate to the company environment. Spec `LOCAL_DEV.md`. Absorb local↔company differences **only through environment variables** (no hardcoding endpoints).

> ⚠️ The current code in `server/` (the arc-dev reference implementation, PI-128~132) was written before this standard was finalized and is therefore **non-compliant** (Spring Boot 3.4 / JPA / Flyway / flat layers / Kotlin 1.9 / JDK 21). Use it only as a working **reference implementation**; the actual backend is re-scaffolded on top of the standard below — PI-133.

## Company Backend Standard (higher-level standard, BINDING)

Source: [Backend Tech Spec (based on BizPlatform)](https://sentbe-product.atlassian.net/wiki/spaces/S2/pages/4173660292). This document is final for architecture, conventions, and versions. ARC provides *what we build* (the domain); *how we build it* all follows here.

### Architecture — Hexagonal (Ports & Adapters)

ARC is **incorporated as a module inside bizplatform**. Package root `com.sentbe.bizplatform.arc.{domain}` (the module name `arc` is **tentative** — rename it once the backend team's convention is finalized; it is mechanical work, a find-and-replace on the package path). Every domain has the same layers:

```
com.sentbe.bizplatform.arc.{도메인}/
├── adapter/
│   ├── in/          # REST 컨트롤러, 요청·응답 DTO
│   └── out/         # JDBC 리포지토리, S3·외부 API 클라이언트
└── application/
    ├── domain/      # 도메인 모델
    ├── port/in/     # 유스케이스 인터페이스 (컨트롤러가 호출)
    ├── port/out/    # 외부 의존 인터페이스 (서비스가 호출)
    ├── service/     # 비즈니스 로직
    ├── event/       # 도메인 이벤트
    └── exception/   # 도메인 예외
```

The business logic (`application`) depends only on `port` interfaces, and concrete technologies such as DB and S3 are isolated in `adapter`.

### ARC Domain Split

| Domain | Tables | Core use cases |
| --- | --- | --- |
| `case` | onboarding_case, case_event | Case creation, 4-stage state transition, timeline |
| `intake` | intake_response | Save/submit the 1st & 2nd responses |
| `document` | document, document_file, revision_request | Document creation, upload, revision loop, approval |
| `rule` | segment, question, doc_template | Rule seed lookup, classification/question/document decisions |
| `customer` | customer | Customer account, email OTP authentication |
| `staff` | staff | Internal account, role authorization (SSO) |
| `global` | — | Common concerns (exceptions, config, event infrastructure) |

Case creation and the 1st/2nd submissions are orchestrations in which the `case` domain service calls the `port/out` of `rule`, `document`, and `intake` (direct DB access across domains is forbidden).

### Persistence — Spring Data JDBC (JPA forbidden)

- JPA-style constructs such as `@Entity` and lazy loading are forbidden. Repositories are per aggregate root.
- `jsonb` (pinned_question_ids, answers, segment_meta, payload, options, etc.) and `text[]` (services, sectors) are mapped with **custom converters** (Jackson jsonb, Postgres array).
- The schema is ported from the contents of `schema.sql` into a **Liquibase changelog** (only the format changes, the contents are identical). DB-level constraints such as the `question` immutability trigger and partial unique indexes are kept as-is, independent of the ORM.

### Conventions & Quality Gates

- **ktlint 1.8.0 enforced** — a violation fails compilation. Before committing, `./gradlew ktlintFormat`.
- Strict compiler null safety (`-Xjsr305=strict`).
- Serialization uses kotlinx-serialization + Jackson in parallel; logging uses Log4j2 (Logback excluded).
- API docs use Springdoc OpenAPI (Swagger UI) + Spring REST Docs.

### Testing

- **Kotest 6.x (BDD style)** + JUnit 5. DB tests use **Testcontainers (PostgreSQL)**, with the schema from Liquibase. mockito-kotlin; external HTTP uses MockWebServer.

### Infrastructure & Build

- Docker multi-stage (temurin 25, alpine, non-root execution), internal Nexus repository — the build needs `NEXUS_USERNAME`/`NEXUS_PASSWORD` (without them, the build fails).
- AWS SDK v2: Secrets Manager, S3, STS/SSO. 4 environment profiles (local/dev/stg/prd).

### AI Coding Cautions (company doc, chapter 6)

- **Spring Boot 4.x, Kotlin 2.3, and JDK 25 are the latest majors** — AI easily suggests 3.x/older-version APIs, so always put the versions in the context.
- If JPA-style code appears, state in the prompt that it will not work (this is Spring Data JDBC).
- Onboarding has no monetary calculations; instead, **state transitions and invariants** are the risk points — review and test even AI-generated code without exception.

## Spec Sources (in this order when they conflict)

> **For architecture, stack, and conventions, the "Company Backend Standard" above takes top priority.** Below are the sources for *what we build* (the domain).

1. [Table Definition](https://sentbe-product.atlassian.net/wiki/spaces/NSBS/pages/4158980234) — schema (the original of `schema.sql`)
2. [PRD](https://sentbe-product.atlassian.net/wiki/spaces/NSBS/pages/4134994324) — scope, screens, workflow (per-section MVP vs Full tables — implement only the MVP column)
3. [ERD](https://sentbe-product.atlassian.net/wiki/spaces/NSBS/pages/4148920321) — design principles and rationale
4. Prototype `prototype-next/` — the living spec for screens and flows. The reference implementation of the server logic is `prototype-next/services/`

Spec changes go document → code. Do not change them in the code first.

## Invariants That Must Never Be Broken

1. **A question row is immutable.** UPDATE is allowed only to set `deactivated_at` (enforced by a trigger in schema.sql). Editing a question = deactivate the existing row + insert a new row (link the lineage via `replaces_question_id`).
2. **A case references rules by "pinning" them.** On case creation, save the list of 1st-phase question ids; on 1st submission, save the list of 2nd-phase question ids in `onboarding_case.pinned_question_ids`. The screens of an in-progress case always render from this list — if a rule change retroactively affects an existing case, that is a bug.
3. **The document list is copied from doc_template into document rows at the moment of judgment** (union + dedup by type). After that, template changes must not affect existing cases.
4. **Segment classification is evaluated once at 1st submission, then the result is stored** (`entity_code`, `services`). No re-evaluation.
5. **case_event is append-only.** No updates, no deletes. The timeline screen = this single table.
6. **One active case per account** — enforced by a partial unique index. A new case is possible only after COMPLETED/CLOSED.
7. **Deletion of rule tables (segment, question, doc_template) is a soft delete** (`deactivated_at`) — past cases reference them, so the rows are preserved.

## Case Status and Transitions

Statuses (PRD 3.1 — named by action; the responsible role is mapped separately):

| Code | Internal label | Owner |
| --- | --- | --- |
| `INQUIRY_RECEIVED` | Case created (entering 1st & 2nd info) | Customer |
| `DOCUMENT_SUBMISSION_REQUIRED` | Awaiting document submission | Customer |
| `INITIAL_SCREENING` | Initial screening | Sales (SALES) |
| `DOCUMENT_SCREENING_REQUIRED` | Document screening | Ops (OPS) |
| `APPROVAL_REVIEW_REQUIRED` | Review, approval | Compliance (COMPLIANCE) |
| `ACCOUNT_SETUP_REQUIRED` | Account setup | Ops (OPS) |
| `REVISION_REQUESTED` | Revision requested (awaiting customer) | Customer |
| `COMPLETED` | Completed | — |
| `CLOSED` | Closed (`close_reason`: DROPPED=internal stop / EXITED=customer drop-off) | — |

Normal flow:

| from | to | Actor | Trigger |
| --- | --- | --- | --- |
| INQUIRY_RECEIVED | DOCUMENT_SUBMISSION_REQUIRED | Customer | 2nd survey submitted (document list generated) |
| DOCUMENT_SUBMISSION_REQUIRED | INITIAL_SCREENING | Customer | Submitted after uploading all required documents |
| INITIAL_SCREENING | DOCUMENT_SCREENING_REQUIRED | Sales | Passed initial screening |
| DOCUMENT_SCREENING_REQUIRED | APPROVAL_REVIEW_REQUIRED | Ops | Passed document screening |
| APPROVAL_REVIEW_REQUIRED | ACCOUNT_SETUP_REQUIRED | Compliance | Review approved (all documents APPROVED) |
| ACCOUNT_SETUP_REQUIRED | COMPLETED | Ops | Account setup completed |

Revision loop (common to all 3 review stages):

- INITIAL_SCREENING / DOCUMENT_SCREENING_REQUIRED / APPROVAL_REVIEW_REQUIRED → **REVISION_REQUESTED** (revision request — a per-document reason is required; record the requesting stage in `revision_requested_from`)
- REVISION_REQUESTED → **returns to the stage recorded in `revision_requested_from`** (on customer resubmission)
- An unresolved revision_request row always originates from the same stage (a case is at one stage at any moment) — `revision_requested_from` is a derived cache; the source of truth is the revision_request table

Closing: from all of the above statuses, an internal user can move to CLOSED(DROPPED) after entering a reason. Automatic drop-off (the EXITED batch) is Full Spec — the MVP is manual only.

Document status (per document): `NOT_REQUESTED → REQUESTED → SUBMITTED → APPROVED or REVISION_REQUIRED → SUBMITTED …`. Only Compliance approves, and only individually (no bulk).

Implementation reference: `prototype-next/services/stateMachine.ts` (if the implementation differs from the table above, PRD 3.1 is correct — confirm with the PM).

## API Candidate List

The functions in the prototype service layer (`prototype-next/services/`) are exactly the endpoints we need. Draft:

| Prototype function | Proposed REST | Permission |
| --- | --- | --- |
| createCase | POST /cases (includes the 1st response) | Customer |
| getIntakeResponse | GET /cases/{id}/intake/{phase} | Customer, internal |
| confirmSecondIntake | POST /cases/{id}/intake/second (submit → confirm classification + generate documents) | Customer |
| getDocuments | GET /cases/{id}/documents | Customer, internal |
| uploadFile | POST /documents/{id}/files | Customer |
| approveDocument | POST /documents/{id}/approve | Compliance |
| requestRevision | POST /documents/{id}/revision-requests (reason required) | Sales, Ops, Compliance |
| resubmitRevision | POST /cases/{id}/resubmit (→ return to the requesting stage) | Customer |
| transitionStatus | POST /cases/{id}/transitions (guard = the transition table above) | Per role |
| changeOwner | PATCH /cases/{id}/assignee | Internal |
| (caseEventStore) | GET /cases/{id}/events (timeline) | Customer (partial), internal |
| (ruleStore) | GET /rules/active (for looking up 1st-phase questions, segments, pin targets) | Customer, internal |

Authentication: customer = **email + email OTP** (confirmed 2026-08-07 — no password; `password_hash` is always null. Storage of OTP codes is a separate short-lived store chosen by the dev team), internal = Google SSO (back-office account) + role authorization via the staff table's role. Internal APIs are on the VDI/back-office network, customer APIs on the internet; the network separation is at the API layer, while the DB is shared.

File upload (confirmed 2026-08-07): allowed formats pdf, png, jpg / cap 10MB / **the MVP is 1 file per document, Full is multi-upload** / virus scanning is Full. Format and size validation is done in the API.

Data destruction (confirmed 2026-08-07): **the MVP is manual destruction only**. **Full is a destruction batch 1 month after case closure** — delete intake_response, document, document_file, revision_request; keep only customer (company_name, contact_name) and onboarding_case (entity_code, services, status). The reason company_name/contact_name are copied into customer is exactly this retention. ⚠️ The 1-month criterion and the basis for retaining the contact name are subject to compliance sign-off.

## What the MVP Does Not Build

Collection, the entire FI segment, the rule management screen (rules are seed-only — seed changes are migrations), comments, notifications, drafts, bulk approval, ad-hoc document addition, automatic business-number duplicate detection, the automatic drop-off batch, and the account/permission management screen. All of these are Full Spec — the schema is already designed to extend by additions/relaxations only, so do not build them ahead of time.

## Data Cautions

Do not put real customer data or production credentials into AI tool inputs. All seed and test data are fake (the examples at the bottom of schema.sql). Do not leave personal-information columns (business number, contact, BO information) in logs.
