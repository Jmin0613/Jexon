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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GameFileDownloadServiceTest {
    private static final String STORAGE_KEY = "game-files/10/download.zip";

    @Mock GameVersionRepository gameVersionRepository;
    @Mock GameFileRepository gameFileRepository;
    @Mock FileStorage fileStorage;
    @InjectMocks GameFileDownloadService service;

    @Test
    void downloadsReleasedGameFileAsStream() throws Exception {
        GameVersion gameVersion = releasedGameVersion();
        GameFile gameFile = gameFile(gameVersion);
        byte[] content = "download content".getBytes(StandardCharsets.UTF_8);
        given(gameVersionRepository.findByStatus(GameVersionStatus.RELEASED)).willReturn(Optional.of(gameVersion));
        given(gameFileRepository.findByGameVersionId(10L)).willReturn(Optional.of(gameFile));
        given(fileStorage.exists(STORAGE_KEY)).willReturn(true);
        given(fileStorage.open(STORAGE_KEY)).willReturn(new ByteArrayInputStream(content));

        GameFileDownloadResponse response = service.downloadLatest();

        assertThat(response.originalFileName()).isEqualTo("jexon-v1.2.3.zip");
        assertThat(response.fileSize()).isEqualTo(1024L);
        assertThat(response.inputStream().readAllBytes()).isEqualTo(content);
        verify(fileStorage).open(STORAGE_KEY);
    }

    @Test
    void rejectsWhenReleasedGameVersionDoesNotExist() {
        given(gameVersionRepository.findByStatus(GameVersionStatus.RELEASED)).willReturn(Optional.empty());

        assertThatThrownBy(service::downloadLatest).isInstanceOf(GameVersionNotFoundException.class);
        verify(gameFileRepository, never()).findByGameVersionId(10L);
    }

    @Test
    void rejectsWhenReleasedGameFileMetadataDoesNotExist() {
        GameVersion gameVersion = releasedGameVersion();
        given(gameVersionRepository.findByStatus(GameVersionStatus.RELEASED)).willReturn(Optional.of(gameVersion));
        given(gameFileRepository.findByGameVersionId(10L)).willReturn(Optional.empty());

        assertThatThrownBy(service::downloadLatest).isInstanceOf(GameFileStateException.class);
        verify(fileStorage, never()).exists(STORAGE_KEY);
    }

    @Test
    void rejectsWhenPhysicalFileDoesNotExist() {
        GameVersion gameVersion = releasedGameVersion();
        given(gameVersionRepository.findByStatus(GameVersionStatus.RELEASED)).willReturn(Optional.of(gameVersion));
        given(gameFileRepository.findByGameVersionId(10L)).willReturn(Optional.of(gameFile(gameVersion)));
        given(fileStorage.exists(STORAGE_KEY)).willReturn(false);

        assertThatThrownBy(service::downloadLatest)
                .isInstanceOf(FileStorageException.class)
                .hasMessage("다운로드할 게임 파일이 존재하지 않습니다.");
        verify(fileStorage, never()).open(STORAGE_KEY);
    }

    private static GameVersion releasedGameVersion() {
        GameVersion gameVersion = GameVersion.createGameVersion(
                "v1.2.3", "Jexon 정식 출시 버전", "Jexon 게임 클라이언트 정식 출시 버전입니다."
        );
        ReflectionTestUtils.setField(gameVersion, "id", 10L);
        gameVersion.release(LocalDateTime.of(2026, 8, 14, 12, 0));
        return gameVersion;
    }

    private static GameFile gameFile(GameVersion gameVersion) {
        GameFile gameFile = GameFile.createGameFile(
                gameVersion, "jexon-v1.2.3.zip", STORAGE_KEY, "zip", "application/zip",
                1024L, "a".repeat(64)
        );
        ReflectionTestUtils.setField(gameFile, "id", 20L);
        return gameFile;
    }
}
