package com.jexon.post.dto.response;

import com.jexon.post.domain.Post;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PostDetailResponse {
    private Long postId;
    private String title;
    private String content;
    private Long writerId;
    private String writerNickname;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PostDetailResponse from(Post post) {
        return new PostDetailResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getWriter().getId(),
                post.getWriter().getNickname(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
