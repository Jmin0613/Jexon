package com.jexon.gamefile.validation;

public record SanitizedFileName(
        // 정제된 파일명, 확장자 저장
        String sanitizedFileName,
        String extension
) {
}
