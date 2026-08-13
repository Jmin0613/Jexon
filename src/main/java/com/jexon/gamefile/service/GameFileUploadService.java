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
        validateActiveAdmin(memberId); // ADMIN + ACTIVE 검사
        validateUploadTarget(gameVersionId); // gameVersionId 존재 + DRAFT 상태 + 중복 업로드 검사

        // 파일 NULL 검사 + 빈 파일 검사
        if (file == null || file.isEmpty()) {
            throw new InvalidGameFileException("업로드할 파일이 필요합니다.");
        }

        // 파일명 가공 (파일명 정제 + 경로 제거 + 확장자 추출)
        SanitizedFileName sanitized = originalFileNameSanitizer.sanitize(file.getOriginalFilename());
        if (!ZIP_EXTENSION.equals(sanitized.extension())) { // only zip file
            throw new InvalidGameFileException("ZIP 파일만 업로드할 수 있습니다.");
        }

        // 클라이언트가 전달한 MIME Type을 보조 메타데이터 처리
        String contentType = normalizeContentType(file.getContentType()); // Content-Type: application/zip

        // 실제 저장소에서 사용할 논리 키 생성: game-files/{gameVersionId}/{UUID}.zip (저장 준비)
        String storageKey = storageKeyGenerator.generate(gameVersionId);

        StorageResult storageResult;

        // 스트림 열기
        try (InputStream originalInputStream = file.getInputStream();
             // stream의 앞부분을 검사해 실제 ZIP 형식인지 확인 + key 손상 복원
             InputStream restoredInputStream = zipSignatureValidator.validateAndRestore(originalInputStream)) {
            storageResult = fileStorage.store(storageKey, restoredInputStream); // 실제 파일 저장
            // storageResult → 저장 과정에서 fileSize와 checksum 받아옴
        } catch (IOException exception) {
            throw new InvalidGameFileException("업로드 파일을 읽을 수 없습니다.", exception);
        }

        // 수집한 메타데이터 → GameFile Entity로 조립해 db에 저장
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
            // 실패시 최종위치의 실제 파일 삭제 → db 불일치 보상
            compensateStoredFile(gameVersionId, storageKey, originalException);
            throw originalException;
        }
    }

    // helper 메서드 -------------------------------------------------------------------------------

    // ADMIN + ACTIVE 검증
    private void validateActiveAdmin(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GameVersionPermissionDeniedException(
                        "게임 파일을 업로드할 권한이 없습니다."
                ));

        if (member.getStatus() != MemberStatus.ACTIVE || member.getRole() != MemberRole.ADMIN) {
            throw new GameVersionPermissionDeniedException("게임 파일을 업로드할 권한이 없습니다.");
        }
    }

    // GameVersionId(업로드 대상) 검증
    private void validateUploadTarget(Long gameVersionId) {
        // 존재 확인
        GameVersion gameVersion = gameVersionRepository.findById(gameVersionId)
                .orElseThrow(GameVersionNotFoundException::new);

        // DRAFT 상태 확인
        if (gameVersion.getStatus() != GameVersionStatus.DRAFT) {
            throw new InvalidGameVersionStateException(
                    "DRAFT 상태의 게임 버전에만 파일을 업로드할 수 있습니다."
            );
        }

        // 해당 versionId로 File 존재하면 예외
        if (gameFileRepository.existsByGameVersionId(gameVersionId)) {
            throw new DuplicateGameFileException();
        }
    }

    // 메타데이터 저장용 contentType 검증 + 추출
    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return null;
        }
        if (contentType.length() > 255) {
            throw new InvalidGameFileException("파일 MIME Type은 255자 이하여야 합니다.");
        }
        return contentType;
    }

    // db 저장 실패 시 보상 → 최종 경로 file 삭제
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
