package com.jexon.gameversion.domain;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameVersionTest {
    private static final String TITLE = "정식 출시 버전 제목";
    private static final String DESCRIPTION = "정식 출시 버전에 대한 상세 설명입니다.";

    @Test
    void createDraftAndPreserveOriginalInput() {
        GameVersion version = GameVersion.createGameVersion("v1.0.0", "  정식 출시 버전 제목  ", "  정식 출시 버전에 대한 상세 설명  ");
        assertThat(version.getVersion()).isEqualTo("v1.0.0");
        assertThat(version.getTitle()).isEqualTo("  정식 출시 버전 제목  ");
        assertThat(version.getDescription()).isEqualTo("  정식 출시 버전에 대한 상세 설명  ");
        assertThat(version.getStatus()).isEqualTo(GameVersionStatus.DRAFT);
        assertThat(version.getReleasedAt()).isNull();
    }

    @Test
    void allowValidVersions() {
        assertThat(version("v1.0.0").getVersion()).isEqualTo("v1.0.0");
        assertThat(version("v10.20.30").getVersion()).isEqualTo("v10.20.30");
    }

    @Test
    void rejectMissingAndInvalidVersions() {
        assertInvalidVersion(null, "게임 버전을 입력해주세요.");
        assertInvalidVersion("   ", "게임 버전을 입력해주세요.");
        assertInvalidVersion("v" + "1".repeat(30), "게임 버전은 30자 이하로 입력해주세요.");
        for (String value : new String[]{"1.0.0", "V1.0.0", "v1.0", "v1.0.0-beta"}) {
            assertInvalidVersion(value, "게임 버전은 vMAJOR.MINOR.PATCH 형식으로 입력해주세요.");
        }
    }

    @Test
    void validateTitleBoundariesAndMessages() {
        assertThat(create("a".repeat(10), DESCRIPTION).getTitle()).hasSize(10);
        assertThat(create("a".repeat(100), DESCRIPTION).getTitle()).hasSize(100);
        assertInvalidTitle(null, "게임 버전 제목을 입력해주세요.");
        assertInvalidTitle("   ", "게임 버전 제목을 입력해주세요.");
        assertInvalidTitle("a".repeat(9), "게임 버전 제목은 10자 이상으로 입력해주세요.");
        assertInvalidTitle("a".repeat(101), "게임 버전 제목은 100자 이하로 입력해주세요.");
    }

    @Test
    void validateDescriptionBoundariesAndMessages() {
        assertThat(create(TITLE, "a".repeat(10)).getDescription()).hasSize(10);
        assertThat(create(TITLE, "a".repeat(500)).getDescription()).hasSize(500);
        assertInvalidDescription(null, "게임 버전 설명을 입력해주세요.");
        assertInvalidDescription("   ", "게임 버전 설명을 입력해주세요.");
        assertInvalidDescription("a".repeat(9), "게임 버전 설명은 10자 이상으로 입력해주세요.");
        assertInvalidDescription("a".repeat(501), "게임 버전 설명은 500자 이하로 입력해주세요.");
    }

    @Test
    void updateDetailsPreservesOtherStateInEveryStatus() {
        for (GameVersionStatus status : GameVersionStatus.values()) {
            GameVersion version = create(TITLE, DESCRIPTION);
            LocalDateTime releasedAt = LocalDateTime.of(2026, 8, 8, 12, 0);
            ReflectionTestUtils.setField(version, "status", status);
            ReflectionTestUtils.setField(version, "releasedAt", releasedAt);
            version.updateDetails("수정된 버전 제목입니다", "수정된 버전에 대한 상세 설명입니다.");
            assertThat(version.getVersion()).isEqualTo("v1.0.0");
            assertThat(version.getStatus()).isEqualTo(status);
            assertThat(version.getReleasedAt()).isEqualTo(releasedAt);
        }
    }

    @Test
    void invalidUpdatePreservesBothValues() {
        GameVersion version = create(TITLE, DESCRIPTION);
        assertThatThrownBy(() -> version.updateDetails("수정된 버전 제목입니다", "짧은설명"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("게임 버전 설명은 10자 이상으로 입력해주세요.");
        assertThat(version.getTitle()).isEqualTo(TITLE);
        assertThat(version.getDescription()).isEqualTo(DESCRIPTION);
    }

    @Test
    void releaseDraftAndInactiveAndRefreshReleasedAt() {
        GameVersion version = create(TITLE, DESCRIPTION);
        LocalDateTime first = LocalDateTime.of(2026, 8, 8, 12, 0);
        version.release(first);
        assertThat(version.getStatus()).isEqualTo(GameVersionStatus.RELEASED);
        assertThat(version.getReleasedAt()).isEqualTo(first);
        version.deactivateForReplacement();
        LocalDateTime second = first.plusHours(1);
        version.release(second);
        assertThat(version.getReleasedAt()).isEqualTo(second);
    }

    @Test
    void rejectInvalidRelease() {
        GameVersion version = create(TITLE, DESCRIPTION);
        assertThatThrownBy(() -> version.release(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("게임 버전 공개 시각이 필요합니다.");
        version.release(LocalDateTime.now());
        assertThatThrownBy(() -> version.release(LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("DRAFT 또는 INACTIVE 상태의 게임 버전만 공개할 수 있습니다.");
    }

    @Test
    void deactivateOnlyReleasedAndKeepReleasedAt() {
        GameVersion draft = create(TITLE, DESCRIPTION);
        assertThatThrownBy(draft::deactivateForReplacement)
                .hasMessage("RELEASED 상태의 게임 버전만 비활성화할 수 있습니다.");
        ReflectionTestUtils.setField(draft, "status", GameVersionStatus.INACTIVE);
        assertThatThrownBy(draft::deactivateForReplacement)
                .hasMessage("RELEASED 상태의 게임 버전만 비활성화할 수 있습니다.");
        GameVersion released = create(TITLE, DESCRIPTION);
        LocalDateTime time = LocalDateTime.now();
        released.release(time);
        released.deactivateForReplacement();
        assertThat(released.getStatus()).isEqualTo(GameVersionStatus.INACTIVE);
        assertThat(released.getReleasedAt()).isEqualTo(time);
    }

    private static GameVersion create(String title, String description) {
        return GameVersion.createGameVersion("v1.0.0", title, description);
    }
    private static GameVersion version(String value) { return GameVersion.createGameVersion(value, TITLE, DESCRIPTION); }
    private static void assertInvalidVersion(String value, String message) { assertThatThrownBy(() -> GameVersion.createGameVersion(value, TITLE, DESCRIPTION)).isInstanceOf(IllegalArgumentException.class).hasMessage(message); }
    private static void assertInvalidTitle(String value, String message) { assertThatThrownBy(() -> create(value, DESCRIPTION)).isInstanceOf(IllegalArgumentException.class).hasMessage(message); }
    private static void assertInvalidDescription(String value, String message) { assertThatThrownBy(() -> create(TITLE, value)).isInstanceOf(IllegalArgumentException.class).hasMessage(message); }
}
