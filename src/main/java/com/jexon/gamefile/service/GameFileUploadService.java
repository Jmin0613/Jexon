package com.jexon.gamefile.service;

import com.jexon.gamefile.domain.GameFile;
import com.jexon.gamefile.dto.response.GameFileUploadResponse;
import com.jexon.gamefile.exception.DuplicateGameFileException;
import com.jexon.gamefile.exception.InvalidGameFileException;
import com.jexon.gamefile.repository.GameFileRepository;
import com.jexon.gamefile.storage.FileStorage;
import com.jexon.gamefile.storage.StorageKeyGenerator;
import com.jexon.gamefile.storage.StorageResult;
import com.jexon.gamefile.validation.OriginalFileNameSanitizer;
import com.jexon.gamefile.validation.SanitizedFileName;
import com.jexon.gamefile.validation.ZipSignatureValidator;
import com.jexon.gameversion.domain.GameVersion;
import com.jexon.gameversion.domain.GameVersionStatus;
import com.jexon.gameversion.exception.GameVersionNotFoundException;
import com.jexon.gameversion.exception.GameVersionPermissionDeniedException;
import com.jexon.gameversion.exception.InvalidGameVersionStateException;
import com.jexon.gameversion.repository.GameVersionRepository;
import com.jexon.member.domain.Member;
import com.jexon.member.domain.MemberRole;
import com.jexon.member.domain.MemberStatus;
import com.jexon.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class GameFileUploadService {
    private static final String ZIP_EXTENSION = "zip";
    private static final Logger log = LoggerFactory.getLogger(GameFileUploadService.class);

    private final MemberRepository memberRepository;
    private final GameVersionRepository gameVersionRepository;
    private final GameFileRepository gameFileRepository;
    private final OriginalFileNameSanitizer originalFileNameSanitizer;
    private final ZipSignatureValidator zipSignatureValidator;
    private final StorageKeyGenerator storageKeyGenerator;
    private final FileStorage fileStorage;
    private final GameFilePersistenceService persistenceService;

    public GameFileUploadResponse upload(
            Long memberId,
            Long gameVersionId,
            MultipartFile file
    ) {
        validateActiveAdmin(memberId);
        validateUploadTarget(gameVersionId);

        if (file == null || file.isEmpty()) {
            throw new InvalidGameFileException("업로드할 파일이 필요합니다.");
        }

        SanitizedFileName sanitized = originalFileNameSanitizer.sanitize(file.getOriginalFilename());
        if (!ZIP_EXTENSION.equals(sanitized.extension())) {
            throw new InvalidGameFileException("ZIP 파일만 업로드할 수 있습니다.");
        }

        String contentType = normalizeContentType(file.getContentType());
        String storageKey = storageKeyGenerator.generate(gameVersionId);
        StorageResult storageResult;

        try (InputStream originalInputStream = file.getInputStream();
             InputStream restoredInputStream = zipSignatureValidator.validateAndRestore(originalInputStream)) {
            storageResult = fileStorage.store(storageKey, restoredInputStream);
        } catch (IOException exception) {
            throw new InvalidGameFileException("업로드 파일을 읽을 수 없습니다.", exception);
        }

        try {
            GameFile gameFile = persistenceService.save(
                    gameVersionId,
                    sanitized.sanitizedFileName(),
                    sanitized.extension(),
                    contentType,
                    storageResult
            );
            return GameFileUploadResponse.from(gameFile);
        } catch (RuntimeException originalException) {
            compensateStoredFile(gameVersionId, storageKey, originalException);
            throw originalException;
        }
    }

    private void validateActiveAdmin(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GameVersionPermissionDeniedException(
                        "게임 파일을 업로드할 권한이 없습니다."
                ));

        if (member.getStatus() != MemberStatus.ACTIVE || member.getRole() != MemberRole.ADMIN) {
            throw new GameVersionPermissionDeniedException("게임 파일을 업로드할 권한이 없습니다.");
        }
    }

    private void validateUploadTarget(Long gameVersionId) {
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
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return null;
        }
        if (contentType.length() > 255) {
            throw new InvalidGameFileException("파일 MIME Type은 255자 이하여야 합니다.");
        }
        return contentType;
    }

    private void compensateStoredFile(
            Long gameVersionId,
            String storageKey,
            RuntimeException originalException
    ) {
        try {
            fileStorage.delete(storageKey);
        } catch (RuntimeException deleteException) {
            originalException.addSuppressed(deleteException);
            log.error(
                    "게임 파일 보상 삭제에 실패했습니다. gameVersionId={}, storageKey={}",
                    gameVersionId,
                    storageKey,
                    deleteException
            );
        }
    }
}
