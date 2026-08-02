package com.jexon.post.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PostCreateRequest {
    @NotBlank(message = "게시글 제목을 입력해주세요.")
    @Size(max = 100, message = "게시글 제목은 100자 이하로 입력해주세요.")
    private String title;

    @NotBlank(message = "게시글 내용을 입력해주세요.")
    @Size(max = 5000, message = "게시글 내용은 5000자 이하로 입력해주세요.")
    private String content;
}
