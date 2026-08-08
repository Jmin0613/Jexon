package com.jexon.gameversion.domain;

import com.jexon.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Getter
@Entity
@Table(
        name = "game_versions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_game_version_version",
                        columnNames = "version"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameVersion extends BaseTimeEntity {
    private static final Pattern VERSION_PATTERN = Pattern.compile("^v\\d+\\.\\d+\\.\\d+$");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 생성 후 변경 금지
    @Column(nullable = false, length = 30, updatable = false)
    private String version;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GameVersionStatus status;

    private LocalDateTime releasedAt;

    @Version
    @Column(nullable = false)
    private Long lockVersion;

    private GameVersion(String version, String title, String description) {
        validateVersion(version);
        validateTitle(title);
        validateDescription(description);

        this.version = version;
        this.title = title;
        this.description = description;
        this.status = GameVersionStatus.DRAFT;
        this.releasedAt = null;
    }

    public static GameVersion createGameVersion(
            String version,
            String title,
            String description
    ) {
        return new GameVersion(version, title, description);
    }

    public void updateDetails(String title, String description) {
        validateTitle(title);
        validateDescription(description);

        this.title = title;
        this.description = description;
    }

    public void release(LocalDateTime releasedAt) {
        validateReleasableStatus();
        validateReleasedAt(releasedAt);

        this.status = GameVersionStatus.RELEASED;
        this.releasedAt = releasedAt;
    }

    public void deactivateForReplacement() {
        validateReleasedStatus();
        this.status = GameVersionStatus.INACTIVE;
    }

    private static void validateVersion(String version) {
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("게임 버전을 입력해주세요.");
        }
        if (version.length() > 30) {
            throw new IllegalArgumentException("게임 버전은 30자 이하로 입력해주세요.");
        }
        if (!VERSION_PATTERN.matcher(version).matches()) {
            throw new IllegalArgumentException("게임 버전은 vMAJOR.MINOR.PATCH 형식으로 입력해주세요.");
        }
    }

    private static void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("게임 버전 제목을 입력해주세요.");
        }
        if (title.length() < 10) {
            throw new IllegalArgumentException("게임 버전 제목은 10자 이상으로 입력해주세요.");
        }
        if (title.length() > 100) {
            throw new IllegalArgumentException("게임 버전 제목은 100자 이하로 입력해주세요.");
        }
    }

    private static void validateDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("게임 버전 설명을 입력해주세요.");
        }
        if (description.length() < 10) {
            throw new IllegalArgumentException("게임 버전 설명은 10자 이상으로 입력해주세요.");
        }
        if (description.length() > 500) {
            throw new IllegalArgumentException("게임 버전 설명은 500자 이하로 입력해주세요.");
        }
    }

    private void validateReleasableStatus() {
        if (status != GameVersionStatus.DRAFT && status != GameVersionStatus.INACTIVE) {
            throw new IllegalArgumentException("DRAFT 또는 INACTIVE 상태의 게임 버전만 공개할 수 있습니다.");
        }
    }

    private static void validateReleasedAt(LocalDateTime releasedAt) {
        if (releasedAt == null) {
            throw new IllegalArgumentException("게임 버전 공개 시각이 필요합니다.");
        }
    }

    private void validateReleasedStatus() {
        if (status != GameVersionStatus.RELEASED) {
            throw new IllegalArgumentException("RELEASED 상태의 게임 버전만 비활성화할 수 있습니다.");
        }
    }
}
