# Jexon REST API 명세

## 1. 공통 규칙

### Base URL
`/api`

### Content-Type
JSON 요청 및 응답: `application/json`

파일 업로드: `multipart/form-data`

파일 다운로드: `application/octet-stream`

### 인증 방식
HTTP Session 기반 인증

### 날짜 형식
ISO-8601 형식

예시: `2026-07-29T14:30:00`

### 페이지 응답 예시
```json
{
    "content": [],
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0,
    "first": true,
    "last": true
}
```

---

## 2. 인증 및 회원 API

### 회원가입 
Request  `POST /api/members`

```json
{
    "loginId": "jumin0613",
    "password": "password123!",
    "passwordConfirm": "password123!",
    "nickname": "주민",
    "email": "jumin@example.com"
}
```

### 로그인
Request  `POST /api/auth/login`

```json
{
    "loginId": "jumin0613",
    "password": "password123!"
}
```

Response  `200 OK`

```json
{
    "memberId": 1,
    "loginId": "jumin0613",
    "nickname": "주민",
    "role": "USER"
}
```

Error
- 400: 입력값 누락
- 401: 아이디 또는 비밀번호 불일치
- 403: 정지 또는 탈퇴 회원

### 로그아웃 
Request  `POST /api/auth/logout`

Response  `204 No Content`

Error : 401: 로그인하지 않은 사용자

### 현재 로그인 회원 조회 
Request  `GET /api/auth/me`

Response  `200 OK`

```json
{
    "authenticated": true,
    "memberId": 1,
    "loginId": "jumin0613",
    "nickname": "주민",
    "role": "USER",
    "status": "ACTIVE"
}
```

비로그인 응답:
```json
{
    "authenticated": false
}
```

### 마이페이지 조회
Request  `GET /api/members/me`

Response  `200 OK`
```json
{
    "memberId": 1,
    "loginId": "jumin0613",
    "nickname": "주민",
    "email": "jumin@example.com",
    "role": "USER",
    "status": "ACTIVE",
    "postCount": 3,
    "commentCount": 10,
    "createdAt": "2026-07-29T14:30:00"
}
```

Error 
- 401: 로그인 필요

### 회원 정보 수정
Request  `PATCH /api/members/me`
```json
{
    "nickname": "새닉네임",
    "email": "new@example.com"
}
```

Response  `200 OK`
```json
{
    "memberId": 1,
    "nickname": "새닉네임",
    "email": "new@example.com"
}
```

Error
- 400: 입력값 검증 실패
- 401: 로그인 필요
- 409: 닉네임 또는 이메일 중복

---

## 3. 게시글 API

### 게시글 목록 조회
Request  `GET /api/posts`

Query Parameters

| 이름         | 필수 |            기본값 | 설명            |
| ---------- | -: | -------------: | ------------- |
| page       |  N |              0 | 페이지 번호        |
| size       |  N |             20 | 페이지 크기        |
| keyword    |  N |                | 검색어           |
| searchType |  N |          TITLE | TITLE, WRITER |
| sort       |  N | createdAt,desc | 정렬            |

Response  `200 OK`
```json
{
"content": [
    {
        "postId": 1,
        "title": "게시글 제목",
        "writerId": 1,
        "writerNickname": "주민",
        "viewCount": 10,
        "commentCount": 2,
        "createdAt": "2026-07-29T14:30:00"
    }
],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
}
```

### 게시글 상세 조회
Request  `GET /api/posts/{postId}`

Response  `200 OK`

```json
{
    "postId": 1,
    "title": "게시글 제목",
    "content": "게시글 내용",
    "writerId": 1,
    "writerNickname": "주민",
    "viewCount": 11,
    "editable": true,
    "deletable": true,
    "createdAt": "2026-07-29T14:30:00",
    "updatedAt": "2026-07-29T14:30:00",
    "comments": [
        {
            "commentId": 1,
            "writerId": 2,
            "writerNickname": "사용자",
            "content": "댓글 내용",
            "editable": false,
            "deletable": false,
            "createdAt": "2026-07-29T15:00:00",
            "updatedAt": "2026-07-29T15:00:00"
        }
    ]
}
```

Error
- 404: 게시글 없음

### 게시글 작성

Request  `POST /api/posts`
```json
{
    "title": "게시글 제목",
    "content": "게시글 내용"
}
```

Response  `201 Created`
```json
{
    "postId": 1
}
```

Error
- 400: 입력값 검증 실패
- 401: 로그인 필요
- 403: 정지 회원

### 게시글 수정

