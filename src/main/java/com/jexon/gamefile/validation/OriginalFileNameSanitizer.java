package com.jexon.gamefile.validation;

import com.jexon.gamefile.exception.InvalidGameFileException;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;

@Component
public class OriginalFileNameSanitizer {
    private static final int MAX_FILE_NAME_LENGTH = 255;

    public SanitizedFileName sanitize(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new InvalidGameFileException("원본 파일명이 필요합니다.");
        }

        String baseName = extractBaseName(originalFileName);
        String normalized = Normalizer.normalize(baseName, Normalizer.Form.NFC);

        validateBaseName(normalized);
        String extension = extractExtension(normalized);

        return new SanitizedFileName(normalized, extension.toLowerCase(Locale.ROOT));
    }

    private String extractBaseName(String originalFileName) {
        int unixSeparator = originalFileName.lastIndexOf('/');
        int windowsSeparator = originalFileName.lastIndexOf('\\');
        int lastSeparator = Math.max(unixSeparator, windowsSeparator);
        return originalFileName.substring(lastSeparator + 1);
    }

    private void validateBaseName(String baseName) {
        if (baseName.isBlank()) {
            throw new InvalidGameFileException("원본 파일명의 basename이 필요합니다.");
        }
        if (baseName.equals(".") || baseName.equals("..")) {
            throw new InvalidGameFileException("올바르지 않은 원본 파일명입니다.");
        }
        if (!baseName.equals(baseName.strip())) {
            throw new InvalidGameFileException("원본 파일명 앞뒤에는 공백을 사용할 수 없습니다.");
        }
        if (baseName.length() > MAX_FILE_NAME_LENGTH) {
            throw new InvalidGameFileException("원본 파일명은 255자 이하여야 합니다.");
        }
        if (baseName.codePoints().anyMatch(this::isControlCharacter)) {
            throw new InvalidGameFileException("원본 파일명에는 제어문자를 사용할 수 없습니다.");
        }
    }

    private String extractExtension(String baseName) {
        int lastDot = baseName.lastIndexOf('.');
        if (lastDot <= 0 || lastDot == baseName.length() - 1) {
            throw new InvalidGameFileException("원본 파일명에는 확장자가 필요합니다.");
        }

        return baseName.substring(lastDot + 1);
    }

    private boolean isControlCharacter(int codePoint) {
        return codePoint <= 0x1F || codePoint >= 0x7F && codePoint <= 0x9F;
    }
}
