package com.jexon.downloadhistory.repository;

import com.jexon.downloadhistory.domain.DownloadHistory;
import com.jexon.gamefile.domain.GameFile;
import com.jexon.gamefile.repository.GameFileRepository;
import com.jexon.gameversion.domain.GameVersion;
import com.jexon.gameversion.domain.GameVersionStatus;
import com.jexon.gameversion.repository.GameVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false"
})
@Testcontainers(disabledWithoutDocker = true)
class DownloadHistoryRepositoryTest {
    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");

    @Autowired DownloadHistoryRepository downloadHistoryRepository;
    @Autowired GameFileRepository gameFileRepository;
    @Autowired GameVersionRepository gameVersionRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clear() {
        downloadHistoryRepository.deleteAll();
        gameFileRepository.deleteAll();
        gameVersionRepository.deleteAll();
    }

    @Test
    @Transactional
    void groupsByVersionAndOrdersByLatestRelease() {
        GameVersion oldVersion = saveReleasedVersion("v1.4.0", LocalDateTime.of(2026, 8, 1, 12, 0));
        oldVersion.deactivateForReplacement();
        gameVersionRepository.flush();
        GameVersion latestVersion = saveReleasedVersion("v1.5.0", LocalDateTime.of(2026, 8, 10, 12, 0));
        GameFile oldFile = gameFileRepository.save(file(oldVersion, "old.zip"));
        GameFile latestFile = gameFileRepository.save(file(latestVersion, "latest.zip"));
        saveHistoryAt(oldVersion, oldFile, LocalDateTime.of(2026, 8, 13, 10, 0));
        saveHistoryAt(oldVersion, oldFile, LocalDateTime.of(2026, 8, 14, 10, 0));
        saveHistoryAt(latestVersion, latestFile, LocalDateTime.of(2026, 8, 14, 11, 0));

        var result = downloadHistoryRepository.findVersionDownloadStatistics();

        assertThat(result).extracting(VersionDownloadStatisticsProjection::getVersion)
                .containsExactly("v1.5.0", "v1.4.0");
        assertThat(result).extracting(VersionDownloadStatisticsProjection::getDownloadCount)
                .containsExactly(1L, 2L);
        assertThat(result.get(1).getStatus()).isEqualTo(GameVersionStatus.INACTIVE);
    }

    @Test
    void groupsByDateAndOrdersAscending() {
        GameVersion version = saveReleasedVersion("v1.5.0", LocalDateTime.of(2026, 8, 10, 12, 0));
        GameFile file = gameFileRepository.save(file(version, "latest.zip"));
        saveHistoryAt(version, file, LocalDateTime.of(2026, 8, 14, 23, 0));
        saveHistoryAt(version, file, LocalDateTime.of(2026, 8, 13, 10, 0));
        saveHistoryAt(version, file, LocalDateTime.of(2026, 8, 14, 1, 0));

        var result = downloadHistoryRepository.findDailyDownloadStatistics();

        assertThat(result).extracting(DailyDownloadStatisticsProjection::getDate)
                .containsExactly(LocalDate.of(2026, 8, 13), LocalDate.of(2026, 8, 14));
        assertThat(result).extracting(DailyDownloadStatisticsProjection::getDownloadCount)
                .containsExactly(1L, 2L);
    }

    private GameVersion saveReleasedVersion(String version, LocalDateTime releasedAt) {
        GameVersion gameVersion = GameVersion.createGameVersion(
                version, "게임 버전 통합 테스트 제목", "게임 버전 통합 테스트를 위한 상세 설명입니다."
        );
        gameVersion.release(releasedAt);
        return gameVersionRepository.save(gameVersion);
    }

    private static GameFile file(GameVersion version, String name) {
        return GameFile.createGameFile(
                version, name, "games/" + name, "zip", "application/zip",
                100L, "a".repeat(64)
        );
    }

    private void saveHistoryAt(
            GameVersion version,
            GameFile file,
            LocalDateTime createdAt
    ) {
        DownloadHistory history = downloadHistoryRepository.saveAndFlush(
                DownloadHistory.createDownloadHistory(version, file)
        );
        jdbcTemplate.update(
                "update download_histories set created_at = ?, updated_at = ? where id = ?",
                createdAt,
                createdAt,
                history.getId()
        );
    }
}
