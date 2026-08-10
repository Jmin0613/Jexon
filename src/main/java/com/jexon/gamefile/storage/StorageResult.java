package com.jexon.gamefile.storage;

public record StorageResult(
        String storageKey,
        long fileSize,
        String checksum
) {
}
