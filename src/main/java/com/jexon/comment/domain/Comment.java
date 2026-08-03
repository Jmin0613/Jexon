package com.jexon.comment.domain;

import com.jexon.global.entity.BaseTimeEntity;
import com.jexon.member.domain.Member;
import com.jexon.post.domain.Post;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "comments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "writer_id", nullable = false)
    private Member writer;

    private Comment(String content, Post post, Member writer) {
        validateContent(content);
        validatePost(post);
        validateWriter(writer);

        this.content = content;
        this.post = post;
        this.writer = writer;
    }

    public static Comment createComment(
            String content,
            Post post,
            Member writer
    ) {
        return new Comment(content, post, writer);
    }

    public void update(String content) {
        validateContent(content);
        this.content = content;
    }

    private static void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("댓글 내용을 입력해주세요.");
        }
        if (content.length() > 1000) {
            throw new IllegalArgumentException("댓글 내용은 1000자 이하로 입력해주세요.");
        }
    }

    private static void validatePost(Post post) {
        if (post == null) {
            throw new IllegalArgumentException("댓글이 작성될 게시글이 필요합니다.");
        }
    }

    private static void validateWriter(Member writer) {
        if (writer == null) {
            throw new IllegalArgumentException("댓글 작성자가 필요합니다.");
        }
    }
}
