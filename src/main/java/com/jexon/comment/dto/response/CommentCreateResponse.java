package com.jexon.comment.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CommentCreateResponse {
    private Long commentId;

    public static CommentCreateResponse of(Long commentId) {
        return new CommentCreateResponse(commentId);
    }
}
