package com.jexon.news.dto.response;

import com.jexon.news.domain.News;
import com.jexon.news.domain.NewsType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NewsDetailResponse {
    private Long newsId;
    private NewsType type;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static NewsDetailResponse from(News news) {
        return new NewsDetailResponse(
                news.getId(),
                news.getType(),
                news.getTitle(),
                news.getContent(),
                news.getCreatedAt(),
                news.getUpdatedAt()
        );
    }
}
