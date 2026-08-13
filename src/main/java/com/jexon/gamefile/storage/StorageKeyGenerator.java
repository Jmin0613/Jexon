package com.jexon.gamefile.storage;

import com.jexon.gamefile.exception.FileStorageException;
import com.jexon.gamefile.exception.InvalidGameFileException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Supplier;

@Component
public class StorageKeyGenerator {
    // 원본 파일명과 실제 저장 식별자를 분리하기 위해
    // game-files/{gameVersionId}/{UUID}.zip 형식의 내부 키를 생성

    private static final int MAX_GENERATION_ATTEMPTS = 5;
    private static final String STORAGE_KEY_FORMAT = "game-files/%d/%s.zip";

    private final FileStorage fileStorage;
    private final Supplier<UUID> uuidSupplier;

    /* 생성자 오버로딩 */
    // Spring
    @Autowired
    public StorageKeyGenerator(FileStorage fileStorage) {
        this(fileStorage, UUID::randomUUID);
    }

    // 개발자/테스트 → 직접 주입
    StorageKeyGenerator(FileStorage fileStorage, Supplier<UUID> uuidSupplier) {
        if (fileStorage == null) {
            throw new IllegalArgumentException("FileStorage가 필요합니다.");
        }
        if (uuidSupplier == null) {
            throw new IllegalArgumentException("UUID 생성기가 필요합니다.");
        }
        this.fileStorage = fileStorage;
        this.uuidSupplier = uuidSupplier;
    }
    /* ----------------------------------------------------------- */

    // 실제 저장소에서 사용할 논리 키 생성
    public String generate(Long gameVersionId) {
        if (gameVersionId == null || gameVersionId <= 0) {
            throw new InvalidGameFileException("gameVersionId는 0보다 커야 합니다.");
        }

        // 충돌 방지 재시도
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            UUID uuid = uuidSupplier.get();

            if (uuid == null) {
                throw new FileStorageException("UUID를 생성할 수 없습니다.");
            }

            // game-files/{gameVersionId}/{UUID}.zip (내부키 생성)
            String storageKey = STORAGE_KEY_FORMAT.formatted(gameVersionId, uuid);

            // 만약의 UUID 중복 사태 대비
            if (!fileStorage.exists(storageKey)) {
                return storageKey;
            }
        }

        throw new FileStorageException("충돌하지 않는 storageKey를 생성할 수 없습니다.");
    }
}
