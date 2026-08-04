package com.jexon.news.dto.response;

import com.jexon.news.domain.News;
import com.jexon.news.domain.NewsType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NewsListResponse {
    private Long newsId;
    private NewsType type;
    private String title;
    private LocalDateTime createdAt;

    public static NewsListResponse from(News news) {
        return new NewsListResponse(
                news.getId(),
                news.getType(),
                news.getTitle(),
                news.getCreatedAt()
        );
    }
}
