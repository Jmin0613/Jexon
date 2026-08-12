package com.jexon.gamefile.validation;

public record SanitizedFileName(
        String sanitizedFileName,
        String extension
) {
}
