package com.jexon.gameversion.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GameVersionReleaseControlTest {
    @Test
    void createSingletonControl() {
        GameVersionReleaseControl control = GameVersionReleaseControl.create();
        assertThat(GameVersionReleaseControl.SINGLETON_ID).isEqualTo(1L);
        assertThat(control.getId()).isEqualTo(GameVersionReleaseControl.SINGLETON_ID);
        assertThat(control.getReleaseSequence()).isZero();
    }

    @Test
    void advanceSequenceCumulatively() {
        GameVersionReleaseControl control = GameVersionReleaseControl.create();
        control.advanceReleaseSequence();
        control.advanceReleaseSequence();
        assertThat(control.getReleaseSequence()).isEqualTo(2L);
    }

    @Test
    void containsOnlyConcurrencyControlState() {
        assertThat(GameVersionReleaseControl.class.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .contains("id", "releaseSequence", "lockVersion")
                .doesNotContain("gameVersion", "currentGameVersionId", "latest");
    }
}
