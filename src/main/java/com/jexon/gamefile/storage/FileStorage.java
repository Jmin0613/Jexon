package com.jexon.gamefile.storage;

import java.io.InputStream;

public interface FileStorage {
    // FileStorage 규격

    // Key + fileStream 저장
    StorageResult store(String storageKey, InputStream inputStream);

    // key 중복 확인
    boolean exists(String storageKey);

    // 실패 시 적재된 실제 파일 삭제
    void delete(String storageKey);
}
