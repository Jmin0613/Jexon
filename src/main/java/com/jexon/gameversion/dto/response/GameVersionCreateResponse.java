package com.jexon.gameversion.dto.response;

import com.jexon.gameversion.domain.GameVersion;
import com.jexon.gameversion.domain.GameVersionStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GameVersionCreateResponse {
    private Long gameVersionId;
    private String version;
    private GameVersionStatus status;

    public static GameVersionCreateResponse from(GameVersion gameVersion) {
        return new GameVersionCreateResponse(
                gameVersion.getId(),
                gameVersion.getVersion(),
                gameVersion.getStatus()
        );
    }
}
