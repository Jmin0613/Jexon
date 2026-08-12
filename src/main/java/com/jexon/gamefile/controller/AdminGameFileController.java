package com.jexon.gamefile.controller;

import com.jexon.auth.principal.CustomUserDetails;
import com.jexon.gamefile.dto.response.GameFileUploadResponse;
import com.jexon.gamefile.service.GameFileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/game-versions")
public class AdminGameFileController {
    private final GameFileUploadService gameFileUploadService;

    @PostMapping(path = "/{gameVersionId}/file", consumes = "multipart/form-data")
    public ResponseEntity<GameFileUploadResponse> upload(
            @PathVariable Long gameVersionId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestPart("file") MultipartFile file
    ) {
        GameFileUploadResponse response = gameFileUploadService.upload(
                userDetails.getMemberId(),
                gameVersionId,
                file
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
