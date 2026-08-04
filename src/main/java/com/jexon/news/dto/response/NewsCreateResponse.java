package com.jexon.news.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NewsCreateResponse {
    private Long newsId;

    public static NewsCreateResponse of(Long newsId) {
        return new NewsCreateResponse(newsId);
    }
}
