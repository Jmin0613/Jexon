package com.jexon.gameversion.config;

import com.jexon.gameversion.domain.GameVersionReleaseControl;
import com.jexon.gameversion.repository.GameVersionReleaseControlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameVersionReleaseControlInitializer implements ApplicationRunner {
    // 배포 제어 데이터 생성

    private final GameVersionReleaseControlRepository releaseControlRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (releaseControlRepository.existsById(GameVersionReleaseControl.SINGLETON_ID)) {
            return;
        }

        releaseControlRepository.save(GameVersionReleaseControl.create());
    }
}
