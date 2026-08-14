package com.jexon.downloadhistory.domain;

import com.jexon.gamefile.domain.GameFile;
import com.jexon.gameversion.domain.GameVersion;
import com.jexon.global.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "download_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DownloadHistory extends BaseTimeEntity {
    // 다운로드 요청 1건을 기록하는 이력 엔티티

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // GameVersion 1 ─── N DownloadHistory
    // GameFile    1 ─── N DownloadHistory

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "game_version_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_download_history_game_version")
    )
    private GameVersion gameVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "game_file_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_download_history_game_file")
    )
    private GameFile gameFile;

    private DownloadHistory(GameVersion gameVersion, GameFile gameFile) {
        if (gameVersion == null) {
            throw new IllegalArgumentException("다운로드 이력의 게임 버전이 필요합니다.");
        }
        if (gameFile == null) {
            throw new IllegalArgumentException("다운로드 이력의 게임 파일이 필요합니다.");
        }

        this.gameVersion = gameVersion;
        this.gameFile = gameFile;
    }

    public static DownloadHistory createDownloadHistory(
            GameVersion gameVersion,
            GameFile gameFile
    ) {
        return new DownloadHistory(gameVersion, gameFile);
    }
}
