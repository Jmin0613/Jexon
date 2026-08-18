# Jexon 기능 요구사항

## 1. 공통 인증·권한 정책

- 인증은 Spring Security HTTP session을 사용한다.
- 비회원은 공개 Home, 최신 버전/다운로드, News, Post, Comment 조회가 가능하다.
- 로그인 사용자의 쓰기 요청은 Service에서 최신 Member가 `ACTIVE`인지 확인한다.
- `/api/admin/**`는 Security의 ADMIN 검사와 Service의 최신 `ACTIVE + ADMIN` 검사를 모두 통과해야 한다.
- 역할은 `USER`, `ADMIN`; 상태는 `ACTIVE`, `SUSPENDED`, `WITHDRAWN`이다.
- SUSPENDED와 WITHDRAWN은 로그인할 수 없다. WITHDRAWN 전환·복구 API는 현재 제공하지 않는다.

## 2. 회원·인증

### 회원가입

- loginId, password, nickname, email, name, phoneNumber를 입력한다.
- 중복·형식 검증 후 BCrypt 비밀번호, 기본 `USER`, `ACTIVE`로 저장한다.
- 성공 시 생성된 memberId를 반환한다.

### 로그인·세션

- loginId/password가 일치하고 상태가 ACTIVE인 경우 SecurityContext를 session에 저장한다.
- `/api/auth/me`는 현재 session의 `memberId`, `loginId`, `nickname`, `role`만 반환한다.
- 로그아웃은 session과 인증을 제거하고 `JSESSIONID` cookie를 삭제한다.

### 관리자 회원 관리

- 회원 목록은 선택적 status 필터, 기본 20/최대 100개, `createdAt DESC, id DESC` 고정 정렬을 사용한다.
- 목록 응답은 memberId, loginId, nickname, email, role, status, createdAt이다.
- 상태 변경은 ACTIVE↔SUSPENDED만 허용한다.
- 같은 상태 요청과 WITHDRAWN 관련 변경은 409, 자기 자신 변경은 403이다.
- 역할 변경, 회원 상세 관리와 개인정보 수정은 현재 지원하지 않는다.

## 3. Community

### 게시글

- 목록·상세는 공개, 작성은 ACTIVE 로그인 사용자만 가능하다.
- 목록은 기본 20/최대 100개, `createdAt DESC, id DESC`다.
- title은 필수 최대 100자, content는 필수 최대 5,000자다.
- 수정은 작성자만 가능하고 삭제는 작성자 또는 ACTIVE ADMIN이 가능하다.

### 댓글

- 게시글별 목록은 공개, 작성은 ACTIVE 로그인 사용자만 가능하다.
- 목록은 기본 20/최대 100개, `createdAt DESC, id DESC`다.
- content는 필수 최대 1,000자다.
- 수정은 작성자만 가능하고 삭제는 작성자 또는 ACTIVE ADMIN이 가능하다.

## 4. News

- 유형은 `NOTICE`, `PATCH_NOTE`, `EVENT`다.
- 공개 목록·상세를 제공한다. 목록은 선택적 type/keyword, 기본 20/최대 100개, `createdAt DESC, id DESC`다.
- ACTIVE ADMIN만 작성·수정·삭제할 수 있다.
- 운영 공동 콘텐츠이므로 다른 ADMIN이 작성한 글도 수정·삭제할 수 있고 최초 writer는 유지한다.
- title은 필수 최대 150자, content는 필수 최대 10,000자다.

## 5. GameVersion

- 상태는 `DRAFT`, `RELEASED`, `INACTIVE`다.
- version은 `vMAJOR.MINOR.PATCH`, 최대 30자, unique이며 생성 후 변경하지 않는다.
- title 10~100자, description 10~500자다.
- ACTIVE ADMIN은 생성, status 필터 목록, 상세, title/description 수정, release를 수행할 수 있다.
- 새 버전은 DRAFT다. DRAFT 또는 INACTIVE만 release할 수 있고 GameFile이 필수다.
- release 시 기존 RELEASED를 INACTIVE로 바꾸고 대상을 RELEASED로 전환하며 releasedAt을 현재 시각으로 갱신한다.
- 공개 API는 현재 RELEASED 하나만 최신 버전으로 조회한다.
- GameVersion과 singleton GameVersionReleaseControl의 낙관적 락으로 동시 release를 제어하며 충돌은 409다.

## 6. GameFile

