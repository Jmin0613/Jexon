package com.jexon.gameversion.controller;

import com.jexon.auth.principal.CustomUserDetails;
import com.jexon.gameversion.domain.GameVersionStatus;
import com.jexon.gameversion.dto.request.GameVersionCreateRequest;
import com.jexon.gameversion.dto.request.GameVersionUpdateRequest;
import com.jexon.gameversion.dto.response.GameVersionCreateResponse;
import com.jexon.gameversion.dto.response.GameVersionDetailResponse;
import com.jexon.gameversion.dto.response.GameVersionListResponse;
import com.jexon.gameversion.dto.response.GameVersionReleaseResponse;
import com.jexon.gameversion.service.GameVersionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/game-versions")
public class AdminGameVersionController {
    private final GameVersionService gameVersionService;

    @PostMapping
    public ResponseEntity<GameVersionCreateResponse> create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody GameVersionCreateRequest request
    ) {
        GameVersionCreateResponse response = gameVersionService.create(
                userDetails.getMemberId(),
                request
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<GameVersionListResponse>> getGameVersions(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) GameVersionStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<GameVersionListResponse> response = gameVersionService.getAdminGameVersions(
                userDetails.getMemberId(),
                status,
                pageable
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{gameVersionId}")
    public ResponseEntity<GameVersionDetailResponse> getGameVersion(
            @PathVariable Long gameVersionId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        GameVersionDetailResponse response = gameVersionService.getAdminGameVersion(
                userDetails.getMemberId(),
                gameVersionId
        );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{gameVersionId}")
    public ResponseEntity<GameVersionDetailResponse> update(
            @PathVariable Long gameVersionId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody GameVersionUpdateRequest request
    ) {
        GameVersionDetailResponse response = gameVersionService.update(
                userDetails.getMemberId(),
                gameVersionId,
                request
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{gameVersionId}/release")
    public ResponseEntity<GameVersionReleaseResponse> release(
            @PathVariable Long gameVersionId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        GameVersionReleaseResponse response = gameVersionService.release(
                userDetails.getMemberId(),
                gameVersionId
        );

        return ResponseEntity.ok(response);
    }
}
