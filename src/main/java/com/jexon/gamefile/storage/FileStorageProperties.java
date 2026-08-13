package com.jexon.gamefile.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

import java.nio.file.Path;

@Component
@ConfigurationProperties(prefix = "jexon.file-storage")
public class FileStorageProperties {
    private Path root = Path.of("./storage"); // yml의 root 값 매핑
    private DataSize maxFileSize = DataSize.ofBytes(536_870_912L); // yml의 max-file-size 값 매핑
    private DataSize bufferSize = DataSize.ofBytes(65_536L); // yml의 buffer-size 값 매핑

    public Path getRoot() {
        return root;
    }

    public void setRoot(Path root) {
        this.root = root;
    }

    public DataSize getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(DataSize maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public DataSize getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(DataSize bufferSize) {
        this.bufferSize = bufferSize;
    }
}