Request  `PUT /api/posts/{postId}`
```json
{
    "title": "수정된 제목",
    "content": "수정된 내용"
}
```

Response  `200 OK`
```json
{
    "postId": 1,
    "title": "수정된 제목",
    "content": "수정된 내용",
    "updatedAt": "2026-07-29T16:00:00"
}
```

Error
- 400: 입력값 검증 실패
- 401: 로그인 필요
- 403: 작성자 또는 관리자가 아님
- 404: 게시글 없음

### 게시글 삭제
Request  `DELETE /api/posts/{postId}`

Response  `204 No Content`

Error
- 401: 로그인 필요
- 403: 작성자 또는 관리자가 아님
- 404: 게시글 없음

---

## 4. 댓글 API

### 댓글 작성
Request  `POST /api/posts/{postId}/comments`
```json
{
    "content": "댓글 내용"
}
```

Response  `201 Created`
```json
{
    "commentId": 1
}
```

Error
- 400: 입력값 검증 실패
- 401: 로그인 필요
- 403: 정지 회원
- 404: 게시글 없음

### 댓글 수정
Request  `PUT /api/comments/{commentId}`
```json
{
    "content": "수정된 댓글"
}
```

Response  `200 OK`
```json
{
    "commentId": 1,
    "content": "수정된 댓글",
    "updatedAt": "2026-07-29T16:00:00"
}
```

Error
- 400: 입력값 검증 실패
- 401: 로그인 필요
- 403: 작성자 또는 관리자가 아님
- 404: 댓글 없음

### 댓글 삭제
Request  `DELETE /api/comments/{commentId}`

Response  `204 No Content`

Error
- 401: 로그인 필요
- 403: 작성자 또는 관리자가 아님
- 404: 댓글 없음

---

## 5. 새소식 사용자 API

### 새소식 목록 조회

권한: 비회원 포함 전체 사용자

Request `GET /api/news`

Query Parameters

| 이름      | 필수 | 기본값 | 설명                        |
| ------- | -: | --: | ------------------------- |
| page    |  N |   0 | 페이지 번호                    |
| size    |  N |  20 | 페이지 크기, 최대 100             |
| type    |  N |     | NOTICE, PATCH_NOTE, EVENT |
| keyword |  N |     | 제목 검색                     |

정렬은 `createdAt DESC`, `id DESC`로 고정하며 요청의 sort 조건은 사용하지 않는다.
keyword가 NULL 또는 공백이면 제목 검색 조건을 적용하지 않는다.

Response `200 OK`
```json
{
    "content": [
        {
            "newsId": 1,
            "type": "PATCH_NOTE",
            "title": "1.1.0 업데이트",
            "createdAt": "2026-07-29T14:30:00"
        }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
}
```

### 새소식 상세 조회

권한: 비회원 포함 전체 사용자

Request `GET /api/news/{newsId}`

Response `200 OK`
```json
{
    "newsId": 1,
    "type": "PATCH_NOTE",
    "title": "1.1.0 업데이트",
    "content": "업데이트 내용",
    "createdAt": "2026-07-29T14:30:00",
    "updatedAt": "2026-07-29T14:30:00"
}
```

Error
- 404: 새소식 없음

---

## 6. 다운로드 사용자 API

### 최신 버전 정보 조회

Request  `GET /api/game/versions/latest`

Response  `200 OK`

```json
{
    "versionId": 2,
    "version": "1.1.0",
    "title": "정식 업데이트",
    "description": "신규 콘텐츠가 추가되었습니다.",
    "releasedAt": "2026-07-29T14:30:00",
    "file": {
        "originalName": "Jexon_Setup_1.1.0.zip",
        "fileSize": 104857600,
        "checksum": "sha256-checksum-value"
    }
}
```

Error
- 404: 배포 중인 버전 없음
- 404: 연결된 파일 없음

### 최신 버전 다운로드

Request  `GET /api/downloads/latest`

Response  `200 OK`

Headers:
```
Content-Type: application/octet-stream
Content-Disposition: attachment; filename="Jexon_Setup_1.1.0.zip"
Content-Length: 104857600
```

Body: 파일 바이너리 스트림

Error
- 404: 최신 버전 없음
- 404: 파일 메타데이터 없음
- 404: 실제 파일 없음
- 500: 파일 읽기 실패

---

## 7. 관리자 회원 API

### 회원 목록 조회

Request  `GET /api/admin/members`

Query Parameters

