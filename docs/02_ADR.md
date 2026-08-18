# Jexon Architecture Decision Record

## 문서 목적
이 문서는 Jexon 프로젝트에서 내린 주요 설계 결정과 선택 이유를 기록한다.

각 결정은 개발 전에 초안을 작성하고, 실제 구현 과정에서 발생한 문제와 변경 사항을 추가한다.

---

# ADR-001. 단일 게임 공식 홈페이지로 구성

## 상태
결정 완료

## 배경
여러 게임을 등록하는 플랫폼 형태도 검토했지만, 게임 개발사, 게임별 권한, 라이브러리, 결제 등 프로젝트 범위가 지나치게 커질 수 있다.

## 결정
Jexon은 여러 게임을 제공하는 플랫폼이 아니라 하나의 게임을 소개하고 배포하는 공식 홈페이지로 구성한다.

## 결정 이유
- 게임 버전과 파일 관리에 집중할 수 있다.
- 다운로드 처리와 통계 기능을 깊이 있게 구현할 수 있다.
- 프로젝트 범위를 포트폴리오 개발 기간에 맞게 제한할 수 있다.

## 검토한 대안
여러 게임을 등록하는 게임 배포 플랫폼

## 구현 후 보완
실제 구현 과정에서 범위가 변경되면 작성한다.

---

# ADR-002. 다운로드를 비회원에게 허용

## 상태
결정 완료

## 배경
게임 공식 홈페이지에서 다운로드 전에 회원가입을 강제하면 사용자의 접근성이 낮아질 수 있다.

## 결정
게임 다운로드는 로그인 여부와 관계없이 허용한다.

커뮤니티 게시글과 댓글 작성은 로그인한 회원만 가능하도록 한다.

## 결정 이유
- 게임 배포 접근성을 높일 수 있다.
- 회원 기능과 다운로드 기능의 책임을 분리할 수 있다.
- 비회원 요청도 다운로드 이력으로 기록할 수 있다.

## 검토한 대안
회원만 게임 파일을 다운로드할 수 있도록 제한하는 방식

## 구현 후 보완
비회원, USER, ADMIN 모두 공개 최신 버전 조회와 다운로드 API를 사용할 수 있다.
DownloadHistory에는 Member를 연결하지 않으며 모든 정상 다운로드 요청을 같은 기준으로 기록한다.

---

# ADR-003. 실제 파일과 DB 메타데이터 분리

## 상태
결정 완료

## 배경
게임 파일은 크기가 클 수 있으며, 버전마다 새로운 파일이 등록된다.

파일 자체를 DB에 저장하면 DB 용량과 백업 부담이 증가하고 파일 관리 책임이 DB에 집중된다.

## 결정
실제 파일은 파일 저장소에 저장하고, DB에는 파일 메타데이터만 저장한다.

## DB 저장 정보
- `originalFileName`, 내부 `storageKey`
- `extension`, `contentType`, `fileSize`, SHA-256 `checksum`
- 공통 `createdAt`, `updatedAt`

## 결정 이유
- 대용량 파일과 일반 데이터를 분리할 수 있다.
- DB 부하를 줄일 수 있다.
- 향후 로컬 저장소에서 S3로 변경하기 쉽다.
- 실제 파일 경로를 외부에 노출하지 않을 수 있다.

## 검토한 대안
파일을 DB의 BLOB 컬럼에 저장하는 방식

## 구현 후 보완
`FileStorage` 추상화와 `LocalFileStorage` 구현을 사용한다. `storageKey`와 실제 경로는 API에 노출하지 않는다. 파일 저장 후 DB 저장이 실패하면 물리 파일을 보상 삭제한다.

---

# ADR-004. 서버 저장 파일명에 UUID 사용

## 상태
결정 완료

## 배경
원본 파일명을 그대로 저장하면 동일 이름 충돌, 특수문자 처리, 경로 노출 문제가 발생할 수 있다.

## 결정
원본 파일명은 DB에 보관하고, 실제 서버 저장 파일명은 UUID로 생성한다.

## 예시
원본 파일명:
`Jexon_Setup_1.0.0.zip`

서버 저장 파일명:
`550e8400-e29b-41d4-a716-446655440000.zip`

