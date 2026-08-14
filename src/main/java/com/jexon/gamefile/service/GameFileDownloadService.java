package com.jexon.gamefile.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameFileDownloadService {
    private final GameVersionRepository gameVersionRepository;
    private final GameFileRepository gameFileRepository;
    private final FileStorage fileStorage;

    // 사용자 GameFile 다운로드
    public GameFileDownloadResponse downloadLatest() {
        GameVersion gameVersion = gameVersionRepository.findByStatus(GameVersionStatus.RELEASED) // RELEASED 조회
                .orElseThrow(GameVersionNotFoundException::new);
        GameFile gameFile = gameFileRepository.findByGameVersionId(gameVersion.getId()) // GameFile 조회
                .orElseThrow(GameFileStateException::new);

        if (!fileStorage.exists(gameFile.getStorageKey())) { // 물리 파일 exists 확인
            throw new FileStorageException("다운로드할 게임 파일이 존재하지 않습니다.");
        }

        return new GameFileDownloadResponse(
                gameFile.getOriginalFileName(),
                gameFile.getFileSize(),
                fileStorage.open(gameFile.getStorageKey()) // open()
        );
    }
}
