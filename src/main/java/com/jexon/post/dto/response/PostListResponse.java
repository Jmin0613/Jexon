package com.jexon.post.dto.response;

import com.jexon.post.domain.Post;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PostListResponse {
    private Long postId;
    private String title;
    private Long writerId;
    private String writerNickname;
    private LocalDateTime createdAt;

    public static PostListResponse from(Post post) {
        return new PostListResponse(
                post.getId(),
                post.getTitle(),
                post.getWriter().getId(),
                post.getWriter().getNickname(),
                post.getCreatedAt()
        );
    }
}
