package com.jexon.gameversion.domain;

import com.jexon.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "game_version_release_control")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameVersionReleaseControl extends BaseTimeEntity {
    public static final Long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @Column(nullable = false)
    private Long releaseSequence;

    @Version
    @Column(nullable = false)
    private Long lockVersion;

    private GameVersionReleaseControl(Long id) {
        this.id = id;
        this.releaseSequence = 0L;
    }

    public static GameVersionReleaseControl create() {
        return new GameVersionReleaseControl(SINGLETON_ID);
    }

    public void advanceReleaseSequence() {
        this.releaseSequence = Math.addExact(this.releaseSequence, 1L);
    }
}
