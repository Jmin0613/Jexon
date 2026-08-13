package com.jexon.gamefile.storage;

public record StorageResult(
        String storageKey,
        long fileSize,
        String checksum // SHA-256으로 만든 체크섬
) {
}
