package com.jexon.news.controller;

import com.jexon.auth.principal.CustomUserDetails;
import com.jexon.news.dto.request.NewsCreateRequest;
import com.jexon.news.dto.request.NewsUpdateRequest;
import com.jexon.news.dto.response.NewsCreateResponse;
import com.jexon.news.dto.response.NewsDetailResponse;
import com.jexon.news.service.NewsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/news")
public class AdminNewsController {
    private final NewsService newsService;

    @PostMapping
    public ResponseEntity<NewsCreateResponse> create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody NewsCreateRequest request
    ) {
        NewsCreateResponse response = newsService.create(
                userDetails.getMemberId(),
                request
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{newsId}")
    public ResponseEntity<NewsDetailResponse> update(
            @PathVariable Long newsId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody NewsUpdateRequest request
    ) {
        NewsDetailResponse response = newsService.update(
                newsId,
                userDetails.getMemberId(),
                request
        );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{newsId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long newsId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        newsService.delete(newsId, userDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}
