package com.jexon.downloadhistory.repository;

import java.time.LocalDate;

public interface DailyDownloadStatisticsProjection {
    LocalDate getDate();

    long getDownloadCount();
}
