package com.jexon.gameversion.repository;

import com.jexon.gameversion.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false"
})
@Testcontainers(disabledWithoutDocker = true)
class GameVersionReleaseConcurrencyTest {
    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");

    @Autowired GameVersionRepository gameVersionRepository;
    @Autowired GameVersionReleaseControlRepository controlRepository;
    @Autowired PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        gameVersionRepository.deleteAll();
        controlRepository.deleteAll();
        controlRepository.saveAndFlush(GameVersionReleaseControl.create());
    }

    @Test
    void concurrentDifferentDraftReleasesLeaveExactlyOneReleasedAndRollbackLoser() throws Exception {
        GameVersion a = gameVersionRepository.saveAndFlush(version("v1.0.0"));
        GameVersion b = gameVersionRepository.saveAndFlush(version("v2.0.0"));

        List<ReleaseOutcome> outcomes = releaseConcurrently(a.getId(), b.getId());

        assertThat(outcomes).filteredOn(ReleaseOutcome::success).hasSize(1);
        assertThat(outcomes).filteredOn(outcome -> !outcome.success()).hasSize(1);
        List<GameVersion> versions = gameVersionRepository.findAll();
        assertThat(versions).filteredOn(v -> v.getStatus() == GameVersionStatus.RELEASED).hasSize(1);
        assertThat(versions).filteredOn(v -> v.getStatus() == GameVersionStatus.DRAFT).hasSize(1);
        GameVersionReleaseControl control = controlRepository.findById(1L).orElseThrow();
        assertThat(control.getReleaseSequence()).isEqualTo(1L);
        assertThat(control.getLockVersion()).isEqualTo(1L);
    }

    @Test
    void concurrentReleasesReplaceExistingOnceAndRollbackAllLosingChanges() throws Exception {
        GameVersion existing = version("v1.0.0");
        existing.release(LocalDateTime.of(2026, 8, 1, 12, 0));
        existing = gameVersionRepository.saveAndFlush(existing);
        GameVersion a = gameVersionRepository.saveAndFlush(version("v2.0.0"));
        GameVersion b = gameVersionRepository.saveAndFlush(version("v3.0.0"));

        List<ReleaseOutcome> outcomes = releaseConcurrently(a.getId(), b.getId());

        assertThat(outcomes).filteredOn(ReleaseOutcome::success).hasSize(1);
        List<GameVersion> versions = gameVersionRepository.findAll();
        assertThat(versions).filteredOn(v -> v.getStatus() == GameVersionStatus.RELEASED).hasSize(1);
        assertThat(versions).filteredOn(v -> v.getStatus() == GameVersionStatus.DRAFT).hasSize(1);
        GameVersion previous = gameVersionRepository.findById(existing.getId()).orElseThrow();
        assertThat(previous.getStatus()).isEqualTo(GameVersionStatus.INACTIVE);
        assertThat(previous.getReleasedAt()).isEqualTo(LocalDateTime.of(2026, 8, 1, 12, 0));
        assertThat(controlRepository.findById(1L).orElseThrow().getReleaseSequence()).isEqualTo(1L);
    }

    private List<ReleaseOutcome> releaseConcurrently(Long firstId, Long secondId) throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<ReleaseOutcome> first = executor.submit(() -> release(firstId, barrier));
            Future<ReleaseOutcome> second = executor.submit(() -> release(secondId, barrier));
            return List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    private ReleaseOutcome release(Long targetId, CyclicBarrier barrier) {
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                GameVersionReleaseControl control = controlRepository.findById(1L).orElseThrow();
                GameVersion target = gameVersionRepository.findById(targetId).orElseThrow();
                await(barrier);
                control.advanceReleaseSequence();
                gameVersionRepository.findByStatus(GameVersionStatus.RELEASED)
                        .ifPresent(GameVersion::deactivateForReplacement);
                target.release(LocalDateTime.now());
                gameVersionRepository.flush();
            });
            return new ReleaseOutcome(targetId, true);
        } catch (RuntimeException exception) {
            return new ReleaseOutcome(targetId, false);
        }
    }

    private static void await(CyclicBarrier barrier) {
        try { barrier.await(10, TimeUnit.SECONDS); }
        catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new IllegalStateException(exception); }
        catch (BrokenBarrierException | TimeoutException exception) { throw new IllegalStateException(exception); }
    }

    private static GameVersion version(String version) {
        return GameVersion.createGameVersion(version, "통합 테스트 게임 버전 제목", "동시 release 통합 테스트를 위한 상세 설명입니다.");
    }

    private record ReleaseOutcome(Long gameVersionId, boolean success) {}
}
