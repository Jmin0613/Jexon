package com.jexon.downloadhistory.controller;

import com.jexon.auth.principal.CustomUserDetails;
import com.jexon.downloadhistory.dto.response.DailyDownloadStatisticsResponse;
import com.jexon.downloadhistory.dto.response.DownloadSummaryResponse;
import com.jexon.downloadhistory.dto.response.VersionDownloadStatisticsResponse;
import com.jexon.downloadhistory.service.DownloadStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/download-statistics")
public class AdminDownloadStatisticsController {
    private final DownloadStatisticsService downloadStatisticsService;

    @GetMapping("/summary")
    public ResponseEntity<DownloadSummaryResponse> getSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                downloadStatisticsService.getSummary(userDetails.getMemberId())
        );
    }

    @GetMapping("/versions")
    public ResponseEntity<List<VersionDownloadStatisticsResponse>> getVersionStatistics(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                downloadStatisticsService.getVersionStatistics(userDetails.getMemberId())
        );
    }

    @GetMapping("/daily")
    public ResponseEntity<List<DailyDownloadStatisticsResponse>> getDailyStatistics(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                downloadStatisticsService.getDailyStatistics(userDetails.getMemberId())
        );
    }
}