## 결정 이유
- 동일 파일명 충돌을 방지할 수 있다.
- 서버 내부 파일명을 추측하기 어렵다.
- 파일명 생성 규칙을 일관되게 유지할 수 있다.

## 구현 후 보완
최종 저장 디렉터리 구조를 작성한다.

---

# ADR-005. 게임 버전 번호 중복 금지

## 상태
결정 완료

## 배경
동일한 버전 번호가 여러 개 존재하면 파일과 통계를 명확하게 구분하기 어렵다.

## 결정
버전 번호에는 DB UNIQUE 제약조건을 적용한다.

애플리케이션에서도 등록 전에 중복 여부를 검사한다.

## 결정 이유
- 버전을 명확하게 식별할 수 있다.
- 버전별 다운로드 통계가 혼동되지 않는다.
- 애플리케이션 검증과 DB 제약조건을 함께 적용할 수 있다.

## 구현 후 보완
등록된 버전의 파일 교체 정책을 작성한다.

---

# ADR-006. 최신 배포 버전은 하나만 유지

## 상태
결정 완료

## 배경
여러 버전이 동시에 최신 상태가 되면 비회원 다운로드 대상이 불명확해진다.

## 결정
RELEASED 상태의 버전은 하나만 존재하도록 한다.

새로운 버전을 RELEASED 상태로 전환할 때 기존 RELEASED 버전은 INACTIVE 상태로 변경한다.

## 결정 이유
- 최신 다운로드 대상을 명확하게 유지할 수 있다.
- 사용자는 별도로 버전을 선택할 필요가 없다.
- 관리자가 배포 시점을 직접 통제할 수 있다.

## 검토한 대안
가장 최근에 생성된 버전을 자동으로 최신 버전으로 판단하는 방식

## 구현 후 보완
release 전체 변경은 하나의 트랜잭션에서 처리한다.

개별 GameVersion과 공통 GameVersionReleaseControl에 낙관적 락을 적용한다.
구체적인 동시성 제어 방식은 `ADR-019. GameVersion release 낙관적 락 및 단일 RELEASED 보장`에 기록한다.

---

# ADR-007. 버전 등록과 배포를 상태로 분리

## 상태
결정 완료

## 배경
관리자가 파일을 업로드했다고 해서 즉시 사용자에게 공개되는 것은 위험하다.

## 결정
게임 버전은 다음 상태를 가진다.

- DRAFT
- RELEASED
- INACTIVE

## 상태 의미

### DRAFT
관리자가 등록했지만 아직 사용자에게 배포되지 않은 상태

### RELEASED
현재 사용자에게 배포 중인 최신 버전

### INACTIVE
이전에 배포되었거나 현재 배포가 중단된 버전

## 결정 이유
- 파일 등록과 실제 배포를 분리할 수 있다.
- 관리자가 검토 후 버전을 공개할 수 있다.
- 이전 버전 정보를 삭제하지 않고 보관할 수 있다.

## 구현 후 보완
- 새 GameVersion은 DRAFT로 생성한다.
- DRAFT 또는 INACTIVE 상태만 RELEASED로 전환할 수 있다.
- 새 버전을 release하면 기존 RELEASED는 INACTIVE로 변경한다.
- RELEASED를 단독으로 INACTIVE 처리하는 API는 제공하지 않는다.
- 별도의 latest Boolean 없이 RELEASED 상태를 최신 버전의 유일한 기준으로 사용한다.

---

# ADR-008. 실제 파일 경로를 외부에 노출하지 않음

## 상태
결정 완료

## 배경
파일 저장 경로를 URL로 직접 제공하면 사용자가 서버 내부 구조를 알 수 있다.

직접 접근을 허용하면 버전 검증과 다운로드 이력 저장을 우회할 수 있다.

## 결정
모든 다운로드는 서버의 다운로드 API를 통해 처리한다.

## 처리 흐름
1. RELEASED 버전 조회
2. GameFile 메타데이터 조회
3. 실제 파일 존재 검사
4. FileStorage를 통한 파일 open
5. 다운로드 응답 정보 구성
6. 다운로드 이력 저장 시도
7. InputStreamResource 파일 스트리밍 응답

## 결정 이유
- 실제 저장 경로를 숨길 수 있다.
- 배포 중인 파일만 제공할 수 있다.
- 다운로드 요청을 일관되게 기록할 수 있다.

