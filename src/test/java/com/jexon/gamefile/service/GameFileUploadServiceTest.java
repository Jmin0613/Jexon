package com.jexon.gamefile.service;

import com.jexon.gamefile.domain.GameFile;
import com.jexon.gamefile.dto.response.GameFileUploadResponse;
import com.jexon.gamefile.exception.DuplicateGameFileException;
import com.jexon.gamefile.exception.FileStorageException;
import com.jexon.gamefile.exception.InvalidGameFileException;
import com.jexon.gamefile.repository.GameFileRepository;
import com.jexon.gamefile.storage.FileStorage;
import com.jexon.gamefile.storage.StorageKeyGenerator;
import com.jexon.gamefile.storage.StorageResult;
import com.jexon.gamefile.validation.OriginalFileNameSanitizer;
import com.jexon.gamefile.validation.SanitizedFileName;
import com.jexon.gamefile.validation.ZipSignatureValidator;
import com.jexon.gameversion.domain.GameVersion;
import com.jexon.gameversion.exception.GameVersionNotFoundException;
import com.jexon.gameversion.exception.GameVersionPermissionDeniedException;
import com.jexon.gameversion.exception.InvalidGameVersionStateException;
import com.jexon.gameversion.repository.GameVersionRepository;
import com.jexon.member.domain.Member;
import com.jexon.member.domain.MemberRole;
import com.jexon.member.domain.MemberStatus;
import com.jexon.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GameFileUploadServiceTest {
    private static final String STORAGE_KEY = "game-files/15/id.zip";
    private static final StorageResult STORAGE_RESULT = new StorageResult(
            STORAGE_KEY,
            4L,
            "a".repeat(64)
    );

    @Mock MemberRepository memberRepository;
    @Mock GameVersionRepository gameVersionRepository;
    @Mock GameFileRepository gameFileRepository;
    @Mock OriginalFileNameSanitizer originalFileNameSanitizer;
    @Mock ZipSignatureValidator zipSignatureValidator;
    @Mock StorageKeyGenerator storageKeyGenerator;
    @Mock FileStorage fileStorage;
    @Mock GameFilePersistenceService persistenceService;
    @InjectMocks GameFileUploadService service;

    private MultipartFile multipartFile;

    @BeforeEach
    void setUp() throws Exception {
        multipartFile = new MockMultipartFile(
                "file",
                "game.ZIP",
                "application/zip",
                new byte[]{0x50, 0x4B, 0x03, 0x04}
        );
    }

    @Test
    void uploadsZipThroughStorageAndPersistence() throws Exception {
        allowUploadChecks();
        GameFile gameFile = gameFile();
        given(persistenceService.save(
                15L, "game.ZIP", "zip", "application/zip", STORAGE_RESULT
        )).willReturn(gameFile);

        GameFileUploadResponse response = service.upload(1L, 15L, multipartFile);

        verify(fileStorage).store(eq(STORAGE_KEY), any(InputStream.class));
        verify(persistenceService).save(
                15L, "game.ZIP", "zip", "application/zip", STORAGE_RESULT
        );
        assertThat(response.getGameFileId()).isEqualTo(20L);
        assertThat(response.getGameVersionId()).isEqualTo(15L);
        assertThat(response.getOriginalFileName()).isEqualTo("game.ZIP");
        assertThat(response.getChecksum()).isEqualTo("a".repeat(64));
    }

    @Test
    void rejectsUserAndInactiveAdmin() {
        given(memberRepository.findById(1L)).willReturn(Optional.of(member(MemberStatus.ACTIVE, MemberRole.USER)));
        assertThatThrownBy(() -> service.upload(1L, 15L, multipartFile))
                .isInstanceOf(GameVersionPermissionDeniedException.class);

        given(memberRepository.findById(1L)).willReturn(Optional.of(member(MemberStatus.SUSPENDED, MemberRole.ADMIN)));
        assertThatThrownBy(() -> service.upload(1L, 15L, multipartFile))
                .isInstanceOf(GameVersionPermissionDeniedException.class);
        verify(gameVersionRepository, never()).findById(any());
    }

    @Test
    void rejectsMissingGameVersion() {
        allowAdmin();
        given(gameVersionRepository.findById(15L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.upload(1L, 15L, multipartFile))
                .isInstanceOf(GameVersionNotFoundException.class);
    }

    @Test
    void rejectsNonDraftAndExistingGameFile() {
        allowAdmin();
        GameVersion released = gameVersion();
        released.release(LocalDateTime.now());
        given(gameVersionRepository.findById(15L)).willReturn(Optional.of(released));
        assertThatThrownBy(() -> service.upload(1L, 15L, multipartFile))
                .isInstanceOf(InvalidGameVersionStateException.class);

        given(gameVersionRepository.findById(15L)).willReturn(Optional.of(gameVersion()));
        given(gameFileRepository.existsByGameVersionId(15L)).willReturn(true);
        assertThatThrownBy(() -> service.upload(1L, 15L, multipartFile))
                .isInstanceOf(DuplicateGameFileException.class);
    }

    @Test
    void rejectsNonZipExtensionAndInvalidSignature() throws Exception {
        allowTarget();
        given(originalFileNameSanitizer.sanitize("game.ZIP"))
                .willReturn(new SanitizedFileName("game.exe", "exe"));
        assertThatThrownBy(() -> service.upload(1L, 15L, multipartFile))
                .isInstanceOf(InvalidGameFileException.class);
        verify(storageKeyGenerator, never()).generate(any());

        given(originalFileNameSanitizer.sanitize("game.ZIP"))
                .willReturn(new SanitizedFileName("game.ZIP", "zip"));
        given(storageKeyGenerator.generate(15L)).willReturn(STORAGE_KEY);
        given(zipSignatureValidator.validateAndRestore(any(InputStream.class)))
                .willThrow(new InvalidGameFileException("invalid signature"));
        assertThatThrownBy(() -> service.upload(1L, 15L, multipartFile))
                .isInstanceOf(InvalidGameFileException.class);
        verify(fileStorage, never()).store(any(), any());
    }

    @Test
    void compensatesStoredFileWhenPersistenceFails() throws Exception {
        allowUploadChecks();
        given(persistenceService.save(any(), any(), any(), any(), any()))
                .willThrow(new DuplicateGameFileException());

        assertThatThrownBy(() -> service.upload(1L, 15L, multipartFile))
                .isInstanceOf(DuplicateGameFileException.class);
        verify(fileStorage).delete(STORAGE_KEY);
    }

    @Test
    void preservesOriginalFailureWhenCompensationDeleteFails() throws Exception {
        allowUploadChecks();
        DuplicateGameFileException original = new DuplicateGameFileException();
        FileStorageException deleteFailure = new FileStorageException("delete failed");
        given(persistenceService.save(any(), any(), any(), any(), any())).willThrow(original);
        doThrow(deleteFailure).when(fileStorage).delete(STORAGE_KEY);

        assertThatThrownBy(() -> service.upload(1L, 15L, multipartFile))
                .isSameAs(original)
                .satisfies(exception -> assertThat(exception.getSuppressed()).containsExactly(deleteFailure));
    }

    @Test
    void doesNotCompensateWhenStorageItselfFails() throws Exception {
        allowUploadChecksBeforeStorage();
        given(fileStorage.store(eq(STORAGE_KEY), any(InputStream.class)))
                .willThrow(new FileStorageException("store failed"));

        assertThatThrownBy(() -> service.upload(1L, 15L, multipartFile))
                .isInstanceOf(FileStorageException.class);
        verify(fileStorage, never()).delete(any());
        verify(persistenceService, never()).save(any(), any(), any(), any(), any());
    }

    private void allowUploadChecks() throws Exception {
        allowUploadChecksBeforeStorage();
        given(fileStorage.store(eq(STORAGE_KEY), any(InputStream.class))).willReturn(STORAGE_RESULT);
    }

    private void allowUploadChecksBeforeStorage() throws Exception {
        allowTarget();
        given(originalFileNameSanitizer.sanitize("game.ZIP"))
                .willReturn(new SanitizedFileName("game.ZIP", "zip"));
        given(storageKeyGenerator.generate(15L)).willReturn(STORAGE_KEY);
        given(zipSignatureValidator.validateAndRestore(any(InputStream.class)))
                .willReturn(new ByteArrayInputStream(new byte[]{0x50, 0x4B, 0x03, 0x04}));
    }

    private void allowTarget() {
        allowAdmin();
        given(gameVersionRepository.findById(15L)).willReturn(Optional.of(gameVersion()));
    }

    private void allowAdmin() {
        given(memberRepository.findById(1L))
                .willReturn(Optional.of(member(MemberStatus.ACTIVE, MemberRole.ADMIN)));
    }

    private static GameFile gameFile() {
        GameVersion gameVersion = gameVersion();
        GameFile gameFile = GameFile.createGameFile(
                gameVersion,
                "game.ZIP",
                STORAGE_KEY,
                "zip",
                "application/zip",
                4L,
                "a".repeat(64)
        );
        ReflectionTestUtils.setField(gameFile, "id", 20L);
        return gameFile;
    }

    private static GameVersion gameVersion() {
        GameVersion gameVersion = GameVersion.createGameVersion(
                "v1.0.0",
                "Jexon 정식 출시 버전",
                "Jexon 게임 클라이언트 정식 출시 버전입니다."
        );
        ReflectionTestUtils.setField(gameVersion, "id", 15L);
        return gameVersion;
    }

    private static Member member(MemberStatus status, MemberRole role) {
        Member member = Member.createMember(
                "admin",
                "encoded",
                "관리자닉네임",
                "admin@example.com",
                "관리자",
                "01000000000"
        );
        ReflectionTestUtils.setField(member, "id", 1L);
        ReflectionTestUtils.setField(member, "status", status);
        ReflectionTestUtils.setField(member, "role", role);
        return member;
    }
}
