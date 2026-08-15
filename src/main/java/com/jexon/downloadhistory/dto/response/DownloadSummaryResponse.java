package com.jexon.downloadhistory.dto.response;

import lombok.Getter;

@Getter
public class DownloadSummaryResponse {
    private final long totalDownloads;

    private DownloadSummaryResponse(long totalDownloads) {
        this.totalDownloads = totalDownloads;
    }

    public static DownloadSummaryResponse of(long totalDownloads) {
        return new DownloadSummaryResponse(totalDownloads);
    }
}