## 구현 후 보완
공개 다운로드 API는 `GET /api/game-versions/latest/download`다.
FileStorage의 `open(storageKey)`으로 InputStream을 열고 `InputStreamResource`로 응답한다.
응답은 `application/octet-stream`, 원본 파일명의 attachment Content-Disposition 및 GameFile.fileSize 기반 Content-Length를 사용한다.
storageKey와 실제 저장 경로는 외부에 노출하지 않는다.

---

# ADR-009. 다운로드 이력을 별도 테이블로 관리

## 상태
결정 완료

## 배경
게임 버전에 다운로드 숫자만 저장하면 버전별 총합은 확인할 수 있지만 날짜별 통계를 만들기 어렵다.

## 결정
다운로드 요청마다 DownloadHistory 데이터를 생성한다.

## 최종 저장 정보
- GameVersion 필수 ManyToOne 관계
- GameFile 필수 ManyToOne 관계
- BaseTimeEntity의 createdAt을 이용한 다운로드 시작 가능 시각
- BaseTimeEntity 상속에 따른 updatedAt

Member, IP, User-Agent는 저장하지 않는다. GameVersion에 누적 downloadCount도 추가하지 않는다.

## 결정 이유
- 전체 다운로드 수를 집계할 수 있다.
- 버전별 다운로드 수를 집계할 수 있다.
- 일별 다운로드 수를 집계할 수 있다.
- 원본 이력을 기반으로 통계를 다시 계산할 수 있다.

## 검토한 대안
GameVersion에 `downloadCount` 숫자만 저장하는 방식

## 구현 후 보완
`download_histories` 테이블에 요청 1회당 1행을 저장한다.
GameVersion 및 GameFile 관계에 cascade와 orphanRemoval은 설정하지 않는다.

Step 6 관리자 통계는 DownloadHistory 전체 Entity를 메모리에 적재하지 않고 DB에서 직접 집계한다.

- 전체 다운로드 수는 `JpaRepository.count()`를 사용한다.
- 버전별 다운로드 수는 JPQL `COUNT`와 `GROUP BY`를 사용하고 `VersionDownloadStatisticsProjection`으로 필요한 값만 조회한다.
- 일별 다운로드 수는 MySQL `DATE(created_at)` 기준의 Native Query로 `COUNT`와 `GROUP BY`를 수행하고 `DailyDownloadStatisticsProjection`으로 조회한다.
- Projection 결과는 Service에서 Response DTO로 변환한다.
- 버전별 결과는 `releasedAt DESC`, 일별 결과는 `date ASC`로 DB에서 정렬한다.

일별 집계는 MySQL 날짜 함수가 필요하고 이를 JPQL만으로 표현하면 불필요하게 복잡해지므로 해당 쿼리에만 Native Query를 선택했다.

이번 단계에서는 별도 통계 인덱스를 추가하지 않았다. `game_version_id`는 현재 MySQL FK 인덱스를 활용할 수 있고 데이터 규모도 작다. `created_at` 기반 인덱스는 DownloadHistory 증가 후 실제 실행계획을 확인하여 추가 여부를 결정한다.

---

# ADR-010. 다운로드 요청 검증 후 이력을 저장

## 상태
결정 완료

## 배경
HTTP 응답이 시작된 이후 사용자가 연결을 종료하면 파일 전송 완료 여부를 정확하게 판단하기 어렵다.

## 결정
다음 검증이 모두 완료된 후 파일 응답을 시작하기 직전에 다운로드 이력을 저장한다.

- 최신 버전 존재
- 버전 상태가 RELEASED
- 파일 메타데이터 존재
- 실제 파일 존재
- 파일 읽기 가능

다운로드 통계는 파일 전송 완료 횟수가 아니라 유효한 다운로드 시작 횟수를 의미한다.

이력 저장은 `FileStorage.open()`까지 성공한 뒤 정확히 한 번 시도한다. RELEASED 버전, GameFile 메타데이터, 물리 파일 또는 파일 open 단계가 실패하면 기록하지 않는다.

## 결정 이유
- 존재하지 않는 파일 요청은 통계에 포함하지 않을 수 있다.
- 일반적인 Spring MVC 구조에서 전송 완료 여부를 추적하는 복잡도를 줄일 수 있다.
- 다운로드 집계 기준을 명확하게 설명할 수 있다.

