package com.jexon.news.controller;

import com.jexon.news.domain.NewsType;
import com.jexon.news.dto.response.NewsDetailResponse;
import com.jexon.news.dto.response.NewsListResponse;
import com.jexon.news.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/news")
public class NewsController {
    private final NewsService newsService;

    @GetMapping
    public ResponseEntity<Page<NewsListResponse>> getNewsList(
            @RequestParam(required = false) NewsType type,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<NewsListResponse> response = newsService.getNewsList(type, keyword, pageable);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{newsId}")
    public ResponseEntity<NewsDetailResponse> getNews(@PathVariable Long newsId) {
        NewsDetailResponse response = newsService.getNews(newsId);

        return ResponseEntity.ok(response);
    }
}
