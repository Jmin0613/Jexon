package com.jexon.gameversion.controller;

import com.jexon.gamefile.dto.response.GameFileDownloadResponse;
import com.jexon.gamefile.service.GameFileDownloadService;
import com.jexon.gameversion.dto.response.LatestGameVersionResponse;
import com.jexon.gameversion.service.GameVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/game-versions")
public class GameVersionController {
    private final GameVersionService gameVersionService;
    private final GameFileDownloadService gameFileDownloadService;

    // 최신 버전 조회
    @GetMapping("/latest")
    public ResponseEntity<LatestGameVersionResponse> getLatestGameVersion() {
        LatestGameVersionResponse response = gameVersionService.getLatestGameVersion();

        return ResponseEntity.ok(response);
    }

    // 최신 버전 다운로드
    @GetMapping("/latest/download")
    public ResponseEntity<InputStreamResource> downloadLatestGameVersion() {
        GameFileDownloadResponse response = gameFileDownloadService.downloadLatest();
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(response.originalFileName(), StandardCharsets.UTF_8) // 내부 UUID 파일명이 아니라 원본 파일명
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(response.fileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(new InputStreamResource(response.inputStream()));
    }
}
