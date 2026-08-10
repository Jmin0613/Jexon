package com.jexon.gamefile.storage;

import java.io.InputStream;

public interface FileStorage {
    StorageResult store(String storageKey, InputStream inputStream);

    boolean exists(String storageKey);

    void delete(String storageKey);
}
