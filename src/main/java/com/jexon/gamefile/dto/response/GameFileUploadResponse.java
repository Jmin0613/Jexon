package com.jexon.gamefile.dto.response;

import com.jexon.gamefile.domain.GameFile;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GameFileUploadResponse {
    private Long gameFileId;
    private Long gameVersionId;
    private String originalFileName;
    private String extension;
    private Long fileSize;
    private String checksum;
    // storageKey 미포함 → 외부 API에 노출X

    public static GameFileUploadResponse from(GameFile gameFile) {
        return new GameFileUploadResponse(
                gameFile.getId(),
                gameFile.getGameVersion().getId(),
                gameFile.getOriginalFileName(),
                gameFile.getExtension(),
                gameFile.getFileSize(),
                gameFile.getChecksum()
        );
    }
}
