package com.jexon.gamefile.repository;

import com.jexon.gamefile.domain.GameFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameFileRepository extends JpaRepository<GameFile, Long> {
    Optional<GameFile> findByGameVersionId(Long gameVersionId);

    boolean existsByGameVersionId(Long gameVersionId);
}
