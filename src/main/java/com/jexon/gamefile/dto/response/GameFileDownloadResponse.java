package com.jexon.gamefile.dto.response;

import java.io.InputStream;

public record GameFileDownloadResponse(
        String originalFileName,
        Long fileSize,
        InputStream inputStream
) {
}