| 이름         | 필수 |      기본값 | 설명                           |
| ---------- | -: | -------: | ---------------------------- |
| page       |  N |        0 | 페이지 번호                       |
| size       |  N |       20 | 페이지 크기                       |
| keyword    |  N |          | 검색어                          |
| searchType |  N | LOGIN_ID | LOGIN_ID, NICKNAME, EMAIL    |
| role       |  N |          | USER, ADMIN                  |
| status     |  N |          | ACTIVE, SUSPENDED, WITHDRAWN |

Response  `200 OK`
```json
{
"content": [
        {
            "memberId": 1,
            "loginId": "jumin0613",
            "nickname": "주민",
            "email": "jumin@example.com",
            "role": "USER",
            "status": "ACTIVE",
            "createdAt": "2026-07-29T14:30:00"
        }
    ],
        "page": 0,
        "size": 20,
        "totalElements": 1,
        "totalPages": 1,
        "first": true,
        "last": true
}
```

### 회원 정지 

Request `PATCH /api/admin/members/{memberId}/suspend`

Response `200 OK`
```json
{
    "memberId": 1,
    "status": "SUSPENDED"
}
```

Error
- 403: 관리자 권한 없음
- 403: 관리자 계정 정지 시도
- 409: 자기 자신 정지 시도
- 404: 회원 없음

### 회원 정지 해제

Request  `PATCH /api/admin/members/{memberId}/activate`

Response  `200 OK`

```json
{
    "memberId": 1,
    "status": "ACTIVE"
}
```

Error
- 403: 관리자 권한 없음
- 404: 회원 없음
- 409: 정지 상태가 아닌 회원

---

## 8. 관리자 새소식 API

### 새소식 등록

권한: ACTIVE 상태의 ADMIN

Request `POST /api/admin/news`
```json
{
    "type": "PATCH_NOTE",
    "title": "1.1.0 업데이트",
    "content": "업데이트 내용"
}
```

Response `201 Created`
```json
{
    "newsId": 1
}
```

Location 헤더는 반환하지 않는다.

Error
- 400: 입력값 검증 실패
- 401: 로그인 필요
- 403: ACTIVE ADMIN 권한 없음

### 새소식 수정

권한: ACTIVE 상태의 ADMIN

작성자 일치 여부를 검사하지 않으며, 다른 관리자가 수정해도 최초 writer를 유지한다.

Request `PUT /api/admin/news/{newsId}`
```json
{
    "type": "PATCH_NOTE",
    "title": "수정된 제목",
    "content": "수정된 내용"
}
```

Response `200 OK`
```json
{
    "newsId": 1,
    "type": "PATCH_NOTE",
    "title": "수정된 제목",
    "content": "수정된 내용",
    "createdAt": "2026-07-29T14:30:00",
    "updatedAt": "2026-07-29T16:00:00"
}
```

Error
- 400: 입력값 검증 실패
- 401: 로그인 필요
- 403: ACTIVE ADMIN 권한 없음
- 404: 새소식 없음

### 새소식 삭제

권한: ACTIVE 상태의 ADMIN

작성자 일치 여부를 검사하지 않는다.

Request `DELETE /api/admin/news/{newsId}`

Response `204 No Content`
응답 본문 없음

Error
- 401: 로그인 필요
- 403: ACTIVE ADMIN 권한 없음
- 404: 새소식 없음

## 9. 관리자 게임 버전 API

### 게임 버전 등록
Request  `POST /api/admin/game-versions`
```json
{
    "version": "1.1.0",
    "title": "정식 업데이트",
    "description": "신규 콘텐츠가 추가되었습니다."
}
```

Response  `201 Created`
```json
{
    "gameVersionId": 2,
    "version": "1.1.0",
    "status": "DRAFT"
}
```

Error
- 400: 입력값 검증 실패
- 403: 관리자 권한 없음
- 409: 버전 번호 중복

### 관리자 게임 버전 목록

Request  `GET /api/admin/game-versions`

Query Parameters

| 이름     | 필수 | 기본값 | 설명                        |
| ------ | -: | --: | ------------------------- |
| page   |  N |   0 | 페이지 번호                    |
| size   |  N |  20 | 페이지 크기                    |
| status |  N |     | DRAFT, RELEASED, INACTIVE |

Response  `200 OK`
```json
{
    "content": [
        {
            "gameVersionId": 2,
            "version": "1.1.0",
            "title": "정식 업데이트",
            "status": "DRAFT",
            "fileRegistered": true,
            "releasedAt": null,
            "createdAt": "2026-07-29T14:30:00"
        }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
}
```

