package com.jexon.downloadhistory.dto.response;

import com.jexon.downloadhistory.repository.DailyDownloadStatisticsProjection;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class DailyDownloadStatisticsResponse {
    private final LocalDate date;
    private final long downloadCount;

    private DailyDownloadStatisticsResponse(LocalDate date, long downloadCount) {
        this.date = date;
        this.downloadCount = downloadCount;
    }

    public static DailyDownloadStatisticsResponse from(
            DailyDownloadStatisticsProjection projection
    ) {
        return new DailyDownloadStatisticsResponse(
                projection.getDate(),
                projection.getDownloadCount()
        );
    }
}
