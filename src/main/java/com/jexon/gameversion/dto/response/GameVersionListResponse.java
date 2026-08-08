package com.jexon.gameversion.dto.response;

import com.jexon.gameversion.domain.GameVersion;
import com.jexon.gameversion.domain.GameVersionStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GameVersionListResponse {
    private Long gameVersionId;
    private String version;
    private String title;
    private GameVersionStatus status;
    private LocalDateTime releasedAt;
    private LocalDateTime createdAt;

    public static GameVersionListResponse from(GameVersion gameVersion) {
        return new GameVersionListResponse(
                gameVersion.getId(),
                gameVersion.getVersion(),
                gameVersion.getTitle(),
                gameVersion.getStatus(),
                gameVersion.getReleasedAt(),
                gameVersion.getCreatedAt()
        );
    }
}
