package com.jexon.gameversion.service;

import com.jexon.gamefile.domain.GameFile;
import com.jexon.gamefile.exception.GameFileStateException;
import com.jexon.gamefile.repository.GameFileRepository;
import com.jexon.gameversion.domain.GameVersion;
import com.jexon.gameversion.domain.GameVersionStatus;
import com.jexon.gameversion.dto.response.LatestGameVersionResponse;
import com.jexon.gameversion.exception.GameVersionNotFoundException;
import com.jexon.gameversion.repository.GameVersionReleaseControlRepository;
import com.jexon.gameversion.repository.GameVersionRepository;
import com.jexon.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LatestGameVersionServiceTest {
    @Mock GameVersionRepository gameVersionRepository;
    @Mock GameVersionReleaseControlRepository releaseControlRepository;
    @Mock MemberRepository memberRepository;
    @Mock GameFileRepository gameFileRepository;
    @InjectMocks GameVersionService service;

    @Test
    void getLatestReleasedGameVersionWithGameFile() {
        GameVersion gameVersion = releasedGameVersion();
        GameFile gameFile = gameFile(gameVersion);
        given(gameVersionRepository.findByStatus(GameVersionStatus.RELEASED))
                .willReturn(Optional.of(gameVersion));
        given(gameFileRepository.findByGameVersionId(10L)).willReturn(Optional.of(gameFile));

        LatestGameVersionResponse response = service.getLatestGameVersion();

        assertThat(response)
                .extracting(
                        LatestGameVersionResponse::getGameVersionId,
                        LatestGameVersionResponse::getVersion,
                        LatestGameVersionResponse::getTitle,
                        LatestGameVersionResponse::getDescription,
                        LatestGameVersionResponse::getReleasedAt,
                        LatestGameVersionResponse::getGameFileId,
                        LatestGameVersionResponse::getOriginalFileName,
                        LatestGameVersionResponse::getFileSize,
                        LatestGameVersionResponse::getChecksum
                )
                .containsExactly(
                        10L, "v1.2.3", "Jexon 정식 출시 버전", "Jexon 게임 클라이언트 정식 출시 버전입니다.",
                        LocalDateTime.of(2026, 8, 14, 12, 0), 20L, "jexon-v1.2.3.zip", 1024L, "a".repeat(64)
                );
    }

    @Test
    void rejectWhenReleasedGameVersionDoesNotExist() {
        given(gameVersionRepository.findByStatus(GameVersionStatus.RELEASED)).willReturn(Optional.empty());

        assertThatThrownBy(service::getLatestGameVersion)
                .isInstanceOf(GameVersionNotFoundException.class);
        verify(gameFileRepository, never()).findByGameVersionId(10L);
    }

    @Test
    void rejectInvalidReleasedStateWithoutGameFile() {
        GameVersion gameVersion = releasedGameVersion();
        given(gameVersionRepository.findByStatus(GameVersionStatus.RELEASED))
                .willReturn(Optional.of(gameVersion));
        given(gameFileRepository.findByGameVersionId(10L)).willReturn(Optional.empty());

        assertThatThrownBy(service::getLatestGameVersion)
                .isInstanceOf(GameFileStateException.class);
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
                gameVersion, "jexon-v1.2.3.zip", "game-versions/10/jexon.zip", ".zip",
                "application/zip", 1024L, "a".repeat(64)
        );
        ReflectionTestUtils.setField(gameFile, "id", 20L);
        return gameFile;
    }
}
