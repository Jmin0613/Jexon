package com.jexon.gameversion.service;

import com.jexon.gameversion.domain.GameVersion;
import com.jexon.gameversion.domain.GameVersionReleaseControl;
import com.jexon.gameversion.domain.GameVersionStatus;
import com.jexon.gameversion.dto.request.GameVersionCreateRequest;
import com.jexon.gameversion.dto.request.GameVersionUpdateRequest;
import com.jexon.gameversion.dto.response.GameVersionCreateResponse;
import com.jexon.gameversion.dto.response.GameVersionDetailResponse;
import com.jexon.gameversion.dto.response.GameVersionListResponse;
import com.jexon.gameversion.dto.response.GameVersionReleaseResponse;
import com.jexon.gameversion.exception.DuplicateGameVersionException;
import com.jexon.gameversion.exception.GameVersionConcurrencyConflictException;
import com.jexon.gameversion.exception.GameVersionNotFoundException;
import com.jexon.gameversion.exception.GameVersionPermissionDeniedException;
import com.jexon.gameversion.exception.InvalidGameVersionStateException;
import com.jexon.gameversion.repository.GameVersionReleaseControlRepository;
import com.jexon.gameversion.repository.GameVersionRepository;
import com.jexon.member.domain.Member;
import com.jexon.member.domain.MemberRole;
import com.jexon.member.domain.MemberStatus;
import com.jexon.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameVersionService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final String VERSION_UNIQUE_CONSTRAINT = "uk_game_version_version";

    private final GameVersionRepository gameVersionRepository;
    private final GameVersionReleaseControlRepository releaseControlRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public GameVersionCreateResponse create(
            Long memberId,
            GameVersionCreateRequest request
    ) {
        findActiveAdmin(memberId, "게임 버전을 등록할 권한이 없습니다.");

        if (gameVersionRepository.existsByVersion(request.getVersion())) {
            throw new DuplicateGameVersionException();
        }

        GameVersion gameVersion = GameVersion.createGameVersion(
                request.getVersion(),
                request.getTitle(),
                request.getDescription()
        );

        try {
            gameVersionRepository.saveAndFlush(gameVersion);
        } catch (DataIntegrityViolationException exception) {
            if (isVersionUniqueConstraintViolation(exception)) {
                throw new DuplicateGameVersionException();
            }
            throw exception;
        }

        return GameVersionCreateResponse.from(gameVersion);
    }

    public Page<GameVersionListResponse> getAdminGameVersions(
            Long memberId,
            GameVersionStatus status,
            Pageable pageable
    ) {
        findActiveAdmin(memberId, "게임 버전을 조회할 권한이 없습니다.");
        Pageable adminPageable = createAdminPageable(pageable);

        Page<GameVersion> gameVersions = status == null
                ? gameVersionRepository.findAll(adminPageable)
                : gameVersionRepository.findAllByStatus(status, adminPageable);

        return gameVersions.map(GameVersionListResponse::from);
    }

    public GameVersionDetailResponse getAdminGameVersion(
            Long memberId,
            Long gameVersionId
    ) {
        findActiveAdmin(memberId, "게임 버전을 조회할 권한이 없습니다.");
        GameVersion gameVersion = findGameVersion(gameVersionId);

        return GameVersionDetailResponse.from(gameVersion);
    }

    @Transactional
    public GameVersionDetailResponse update(
            Long memberId,
            Long gameVersionId,
            GameVersionUpdateRequest request
    ) {
        findActiveAdmin(memberId, "게임 버전을 수정할 권한이 없습니다.");
        GameVersion gameVersion = findGameVersion(gameVersionId);

        String title = request.getTitle() != null
                ? request.getTitle()
                : gameVersion.getTitle();
        String description = request.getDescription() != null
                ? request.getDescription()
                : gameVersion.getDescription();

        try {
            gameVersion.updateDetails(title, description);
            gameVersionRepository.flush();
        } catch (OptimisticLockingFailureException exception) {
            throw new GameVersionConcurrencyConflictException();
        }

        return GameVersionDetailResponse.from(gameVersion);
    }

    @Transactional
    public GameVersionReleaseResponse release(Long memberId, Long gameVersionId) {
        findActiveAdmin(memberId, "게임 버전을 배포할 권한이 없습니다.");

        try {
            GameVersionReleaseControl releaseControl = findReleaseControl();
            releaseControl.advanceReleaseSequence(); // releaseControl 객체 값 변경

            GameVersion target = findGameVersion(gameVersionId);
            validateReleasableStatus(target);

            // 기존 버전 비활성화 → JPA 장바구니에 기존 GameVersion 변경
            gameVersionRepository.findByStatus(GameVersionStatus.RELEASED)
                    .ifPresent(GameVersion::deactivateForReplacement);

            LocalDateTime releasedAt = LocalDateTime.now();
            target.release(releasedAt); // 새 버전을 release 상태로 변경

            // 영속성 컨텍스트 안에 있는 모든 번경사항 db로 flush
            gameVersionRepository.flush();
            // ReleaseControl 변경
            // 기존 RELEASED 변경
            // 대상 GameVersion 변경

            return GameVersionReleaseResponse.from(target);
        } catch (OptimisticLockingFailureException exception) {
            throw new GameVersionConcurrencyConflictException();
        }
    }

    private Member findActiveAdmin(Long memberId, String deniedMessage) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GameVersionPermissionDeniedException(deniedMessage));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new GameVersionPermissionDeniedException(deniedMessage);
        }
        if (member.getRole() != MemberRole.ADMIN) {
            throw new GameVersionPermissionDeniedException(deniedMessage);
        }

        return member;
    }

    private GameVersion findGameVersion(Long gameVersionId) {
        return gameVersionRepository.findById(gameVersionId)
                .orElseThrow(GameVersionNotFoundException::new);
    }

    private GameVersionReleaseControl findReleaseControl() {
        return releaseControlRepository.findById(GameVersionReleaseControl.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("게임 버전 배포 제어 정보가 초기화되지 않았습니다."));
    }

    private Pageable createAdminPageable(Pageable pageable) {
        int pageSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        Sort sort = Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id")
        );

        return PageRequest.of(pageable.getPageNumber(), pageSize, sort);
    }

    private void validateReleasableStatus(GameVersion gameVersion) {
        if (gameVersion.getStatus() != GameVersionStatus.DRAFT
                && gameVersion.getStatus() != GameVersionStatus.INACTIVE) {
            throw new InvalidGameVersionStateException(
                    "DRAFT 또는 INACTIVE 상태의 게임 버전만 공개할 수 있습니다."
            );
        }
    }

    private boolean isVersionUniqueConstraintViolation(DataIntegrityViolationException exception) {
        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolationException) {
                String constraintName = constraintViolationException.getConstraintName();
                return constraintName != null
                        && VERSION_UNIQUE_CONSTRAINT.equalsIgnoreCase(constraintName);
            }
            cause = cause.getCause();
        }

        return false;
    }
}
