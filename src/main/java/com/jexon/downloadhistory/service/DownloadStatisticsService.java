package com.jexon.downloadhistory.service;

import com.jexon.downloadhistory.dto.response.DailyDownloadStatisticsResponse;
import com.jexon.downloadhistory.dto.response.DownloadSummaryResponse;
import com.jexon.downloadhistory.dto.response.VersionDownloadStatisticsResponse;
import com.jexon.downloadhistory.repository.DownloadHistoryRepository;
import com.jexon.gameversion.exception.GameVersionPermissionDeniedException;
import com.jexon.member.domain.Member;
import com.jexon.member.domain.MemberRole;
import com.jexon.member.domain.MemberStatus;
import com.jexon.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DownloadStatisticsService {
    private static final String DENIED_MESSAGE = "다운로드 통계를 조회할 권한이 없습니다.";

    private final DownloadHistoryRepository downloadHistoryRepository;
    private final MemberRepository memberRepository;

    public DownloadSummaryResponse getSummary(Long memberId) {
        validateActiveAdmin(memberId);
        return DownloadSummaryResponse.of(downloadHistoryRepository.count());
    }

    public List<VersionDownloadStatisticsResponse> getVersionStatistics(Long memberId) {
        validateActiveAdmin(memberId);
        return downloadHistoryRepository.findVersionDownloadStatistics().stream()
                .map(VersionDownloadStatisticsResponse::from)
                .toList();
    }

    public List<DailyDownloadStatisticsResponse> getDailyStatistics(Long memberId) {
        validateActiveAdmin(memberId);
        return downloadHistoryRepository.findDailyDownloadStatistics().stream()
                .map(DailyDownloadStatisticsResponse::from)
                .toList();
    }

    private void validateActiveAdmin(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GameVersionPermissionDeniedException(DENIED_MESSAGE));

        if (member.getStatus() != MemberStatus.ACTIVE || member.getRole() != MemberRole.ADMIN) {
            throw new GameVersionPermissionDeniedException(DENIED_MESSAGE);
        }
    }
}
