package com.jexon.gamefile.storage;

import com.jexon.gamefile.exception.FileStorageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.util.unit.DataSize;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileStorageTest {
    @TempDir
    Path tempDir;

    @Test
    void storesFileAndReturnsActualSizeAndSha256() throws Exception {
        LocalFileStorage storage = storage(1_024, 8);
        byte[] content = "hello storage".getBytes(StandardCharsets.UTF_8);

        StorageResult result = storage.store(
                "game-files/15/test.zip",
                new ByteArrayInputStream(content)
        );

        Path storedFile = tempDir.resolve("game-files/15/test.zip");
        String expectedChecksum = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(content)
        );

        assertThat(storedFile).isRegularFile();
        assertThat(Files.readAllBytes(storedFile)).isEqualTo(content);
        assertThat(result.storageKey()).isEqualTo("game-files/15/test.zip");
        assertThat(result.fileSize()).isEqualTo(content.length);
        assertThat(result.checksum()).isEqualTo(expectedChecksum);
        assertThat(result.checksum()).matches("[0-9a-f]{64}");
        assertThat(storage.exists("game-files/15/test.zip")).isTrue();
    }

    @Test
    void deletesExistingFileAndTreatsMissingFileAsSuccess() {
        LocalFileStorage storage = storage(1_024, 8);
        String storageKey = "game-files/15/delete.zip";
        storage.store(storageKey, new ByteArrayInputStream(new byte[]{1}));

        storage.delete(storageKey);
        storage.delete(storageKey);

        assertThat(storage.exists(storageKey)).isFalse();
    }

    @Test
    void opensStoredFileAsStream() throws Exception {
        LocalFileStorage storage = storage(1_024, 8);
        byte[] content = "download content".getBytes(StandardCharsets.UTF_8);
        storage.store("game-files/15/download.zip", new ByteArrayInputStream(content));

        try (InputStream inputStream = storage.open("game-files/15/download.zip")) {
            assertThat(inputStream.readAllBytes()).isEqualTo(content);
        }
    }

    @Test
    void rejectsOpeningMissingFile() {
        LocalFileStorage storage = storage(1_024, 8);

        assertThatThrownBy(() -> storage.open("game-files/15/missing.zip"))
                .isInstanceOf(FileStorageException.class);
    }

    @Test
    void doesNotOverwriteExistingFile() throws Exception {
        LocalFileStorage storage = storage(1_024, 8);
        String storageKey = "game-files/15/existing.zip";
        byte[] original = "original".getBytes(StandardCharsets.UTF_8);
        storage.store(storageKey, new ByteArrayInputStream(original));

        assertThatThrownBy(() -> storage.store(
                storageKey,
                new ByteArrayInputStream("replacement".getBytes(StandardCharsets.UTF_8))
        )).isInstanceOf(FileStorageException.class);

        assertThat(Files.readAllBytes(tempDir.resolve(storageKey))).isEqualTo(original);
        assertThat(uploadTempFiles()).isZero();
    }

    @Test
    void rejectsEmptyInputAndCleansTempFile() {
        LocalFileStorage storage = storage(1_024, 8);

        assertThatThrownBy(() -> storage.store(
                "game-files/15/empty.zip",
                new ByteArrayInputStream(new byte[0])
        )).isInstanceOf(FileStorageException.class);

        assertThat(storage.exists("game-files/15/empty.zip")).isFalse();
        assertThat(uploadTempFiles()).isZero();
    }

    @Test
    void rejectsActualSizeOverLimitAndCleansTempFile() {
        LocalFileStorage storage = storage(10, 4);

        assertThatThrownBy(() -> storage.store(
                "game-files/15/large.zip",
                new GeneratedInputStream(11)
        )).isInstanceOf(FileStorageException.class);

        assertThat(storage.exists("game-files/15/large.zip")).isFalse();
        assertThat(uploadTempFiles()).isZero();
    }

    @Test
    void cleansTempFileWhenInputStreamFailsMidway() {
        LocalFileStorage storage = storage(1_024, 4);

        assertThatThrownBy(() -> storage.store(
                "game-files/15/broken.zip",
                new FailingInputStream(6)
        )).isInstanceOf(FileStorageException.class)
                .hasCauseInstanceOf(IOException.class);

        assertThat(storage.exists("game-files/15/broken.zip")).isFalse();
        assertThat(uploadTempFiles()).isZero();
    }

    @Test
    void rejectsParentTraversalForStoreExistsAndDelete() {
        LocalFileStorage storage = storage(1_024, 8);

        assertThatThrownBy(() -> storage.store(
                "../outside.zip",
                new ByteArrayInputStream(new byte[]{1})
        )).isInstanceOf(FileStorageException.class);
        assertThatThrownBy(() -> storage.exists("../../outside.zip"))
                .isInstanceOf(FileStorageException.class);
        assertThatThrownBy(() -> storage.open("../outside.zip"))
                .isInstanceOf(FileStorageException.class);
        assertThatThrownBy(() -> storage.delete("../outside.zip"))
                .isInstanceOf(FileStorageException.class);
    }

    @Test
    void rejectsAbsolutePathForStoreExistsAndDelete() {
        LocalFileStorage storage = storage(1_024, 8);
        String absolutePath = tempDir.resolveSibling("outside.zip").toAbsolutePath().toString();

        assertThatThrownBy(() -> storage.store(
                absolutePath,
                new ByteArrayInputStream(new byte[]{1})
        )).isInstanceOf(FileStorageException.class);
        assertThatThrownBy(() -> storage.exists(absolutePath))
                .isInstanceOf(FileStorageException.class);
        assertThatThrownBy(() -> storage.open(absolutePath))
                .isInstanceOf(FileStorageException.class);
        assertThatThrownBy(() -> storage.delete(absolutePath))
                .isInstanceOf(FileStorageException.class);
    }

    private LocalFileStorage storage(long maxFileSize, int bufferSize) {
        FileStorageProperties properties = new FileStorageProperties();
        properties.setRoot(tempDir);
        properties.setMaxFileSize(DataSize.ofBytes(maxFileSize));
        properties.setBufferSize(DataSize.ofBytes(bufferSize));
        return new LocalFileStorage(properties);
    }

    private long uploadTempFiles() {
        try (Stream<Path> paths = Files.walk(tempDir)) {
            return paths
                    .filter(path -> path.getFileName().toString().startsWith(".upload-"))
                    .count();
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static final class GeneratedInputStream extends InputStream {
        private long remaining;

        private GeneratedInputStream(long size) {
            this.remaining = size;
        }

        @Override
        public int read() {
            if (remaining == 0) {
                return -1;
            }
            remaining--;
            return 1;
        }
    }

    private static final class FailingInputStream extends InputStream {
        private int remainingBeforeFailure;

        private FailingInputStream(int remainingBeforeFailure) {
            this.remainingBeforeFailure = remainingBeforeFailure;
        }

        @Override
        public int read() throws IOException {
            if (remainingBeforeFailure == 0) {
                throw new IOException("simulated read failure");
            }
            remainingBeforeFailure--;
            return 1;
        }
    }
}
