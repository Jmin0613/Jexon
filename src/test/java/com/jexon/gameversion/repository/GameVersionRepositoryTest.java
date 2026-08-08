package com.jexon.gameversion.repository;

import com.jexon.gameversion.domain.GameVersion;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.RollbackException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false"
})
@Testcontainers(disabledWithoutDocker = true)
class GameVersionRepositoryTest {
    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");

    @Autowired GameVersionRepository repository;
    @Autowired EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void clear() { repository.deleteAll(); }

    @Test
    void databaseUniqueConstraintRejectsDuplicateVersion() {
        repository.saveAndFlush(version("v1.0.0", "첫 번째 게임 버전 제목"));
        assertThatThrownBy(() -> repository.saveAndFlush(version("v1.0.0", "두 번째 게임 버전 제목")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasRootCauseInstanceOf(java.sql.SQLIntegrityConstraintViolationException.class);
        assertThat(repository.findAll()).hasSize(1);
    }

    @Test
    void optimisticVersionPreventsLostUpdate() {
        GameVersion saved = repository.saveAndFlush(version("v1.0.0", "최초 게임 버전 제목입니다"));
        Long initialLockVersion = saved.getLockVersion();
        EntityManager first = entityManagerFactory.createEntityManager();
        EntityManager second = entityManagerFactory.createEntityManager();
        try {
            first.getTransaction().begin();
            second.getTransaction().begin();
            GameVersion firstCopy = first.find(GameVersion.class, saved.getId());
            GameVersion secondCopy = second.find(GameVersion.class, saved.getId());
            firstCopy.updateDetails("첫 번째 관리자 수정 제목", "첫 번째 관리자가 수정한 상세 설명입니다.");
            secondCopy.updateDetails("두 번째 관리자 수정 제목", "두 번째 관리자가 수정한 상세 설명입니다.");
            first.getTransaction().commit();
            assertThatThrownBy(second.getTransaction()::commit)
                    .isInstanceOf(RollbackException.class);
        } finally {
            if (first.getTransaction().isActive()) first.getTransaction().rollback();
            if (second.getTransaction().isActive()) second.getTransaction().rollback();
            first.close(); second.close();
        }
        GameVersion actual = repository.findById(saved.getId()).orElseThrow();
        assertThat(actual.getTitle()).isEqualTo("첫 번째 관리자 수정 제목");
        assertThat(actual.getLockVersion()).isGreaterThan(initialLockVersion);
    }

    private static GameVersion version(String version, String title) {
        return GameVersion.createGameVersion(version, title, "게임 버전 통합 테스트를 위한 상세 설명입니다.");
    }
}