## 구현 후 보완
GameFileDownloadService는 읽기 전용 트랜잭션에서 다운로드 대상을 검증하고 파일을 연다.
DownloadHistoryService.record는 `REQUIRES_NEW` 트랜잭션에서 `saveAndFlush()`한다.
이력 저장 실패는 WARN 로그로 남기고 다운로드 흐름으로 전파하지 않는다. 따라서 DownloadHistory는 통계 정확성보다 다운로드 UX를 우선하는 best-effort 운영 데이터다.

---

# ADR-011. 새소식은 독립 운영 콘텐츠로 관리

## 상태
결정 완료

## 배경
패치노트는 특정 버전과 관련되지만, 이벤트와 점검 공지는 특정 버전과 관계없을 수 있다.

## 결정
현재 News 엔티티는 GameVersion을 참조하지 않는다. `NOTICE`, `PATCH_NOTE`, `EVENT` 유형을 갖는 독립 운영 콘텐츠로 관리하며 ACTIVE ADMIN 누구나 다른 관리자가 작성한 글까지 수정·삭제할 수 있다. 최초 writer는 유지한다.

## 결정 이유
- 일반 공지와 패치노트를 하나의 도메인으로 관리할 수 있다.
- 관리자 CRUD 중복을 줄일 수 있다.

## 구현 후 보완
GameVersion 연결은 구현하지 않았다. 공개 목록·상세 응답에도 writer를 노출하지 않는다. 관리자 공동 관리 세부 정책은 별도 `ADR018_Admin_Shared_Management_Policy.md`에 기록한다.

---

# ADR-012. 회원 권한과 회원 상태를 분리

## 상태
결정 완료

## 배경
USER와 ADMIN은 권한을 나타내며, 회원의 정상 이용 여부와는 다른 개념이다.

## 결정
회원의 권한과 상태를 별도로 관리한다.

## 권한
- USER
- ADMIN

## 상태
- ACTIVE
- SUSPENDED
- WITHDRAWN

## 초기 정책
- ACTIVE 회원만 로그인할 수 있다.
- SUSPENDED 회원은 로그인이 차단된다.
- WITHDRAWN 회원은 로그인이 차단된다.
- 비회원 조회가 가능한 콘텐츠는 로그인 없이 계속 조회할 수 있다.

## 결정 이유
- 관리자 권한과 계정 상태를 분리할 수 있다.
- 정지와 탈퇴 상태를 명확하게 표현할 수 있다.

## 구현 후 보완
회원 탈퇴 구현 범위와 데이터 보관 정책을 작성한다.

---

# ADR-013. 커뮤니티 조회와 작성 권한 분리

## 상태
결정 완료

## 결정
게시글과 댓글 조회는 비회원에게도 허용한다.

게시글과 댓글 작성은 ACTIVE 상태의 로그인 회원만 가능하다.

수정은 작성자 본인만 가능하고 삭제는 작성자 본인 또는 ACTIVE ADMIN이 가능하다.

## 결정 이유
- 커뮤니티 접근성을 높일 수 있다.
- 작성 행위에만 인증을 요구할 수 있다.
- 작성자 권한과 관리자 운영 권한을 구분할 수 있다.

## 구현 후 보완
Service가 매 쓰기 요청에서 최신 Member를 조회해 ACTIVE 상태를 확인한다. 관리자도 타 작성자의 게시글·댓글을 수정할 수는 없고 삭제만 할 수 있다.

---

# ADR-014. 관리자 API 경로 분리

## 상태
결정 완료

## 결정
관리자 기능은 `/api/admin` 경로 아래에 구성한다.

## 예시
- `/api/admin/members`
- `/api/admin/news`
- `/api/admin/game-versions`
- `/api/admin/download-statistics`

## 결정 이유
- 일반 사용자 API와 운영 API를 구분할 수 있다.
- 관리자 권한 검사를 공통 적용하기 쉽다.
- API 문서의 가독성을 높일 수 있다.

## 구현 후 보완
Spring Security에서 `/api/admin/**`에 `hasRole("ADMIN")`을 적용한다.
Spring Security가 세션의 ADMIN 역할을 1차 검사하고 각 관리자 Service가 DB의 최신 ACTIVE + ADMIN을 2차 검사한다.

---

# ADR-015. 파일 체크섬에 SHA-256 사용

