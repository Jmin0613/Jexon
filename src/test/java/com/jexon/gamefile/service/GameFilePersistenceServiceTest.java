package com.jexon.gamefile.service;

import com.jexon.gamefile.domain.GameFile;
import com.jexon.gamefile.exception.DuplicateGameFileException;
import com.jexon.gamefile.repository.GameFileRepository;
import com.jexon.gamefile.storage.StorageResult;
import com.jexon.gameversion.domain.GameVersion;
import com.jexon.gameversion.exception.GameVersionNotFoundException;
import com.jexon.gameversion.exception.InvalidGameVersionStateException;
import com.jexon.gameversion.repository.GameVersionRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GameFilePersistenceServiceTest {
    private static final StorageResult STORAGE_RESULT = new StorageResult(
            "game-files/15/id.zip",
            4L,
            "a".repeat(64)
    );

    @Mock GameVersionRepository gameVersionRepository;
    @Mock GameFileRepository gameFileRepository;
    @InjectMocks GameFilePersistenceService service;

    @Test
    void savesMetadataWithSaveAndFlush() {
        GameVersion gameVersion = draft();
        given(gameVersionRepository.findById(15L)).willReturn(Optional.of(gameVersion));
        given(gameFileRepository.saveAndFlush(any(GameFile.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        GameFile saved = service.save(15L, "game.zip", "zip", "application/zip", STORAGE_RESULT);

        ArgumentCaptor<GameFile> captor = ArgumentCaptor.forClass(GameFile.class);
        verify(gameFileRepository).saveAndFlush(captor.capture());
        assertThat(saved).isSameAs(captor.getValue());
        assertThat(saved.getGameVersion()).isSameAs(gameVersion);
        assertThat(saved.getOriginalFileName()).isEqualTo("game.zip");
        assertThat(saved.getStorageKey()).isEqualTo(STORAGE_RESULT.storageKey());
        assertThat(saved.getFileSize()).isEqualTo(4L);
        assertThat(saved.getChecksum()).isEqualTo("a".repeat(64));
    }

    @Test
    void rejectsMissingGameVersion() {
        given(gameVersionRepository.findById(15L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.save(15L, "game.zip", "zip", null, STORAGE_RESULT))
                .isInstanceOf(GameVersionNotFoundException.class);
        verify(gameFileRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsNonDraftGameVersion() {
        GameVersion released = draft();
        released.release(LocalDateTime.now());
        given(gameVersionRepository.findById(15L)).willReturn(Optional.of(released));

        assertThatThrownBy(() -> service.save(15L, "game.zip", "zip", null, STORAGE_RESULT))
                .isInstanceOf(InvalidGameVersionStateException.class);
        verify(gameFileRepository, never()).existsByGameVersionId(15L);
        verify(gameFileRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsExistingGameFile() {
        given(gameVersionRepository.findById(15L)).willReturn(Optional.of(draft()));
        given(gameFileRepository.existsByGameVersionId(15L)).willReturn(true);

        assertThatThrownBy(() -> service.save(15L, "game.zip", "zip", null, STORAGE_RESULT))
                .isInstanceOf(DuplicateGameFileException.class);
        verify(gameFileRepository, never()).saveAndFlush(any());
    }

    @Test
    void convertsGameVersionUniqueConstraintViolationToDuplicateGameFile() {
        given(gameVersionRepository.findById(15L)).willReturn(Optional.of(draft()));
        ConstraintViolationException constraintViolation = mock(ConstraintViolationException.class);
        given(constraintViolation.getConstraintName()).willReturn("uk_game_file_game_version_id");
        given(gameFileRepository.saveAndFlush(any(GameFile.class)))
                .willThrow(new DataIntegrityViolationException("duplicate", constraintViolation));

        assertThatThrownBy(() -> service.save(15L, "game.zip", "zip", null, STORAGE_RESULT))
                .isInstanceOf(DuplicateGameFileException.class);
    }

    private static GameVersion draft() {
        return GameVersion.createGameVersion(
                "v1.0.0",
                "Jexon 정식 출시 버전",
                "Jexon 게임 클라이언트 정식 출시 버전입니다."
        );
    }
}
