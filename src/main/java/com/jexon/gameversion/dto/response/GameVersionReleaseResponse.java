package com.jexon.gameversion.dto.response;

import com.jexon.gameversion.domain.GameVersion;
import com.jexon.gameversion.domain.GameVersionStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GameVersionReleaseResponse {
    private Long gameVersionId;
    private String version;
    private GameVersionStatus status;
    private LocalDateTime releasedAt;

    public static GameVersionReleaseResponse from(GameVersion gameVersion) {
        return new GameVersionReleaseResponse(
                gameVersion.getId(),
                gameVersion.getVersion(),
                gameVersion.getStatus(),
                gameVersion.getReleasedAt()
        );
    }
}
