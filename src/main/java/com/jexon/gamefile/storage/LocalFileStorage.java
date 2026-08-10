package com.jexon.gamefile.storage;

import com.jexon.gamefile.exception.FileStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class LocalFileStorage implements FileStorage {
    private static final String SHA_256 = "SHA-256";
    private static final Logger log = LoggerFactory.getLogger(LocalFileStorage.class);

    private final Path root;
    private final long maxFileSize;
    private final int bufferSize;

    public LocalFileStorage(FileStorageProperties properties) {
        if (properties == null) {
            throw new IllegalArgumentException("파일 저장소 설정이 필요합니다.");
        }
        if (properties.getRoot() == null) {
            throw new IllegalArgumentException("파일 저장 루트가 필요합니다.");
        }
        if (properties.getMaxFileSize() == null || properties.getMaxFileSize().toBytes() <= 0) {
            throw new IllegalArgumentException("최대 파일 크기는 0보다 커야 합니다.");
        }
        if (properties.getBufferSize() == null
                || properties.getBufferSize().toBytes() <= 0
                || properties.getBufferSize().toBytes() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("버퍼 크기는 1 이상 Integer 최대값 이하여야 합니다.");
        }

        this.root = properties.getRoot().toAbsolutePath().normalize();
        this.maxFileSize = properties.getMaxFileSize().toBytes();
        this.bufferSize = Math.toIntExact(properties.getBufferSize().toBytes());
    }

    @Override
    public StorageResult store(String storageKey, InputStream inputStream) {
        if (inputStream == null) {
            throw new FileStorageException("저장할 파일 스트림이 필요합니다.");
        }

        Path target = resolveSafely(storageKey);
        Path temp = null;

        try {
            Path parent = target.getParent();
            Files.createDirectories(parent);

            if (Files.exists(target)) {
                throw new FileStorageException("이미 존재하는 storageKey에는 파일을 저장할 수 없습니다.");
            }

            temp = Files.createTempFile(parent, ".upload-", ".tmp");
            MessageDigest digest = createSha256Digest();
            long fileSize = writeToTemp(inputStream, temp, digest);

            moveToTarget(temp, target);
            temp = null;

            return new StorageResult(
                    storageKey,
                    fileSize,
                    HexFormat.of().formatHex(digest.digest())
            );
        } catch (FileStorageException exception) {
            cleanupTemp(temp, exception);
            throw exception;
        } catch (IOException exception) {
            FileStorageException storageException = new FileStorageException("파일 저장에 실패했습니다.", exception);
            cleanupTemp(temp, storageException);
            throw storageException;
        }
    }

    @Override
    public boolean exists(String storageKey) {
        Path target = resolveSafely(storageKey);
        return Files.isRegularFile(target);
    }

    @Override
    public void delete(String storageKey) {
        Path target = resolveSafely(storageKey);

        try {
            Files.deleteIfExists(target);
        } catch (IOException exception) {
            throw new FileStorageException("파일 삭제에 실패했습니다.", exception);
        }
    }

    private long writeToTemp(
            InputStream inputStream,
            Path temp,
            MessageDigest digest
    ) throws IOException {
        long fileSize = 0;
        byte[] buffer = new byte[bufferSize];

        try (OutputStream outputStream = Files.newOutputStream(
                temp,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
        )) {
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                if (bytesRead == 0) {
                    continue;
                }

                if (fileSize > maxFileSize - bytesRead) {
                    throw new FileStorageException("최대 파일 크기를 초과했습니다.");
                }

                outputStream.write(buffer, 0, bytesRead);
                digest.update(buffer, 0, bytesRead);
                fileSize += bytesRead;
            }
        }

        if (fileSize == 0) {
            throw new FileStorageException("빈 파일은 저장할 수 없습니다.");
        }

        return fileSize;
    }

    private synchronized void moveToTarget(Path temp, Path target) throws IOException {
        if (Files.exists(target)) {
            throw new FileStorageException("이미 존재하는 storageKey에는 파일을 저장할 수 없습니다.");
        }

        try {
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temp, target);
        }
    }

    private MessageDigest createSha256Digest() {
        try {
            return MessageDigest.getInstance(SHA_256);
        } catch (NoSuchAlgorithmException exception) {
            throw new FileStorageException("SHA-256 checksum 계산을 초기화할 수 없습니다.", exception);
        }
    }

    private Path resolveSafely(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new FileStorageException("storageKey가 필요합니다.");
        }

        final Path keyPath;
        try {
            keyPath = Path.of(storageKey);
        } catch (RuntimeException exception) {
            throw new FileStorageException("올바르지 않은 storageKey입니다.", exception);
        }

        if (keyPath.isAbsolute()) {
            throw new FileStorageException("절대경로 storageKey는 사용할 수 없습니다.");
        }

        Path resolved = root.resolve(keyPath).normalize();
        if (resolved.equals(root) || !resolved.startsWith(root)) {
            throw new FileStorageException("저장 루트 밖의 경로에는 접근할 수 없습니다.");
        }

        return resolved;
    }

    private void cleanupTemp(Path temp, FileStorageException originalException) {
        if (temp == null) {
            return;
        }

        try {
            Files.deleteIfExists(temp);
        } catch (IOException cleanupException) {
            originalException.addSuppressed(cleanupException);
            log.error("임시 파일 정리에 실패했습니다. temp={}", temp, cleanupException);
        }
    }
}
