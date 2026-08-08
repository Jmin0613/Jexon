package com.jexon.gameversion.dto.response;

import com.jexon.gameversion.domain.GameVersion;
import com.jexon.gameversion.domain.GameVersionStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GameVersionDetailResponse {
    private Long gameVersionId;
    private String version;
    private String title;
    private String description;
    private GameVersionStatus status;
    private LocalDateTime releasedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static GameVersionDetailResponse from(GameVersion gameVersion) {
        return new GameVersionDetailResponse(
                gameVersion.getId(),
                gameVersion.getVersion(),
                gameVersion.getTitle(),
                gameVersion.getDescription(),
                gameVersion.getStatus(),
                gameVersion.getReleasedAt(),
                gameVersion.getCreatedAt(),
                gameVersion.getUpdatedAt()
        );
    }
}
