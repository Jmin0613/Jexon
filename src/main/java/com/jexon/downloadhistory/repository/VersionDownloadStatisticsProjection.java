package com.jexon.downloadhistory.repository;

import com.jexon.gameversion.domain.GameVersionStatus;

public interface VersionDownloadStatisticsProjection {
    Long getGameVersionId();

    String getVersion();

    GameVersionStatus getStatus();

    long getDownloadCount();
}
