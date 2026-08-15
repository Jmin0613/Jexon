package com.jexon.downloadhistory.service;

import com.jexon.downloadhistory.repository.DailyDownloadStatisticsProjection;
import com.jexon.downloadhistory.repository.DownloadHistoryRepository;
import com.jexon.downloadhistory.repository.VersionDownloadStatisticsProjection;
import com.jexon.gameversion.domain.GameVersionStatus;
import com.jexon.gameversion.exception.GameVersionPermissionDeniedException;
import com.jexon.member.domain.Member;
import com.jexon.member.domain.MemberRole;
import com.jexon.member.domain.MemberStatus;
import com.jexon.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DownloadStatisticsServiceTest {
    @Mock DownloadHistoryRepository downloadHistoryRepository;
    @Mock MemberRepository memberRepository;
    @Mock VersionDownloadStatisticsProjection versionProjection;
    @Mock DailyDownloadStatisticsProjection dailyProjection;

    DownloadStatisticsService service;

    @BeforeEach
    void setUp() {
        service = new DownloadStatisticsService(downloadHistoryRepository, memberRepository);
    }

    @Test
    void activeAdminCanGetSummaryIncludingZero() {
        allowAdmin();
        given(downloadHistoryRepository.count()).willReturn(7L);
        assertThat(service.getSummary(1L).getTotalDownloads()).isEqualTo(7L);

        given(downloadHistoryRepository.count()).willReturn(0L);
        assertThat(service.getSummary(1L).getTotalDownloads()).isZero();
    }

    @Test
    void inactiveAdminAndActiveUserAreDeniedBeforeAggregation() {
        assertDenied(member(MemberStatus.SUSPENDED, MemberRole.ADMIN));
        assertDenied(member(MemberStatus.ACTIVE, MemberRole.USER));
    }

    @Test
    void versionStatisticsIncludeInactiveAndKeepRepositoryOrder() {
        allowAdmin();
        VersionDownloadStatisticsProjection inactive = versionProjection;
        VersionDownloadStatisticsProjection released = org.mockito.Mockito.mock(VersionDownloadStatisticsProjection.class);
        given(inactive.getGameVersionId()).willReturn(2L);
        given(inactive.getVersion()).willReturn("v1.4.0");
        given(inactive.getStatus()).willReturn(GameVersionStatus.INACTIVE);
        given(inactive.getDownloadCount()).willReturn(350L);
        given(released.getGameVersionId()).willReturn(3L);
        given(released.getVersion()).willReturn("v1.5.0");
        given(released.getStatus()).willReturn(GameVersionStatus.RELEASED);
        given(released.getDownloadCount()).willReturn(530L);
        given(downloadHistoryRepository.findVersionDownloadStatistics())
                .willReturn(List.of(released, inactive));

        var result = service.getVersionStatistics(1L);

        assertThat(result).extracting("gameVersionId").containsExactly(3L, 2L);
        assertThat(result.get(1).getStatus()).isEqualTo(GameVersionStatus.INACTIVE);
        assertThat(result.get(0).getDownloadCount()).isEqualTo(530L);
    }

    @Test
    void dailyStatisticsKeepAscendingRepositoryOrderAndEmptyLists() {
        allowAdmin();
        DailyDownloadStatisticsProjection later = dailyProjection;
        DailyDownloadStatisticsProjection earlier = org.mockito.Mockito.mock(DailyDownloadStatisticsProjection.class);
        given(earlier.getDate()).willReturn(LocalDate.of(2026, 8, 13));
        given(earlier.getDownloadCount()).willReturn(25L);
        given(later.getDate()).willReturn(LocalDate.of(2026, 8, 14));
        given(later.getDownloadCount()).willReturn(42L);
        given(downloadHistoryRepository.findDailyDownloadStatistics())
                .willReturn(List.of(earlier, later));
        assertThat(service.getDailyStatistics(1L)).extracting("date")
                .containsExactly(LocalDate.of(2026, 8, 13), LocalDate.of(2026, 8, 14));

        given(downloadHistoryRepository.findVersionDownloadStatistics()).willReturn(List.of());
        given(downloadHistoryRepository.findDailyDownloadStatistics()).willReturn(List.of());
        assertThat(service.getVersionStatistics(1L)).isEmpty();
        assertThat(service.getDailyStatistics(1L)).isEmpty();
    }

    private void allowAdmin() {
        given(memberRepository.findById(1L))
                .willReturn(Optional.of(member(MemberStatus.ACTIVE, MemberRole.ADMIN)));
    }

    private void assertDenied(Member member) {
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        assertThatThrownBy(() -> service.getSummary(1L))
                .isInstanceOf(GameVersionPermissionDeniedException.class)
                .hasMessage("다운로드 통계를 조회할 권한이 없습니다.");
        verify(downloadHistoryRepository, never()).count();
    }

    private static Member member(MemberStatus status, MemberRole role) {
        Member member = Member.createMember(
                "admin", "encoded", "관리자닉네임", "admin@example.com",
                "관리자", "01000000000"
        );
        ReflectionTestUtils.setField(member, "id", 1L);
        ReflectionTestUtils.setField(member, "status", status);
        ReflectionTestUtils.setField(member, "role", role);
        return member;
    }
}
