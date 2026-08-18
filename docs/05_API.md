# Jexon REST API 명세

## 1. 공통 규칙

- Base path: `/api`
- JSON 요청은 `Content-Type: application/json`; 파일 업로드는 `multipart/form-data`다.
- 인증은 Spring Security session cookie를 사용한다. frontend fetch는 `credentials: include`를 보낸다.
- 공개 GET 이외의 요청은 인증이 필요하고 `/api/admin/**`는 ACTIVE ADMIN 전용이다.
- 일반 오류 형식은 `ErrorResponse`; 대표 상태는 400 validation, 401 미인증, 403 권한, 404 없음, 409 상태·중복·동시성 충돌, 500 저장소/서버 오류다.
- Page endpoint는 `page` 기본 0, `size` 기본 20/최대 100이며 응답은 Spring Data `Page` JSON이다.

## 2. Auth와 Member

| Method | Path | 권한 | Request | 성공 응답 |
| --- | --- | --- | --- | --- |
| POST | `/api/members/signup` | 공개 | `loginId`, `password`, `passwordConfirm`, `nickname`, `email`, `name`, `phoneNumber` | `201`, body: 생성된 memberId 숫자 |
| POST | `/api/auth/login` | 공개 | `loginId`, `password` | `200`, body 없음, session 생성 |
| POST | `/api/auth/logout` | 인증 | 없음 | `200`, body 없음, session/cookie 제거 |
| GET | `/api/auth/me` | 인증 | 없음 | `200`, CurrentUserResponse |

`GET /api/auth/me` 응답:

```json
{
  "memberId": 1,
  "loginId": "jexonUser",
  "nickname": "Jexon",
  "role": "USER"
}
```

password, email, name, phoneNumber, status 등 민감·불필요 정보는 반환하지 않는다.

## 3. Posts와 Comments

| Method | Path | 권한 | Request / Query | 성공 응답 |
| --- | --- | --- | --- | --- |
| GET | `/api/posts` | 공개 | `page`, `size` | `200 Page<PostListResponse>` |
| GET | `/api/posts/{postId}` | 공개 | - | `200 PostDetailResponse` |
| POST | `/api/posts` | ACTIVE 인증 | `title`, `content` | `201 { "postId": number }` |
| PUT | `/api/posts/{postId}` | 작성자 | `title`, `content` | `200 PostDetailResponse` |
| DELETE | `/api/posts/{postId}` | 작성자/ACTIVE ADMIN | - | `204` |
| GET | `/api/posts/{postId}/comments` | 공개 | `page`, `size` | `200 Page<CommentResponse>` |
| POST | `/api/posts/{postId}/comments` | ACTIVE 인증 | `content` | `201 { "commentId": number }` |
| PUT | `/api/comments/{commentId}` | 작성자 | `content` | `200 CommentResponse` |
| DELETE | `/api/comments/{commentId}` | 작성자/ACTIVE ADMIN | - | `204` |

DTO 필드:

- `PostListResponse`: postId, title, writerId, writerNickname, createdAt
- `PostDetailResponse`: 위 필드 + content, updatedAt
- `CommentResponse`: commentId, content, writerId, writerNickname, createdAt, updatedAt
- 목록 정렬은 `createdAt DESC, id DESC`; client sort는 사용하지 않는다.

## 4. News

| Method | Path | 권한 | Request / Query | 성공 응답 |
| --- | --- | --- | --- | --- |
| GET | `/api/news` | 공개 | 선택 `type`, `keyword`, `page`, `size` | `200 Page<NewsListResponse>` |
| GET | `/api/news/{newsId}` | 공개 | - | `200 NewsDetailResponse` |
| POST | `/api/admin/news` | ACTIVE ADMIN | `type`, `title`, `content` | `201 { "newsId": number }` |
| PUT | `/api/admin/news/{newsId}` | ACTIVE ADMIN | `type`, `title`, `content` | `200 NewsDetailResponse` |
| DELETE | `/api/admin/news/{newsId}` | ACTIVE ADMIN | - | `204` |

- `NewsListResponse`: newsId, type, title, createdAt
- `NewsDetailResponse`: newsId, type, title, content, createdAt, updatedAt
- type은 `NOTICE`, `PATCH_NOTE`, `EVENT`; 정렬은 `createdAt DESC, id DESC`다.
- ACTIVE ADMIN은 작성자와 관계없이 수정·삭제할 수 있다.

## 5. 공개 GameVersion과 Download

| Method | Path | 권한 | 성공 응답 |
| --- | --- | --- | --- |
| GET | `/api/game-versions/latest` | 공개 | `200 LatestGameVersionResponse` |
| GET | `/api/game-versions/latest/download` | 공개 | `200` ZIP binary stream |

