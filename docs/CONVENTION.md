## Entity
- setter를 사용하지 않는다.
- 생성은 정적 팩토리 메서드 `createXxx()`를 사용한다.
- 수정은 의미가 드러나는 도메인 메서드 `update()` 등을 사용한다.

## DTO
- 도메인별 `dto/request`, `dto/response` 패키지로 분리한다.
- 응답 DTO는 단일 값 생성 시 `of()`, 엔티티 변환 시 `from()`을 사용한다.
- Entity를 API 응답으로 직접 반환하지 않는다.

## Repository
- ToOne 연관관계를 함께 조회할 때 `@EntityGraph`를 우선 검토한다.
- 필요한 경우 기본 `findById()`를 override할 수 있다.

## Service
- 클래스 기본 트랜잭션은 `@Transactional(readOnly = true)`로 설정한다.
- 쓰기 메서드에만 `@Transactional`을 적용한다.

## Exception
- 도메인별 사용자 정의 예외를 생성한다.
- 예외 응답은 공통 `ErrorResponse` 형식을 사용한다.

## Validation
- 요청 DTO에서 외부 입력을 1차 검증한다.
- Entity에서 핵심 불변조건을 방어적으로 다시 검증한다.
