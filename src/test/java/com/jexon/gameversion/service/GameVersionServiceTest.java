package com.jexon.gameversion.service;

import com.jexon.gamefile.repository.GameFileRepository;
import com.jexon.gameversion.domain.*;
import com.jexon.gameversion.dto.request.GameVersionCreateRequest;
import com.jexon.gameversion.dto.request.GameVersionUpdateRequest;
import com.jexon.gameversion.dto.response.*;
import com.jexon.gameversion.exception.*;
import com.jexon.gameversion.repository.GameVersionReleaseControlRepository;
import com.jexon.gameversion.repository.GameVersionRepository;
import com.jexon.member.domain.*;
import com.jexon.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameVersionServiceTest {
    private static final String TITLE = "Jexon 정식 출시 버전";
    private static final String DESCRIPTION = "Jexon 게임 클라이언트 정식 출시 버전입니다.";

    @Mock GameVersionRepository gameVersionRepository;
    @Mock GameVersionReleaseControlRepository releaseControlRepository;
    @Mock MemberRepository memberRepository;
    @Mock GameFileRepository gameFileRepository;
    @Mock GameVersionCreateRequest createRequest;
    @Mock GameVersionUpdateRequest updateRequest;
    @InjectMocks GameVersionService service;

    @Test
    void createByActiveAdmin() {
        allowAdmin();
        given(createRequest.getVersion()).willReturn("v1.0.0");
        given(createRequest.getTitle()).willReturn(TITLE);
        given(createRequest.getDescription()).willReturn(DESCRIPTION);
        doAnswer(invocation -> {
            GameVersion saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 10L);
            return saved;
        })
                .when(gameVersionRepository).saveAndFlush(any(GameVersion.class));

        GameVersionCreateResponse response = service.create(1L, createRequest);

        ArgumentCaptor<GameVersion> captor = ArgumentCaptor.forClass(GameVersion.class);
        verify(gameVersionRepository).existsByVersion("v1.0.0");
        verify(gameVersionRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue()).extracting(GameVersion::getVersion, GameVersion::getTitle,
                GameVersion::getDescription, GameVersion::getStatus)
                .containsExactly("v1.0.0", TITLE, DESCRIPTION, GameVersionStatus.DRAFT);
        assertThat(response.getGameVersionId()).isEqualTo(10L);
    }

    @Test
    void rejectCreateForEveryUnauthorizedMemberState() {
        assertCreateDenied(null);
        assertCreateDenied(member(MemberStatus.SUSPENDED, MemberRole.ADMIN));
        assertCreateDenied(member(MemberStatus.WITHDRAWN, MemberRole.ADMIN));
        assertCreateDenied(member(MemberStatus.ACTIVE, MemberRole.USER));
    }

    @Test
    void rejectDuplicateBeforeSaving() {
        allowAdmin();
        given(createRequest.getVersion()).willReturn("v1.0.0");
        given(gameVersionRepository.existsByVersion("v1.0.0")).willReturn(true);
        assertThatThrownBy(() -> service.create(1L, createRequest)).isInstanceOf(DuplicateGameVersionException.class);
        verify(gameVersionRepository, never()).saveAndFlush(any());
    }

    @Test
    void listWithoutStatusAppliesMaximumAndStableSort() {
        allowAdmin();
        GameVersion entity = gameVersion(10L, "v1.0.0");
        given(gameVersionRepository.findAll(any(Pageable.class))).willReturn(new PageImpl<>(List.of(entity)));
        Page<GameVersionListResponse> result = service.getAdminGameVersions(1L, null,
                PageRequest.of(3, 500, Sort.by("title")));
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(gameVersionRepository).findAll(captor.capture());
        Pageable applied = captor.getValue();
        assertThat(applied.getPageNumber()).isEqualTo(3);
        assertThat(applied.getPageSize()).isEqualTo(100);
        assertThat(applied.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(applied.getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(applied.getSort().getOrderFor("title")).isNull();
        assertThat(result.getContent().getFirst().getGameVersionId()).isEqualTo(10L);
    }

    @Test
    void listWithStatusKeepsAllowedSize() {
        allowAdmin();
        given(gameVersionRepository.findAllByStatus(eq(GameVersionStatus.DRAFT), any(Pageable.class))).willReturn(Page.empty());
        service.getAdminGameVersions(1L, GameVersionStatus.DRAFT, PageRequest.of(2, 30));
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(gameVersionRepository).findAllByStatus(eq(GameVersionStatus.DRAFT), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(30);
    }

    @Test
    void getDetailAfterPermissionAndRejectMissing() {
        allowAdmin();
        GameVersion entity = gameVersion(10L, "v1.0.0");
        given(gameVersionRepository.findById(10L)).willReturn(Optional.of(entity));
        assertThat(service.getAdminGameVersion(1L, 10L).getVersion()).isEqualTo("v1.0.0");
        given(gameVersionRepository.findById(20L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.getAdminGameVersion(1L, 20L)).isInstanceOf(GameVersionNotFoundException.class);
    }

    @Test
    void permissionFailureStopsDetailLookup() {
        given(memberRepository.findById(1L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.getAdminGameVersion(1L, 10L))
                .isInstanceOf(GameVersionPermissionDeniedException.class)
                .hasMessage("게임 버전을 조회할 권한이 없습니다.");
        verify(gameVersionRepository, never()).findById(anyLong());
    }

    @Test
    void updateMergesNullFieldsFlushesAndDoesNotSave() {
        allowAdmin();
        GameVersion entity = gameVersion(10L, "v1.0.0");
        given(gameVersionRepository.findById(10L)).willReturn(Optional.of(entity));
        given(updateRequest.getTitle()).willReturn("수정된 게임 버전 제목");
        given(updateRequest.getDescription()).willReturn(null);
        GameVersionDetailResponse response = service.update(1L, 10L, updateRequest);
        assertThat(response.getTitle()).isEqualTo("수정된 게임 버전 제목");
        assertThat(response.getDescription()).isEqualTo(DESCRIPTION);
        verify(gameVersionRepository).flush();
        verify(gameVersionRepository, never()).save(any());
    }

    @Test
    void convertUpdateOptimisticConflict() {
        allowAdmin();
        given(gameVersionRepository.findById(10L)).willReturn(Optional.of(gameVersion(10L, "v1.0.0")));
        given(updateRequest.getTitle()).willReturn("수정된 게임 버전 제목");
        doThrow(new OptimisticLockingFailureException("conflict")).when(gameVersionRepository).flush();
        assertThatThrownBy(() -> service.update(1L, 10L, updateRequest))
                .isInstanceOf(GameVersionConcurrencyConflictException.class)
                .hasMessage("다른 관리자가 게임 버전 정보를 변경했습니다. 최신 상태를 확인한 후 다시 시도해주세요.");
    }

    @Test
    void releaseDraftWithoutExistingRelease() {
        allowAdmin();
        GameVersionReleaseControl control = GameVersionReleaseControl.create();
        GameVersion target = gameVersion(10L, "v1.0.0");
        given(releaseControlRepository.findById(1L)).willReturn(Optional.of(control));
        given(gameVersionRepository.findById(10L)).willReturn(Optional.of(target));
        given(gameFileRepository.existsByGameVersionId(10L)).willReturn(true);
        given(gameVersionRepository.findByStatus(GameVersionStatus.RELEASED)).willReturn(Optional.empty());
        GameVersionReleaseResponse response = service.release(1L, 10L);
        assertThat(control.getReleaseSequence()).isEqualTo(1L);
        assertThat(target.getStatus()).isEqualTo(GameVersionStatus.RELEASED);
        assertThat(response.getReleasedAt()).isNotNull();
        verify(gameVersionRepository).flush();
    }

    @Test
    void releaseReplacesExistingAndKeepsItsTime() {
        allowAdmin();
        GameVersionReleaseControl control = GameVersionReleaseControl.create();
        GameVersion target = gameVersion(10L, "v2.0.0");
        GameVersion existing = gameVersion(20L, "v1.0.0");
        LocalDateTime oldTime = LocalDateTime.of(2026, 8, 1, 12, 0);
        existing.release(oldTime);
        given(releaseControlRepository.findById(1L)).willReturn(Optional.of(control));
        given(gameVersionRepository.findById(10L)).willReturn(Optional.of(target));
        given(gameFileRepository.existsByGameVersionId(10L)).willReturn(true);
        given(gameVersionRepository.findByStatus(GameVersionStatus.RELEASED)).willReturn(Optional.of(existing));
        service.release(1L, 10L);
        assertThat(existing.getStatus()).isEqualTo(GameVersionStatus.INACTIVE);
        assertThat(existing.getReleasedAt()).isEqualTo(oldTime);
        assertThat(target.getStatus()).isEqualTo(GameVersionStatus.RELEASED);
    }

    @Test
    void rejectAlreadyReleasedBeforeExistingLookupAndFlush() {
        allowAdmin();
        GameVersion target = gameVersion(10L, "v1.0.0");
        target.release(LocalDateTime.now());
        given(releaseControlRepository.findById(1L)).willReturn(Optional.of(GameVersionReleaseControl.create()));
        given(gameVersionRepository.findById(10L)).willReturn(Optional.of(target));
        assertThatThrownBy(() -> service.release(1L, 10L)).isInstanceOf(InvalidGameVersionStateException.class);
        verify(gameVersionRepository, never()).findByStatus(any());
        verify(gameVersionRepository, never()).flush();
    }

    @Test
    void rejectReleaseWithoutGameFile() {
        allowAdmin();
        given(releaseControlRepository.findById(1L))
                .willReturn(Optional.of(GameVersionReleaseControl.create()));
        given(gameVersionRepository.findById(10L))
                .willReturn(Optional.of(gameVersion(10L, "v1.0.0")));
        given(gameFileRepository.existsByGameVersionId(10L)).willReturn(false);

        assertThatThrownBy(() -> service.release(1L, 10L))
                .isInstanceOf(InvalidGameVersionStateException.class)
                .hasMessage("게임 파일이 등록된 게임 버전만 공개할 수 있습니다.");
        verify(gameVersionRepository, never()).findByStatus(any());
        verify(gameVersionRepository, never()).flush();
    }

    @Test
    void rejectMissingControlAndConvertReleaseConflict() {
        allowAdmin();
        given(releaseControlRepository.findById(1L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.release(1L, 10L)).isInstanceOf(IllegalStateException.class)
                .hasMessage("게임 버전 배포 제어 정보가 초기화되지 않았습니다.");
        given(releaseControlRepository.findById(1L)).willReturn(Optional.of(GameVersionReleaseControl.create()));
        given(gameVersionRepository.findById(10L)).willReturn(Optional.of(gameVersion(10L, "v1.0.0")));
        given(gameFileRepository.existsByGameVersionId(10L)).willReturn(true);
        given(gameVersionRepository.findByStatus(GameVersionStatus.RELEASED)).willReturn(Optional.empty());
        doThrow(new OptimisticLockingFailureException("conflict")).when(gameVersionRepository).flush();
        assertThatThrownBy(() -> service.release(1L, 10L)).isInstanceOf(GameVersionConcurrencyConflictException.class);
        verify(gameVersionRepository, times(1)).flush();
    }

    private void allowAdmin() { given(memberRepository.findById(1L)).willReturn(Optional.of(member(MemberStatus.ACTIVE, MemberRole.ADMIN))); }
    private void assertCreateDenied(Member member) {
        given(memberRepository.findById(1L)).willReturn(Optional.ofNullable(member));
        assertThatThrownBy(() -> service.create(1L, createRequest)).isInstanceOf(GameVersionPermissionDeniedException.class)
                .hasMessage("게임 버전을 등록할 권한이 없습니다.");
        verify(gameVersionRepository, never()).saveAndFlush(any());
        clearInvocations(gameVersionRepository, memberRepository);
    }
    private static Member member(MemberStatus status, MemberRole role) {
        Member member = Member.createMember("admin", "encoded", "관리자닉네임", "admin@example.com", "관리자", "01000000000");
        ReflectionTestUtils.setField(member, "id", 1L); ReflectionTestUtils.setField(member, "status", status); ReflectionTestUtils.setField(member, "role", role); return member;
    }
    private static GameVersion gameVersion(Long id, String version) {
        GameVersion entity = GameVersion.createGameVersion(version, TITLE, DESCRIPTION);
        ReflectionTestUtils.setField(entity, "id", id);
        ReflectionTestUtils.setField(entity, "createdAt", LocalDateTime.of(2026, 8, 8, 12, 0));
        ReflectionTestUtils.setField(entity, "updatedAt", LocalDateTime.of(2026, 8, 8, 12, 0));
        return entity;
    }
}