LatestGameVersionResponse:

```json
{
  "gameVersionId": 2,
  "version": "v1.1.0",
  "title": "Jexon 정식 업데이트 버전",
  "description": "신규 콘텐츠가 추가된 버전입니다.",
  "releasedAt": "2026-08-18T12:00:00",
  "gameFileId": 1,
  "originalFileName": "Jexon_Dino.zip",
  "fileSize": 49283072,
  "checksum": "64자리-lowercase-sha256"
}
```

다운로드 응답은 `Content-Type: application/octet-stream`, UTF-8 originalFileName의 attachment `Content-Disposition`, GameFile.fileSize의 `Content-Length`와 InputStream body다. storageKey와 실제 경로는 공개하지 않는다. 최신 RELEASED나 연결 파일/물리 파일이 없으면 404 또는 500이며 이력은 파일 open 성공 후 best-effort로 한 번 기록한다.

## 6. 관리자 회원

| Method | Path | Query / Request | 성공 응답 |
| --- | --- | --- | --- |
| GET | `/api/admin/members` | 선택 `status`, `page`, `size` | `200 Page<AdminMemberListResponse>` |
| PATCH | `/api/admin/members/{memberId}/status` | `{ "status": "ACTIVE" }` 또는 `SUSPENDED` | `200 AdminMemberStatusUpdateResponse` |

- 목록 필드: memberId, loginId, nickname, email, role, status, createdAt
- 목록 정렬: `createdAt DESC, id DESC`; size 최대 100
- 같은 상태 또는 WITHDRAWN 관련 변경은 409, 자기 자신 변경은 403, 없는 회원은 404
- keyword/searchType/role query와 별도의 `/suspend`, `/activate` endpoint는 없다.

상태 변경 응답:

```json
{ "memberId": 2, "status": "SUSPENDED" }
```

## 7. 관리자 GameVersion과 GameFile

| Method | Path | Request / Query | 성공 응답 |
| --- | --- | --- | --- |
| POST | `/api/admin/game-versions` | `version`, `title`, `description` | `201 GameVersionCreateResponse` |
| GET | `/api/admin/game-versions` | 선택 `status`, `page`, `size` | `200 Page<GameVersionListResponse>` |
| GET | `/api/admin/game-versions/{gameVersionId}` | - | `200 GameVersionDetailResponse` |
| PUT | `/api/admin/game-versions/{gameVersionId}` | 선택 `title`, `description`(최소 하나) | `200 GameVersionDetailResponse` |
| POST | `/api/admin/game-versions/{gameVersionId}/file` | multipart part `file` | `201 GameFileUploadResponse` |
| POST | `/api/admin/game-versions/{gameVersionId}/release` | - | `200 GameVersionReleaseResponse` |

DTO 필드:

- Create: gameVersionId, version, status
- List: gameVersionId, version, title, status, releasedAt, createdAt
- Detail: List 필드 + description, updatedAt
- Release: gameVersionId, version, status, releasedAt
- File upload: gameFileId, gameVersionId, originalFileName, extension, fileSize, checksum

정책:

- 목록은 선택적 DRAFT/RELEASED/INACTIVE 필터, `createdAt DESC, id DESC`, size 최대 100이다.
- upload는 DRAFT에 ZIP 하나만 허용하며 최대 512 MiB다. storageKey/contentType은 응답하지 않는다.
- release는 DRAFT/INACTIVE + GameFile을 요구한다. 기존 RELEASED는 INACTIVE가 되며 동시성 충돌은 409다.
- GameFile 삭제와 과거 버전 지정 다운로드 endpoint는 없다.

## 8. 관리자 다운로드 통계

| Method | Path | 성공 응답 |
| --- | --- | --- |
| GET | `/api/admin/download-statistics/summary` | `200 { "totalDownloads": number }` |
| GET | `/api/admin/download-statistics/versions` | `200 [{ gameVersionId, version, status, downloadCount }]` |
| GET | `/api/admin/download-statistics/daily` | `200 [{ date, downloadCount }]` |

- 세 endpoint 모두 ACTIVE ADMIN 전용이다.
- versions는 이력이 있는 버전을 `releasedAt DESC`, daily는 전체 기간을 `date ASC`로 반환한다.
- 기간 관련 query parameter는 없다. 데이터가 없으면 0 또는 빈 배열이다.

## 9. 구현하지 않은 API

마이페이지/회원정보 수정, 회원 역할 변경, GameFile 교체·삭제, 과거 버전 다운로드, 기간별 통계 endpoint는 현재 존재하지 않는다.
