package com.jexon.downloadhistory.service;

import com.jexon.downloadhistory.domain.DownloadHistory;
import com.jexon.downloadhistory.repository.DownloadHistoryRepository;
import com.jexon.gamefile.domain.GameFile;
import com.jexon.gameversion.domain.GameVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DownloadHistoryService {
    private final DownloadHistoryRepository downloadHistoryRepository;

    // 다운로드 조회 트랜잭션과 분리된 별도 트랜잭션에서 이력 저장
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(GameVersion gameVersion, GameFile gameFile) {
        DownloadHistory downloadHistory = DownloadHistory.createDownloadHistory(
                gameVersion,
                gameFile
        );
        downloadHistoryRepository.saveAndFlush(downloadHistory);
    }
}
