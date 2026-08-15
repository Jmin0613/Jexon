package com.jexon.downloadhistory.dto.response;

import com.jexon.downloadhistory.repository.VersionDownloadStatisticsProjection;
import com.jexon.gameversion.domain.GameVersionStatus;
import lombok.Getter;

@Getter
public class VersionDownloadStatisticsResponse {
    private final Long gameVersionId;
    private final String version;
    private final GameVersionStatus status;
    private final long downloadCount;

    private VersionDownloadStatisticsResponse(
            Long gameVersionId,
            String version,
            GameVersionStatus status,
            long downloadCount
    ) {
        this.gameVersionId = gameVersionId;
        this.version = version;
        this.status = status;
        this.downloadCount = downloadCount;
    }

    public static VersionDownloadStatisticsResponse from(
            VersionDownloadStatisticsProjection projection
    ) {
        return new VersionDownloadStatisticsResponse(
                projection.getGameVersionId(),
                projection.getVersion(),
                projection.getStatus(),
                projection.getDownloadCount()
        );
    }
}
