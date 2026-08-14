package com.jexon.gameversion.dto.response;

import com.jexon.gamefile.domain.GameFile;
import com.jexon.gameversion.domain.GameVersion;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LatestGameVersionResponse {
    private Long gameVersionId;
    private String version;
    private String title;
    private String description;
    private LocalDateTime releasedAt;
    private Long gameFileId;
    private String originalFileName;
    private Long fileSize;
    private String checksum;

    public static LatestGameVersionResponse of(GameVersion gameVersion, GameFile gameFile) {
        return new LatestGameVersionResponse(
                gameVersion.getId(),
                gameVersion.getVersion(),
                gameVersion.getTitle(),
                gameVersion.getDescription(),
                gameVersion.getReleasedAt(),
                gameFile.getId(),
                gameFile.getOriginalFileName(),
                gameFile.getFileSize(),
                gameFile.getChecksum()
        );
    }
}