## 상태
결정 완료

## 배경
DB에 저장된 메타데이터와 실제 파일의 동일성을 확인하기 위해 파일 무결성 값이 필요하다.

## 결정
LocalFileStorage가 temp 파일에 streaming 저장하는 단일 패스에서 각 byte chunk를 기록하면서 SHA-256 digest와 실제 fileSize를 함께 계산한다. checksum은 64자리 lowercase hexadecimal로 GameFile에 저장한다.

## 결정 이유
- 파일 무결성을 확인할 수 있다.
- 파일 손상 또는 변경 여부를 확인할 근거가 된다.
- 관리자 화면이나 다운로드 안내에서 체크섬을 제공할 수 있다.

## 구현 결과
- 전체 파일을 byte 배열로 올리지 않는다.
- FileStorage가 반환하는 StorageResult에 storageKey, 실제 fileSize, checksum을 담는다.
- release 시 checksum 전체 재계산은 현재 수행하지 않는다.

---

# ADR-016. 파일 저장과 DB 저장 실패에 보상 처리 적용

## 상태
구현 완료

## 배경
파일 시스템 저장은 DB 트랜잭션에 포함되지 않는다.

파일 저장 성공 후 DB 저장이 실패하면 실제 파일만 서버에 남을 수 있다.

## 결정 및 구현
1. 실제 파일 저장
2. GameFilePersistenceService의 짧은 별도 트랜잭션으로 DB 메타데이터 저장
3. `saveAndFlush()` 및 commit 실패가 GameFileUploadService로 전파되면 `FileStorage.delete(storageKey)` 실행
4. 보상 삭제 실패 시 원래 DB 예외를 유지하고 삭제 예외를 suppressed exception과 ERROR 로그로 기록

GameFileUploadService에는 `@Transactional`을 적용하지 않아 대용량 파일 streaming 동안 DB 트랜잭션을 유지하지 않는다. LocalFileStorage 저장 자체가 실패한 경우에는 구현체가 temp 파일을 정리하므로 별도 보상 삭제를 수행하지 않는다.

## 현재 제외 범위
- 보상 삭제 retry
- background orphan cleanup

---

# ADR-017. 로컬 파일 저장소로 시작

## 상태
결정 완료

## 결정
MVP에서는 서버 로컬 디렉터리에 게임 파일을 저장한다.

파일 저장 로직은 별도 컴포넌트로 분리하여 향후 S3로 교체할 수 있도록 한다.

## 구현 구조
- `FileStorage`: `store`, `exists`, `open`, `delete`
- `LocalFileStorage`: temp 파일, streaming 저장, SHA-256, 실제 fileSize, 최대 크기, 안전한 경로 resolve, 덮어쓰기 방지, atomic move 우선 및 fallback, 실패 시 temp 정리, idempotent delete, READ InputStream 생성
- `FileStorageProperties`: 환경변수 기반 root, 최대 512 MiB, 64 KiB buffer
- 향후 S3FileStorage로 교체할 수 있도록 업로드 및 다운로드 Service는 FileStorage 인터페이스에 의존한다. S3 구현은 현재 존재하지 않는다.

## 결정 이유
- MVP 개발 속도를 높일 수 있다.
- 파일 저장 책임을 비즈니스 로직과 분리할 수 있다.
- 테스트에서 임시 디렉터리를 사용할 수 있다.

## 설정
- root: `${JEXON_FILE_STORAGE_ROOT:./storage}`
- max-file-size: `536870912B`
- buffer-size: `65536B`
- Spring multipart max-file-size: `536870912B`
- Spring multipart max-request-size: `545259520B`

---

# ADR-018. AI를 반복 구현과 코드 검토에 활용

## 상태
결정 완료

## 결정
핵심 정책과 설계는 직접 결정하고, Codex는 반복 구현과 검토에 활용한다.

## 직접 담당
- 요구사항
- ERD
- API
- 권한 정책
- 버전 상태 흐름
- 파일 정책
- 다운로드 집계 기준
- 테스트 기준

## Codex 활용
- React UI
- CSS
- CRUD 코드 초안
- DTO와 Controller 초안
- 테스트 코드 초안
- 코드 리뷰
- 리팩터링

