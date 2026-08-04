package com.jexon.news.dto.request;

import com.jexon.news.domain.NewsType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NewsCreateRequest {
    @NotNull(message = "새소식 유형을 선택해주세요.")
    private NewsType type;

    @NotBlank(message = "새소식 제목을 입력해주세요.")
    @Size(max = 150, message = "새소식 제목은 150자 이하로 입력해주세요.")
    private String title;

    @NotBlank(message = "새소식 내용을 입력해주세요.")
    @Size(max = 10000, message = "새소식 내용은 10000자 이하로 입력해주세요.")
    private String content;
}
