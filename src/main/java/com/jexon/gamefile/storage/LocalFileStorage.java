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
    // FileStorage 구현체

    private static final String SHA_256 = "SHA-256";
    private static final Logger log = LoggerFactory.getLogger(LocalFileStorage.class);

    private final Path root;
    private final long maxFileSize;
    private final int bufferSize;

    // 생성자
    public LocalFileStorage(FileStorageProperties properties) {
        // 설정 검즘
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

        // 경로 및 파일 크기, 버퍼 크기 제한 설정
        this.root = properties.getRoot().toAbsolutePath().normalize();
        this.maxFileSize = properties.getMaxFileSize().toBytes();
        // 설정된 버퍼 크기를 byte[] 생성에 사용할 int 값으로 변환
        this.bufferSize = Math.toIntExact(properties.getBufferSize().toBytes());
    }

    // Key + fileStream 저장
    @Override
    public StorageResult store(String storageKey, InputStream inputStream) {
        if (inputStream == null) {
            throw new FileStorageException("저장할 파일 스트림이 필요합니다.");
        }

        Path target = resolveSafely(storageKey); // key → 저장할 파일 경로로 변환
        Path temp = null; // 임시파일 선언

        try {
            Path parent = target.getParent();
            Files.createDirectories(parent); //상위 폴더 없으면 자동 폴더 생성 (mkdir -p)

            // 저장소 파일 덮어쓰기 방지 위해 파일 중복 검사
            if (Files.exists(target)) {
                throw new FileStorageException("이미 존재하는 storageKey에는 파일을 저장할 수 없습니다.");
            }

            temp = Files.createTempFile(parent, ".upload-", ".tmp");
            MessageDigest digest = createSha256Digest(); // SHA-256 해시 계산기 준비

            // 임시 파일 작성 + (최대 용량 초과 체크 + 총 파일 크기 계산 + SHA-256 해시 데이터 적재)
            long fileSize = writeToTemp(inputStream, temp, digest); // 작업 후 파일 크기 가져오기

            // 임시 파일 생성 완료 시,
            moveToTarget(temp, target);
            temp = null; // 최종 위치 이동 완료 → temp 정리 필요 없음

            // 메타데이터 조각 반환
            return new StorageResult(
                    storageKey, // File이 저장된 Key
                    fileSize, // File 크기
                    HexFormat.of().formatHex(digest.digest()) // 계산된 SHA-256 해시값
            );
        } catch (FileStorageException exception) {
            // 에러 발생 시 임시파일 청소
            cleanupTemp(temp, exception);
            throw exception;
        } catch (IOException exception) {
            FileStorageException storageException = new FileStorageException("파일 저장에 실패했습니다.", exception);
            cleanupTemp(temp, storageException);
            throw storageException;
        }
    }

    // key 중복 확인
    @Override
    public boolean exists(String storageKey) {
        Path target = resolveSafely(storageKey);
        return Files.isRegularFile(target);
    }

    // storageKey에 해당하는 실제 파일 삭제 (현재는 DB 실패 보상에 사용)
    @Override
    public void delete(String storageKey) {
        Path target = resolveSafely(storageKey);

        try {
            Files.deleteIfExists(target);
        } catch (IOException exception) {
            throw new FileStorageException("파일 삭제에 실패했습니다.", exception);
        }
    }

    // helper 메서드 -------------------------------------------------------------------------------

    // 임시 파일 작성
    private long writeToTemp(
            InputStream inputStream,
            Path temp,
            MessageDigest digest
    ) throws IOException {
        long fileSize = 0;
        byte[] buffer = new byte[bufferSize];

        // 스트림 열기
        try (OutputStream outputStream = Files.newOutputStream(
                temp,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING // 기존 파일 지우고 0바이트로 열기
        )) {
            int bytesRead;

            // bufferSize 이하 단위로 반복해서 스트림 읽기
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                if (bytesRead == 0) {
                    continue; // 이번 read에서 읽은 데이터가 없으면 다시 시도
                }

                // 용량 제한 체크 → 디스크 폭주 방지
                if (fileSize > maxFileSize - bytesRead) {
                    throw new FileStorageException("최대 파일 크기를 초과했습니다.");
                }

                outputStream.write(buffer, 0, bytesRead); // 임시파일 작성
                digest.update(buffer, 0, bytesRead); // SHA-256 해시값 누적
                fileSize += bytesRead; // 파일 크기 누적
            }
        }

        // 빈 파일 거부
        if (fileSize == 0) {
            throw new FileStorageException("빈 파일은 저장할 수 없습니다.");
        }

        return fileSize;
    }

    // 최종 저장 위치로 File 이동
    private synchronized void moveToTarget(Path temp, Path target) throws IOException {
        // 이동 전, target 경로에 파일 중복 검사
        if (Files.exists(target)) {
            throw new FileStorageException("이미 존재하는 storageKey에는 파일을 저장할 수 없습니다.");
        }

        try { // 원자적 이동 처리
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) { // Fallback
            Files.move(temp, target); // 일반 이동으로 재시도
        }
    }

    // SHA-256 해시 객체 준비
    private MessageDigest createSha256Digest() {
        try {
            return MessageDigest.getInstance(SHA_256);
        } catch (NoSuchAlgorithmException exception) {
            throw new FileStorageException("SHA-256 checksum 계산을 초기화할 수 없습니다.", exception);
        }
    }

    // storageKey → 파일 경로 Path로 변환
    // String "game-files/3/abc.zip"
    // Path game-files/3/abc.zip
    private Path resolveSafely(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new FileStorageException("storageKey가 필요합니다.");
        }

        // storageKey를 Path객체로 변환
        final Path keyPath;
        try {
            keyPath = Path.of(storageKey);
        } catch (RuntimeException exception) {
            throw new FileStorageException("올바르지 않은 storageKey입니다.", exception);
        }

        // 절대 경로 접근 차단
        if (keyPath.isAbsolute()) {
            throw new FileStorageException("절대경로 storageKey는 사용할 수 없습니다.");
        }

        // root와 상대 storageKey를 결합하고 . / .. 등을 정규화
        Path resolved = root.resolve(keyPath).normalize();
        // resolve
        // → (절대경로 root)C:\jexon\storage + (상대경로)game-files\3\abc.zip
        // = (절대경로)C:\jexon\storage\game-files\3\abc.zip

        // 경로 탈출 방지 검증
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
            // temp 삭제 실패가 원래 저장 실패 원인을 덮어쓰지 않도록 부가 예외로 보존
            originalException.addSuppressed(cleanupException);
            log.error("임시 파일 정리에 실패했습니다. temp={}", temp, cleanupException);
        }
    }
}
