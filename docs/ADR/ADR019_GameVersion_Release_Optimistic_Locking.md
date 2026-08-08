# ADR-019. GameVersion release 낙관적 락 및 단일 RELEASED 보장

## 상태

결정 완료

## 배경

최신 공개 버전의 유일한 기준은 `GameVersion.status == RELEASED`이며, 프로젝트 전체에서 RELEASED는 최대 하나만 존재해야 한다.

GameVersion 각 행의 `@Version`은 동일한 GameVersion 행에 대한 동시 수정과 release 충돌은 감지할 수 있다.
그러나 서로 다른 DRAFT 버전 A와 B를 동시에 release하면 두 트랜잭션이 서로 다른 행을 변경하므로 GameVersion의 `@Version`만으로는 충돌을 감지할 수 없다.

## 결정

- GameVersion에 `@Version`을 적용하여 동일 행의 수정 및 release 충돌을 감지한다.
- `id = 1`인 GameVersionReleaseControl singleton 행을 사용한다.
- 모든 release 요청은 같은 트랜잭션에서 ReleaseControl의 releaseSequence를 증가시킨다.
- GameVersionReleaseControl에도 `@Version`을 적용하여 서로 다른 GameVersion release가 공통 충돌 지점을 사용하도록 한다.
- release 전체 변경은 하나의 트랜잭션으로 처리한다.
- 낙관적 락 충돌은 자동 재시도하지 않고 409 Conflict로 반환한다.
- latest Boolean은 두지 않고 RELEASED 상태만 최신 버전의 기준으로 사용한다.

GameVersionReleaseControl은 현재 공개 버전 ID, version 문자열 또는 latest 여부를 저장하지 않는다.
releaseSequence도 버전 번호나 통계가 아니라 공통 행을 실제 변경하기 위한 동시성 토큰이다.

## 이유

- 관리자 release는 충돌 빈도가 낮아 비관적 락이나 분산 락보다 낙관적 락이 적합하다.
- 개별 행과 RELEASED 집합 수준의 동시성 책임을 분리할 수 있다.
- 별도의 latest 필드를 두지 않아 status와 최신 여부가 불일치하는 중복 상태를 방지한다.
- 충돌한 관리자가 최신 상태를 확인한 뒤 명시적으로 다시 요청하도록 하여 자동 retry로 인한 의도하지 않은 배포 순서 변경을 피한다.

## 검토한 대안

### GameVersion의 @Version만 사용

서로 다른 GameVersion 행을 동시에 release하는 경우 충돌하지 않아 선택하지 않았다.

### 비관적 락

release 충돌 빈도에 비해 DB 락 대기와 구현 복잡도가 커서 선택하지 않았다.

### Redis 분산 락

현재 단일 애플리케이션과 단일 DB 구조에 별도 인프라를 추가할 필요가 없어 선택하지 않았다.

### MySQL generated column과 UNIQUE 제약

DB 수준의 추가 방어선이 될 수 있지만 이번 구현 범위에는 포함하지 않았다.

## 테스트

Testcontainers와 MySQL 8.4를 사용하여 다음 DB 의존 동작을 검증한다.

- version UNIQUE 제약
- GameVersion `@Version` 충돌
- 서로 다른 GameVersion의 동시 release
- 충돌 트랜잭션 전체 rollback
- 최종 RELEASED 단일성
- 성공한 트랜잭션의 releaseSequence와 lockVersion만 반영되는지 여부

H2가 아니라 실제 MySQL을 사용하여 운영 DB와 같은 UNIQUE, 트랜잭션, flush 및 낙관적 락 동작을 확인한다.

## 결과

- 동일 GameVersion의 update/update, update/release 충돌을 감지한다.
- 서로 다른 GameVersion의 release/release 충돌을 공통 ReleaseControl 행에서 감지한다.
- 충돌 시 GameVersionConcurrencyConflictException을 통해 409 Conflict를 반환한다.
- 충돌한 release 트랜잭션의 상태 변경은 모두 rollback된다.
- 자동 재시도는 수행하지 않는다.
