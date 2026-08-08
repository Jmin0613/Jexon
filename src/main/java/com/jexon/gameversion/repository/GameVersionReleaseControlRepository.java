package com.jexon.gameversion.repository;

import com.jexon.gameversion.domain.GameVersionReleaseControl;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameVersionReleaseControlRepository
        extends JpaRepository<GameVersionReleaseControl, Long> {
}
