package com.jexon.downloadhistory.repository;

import com.jexon.downloadhistory.domain.DownloadHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DownloadHistoryRepository extends JpaRepository<DownloadHistory, Long> {
}
