package com.jexon.gamefile.validation;

import com.jexon.gamefile.exception.InvalidGameFileException;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;

@Component
public class OriginalFileNameSanitizer {
    // 파일명 가공 (파일명 정제 + 경로 제거 + 확장자 추출)

    private static final int MAX_FILE_NAME_LENGTH = 255; // 파일명 길이 제한

    public SanitizedFileName sanitize(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new InvalidGameFileException("원본 파일명이 필요합니다.");
        }

        String baseName = extractBaseName(originalFileName); // 경로 제거, 파일명 추출

        // Unicode 표현을 NFC로 통일해 동일한 파일명이 다른 문자열로 취급되는 것을 방지
        String normalized = Normalizer.normalize(baseName, Normalizer.Form.NFC);

        validateBaseName(normalized); // 정제된 파일명 적합 검증
        String extension = extractExtension(normalized); // 확장자 추출

        return new SanitizedFileName(normalized, extension.toLowerCase(Locale.ROOT)); // 파일명, 확장자 반환
    }

    // helper 메서드 -------------------------------------------------------------------------------

    // 경로 제거, 파일명 추출
    private String extractBaseName(String originalFileName) {
        int unixSeparator = originalFileName.lastIndexOf('/'); // 유닉스/리눅스/맥
        int windowsSeparator = originalFileName.lastIndexOf('\\'); // 윈도우
        // 없으면 둘 다 -1 반환

        int lastSeparator = Math.max(unixSeparator, windowsSeparator); // 마지막 경로 구분자 인덱스
        return originalFileName.substring(lastSeparator + 1); // 해당 위치부터 문자열 추출 → 파일명만 추출
    }

    // 정제된 파일명 적합 검증
    private void validateBaseName(String baseName) {
        // 파일명 비어있거나 공백 검사
        if (baseName.isBlank()) {
            throw new InvalidGameFileException("원본 파일명의 basename이 필요합니다.");
        }

        // 파일 이름 자체가 현재 디렉토리 또는 상위 디렉토리인지 검사 → ., ..은 파일이 아닌 경로 명령.
        if (baseName.equals(".") || baseName.equals("..")) {
            throw new InvalidGameFileException("올바르지 않은 원본 파일명입니다.");
        }

        // 파일 이름 맨 앞이나 맨 뒤 공백 검사
        if (!baseName.equals(baseName.strip())) {
            throw new InvalidGameFileException("원본 파일명 앞뒤에는 공백을 사용할 수 없습니다.");
        }

        // 파일명 길이 255자 검사. VARCHAR(255)
        if (baseName.length() > MAX_FILE_NAME_LENGTH) {
            throw new InvalidGameFileException("원본 파일명은 255자 이하여야 합니다.");
        }

        // 특수 제어 문자 섞여있나 검사 (\n, \t, \0 etc.)
        if (baseName.codePoints().anyMatch(this::isControlCharacter)) {
            throw new InvalidGameFileException("원본 파일명에는 제어문자를 사용할 수 없습니다.");
        }
    }

    // 확장자 추출
    private String extractExtension(String baseName) {
        int lastDot = baseName.lastIndexOf('.'); // . 인덱스

        if (lastDot <= 0 || lastDot == baseName.length() - 1) {
            throw new InvalidGameFileException("원본 파일명에는 확장자가 필요합니다.");
        }

        // . 인덱스 + 1 → 확장자 시작 위치
        return baseName.substring(lastDot + 1);
    }

    // 제어문자 범위 체크
    private boolean isControlCharacter(int codePoint) {
        return codePoint <= 0x1F || codePoint >= 0x7F && codePoint <= 0x9F;
    }
}
