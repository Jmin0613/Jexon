package com.jexon.gameversion.repository;

import com.jexon.gameversion.domain.GameVersion;
import com.jexon.gameversion.domain.GameVersionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameVersionRepository extends JpaRepository<GameVersion, Long> {
    boolean existsByVersion(String version);

    Optional<GameVersion> findByStatus(GameVersionStatus status);

    Page<GameVersion> findAllByStatus(
            GameVersionStatus status,
            Pageable pageable
    );
}
