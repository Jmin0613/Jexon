package com.jexon.gamefile.domain;

import com.jexon.gameversion.domain.GameVersion;
import com.jexon.global.entity.BaseTimeEntity;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

@Getter
@Entity
@Table(
        name = "game_files",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_game_file_game_version_id",
                        columnNames = "game_version_id"
                ),
                @UniqueConstraint(
                        name = "uk_game_file_storage_key",
                        columnNames = "storage_key"
                )
        },
        check = {
                @CheckConstraint(
                        name = "ck_game_file_file_size_positive",
                        constraint = "file_size > 0"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameFile extends BaseTimeEntity {
    private static final Pattern SHA_256_PATTERN = Pattern.compile("^[0-9a-f]{64}$");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "game_version_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_game_file_game_version")
    )
    private GameVersion gameVersion;

    @Column(nullable = false, length = 255)
    private String originalFileName;

    @Column(nullable = false, length = 500)
    private String storageKey;

    @Column(nullable = false, length = 20)
    private String extension;

    @Column(length = 255)
    private String contentType;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false, length = 64)
    private String checksum;

    private GameFile(
            GameVersion gameVersion,
            String originalFileName,
            String storageKey,
            String extension,
            String contentType,
            Long fileSize,
            String checksum
    ) {
        validateGameVersion(gameVersion);
        validateOriginalFileName(originalFileName);
        validateStorageKey(storageKey);
        validateExtension(extension);
        validateContentType(contentType);
        validateFileSize(fileSize);
        validateChecksum(checksum);

        this.gameVersion = gameVersion;
        this.originalFileName = originalFileName;
        this.storageKey = storageKey;
        this.extension = extension;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.checksum = checksum;
    }

    public static GameFile createGameFile(
            GameVersion gameVersion,
            String originalFileName,
            String storageKey,
            String extension,
            String contentType,
            Long fileSize,
            String checksum
    ) {
        return new GameFile(
                gameVersion,
                originalFileName,
                storageKey,
                extension,
                contentType,
                fileSize,
                checksum
        );
    }

    private static void validateGameVersion(GameVersion gameVersion) {
        if (gameVersion == null) {
            throw new IllegalArgumentException("게임 파일에 연결할 게임 버전이 필요합니다.");
        }
    }

    private static void validateOriginalFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new IllegalArgumentException("원본 파일명이 필요합니다.");
        }
        if (originalFileName.length() > 255) {
            throw new IllegalArgumentException("원본 파일명은 255자 이하로 입력해주세요.");
        }
    }

    private static void validateStorageKey(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("파일 저장 키가 필요합니다.");
        }
        if (storageKey.length() > 500) {
            throw new IllegalArgumentException("파일 저장 키는 500자 이하로 입력해주세요.");
        }
    }

    private static void validateExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            throw new IllegalArgumentException("파일 확장자가 필요합니다.");
        }
        if (extension.length() > 20) {
            throw new IllegalArgumentException("파일 확장자는 20자 이하로 입력해주세요.");
        }
    }

    private static void validateContentType(String contentType) {
        if (contentType == null) {
            return;
        }
        if (contentType.isBlank()) {
            throw new IllegalArgumentException("파일 MIME Type은 빈 값일 수 없습니다.");
        }
        if (contentType.length() > 255) {
            throw new IllegalArgumentException("파일 MIME Type은 255자 이하로 입력해주세요.");
        }
    }

    private static void validateFileSize(Long fileSize) {
        if (fileSize == null) {
            throw new IllegalArgumentException("파일 크기가 필요합니다.");
        }
        if (fileSize <= 0) {
            throw new IllegalArgumentException("파일 크기는 0보다 커야 합니다.");
        }
    }

    private static void validateChecksum(String checksum) {
        if (checksum == null || checksum.isBlank()) {
            throw new IllegalArgumentException("파일 체크섬이 필요합니다.");
        }
        if (!SHA_256_PATTERN.matcher(checksum).matches()) {
            throw new IllegalArgumentException("파일 체크섬은 64자리 lowercase hexadecimal 형식이어야 합니다.");
        }
    }
}
