# Jexon 프로젝트 개발 로드맵

## 1. 프로젝트 개요

Jexon은 하나의 게임을 소개하고 실제 ZIP 파일을 배포하는 공식 홈페이지다. Spring Boot 백엔드, React/Vite 프론트엔드, MySQL, Nginx를 하나의 서비스로 통합했으며 AWS EC2에서 Docker Compose로 운영한다.

## 2. 최종 완료 상태

- Backend 핵심 기능과 Frontend 전체 연결 완료
- 비회원/USER/ADMIN 전체 수동 통합 테스트 완료
- 백엔드 자동 테스트와 frontend production build 성공
- Docker Compose 운영 구성 및 로컬 production 검증 완료
- AWS EC2 배포, Elastic IP 적용, 배포 환경 기능 검증 완료
- Docker 재기동과 `down` 후 `up`에서 DB 및 게임 파일 영속성 검증 완료
- Windows executable을 포함한 약 47MB Jexon Dino ZIP을 실제 배포할 수 있는 상태
- Jexon Dino Home UI와 이미지 assets 적용 완료

## 3. 사용자별 완료 기능

### 비회원

- Home, Download, News, Community 목록·상세 접근
- 최신 RELEASED 버전 조회와 실제 ZIP 스트리밍 다운로드
- 게시글별 댓글 조회

### USER

- 회원가입, 로그인, 새로고침 후 세션 유지, 로그아웃
- 게시글과 댓글 작성·본인 수정·본인 삭제
- 관리자 화면 차단

### ADMIN

- ACTIVE/SUSPENDED 상태별 회원 목록, 정지·해제
- GameVersion 생성·수정·목록·상세·release와 ZIP 업로드
- 새소식 공동 CRUD
- 전체·버전별·일별 다운로드 통계 조회
- 게시글·댓글 관리자 삭제

## 4. 단계별 결과

| 단계 | 결과 |
| --- | --- |
| 1. 기획·설계 | 완료 |
| 2. 기반 설정·MySQL·예외·JPA | 완료 |
| 3. 세션 인증·회원 | 회원가입/로그인/로그아웃/`/api/auth/me` 및 USER/ADMIN, 상태 검증 완료 |
| 4. 커뮤니티·새소식 | 게시글/댓글 CRUD, 공개 News와 관리자 News CRUD 완료 |
| 5. 버전·파일 | 상태 흐름, 단일 ZIP, LocalFileStorage, release 동시성 제어 완료 |
| 6. 다운로드·통계 | 실제 streaming, best-effort DownloadHistory, 전체/버전별/일별 backend 및 관리자 화면 완료 |
| 7. Frontend | React/Vite 전체 API 연결, 인증 Context, AdminRoute, Jexon Dino Home 완료 |
| 8. 검증 | 자동 테스트, production build, 전체 수동 통합 테스트 완료 |
| 9. 운영 배포 | Docker Compose 로컬 검증, AWS EC2/Elastic IP HTTP 배포 완료 |
| 10. 문서화 | 최종 구현 기준 docs 동기화 완료, README는 다음 작업 |

## 5. 최종 검증 요약

### 자동 검증

- Backend: `./gradlew test --rerun-tasks` 209개 성공, failures 0, errors 0, skipped 0, `BUILD SUCCESSFUL`.
- Frontend: `npm run build` 성공(Vite 7.3.6, 78 modules transformed).

### 수동 통합 검증

- 비회원: 공개 화면, 최신 버전, ZIP 다운로드, News/Post/Comment 조회와 비인가 UI 차단 확인
- USER: 가입·로그인·세션 유지·게시글/댓글 CUD·타 작성자 UI 차단·관리자 차단·로그아웃 확인
- ADMIN: 회원 필터/정지/해제, 정지 로그인 차단, 자기 상태 변경 차단, 버전 생성/업로드/release, 기존 RELEASED 비활성화, News CRUD, 통계와 DB 집계 일치 확인
- 다운로드 1요청마다 DownloadHistory 1행 증가 확인

### 운영 검증

- 로컬: 세 컨테이너 정상, MySQL healthcheck, `localhost:80`, `/api` proxy, SPA 직접 URL, 실제 ZIP, DB/storage 복원 확인
- AWS(ap-northeast-2): Home·로그인/API·ZIP·관리자 기능 외부 접속 성공, Compose restart 후 DB와 storage 유지 확인
- t3.micro의 약 1GB RAM에서 Docker·MySQL·Gradle build 메모리 부족을 줄이기 위해 2GB swap 구성

## 6. 최종 운영 구조

```text
사용자
  ↓
Elastic IP
  ↓
Nginx :80
  ├─ React static assets
  └─ /api/** → Spring Boot :8080 → MySQL :3306
```

- 외부 공개 포트: HTTP 80
- SSH 22: 사용자 IP로 제한
- backend 8080, MySQL 3306: host 외부 미공개
- DB: Docker named volume `mysql-data` ↔ `/var/lib/mysql`
- 게임 파일: EC2 host `./storage` ↔ backend `/app/storage`

## 7. 운영 설정 정책

- `SPRING_PROFILES_ACTIVE=prod`; datasource는 환경변수로 주입
- `.env.prod`는 Git 제외, `.env.prod.example`만 추적
- `ddl-auto: validate`; 운영 schema는 dump/import로 초기화하며 Flyway/Liquibase는 미사용
- backend는 non-root UID/GID 10001로 실행하므로 EC2 host storage 쓰기 권한이 필요
- EC2 `jexon-server`: Ubuntu Server 26.04 LTS, t3.micro, 30 GiB gp3, Elastic IP
- 현재 RDS/S3 없이 EC2 한 대의 Docker Compose로 운영

## 8. 미구현 및 향후 개선

- HTTPS, SSL 인증서, 도메인, Route53
- RDS, S3, CDN
- CI/CD, GitHub Actions
- Flyway/Liquibase
- Range Request, 다운로드 완료 추적, release 시 checksum 재검증, 과거 버전 다운로드
- 기간별 통계, 7일/30일 필터, 차트
- 관리자 역할 변경, 회원 상세 관리·개인정보 수정, 검색 기능 확대
- 파일 교체·삭제 및 orphan cleanup

현재 배포는 **HTTP + Elastic IP**이며 위 항목은 완료로 처리하지 않는다.
