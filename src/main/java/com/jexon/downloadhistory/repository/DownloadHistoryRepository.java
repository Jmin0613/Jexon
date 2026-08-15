package com.jexon.downloadhistory.repository;

import com.jexon.downloadhistory.domain.DownloadHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DownloadHistoryRepository extends JpaRepository<DownloadHistory, Long> {
    @Query("""
            select dh.gameVersion.id as gameVersionId,
                   dh.gameVersion.version as version,
                   dh.gameVersion.status as status,
                   count(dh) as downloadCount
            from DownloadHistory dh
            group by dh.gameVersion.id, dh.gameVersion.version,
                     dh.gameVersion.status, dh.gameVersion.releasedAt
            order by dh.gameVersion.releasedAt desc
            """)
    List<VersionDownloadStatisticsProjection> findVersionDownloadStatistics();

    @Query(value = """
            select date(dh.created_at) as date,
                   count(*) as downloadCount
            from download_histories dh
            group by date(dh.created_at)
            order by date(dh.created_at) asc
            """, nativeQuery = true)
    List<DailyDownloadStatisticsProjection> findDailyDownloadStatistics();
}
