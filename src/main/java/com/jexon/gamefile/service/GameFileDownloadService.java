package com.jexon.gamefile.service;

import com.jexon.downloadhistory.service.DownloadHistoryService;
import com.jexon.gamefile.domain.GameFile;
import com.jexon.gamefile.dto.response.GameFileDownloadResponse;
import com.jexon.gamefile.exception.FileStorageException;
import com.jexon.gamefile.exception.GameFileStateException;
import com.jexon.gamefile.repository.GameFileRepository;
import com.jexon.gamefile.storage.FileStorage;
import com.jexon.gameversion.domain.GameVersion;
import com.jexon.gameversion.domain.GameVersionStatus;
import com.jexon.gameversion.exception.GameVersionNotFoundException;
import com.jexon.gameversion.repository.GameVersionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameFileDownloadService {
    private static final Logger log = LoggerFactory.getLogger(GameFileDownloadService.class);

    private final GameVersionRepository gameVersionRepository;
    private final GameFileRepository gameFileRepository;
    private final FileStorage fileStorage;
    private final DownloadHistoryService downloadHistoryService;

    // 사용자 GameFile 다운로드
    public GameFileDownloadResponse downloadLatest() {
        GameVersion gameVersion = gameVersionRepository.findByStatus(GameVersionStatus.RELEASED) // RELEASED 조회
                .orElseThrow(GameVersionNotFoundException::new);
        GameFile gameFile = gameFileRepository.findByGameVersionId(gameVersion.getId()) // GameFile 조회
                .orElseThrow(GameFileStateException::new);

        if (!fileStorage.exists(gameFile.getStorageKey())) { // storageKey에 해당하는 실제 파일 존재 여부 확인
            throw new FileStorageException("다운로드할 게임 파일이 존재하지 않습니다.");
        }

        // 다운로드 응답에 필요한 파일명, 크기, InputStream 준비
        GameFileDownloadResponse response = new GameFileDownloadResponse(
                gameFile.getOriginalFileName(),
                gameFile.getFileSize(),
                fileStorage.open(gameFile.getStorageKey())
                // 파일명, 크기, 파일 스트림 전달
        );

        recordDownloadHistory(gameVersion, gameFile); // 다운로드 이력 저장 시도

        return response; // Controller에 다운로드 응답 데이터 전달
    }

    // 다운로드 이력 저장 실패가 실제 다운로드에 영향을 주지 않도록 분리
    private void recordDownloadHistory(GameVersion gameVersion, GameFile gameFile) {
        try {
            downloadHistoryService.record(gameVersion, gameFile);
        } catch (RuntimeException exception) {
            // 이력 저장 실패는 로그만 남기고 다운로드는 계속 진행
            log.warn(
                    "다운로드 이력 저장에 실패했습니다. gameVersionId={}, gameFileId={}",
                    gameVersion.getId(),
                    gameFile.getId(),
                    exception
            );
        }
    }
}
