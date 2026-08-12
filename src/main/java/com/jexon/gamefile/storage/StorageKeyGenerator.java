package com.jexon.gamefile.storage;

import com.jexon.gamefile.exception.FileStorageException;
import com.jexon.gamefile.exception.InvalidGameFileException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Supplier;

@Component
public class StorageKeyGenerator {
    private static final int MAX_GENERATION_ATTEMPTS = 5;
    private static final String STORAGE_KEY_FORMAT = "game-files/%d/%s.zip";

    private final FileStorage fileStorage;
    private final Supplier<UUID> uuidSupplier;

    @Autowired
    public StorageKeyGenerator(FileStorage fileStorage) {
        this(fileStorage, UUID::randomUUID);
    }

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

    public String generate(Long gameVersionId) {
        if (gameVersionId == null || gameVersionId <= 0) {
            throw new InvalidGameFileException("gameVersionId는 0보다 커야 합니다.");
        }

        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            UUID uuid = uuidSupplier.get();
            if (uuid == null) {
                throw new FileStorageException("UUID를 생성할 수 없습니다.");
            }

            String storageKey = STORAGE_KEY_FORMAT.formatted(gameVersionId, uuid);
            if (!fileStorage.exists(storageKey)) {
                return storageKey;
            }
        }

        throw new FileStorageException("충돌하지 않는 storageKey를 생성할 수 없습니다.");
    }
}
