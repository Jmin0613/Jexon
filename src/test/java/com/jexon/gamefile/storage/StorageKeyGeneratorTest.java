package com.jexon.gamefile.storage;

import com.jexon.gamefile.exception.FileStorageException;
import com.jexon.gamefile.exception.InvalidGameFileException;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class StorageKeyGeneratorTest {
    private static final UUID FIRST_UUID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID SECOND_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @Test
    void generatesExpectedKeyAndReturnsFirstAvailableCandidate() {
        FileStorage fileStorage = mock(FileStorage.class);
        StorageKeyGenerator generator = generator(fileStorage, FIRST_UUID);
        String expected = "game-files/15/550e8400-e29b-41d4-a716-446655440000.zip";
        given(fileStorage.exists(expected)).willReturn(false);

        String storageKey = generator.generate(15L);

        assertThat(storageKey).isEqualTo(expected);
        assertThat(storageKey).matches("game-files/15/[0-9a-f-]{36}\\.zip");
        verify(fileStorage).exists(expected);
    }

    @Test
    void retriesWithNextUuidWhenFirstCandidateExists() {
        FileStorage fileStorage = mock(FileStorage.class);
        StorageKeyGenerator generator = generator(fileStorage, FIRST_UUID, SECOND_UUID);
        String first = "game-files/15/550e8400-e29b-41d4-a716-446655440000.zip";
        String second = "game-files/15/123e4567-e89b-12d3-a456-426614174000.zip";
        given(fileStorage.exists(first)).willReturn(true);
        given(fileStorage.exists(second)).willReturn(false);

        assertThat(generator.generate(15L)).isEqualTo(second);
        verify(fileStorage).exists(first);
        verify(fileStorage).exists(second);
    }

    @Test
    void failsAfterFiveCollisions() {
        FileStorage fileStorage = mock(FileStorage.class);
        String candidate = "game-files/15/550e8400-e29b-41d4-a716-446655440000.zip";
        given(fileStorage.exists(candidate)).willReturn(true);
        StorageKeyGenerator generator = generator(
                fileStorage,
                FIRST_UUID,
                FIRST_UUID,
                FIRST_UUID,
                FIRST_UUID,
                FIRST_UUID
        );

        assertThatThrownBy(() -> generator.generate(15L))
                .isInstanceOf(FileStorageException.class);
        verify(fileStorage, times(5)).exists(candidate);
    }

    @Test
    void rejectsNullZeroAndNegativeGameVersionIds() {
        StorageKeyGenerator generator = generator(mock(FileStorage.class), FIRST_UUID);

        for (Long gameVersionId : List.of(0L, -1L)) {
            assertThatThrownBy(() -> generator.generate(gameVersionId))
                    .isInstanceOf(InvalidGameFileException.class);
        }
        assertThatThrownBy(() -> generator.generate(null))
                .isInstanceOf(InvalidGameFileException.class);
    }

    private StorageKeyGenerator generator(FileStorage fileStorage, UUID... uuids) {
        Queue<UUID> values = new ArrayDeque<>(List.of(uuids));
        Supplier<UUID> supplier = values::remove;
        return new StorageKeyGenerator(fileStorage, supplier);
    }
}
