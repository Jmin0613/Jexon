package com.jexon.post.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PostCreateResponse {
    private Long postId;

    public static PostCreateResponse of(Long postId) {
        return new PostCreateResponse(postId);
    }
}