- ACTIVE ADMIN은 DRAFT GameVersion에 ZIP 파일 하나만 업로드할 수 있다.
- 원본 파일명 경로 제거·Unicode NFC 정규화, `.zip` 확장자와 ZIP signature를 검사한다.
- 최대 크기는 512 MiB이며 streaming 저장 중 실제 fileSize와 SHA-256 checksum을 계산한다.
- DB에는 GameVersion, originalFileName, storageKey, extension, contentType, fileSize, checksum과 공통 시각을 저장한다.
- 실제 파일은 `FileStorage` 추상화를 거쳐 `LocalFileStorage`에 저장한다.
- DB 메타데이터 저장 실패 시 저장된 물리 파일을 보상 삭제한다.
- storageKey와 실제 경로는 외부 응답에 노출하지 않는다.
- 파일 교체·삭제와 release 시 checksum/물리 파일 재검증은 현재 지원하지 않는다.

Jexon Dino 소스는 별도 저장소에 있다. Jexon에서는 Windows executable을 포함한 약 47MB ZIP을 실제 업로드·다운로드 대상으로 사용하고 Home 소개 이미지에 활용한다.

## 7. Download와 DownloadHistory

- 비회원 포함 누구나 최신 RELEASED 파일을 다운로드할 수 있다.
- 서버는 대상과 물리 파일을 검증하고 FileStorage InputStream을 `application/octet-stream`으로 streaming한다.
- Content-Disposition은 originalFileName, Content-Length는 fileSize를 사용한다.
- 파일 open 성공 후 요청 1건당 DownloadHistory 저장을 정확히 한 번 시도한다.
- DownloadHistory는 GameVersion과 GameFile 필수 관계, createdAt/updatedAt만 가진다. Member, IP, User-Agent는 저장하지 않는다.
- 기록은 `REQUIRES_NEW`와 `saveAndFlush()`를 사용한다. 실패는 WARN으로 남기고 다운로드는 계속하는 best-effort 정책이다.
- 집계 기준은 전송 완료가 아니라 유효한 다운로드 시작 요청이다.

## 8. 다운로드 통계

- ACTIVE ADMIN만 전체 수, 버전별 수, 일별 수를 조회한다.
- 전체는 repository `count()`, 버전별은 JPQL COUNT/GROUP BY + Projection, 일별은 MySQL `DATE(created_at)` native query + Projection으로 DB에서 직접 집계한다.
- 버전별은 이력이 있는 과거 INACTIVE 버전도 포함하고 `releasedAt DESC`; 일별은 전체 기간을 `date ASC`로 반환한다.
- 기간 query parameter, 7/30일 filter, chart는 현재 제공하지 않는다.
- 데이터 규모와 현재 query를 고려해 별도 통계 index는 추가하지 않았다.

## 9. Frontend

- React, Vite, React Router를 사용한다.
- 공개 Home/Download/News/Community/Login/Signup과 게시글·댓글 CUD UI를 제공한다.
- Context가 `/api/auth/me`로 인증 상태를 복원하고 공통 fetch client가 `credentials: include`를 사용한다.
- AdminRoute는 ADMIN UI 진입을 guard하며 Members, Game Versions, News, Download Statistics 화면을 제공한다.
- Nginx가 production static assets를 제공하고 SPA fallback을 처리한다.
- UI guard는 편의 기능이며 권한의 최종 기준은 backend다.

## 10. 배포·운영

- AWS ap-northeast-2의 EC2 `jexon-server` 한 대에서 Docker Compose 세 컨테이너(MySQL, Spring Boot, Nginx/React)를 운영한다.
- Elastic IP의 HTTP 80만 공개하고 SSH 22는 사용자 IP로 제한한다. 8080/3306은 외부에 publish하지 않는다.
- Nginx는 React static assets와 `/api/**` reverse proxy, SPA fallback을 담당한다.
- `SPRING_PROFILES_ACTIVE=prod`, 환경변수 datasource, `ddl-auto: validate`를 사용한다.
- `.env.prod`는 Git에서 제외하고 `.env.prod.example`만 추적한다.
- schema는 dump/import 방식으로 초기화하며 Flyway/Liquibase는 현재 사용하지 않는다.
- DB는 named volume, 게임 파일은 EC2 host `storage` bind mount로 영속화한다.
- backend는 non-root UID/GID 10001이므로 host storage 권한을 준비해야 한다.
- 현재는 HTTP + Elastic IP이며 HTTPS, SSL, domain, Route53은 미구현이다.