### 게임 버전 상세 조회

Request  `GET /api/admin/game-versions/{gameVersionId}`

Response  `200 OK`
```json
{
    "gameVersionId": 2,
    "version": "1.1.0",
    "title": "정식 업데이트",
    "description": "신규 콘텐츠가 추가되었습니다.",
    "status": "DRAFT",
    "releasedAt": null,
    "file": {
        "gameFileId": 1,
        "originalName": "Jexon_Setup_1.1.0.zip",
        "fileSize": 104857600,
        "checksum": "sha256-checksum-value",
        "createdAt": "2026-07-29T14:30:00"
    }
}
```

### 게임 버전 수정

Request  `PUT /api/admin/game-versions/{gameVersionId}`
```json
{
    "title": "수정된 버전 제목",
    "description": "수정된 설명"
}
```

Response  `200 OK`
```json
{
    "gameVersionId": 2,
    "updatedAt": "2026-07-29T16:00:00"
}
```

정책 : 버전 번호는 생성 후 변경하지 않는다.

### 게임 파일 업로드

Request  `POST /api/admin/game-versions/{gameVersionId}/file`

Content-Type:  `multipart/form-data`

Form Data:  `file: Jexon_Setup_1.1.0.zip`

Response  `201 Created`
```json
{
    "gameFileId": 1,
    "gameVersionId": 2,
    "originalName": "Jexon_Setup_1.1.0.zip",
    "fileSize": 104857600,
    "checksum": "sha256-checksum-value"
}
```

Error
- 400: 허용되지 않은 확장자
- 400: 파일 크기 초과
- 403: 관리자 권한 없음
- 404: 게임 버전 없음
- 409: 이미 파일이 등록된 버전
- 409: RELEASED 상태의 버전
- 500: 파일 저장 실패

### 최신 버전으로 배포

Request  `PATCH /api/admin/game-versions/{gameVersionId}/release`

Response  `200 OK`
```json
{
    "gameVersionId": 2,
    "version": "1.1.0",
    "status": "RELEASED",
    "releasedAt": "2026-07-29T16:00:00"
}
```

Error
- 403: 관리자 권한 없음
- 404: 게임 버전 없음
- 404: 연결 파일 없음
- 404: 실제 파일 없음
- 409: 이미 RELEASED 상태
- 409: 상태 전환 불가

### DRAFT 파일 삭제

Request  `DELETE /api/admin/game-versions/{gameVersionId}/file`

Response `204 No Content`

Error
- 403: 관리자 권한 없음
- 404: 버전 또는 파일 없음
- 409: RELEASED 상태 파일
- 409: 다운로드 이력 존재 
- 500: 실제 파일 삭제 실패

---
## 관리자 다운로드 통계 API

### 통계 요약

Request `GET /api/admin/download-statistics/summary`

Response  `200 OK`
```json
{
    "totalDownloads": 1500,
    "latestVersion": "1.1.0",
    "latestVersionDownloads": 500
}
```

### 버전별 통계

Request  `GET /api/admin/download-statistics/by-version`

Response `200 OK`
```json
[
    {
        "gameVersionId": 2,
        "version": "1.1.0",
        "status": "RELEASED",
        "downloadCount": 500
    },
    {
        "gameVersionId": 1,
        "version": "1.0.0",
        "status": "INACTIVE",
        "downloadCount": 1000
    }
]
```

### 일별 통계

Request  `GET /api/admin/download-statistics/daily`

Query Parameters

| 이름        | 필수 | 설명                 |
| --------- | -: | ------------------ |
| startDate |  N | 조회 시작일, YYYY-MM-DD |
| endDate   |  N | 조회 종료일, YYYY-MM-DD |

Response  `200 OK`
```json
[
    {
        "date": "2026-07-28",
        "downloadCount": 100
    },
    {
        "date": "2026-07-29",
        "downloadCount": 150
    }
]
```

Error
- 400: 날짜 형식 오류
- 400: 시작일이 종료일보다 늦음
- 403: 관리자 권한 없음

---

## 11. 구현 전 변경 가능 항목

다음 내용은 개발 과정에서 변경될 수 있다.

- 공통 응답 객체 사용 여부
- 로그인 실패 응답 코드
- 게시글 상세와 댓글 목록 응답 분리 여부
- 게임 버전 API 경로명
- 파일 최대 크기
- 다운로드 이력 저장 컬럼
- 통계 응답 구조
- 게시글 조회 수 증가 방식

변경된 내용은 실제 코드와 API 문서에 함께 반영한다.
