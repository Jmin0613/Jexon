package com.jexon.gamefile.service;

import com.jexon.gamefile.domain.GameFile;
import com.jexon.gamefile.exception.DuplicateGameFileException;
import com.jexon.gamefile.repository.GameFileRepository;
import com.jexon.gamefile.storage.StorageResult;
import com.jexon.gameversion.domain.GameVersion;
import com.jexon.gameversion.domain.GameVersionStatus;
import com.jexon.gameversion.exception.GameVersionNotFoundException;
import com.jexon.gameversion.exception.InvalidGameVersionStateException;
import com.jexon.gameversion.repository.GameVersionRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GameFilePersistenceService {
    private static final String GAME_VERSION_UNIQUE_CONSTRAINT = "uk_game_file_game_version_id";

    private final GameVersionRepository gameVersionRepository;
    private final GameFileRepository gameFileRepository;

    @Transactional
    public GameFile save(
            Long gameVersionId,
            String originalFileName,
            String extension,
            String contentType,
            StorageResult storageResult
    ) {
        GameVersion gameVersion = gameVersionRepository.findById(gameVersionId)
                .orElseThrow(GameVersionNotFoundException::new);

        if (gameVersion.getStatus() != GameVersionStatus.DRAFT) {
            throw new InvalidGameVersionStateException(
                    "DRAFT 상태의 게임 버전에만 파일을 업로드할 수 있습니다."
            );
        }
        if (gameFileRepository.existsByGameVersionId(gameVersionId)) {
            throw new DuplicateGameFileException();
        }

        GameFile gameFile = GameFile.createGameFile(
                gameVersion,
                originalFileName,
                storageResult.storageKey(),
                extension,
                contentType,
                storageResult.fileSize(),
                storageResult.checksum()
        );

        try {
            return gameFileRepository.saveAndFlush(gameFile);
        } catch (DataIntegrityViolationException exception) {
            if (isGameVersionUniqueConstraintViolation(exception)) {
                throw new DuplicateGameFileException();
            }
            throw exception;
        }
    }

    private boolean isGameVersionUniqueConstraintViolation(DataIntegrityViolationException exception) {
        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolationException) {
                String constraintName = constraintViolationException.getConstraintName();
                return constraintName != null
                        && GAME_VERSION_UNIQUE_CONSTRAINT.equalsIgnoreCase(constraintName);
            }
            cause = cause.getCause();
        }

        return false;
    }
}