## 개발 중 기록할 내용
- 어떤 작업에 Codex를 사용했는가
- 생성된 코드에서 발견한 문제
- 직접 수정한 부분
- AI 사용으로 단축된 작업
- AI 사용이 적합하지 않았던 작업

---

# ADR-020. 세션 기반 통합 웹과 단일 EC2 Docker Compose 운영

## 상태
결정 및 배포 완료

## 배경
React UI, Spring Boot API, MySQL과 로컬 게임 파일을 실제로 운영하면서 인증 쿠키, SPA route, 내부 서비스 노출, DB와 파일 영속성을 일관되게 관리해야 한다.

## 결정

### 인증과 권한

- Spring Security 세션 인증과 `JSESSIONID`를 사용한다.
- `/api/members/**`는 회원 도메인(현재 signup), `/api/auth/**`는 login/logout/me 인증 생명주기로 분리한다.
- `/api/auth/me`는 `memberId`, `loginId`, `nickname`, `role`만 반환해 React Context가 세션 상태를 복원한다.
- `/api/admin/**`는 Spring Security `hasRole("ADMIN")`을 적용하고, 쓰기 및 운영 조회 Service는 DB의 최신 `ACTIVE + ADMIN`을 재검증한다.

### Frontend

- React, Vite, React Router를 사용하며 fetch API client는 `credentials: include`를 공통 적용한다.
- `AdminRoute`는 인증 로딩 후 ADMIN만 관리자 UI에 진입시키는 UX guard다. 최종 권한 보장은 backend가 담당한다.
- Nginx가 production 정적 파일을 제공하며 미일치 frontend 경로는 `/index.html`로 fallback한다.

### 운영 구조

- ap-northeast-2의 EC2 한 대에서 Docker Compose로 MySQL, Spring Boot, Nginx/React 세 컨테이너를 운영한다.
- Elastic IP의 HTTP 80만 공개한다. backend 8080과 MySQL 3306은 내부 network에서만 사용하며 Nginx가 `/api/**`를 backend로 reverse proxy한다.
- MySQL `/var/lib/mysql`은 named volume `mysql-data`, backend `/app/storage`는 EC2 host `./storage` bind mount로 영속화한다.
- backend runtime은 non-root UID/GID 10001이다.
- `SPRING_PROFILES_ACTIVE=prod`, 환경변수 datasource, `ddl-auto: validate`를 사용한다. `.env.prod`는 추적하지 않고 `.env.prod.example`만 제공한다.
- 운영 schema는 현재 dump/import로 초기화하고 Flyway/Liquibase는 사용하지 않는다.

## 이유

- 같은 origin의 Nginx 진입점으로 세션 쿠키와 API 연결을 단순화한다.
- 8080/3306을 외부에 공개하지 않고 HTTP 진입점을 하나로 제한한다.
- 현재 트래픽과 운영 규모에서는 EC2 단일 서버가 충분하고 RDS/S3를 추가하지 않아 비용과 복잡도를 줄인다.
- DB와 게임 파일의 서로 다른 영속성 요구를 named volume과 bind mount로 분리한다.

## 검증 결과

- 로컬 production Compose에서 healthcheck, `/api` proxy, SPA 직접 진입, ZIP 다운로드, `down`/`up` 후 DB/storage 복원을 확인했다.
- AWS에서 Home, 로그인/API, ZIP, 관리자 기능과 Compose restart 후 데이터·파일 유지를 확인했다.
- t3.micro 메모리 제약을 보완하기 위해 EC2 host에 2GB swap을 구성했다.

## 현재 제외 범위

HTTPS/도메인/Route53, RDS, S3, CDN, CI/CD와 GitHub Actions는 현재 구조에 포함하지 않는다.

---

# ADR 추가 작성 양식

## ADR-번호. 제목

### 상태
초안 / 결정 완료 / 변경 / 폐기

### 배경
어떤 문제를 해결해야 했는지 작성한다.

### 결정
최종적으로 선택한 방식을 작성한다.

### 결정 이유
해당 방식을 선택한 근거를 작성한다.

### 검토한 대안
다른 방법과 장단점을 작성한다.

### 구현 결과
실제 구현 구조를 작성한다.

### 문제 및 변경 사항
구현 중 발생한 문제와 설계 변경 내용을 작성한다.

### 한계 및 향후 개선
현재 구현의 한계와 개선 방향을 작성한다.
